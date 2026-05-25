package net.emutils.client.skyblock.auction;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.skyblock.SkyblockContext;
import net.emutils.client.skyblock.SkyblockPriceExecutor;
import net.emutils.client.skyblock.SkyblockPriceNeeds;
import net.emutils.client.skyblock.bazaar.SkyblockItemIds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuctionPriceFetcher {
	private static final Logger LOGGER = LoggerFactory.getLogger("emutils.auction");
	private static final String LOWEST_BIN_URL = "https://lb.tricked.dev/lowestbins.json?type=auction&price=available";
	private static final String AVERAGE_24H_URL = "https://lb.tricked.dev/averages/1day.json?type=auction";
	private static final long FETCH_INTERVAL_MS = 90_000L;
	private static final Gson GSON = new Gson();

	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	private volatile Map<String, Double> lowestBins = Map.of();
	private volatile Map<String, Double> averages24h = Map.of();
	private long lastFetchAttemptMs;
	private long fetchStartedMs;
	private boolean fetching;

	public int lowestBinCount() {
		return lowestBins.size();
	}

	public Optional<AuctionProductPrice> price(String itemId) {
		Double lowestBin = lowestBins.get(itemId);
		Double average24h = averages24h.get(itemId);
		if (lowestBin == null && average24h == null) {
			return Optional.empty();
		}

		return Optional.of(new AuctionProductPrice(
			lowestBin != null ? lowestBin : 0.0D,
			average24h != null ? average24h : 0.0D
		));
	}

	public Optional<AuctionProductPrice> price(ItemStack stack) {
		for (String itemId : SkyblockItemIds.auctionLookupIds(stack)) {
			Optional<AuctionProductPrice> resolved = price(itemId);
			if (resolved.isPresent() && resolved.get().hasAny()) {
				return resolved;
			}
		}

		return Optional.empty();
	}

	public void tick(@Nullable MinecraftClient client) {
		recoverStuckFetch();
		if (!shouldRun(client)) {
			return;
		}

		long now = System.currentTimeMillis();
		if (fetching || now - lastFetchAttemptMs < FETCH_INTERVAL_MS) {
			return;
		}

		startFetch();
	}

	public void fetchNow() {
		recoverStuckFetch();
		if (fetching) {
			return;
		}

		lastFetchAttemptMs = 0L;
		startFetch();
	}

	public void requestImmediateFetch() {
		lastFetchAttemptMs = 0L;
	}

	public void clear() {
		lowestBins = Map.of();
		averages24h = Map.of();
		lastFetchAttemptMs = 0L;
		fetchStartedMs = 0L;
		fetching = false;
	}

	private void recoverStuckFetch() {
		if (!fetching || fetchStartedMs <= 0L) {
			return;
		}

		if (System.currentTimeMillis() - fetchStartedMs > 45_000L) {
			LOGGER.warn("Auction price fetch timed out; allowing retry.");
			fetching = false;
			fetchStartedMs = 0L;
		}
	}

	private boolean shouldRun(@Nullable MinecraftClient client) {
		if (!SkyblockPriceNeeds.anyEnabled(EMUtilsClient.config())) {
			return false;
		}

		return SkyblockContext.onHypixel(client);
	}

	private void startFetch() {
		lastFetchAttemptMs = System.currentTimeMillis();
		fetchStartedMs = lastFetchAttemptMs;
		fetching = true;
		CompletableFuture.runAsync(this::fetchAll, SkyblockPriceExecutor.EXECUTOR)
			.whenComplete((ignored, throwable) -> {
				fetching = false;
				fetchStartedMs = 0L;
				if (throwable != null) {
					LOGGER.warn("Auction price fetch failed.", throwable);
				}
			});
	}

	private void fetchAll() {
		CompletableFuture<Map<String, Double>> binsFuture = CompletableFuture.supplyAsync(
			() -> fetchJsonMap(LOWEST_BIN_URL),
			SkyblockPriceExecutor.EXECUTOR
		);
		CompletableFuture<Map<String, Double>> averagesFuture = CompletableFuture.supplyAsync(
			() -> fetchJsonMap(AVERAGE_24H_URL),
			SkyblockPriceExecutor.EXECUTOR
		);

		Map<String, Double> bins = binsFuture.join();
		Map<String, Double> averages = averagesFuture.join();
		if (!bins.isEmpty()) {
			lowestBins = bins;
		}
		if (!averages.isEmpty()) {
			averages24h = averages;
		}

		if (bins.isEmpty() && averages.isEmpty()) {
			LOGGER.warn("Auction price APIs returned no data.");
			lastFetchAttemptMs = System.currentTimeMillis() - FETCH_INTERVAL_MS + 5_000L;
			return;
		}

		LOGGER.info("Loaded {} auction lowest BIN entries and {} 24h averages.", bins.size(), averages.size());
	}

	private Map<String, Double> fetchJsonMap(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(30))
				.header("Accept", "application/json")
				.header("User-Agent", "mwsk75996/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)")
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				LOGGER.warn("Auction price request failed with HTTP {} for {}.", response.statusCode(), url);
				return Map.of();
			}

			return parsePriceMap(response.body());
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			LOGGER.warn("Auction price fetch failed for {}.", url, exception);
			return Map.of();
		}
	}

	private static Map<String, Double> parsePriceMap(String body) {
		JsonObject root;
		try {
			root = GSON.fromJson(body, JsonObject.class);
		} catch (JsonParseException exception) {
			LOGGER.warn("Auction price API returned malformed JSON.", exception);
			return Map.of();
		}

		if (root == null || root.isEmpty()) {
			return Map.of();
		}

		Map<String, Double> result = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			if (!entry.getValue().isJsonPrimitive()) {
				continue;
			}

			try {
				double value = entry.getValue().getAsDouble();
				if (value > 0.0D) {
					result.put(entry.getKey(), value);
				}
			} catch (NumberFormatException | UnsupportedOperationException ignored) {
			}
		}

		return Map.copyOf(result);
	}
}
