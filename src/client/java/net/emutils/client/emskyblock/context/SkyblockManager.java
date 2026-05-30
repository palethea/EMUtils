package net.emutils.client.emskyblock.context;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.emskyblock.api.SkyblockApiContext;
import net.emutils.client.emskyblock.api.modapi.SkyblockModApiLocationData;
import net.emutils.client.emskyblock.sacks.SkyblockSackTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class SkyblockManager {
	private static final Pattern PROFILE_CHANGED = Pattern.compile(
		"your profile was changed to:\\s*(.+)",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern PLAYING_PROFILE = Pattern.compile(
		"you are playing on profile:\\s*(.+)",
		Pattern.CASE_INSENSITIVE
	);
	private static final int UPDATE_INTERVAL = 10;
	private static final int LOCRAW_INTERVAL_TICKS = 20 * 15;

	private final SkyblockEvents events = new SkyblockEvents();
	private SkyblockSnapshot snapshot = SkyblockSnapshot.empty();
	@Nullable
	private Text tabHeader;
	@Nullable
	private Text tabFooter;
	private int updateCooldown;
	private int locrawCooldown;
	private boolean requestedLocraw;

	public SkyblockEvents events() {
		return events;
	}

	public SkyblockSnapshot snapshot() {
		return snapshot;
	}

	public void onWorldJoin(MinecraftClient client) {
		reset();
		refresh(client, true);
	}

	public void onWorldLeave(MinecraftClient client) {
		if (snapshot.onHypixel()) {
			events.post(new SkyblockEvent.HypixelLeave());
		}

		reset();
	}

	public void tick(MinecraftClient client) {
		if (client.world == null || client.getNetworkHandler() == null) {
			return;
		}

		if (updateCooldown > 0) {
			updateCooldown--;
		} else {
			updateCooldown = UPDATE_INTERVAL;
			refresh(client, false);
		}

		maybeRequestLocraw(client);
	}

	public void onTabListUpdated(MinecraftClient client) {
		updateCooldown = 0;
		refresh(client, false);
	}

	public void onTabListHeader(@Nullable Text header, @Nullable Text footer) {
		tabHeader = header;
		tabFooter = footer;
	}

	public boolean onChatMessage(Text message) {
		String text = SkyblockTextUtils.strip(message);
		if (text.isEmpty()) {
			return false;
		}

		SkyblockLocrawData locraw = SkyblockLocrawData.tryParseMessage(text);
		if (locraw != null) {
			parseLocraw(locraw);
			return true;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return false;
		}

		Matcher changed = PROFILE_CHANGED.matcher(text);
		if (changed.find()) {
			applyProfileHint(SkyblockTextUtils.normalizeProfile(changed.group(1)), client);
			return false;
		}

		Matcher playing = PLAYING_PROFILE.matcher(text);
		if (playing.find()) {
			applyProfileHint(SkyblockTextUtils.normalizeProfile(playing.group(1)), client);
		}

		SkyblockSackTracker.onChat(message);
		net.emutils.client.emskyblock.features.fishing.common.FishingChatHandler.onChat(message);
		return false;
	}

	private void parseLocraw(SkyblockLocrawData locraw) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			refresh(client, false, null, locraw);
		}
	}

	private void applyProfileHint(String profileName, MinecraftClient client) {
		if (profileName.isBlank()) {
			return;
		}

		refresh(client, false, profileName);
	}

	private void maybeRequestLocraw(MinecraftClient client) {
		SkyblockModApiLocationData modLocation = SkyblockApiContext.location();
		if (!isHypixel(client)
			|| snapshot.inSkyBlock()
			|| snapshot.locraw().hasData()
			|| modLocation.fresh()) {
			return;
		}

		if (locrawCooldown > 0) {
			locrawCooldown--;
			return;
		}

		locrawCooldown = LOCRAW_INTERVAL_TICKS;
		if (requestedLocraw || client.player == null) {
			return;
		}

		client.player.networkHandler.sendChatCommand("locraw");
		requestedLocraw = true;
	}

	private void refresh(MinecraftClient client, boolean force) {
		refresh(client, force, null, null);
	}

	private void refresh(MinecraftClient client, boolean force, @Nullable String profileHint) {
		refresh(client, force, profileHint, null);
	}

	private void refresh(MinecraftClient client, boolean force, @Nullable String profileHint, @Nullable SkyblockLocrawData locrawHint) {
		SkyblockSnapshot previous = snapshot;
		SkyblockModApiLocationData modLocation = SkyblockApiContext.location();
		boolean onHypixel = isHypixel(client) || (modLocation.fresh() && modLocation.onHypixel());
		boolean onAlpha = isAlphaHypixel(client) || (modLocation.fresh() && modLocation.onAlpha());

		SkyblockScoreboardReader.ParsedScoreboard scoreboard = onHypixel
			? SkyblockScoreboardReader.read(client)
			: SkyblockScoreboardReader.ParsedScoreboard.empty();
		SkyblockTabListReader.ParsedTabList tabList = onHypixel
			? SkyblockTabListReader.read(client, tabHeader, tabFooter)
			: SkyblockTabListReader.ParsedTabList.empty();

		String profileName = firstNonBlank(profileHint, tabList.profileName());
		SkyblockProfileModes profileModes = scoreboard.profileModes();
		SkyblockLocrawData locraw = locrawHint != null ? locrawHint : previous.locraw();

		boolean inSkyBlock = onHypixel && (scoreboard.showsSkyBlock() || locraw.inSkyBlock() || modLocation.inSkyBlock());

		String serverId = firstNonBlank(tabList.serverId(), scoreboard.serverId(), locraw.server(), modLocation.serverName());
		String area = scoreboard.area();
		SkyblockIsland island = inSkyBlock ? resolveIsland(scoreboard, tabList, locraw, modLocation) : SkyblockIsland.NONE;

		SkyblockSnapshot next = new SkyblockSnapshot(
			onHypixel,
			onAlpha,
			inSkyBlock,
			locraw.inLobby(),
			locraw.inLimbo(),
			blankToNull(profileName),
			profileModes,
			island,
			blankToNull(scoreboard.title()),
			blankToNull(area),
			blankToNull(scoreboard.areaWithSymbol()),
			blankToNull(serverId),
			scoreboard.purse(),
			scoreboard.piggyBank(),
			blankToNull(tabList.bankBalance()),
			tabList.playerCount(),
			scoreboard.islandVisitorsCurrent(),
			scoreboard.islandVisitorsMax(),
			locraw,
			System.currentTimeMillis()
		);

		if (!force && next.equals(previous)) {
			return;
		}

		snapshot = next;
		postTransitionEvents(previous, next);
	}

	private SkyblockIsland resolveIsland(
		SkyblockScoreboardReader.ParsedScoreboard scoreboard,
		SkyblockTabListReader.ParsedTabList tabList,
		SkyblockLocrawData locraw,
		SkyblockModApiLocationData modLocation
	) {
		SkyblockIsland island = SkyblockIsland.fromTabName(tabList.islandName());
		if (island == SkyblockIsland.UNKNOWN || island == SkyblockIsland.NONE) {
			island = SkyblockIsland.fromLocrawMode(locraw.mode());
		}
		if (island == SkyblockIsland.UNKNOWN || island == SkyblockIsland.NONE) {
			island = SkyblockIsland.fromLocrawMode(modLocation.mode());
		}
		if (island == SkyblockIsland.UNKNOWN || island == SkyblockIsland.NONE) {
			island = SkyblockIsland.fromTabName(locraw.map());
		}
		if (island == SkyblockIsland.UNKNOWN || island == SkyblockIsland.NONE) {
			island = SkyblockIsland.fromTabName(modLocation.map());
		}

		boolean guest = scoreboard.profileModes().guest()
			|| (scoreboard.title() != null && scoreboard.title().toUpperCase(Locale.ROOT).contains("GUEST"));
		if (guest) {
			if (island == SkyblockIsland.PRIVATE_ISLAND) {
				return SkyblockIsland.PRIVATE_ISLAND_GUEST;
			}
			if (island == SkyblockIsland.GARDEN) {
				return SkyblockIsland.GARDEN_GUEST;
			}
		}

		return island;
	}

	private void postTransitionEvents(SkyblockSnapshot previous, SkyblockSnapshot next) {
		if (next.onHypixel() && !previous.onHypixel()) {
			events.post(new SkyblockEvent.HypixelJoin(next.onAlpha()));
		}

		if (!Objects.equals(previous.profileName(), next.profileName()) && next.hasProfile()) {
			events.post(new SkyblockEvent.ProfileJoin(next.profileName(), previous.profileName()));
		}

		if (previous.inSkyBlock() && !next.inSkyBlock() && previous.island() != SkyblockIsland.NONE) {
			events.post(new SkyblockEvent.IslandLeave(previous.island()));
		}

		if (next.inSkyBlock() && !previous.inSkyBlock() && next.island() != SkyblockIsland.NONE) {
			events.post(new SkyblockEvent.IslandJoin(next.island(), previous.island()));
		} else if (next.inSkyBlock()
			&& previous.inSkyBlock()
			&& previous.island() != SkyblockIsland.NONE
			&& next.island() != SkyblockIsland.NONE
			&& previous.island() != next.island()) {
			events.post(new SkyblockEvent.IslandLeave(previous.island()));
			events.post(new SkyblockEvent.IslandJoin(next.island(), previous.island()));
		}

		if (!Objects.equals(previous.area(), next.area())) {
			events.post(new SkyblockEvent.AreaChange(next.area(), previous.area()));
		}

		events.post(new SkyblockEvent.SnapshotUpdate(next, previous));
	}

	private void reset() {
		snapshot = SkyblockSnapshot.empty();
		tabHeader = null;
		tabFooter = null;
		updateCooldown = 0;
		locrawCooldown = 0;
		requestedLocraw = false;
	}

	public static boolean isHypixel(MinecraftClient client) {
		ServerInfo serverInfo = client.getCurrentServerEntry();
		if (serverInfo == null || serverInfo.address == null) {
			return false;
		}

		return serverInfo.address.toLowerCase(Locale.ROOT).contains("hypixel.net");
	}

	public static boolean isAlphaHypixel(MinecraftClient client) {
		ServerInfo serverInfo = client.getCurrentServerEntry();
		if (serverInfo == null || serverInfo.address == null) {
			return false;
		}

		return serverInfo.address.toLowerCase(Locale.ROOT).contains("alpha.hypixel.net");
	}

	@Nullable
	private static String firstNonBlank(
		@Nullable String primary,
		@Nullable String secondary,
		@Nullable String tertiary,
		@Nullable String quaternary
	) {
		String value = firstNonBlank(primary, secondary, tertiary);
		return value != null ? value : blankToNull(quaternary);
	}

	@Nullable
	private static String firstNonBlank(@Nullable String primary, @Nullable String secondary, @Nullable String tertiary) {
		String value = firstNonBlank(primary, secondary);
		return value != null ? value : blankToNull(tertiary);
	}

	@Nullable
	private static String firstNonBlank(@Nullable String primary, @Nullable String secondary) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}

		return blankToNull(secondary);
	}

	@Nullable
	private static String blankToNull(@Nullable String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value;
	}
}
