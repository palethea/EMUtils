package net.emutils.client.emskyblock.api;

import java.util.List;
import net.emutils.client.emskyblock.api.core.SkyblockApiStatus;
import net.emutils.client.emskyblock.api.core.SkyblockHttpClient;
import net.emutils.client.emskyblock.api.fetcher.EliteLowestBinFetcher;
import net.emutils.client.emskyblock.api.fetcher.SkyblockRawJsonFetcher;
import net.emutils.client.emskyblock.api.modapi.HypixelModApiBridge;
import net.emutils.client.emskyblock.api.modapi.SkyblockModApiLocationData;
import net.emutils.client.emskyblock.pricing.auction.AuctionPriceFetcher;
import net.emutils.client.emskyblock.pricing.bazaar.BazaarPriceFetcher;
import net.emutils.client.emskyblock.pricing.npc.NpcPriceFetcher;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class SkyblockApiManager {
	private static final long ELECTION_INTERVAL_MS = 60L * 60L * 1000L;
	private static final long BINGO_INTERVAL_MS = 60L * 60L * 1000L;
	private static final long MINING_EVENTS_INTERVAL_MS = 60L * 1000L;

	private final SkyblockHttpClient httpClient = new SkyblockHttpClient();
	private final BazaarPriceFetcher bazaar = new BazaarPriceFetcher();
	private final AuctionPriceFetcher auction = new AuctionPriceFetcher();
	private final NpcPriceFetcher items = new NpcPriceFetcher();
	private final EliteLowestBinFetcher eliteLowestBins = new EliteLowestBinFetcher(httpClient);
	private final SkyblockRawJsonFetcher election = new SkyblockRawJsonFetcher(
		httpClient,
		"Hypixel Election",
		"https://api.hypixel.net/v2/resources/skyblock/election",
		ELECTION_INTERVAL_MS
	);
	private final SkyblockRawJsonFetcher bingo = new SkyblockRawJsonFetcher(
		httpClient,
		"Hypixel Bingo",
		"https://api.hypixel.net/v2/resources/skyblock/bingo",
		BINGO_INTERVAL_MS
	);
	private final SkyblockRawJsonFetcher miningEvents = new SkyblockRawJsonFetcher(
		httpClient,
		"Soopy Mining Events",
		"https://api.soopy.dev/skyblock/chevents/get",
		MINING_EVENTS_INTERVAL_MS
	);
	private volatile SkyblockModApiLocationData modApiLocation = SkyblockModApiLocationData.EMPTY;
	private volatile boolean modApiAvailable;

	public SkyblockApiManager() {
		HypixelModApiBridge.tryRegister(this);
	}

	public void tick(@Nullable MinecraftClient client) {
		bazaar.tick(client);
		auction.tick(client);
		items.tick(client);
		eliteLowestBins.tick(client);
		election.tick(client);
		bingo.tick(client);
		miningEvents.tick(client);
	}

	public void fetchNow(@Nullable MinecraftClient client) {
		bazaar.fetchNow(client);
		auction.fetchNow(client);
		items.fetchNow(client);
		eliteLowestBins.fetchNow(client);
		election.fetchNow(client);
		bingo.fetchNow(client);
		miningEvents.fetchNow(client);
	}

	public void requestImmediateFetch() {
		bazaar.requestImmediateFetch();
		auction.requestImmediateFetch();
		items.requestImmediateFetch();
		eliteLowestBins.requestImmediateFetch();
		election.requestImmediateFetch();
		bingo.requestImmediateFetch();
		miningEvents.requestImmediateFetch();
	}

	public void clear() {
		bazaar.clear();
		auction.clear();
		items.clear();
		eliteLowestBins.clear();
		election.clear();
		bingo.clear();
		miningEvents.clear();
		modApiLocation = SkyblockModApiLocationData.EMPTY;
	}

	public BazaarPriceFetcher bazaar() {
		return bazaar;
	}

	public AuctionPriceFetcher auction() {
		return auction;
	}

	public NpcPriceFetcher items() {
		return items;
	}

	public NpcPriceFetcher npc() {
		return items;
	}

	public EliteLowestBinFetcher eliteLowestBins() {
		return eliteLowestBins;
	}

	public SkyblockRawJsonFetcher election() {
		return election;
	}

	public SkyblockRawJsonFetcher bingo() {
		return bingo;
	}

	public SkyblockRawJsonFetcher miningEvents() {
		return miningEvents;
	}

	public SkyblockModApiLocationData modApiLocation() {
		return modApiLocation;
	}

	public boolean modApiAvailable() {
		return modApiAvailable;
	}

	public void markModApiAvailable() {
		modApiAvailable = true;
	}

	public void onModApiHello(boolean alpha) {
		modApiAvailable = true;
		SkyblockModApiLocationData previous = modApiLocation;
		modApiLocation = new SkyblockModApiLocationData(
			true,
			alpha,
			previous.serverName(),
			previous.serverType(),
			previous.lobbyName(),
			previous.mode(),
			previous.map(),
			System.currentTimeMillis()
		);
	}

	public void onModApiLocation(
		@Nullable String serverName,
		@Nullable String serverType,
		@Nullable String lobbyName,
		@Nullable String mode,
		@Nullable String map
	) {
		modApiAvailable = true;
		SkyblockModApiLocationData previous = modApiLocation;
		modApiLocation = new SkyblockModApiLocationData(
			true,
			previous.onAlpha(),
			blankToNull(serverName),
			blankToNull(serverType),
			blankToNull(lobbyName),
			blankToNull(mode),
			blankToNull(map),
			System.currentTimeMillis()
		);
	}

	public List<SkyblockApiStatus> statuses() {
		return List.of(
			eliteLowestBins.status(),
			election.status(),
			bingo.status(),
			miningEvents.status()
		);
	}

	@Nullable
	private static String blankToNull(@Nullable String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
