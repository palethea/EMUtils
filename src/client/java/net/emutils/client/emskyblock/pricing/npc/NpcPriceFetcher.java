package net.emutils.client.emskyblock.pricing.npc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.api.core.SkyblockPriceFetchTask;
import net.emutils.client.emskyblock.pricing.SkyblockPriceNeeds;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NpcPriceFetcher {
	private static final Logger LOGGER = LoggerFactory.getLogger("emutils.npc");
	private static final String API_URL = "https://api.hypixel.net/v2/resources/skyblock/items";
	private static final long FETCH_INTERVAL_MS = 86_400_000L;
	private static final Gson GSON = new Gson();

	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();
	private final SkyblockPriceFetchTask<ItemResourceData> fetchTask = new SkyblockPriceFetchTask<>(
		LOGGER,
		"Hypixel Items",
		FETCH_INTERVAL_MS,
		NpcPriceFetcher::shouldRun,
		this::fetch,
		this::publish
	);

	private volatile Map<String, Double> npcSellPrices = Map.of();
	private volatile Map<String, Double> motesPrices = Map.of();
	private volatile Map<String, Map<String, Integer>> baseStats = Map.of();

	public Optional<Double> npcSellPrice(String itemId) {
		return Optional.ofNullable(npcSellPrices.get(itemId));
	}

	public Optional<Double> motesPrice(String itemId) {
		return Optional.ofNullable(motesPrices.get(itemId));
	}

	public Map<String, Integer> baseStats(String itemId) {
		return baseStats.getOrDefault(itemId, Map.of());
	}

	public int itemCount() {
		return Math.max(npcSellPrices.size(), Math.max(motesPrices.size(), baseStats.size()));
	}

	public void tick(@Nullable MinecraftClient client) {
		fetchTask.tick(client);
	}

	private static boolean shouldRun(@Nullable MinecraftClient client) {
		if (!SkyblockPriceNeeds.anyEnabled()) {
			return false;
		}

		return SkyblockContext.onHypixel(client);
	}

	public void requestImmediateFetch() {
		fetchTask.requestImmediateFetch();
	}

	public void fetchNow(@Nullable MinecraftClient client) {
		fetchTask.fetchNow(client);
	}

	public void clear() {
		npcSellPrices = Map.of();
		motesPrices = Map.of();
		baseStats = Map.of();
		fetchTask.clear();
	}

	private boolean publish(ItemResourceData parsed) {
		if (parsed.isEmpty()) {
			return false;
		}

		npcSellPrices = parsed.npcSellPrices();
		motesPrices = parsed.motesPrices();
		baseStats = parsed.baseStats();
		LOGGER.info(
			"Loaded Hypixel item resources: {} NPC prices, {} motes prices, {} base stat entries.",
			npcSellPrices.size(),
			motesPrices.size(),
			baseStats.size()
		);
		return true;
	}

	private ItemResourceData fetch() {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
				.timeout(Duration.ofSeconds(45))
				.header("Accept", "application/json")
				.header("User-Agent", "mwsk75996/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)")
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				LOGGER.warn("Hypixel items request failed with HTTP {}.", response.statusCode());
				return ItemResourceData.empty();
			}

			return parseResponse(response.body());
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			LOGGER.debug("Hypixel items fetch failed.", exception);
			return ItemResourceData.empty();
		}
	}

	private static ItemResourceData parseResponse(String body) {
		JsonObject root;
		try {
			root = GSON.fromJson(body, JsonObject.class);
		} catch (JsonParseException exception) {
			LOGGER.warn("Hypixel items API returned malformed JSON.", exception);
			return ItemResourceData.empty();
		}

		if (root == null) {
			return ItemResourceData.empty();
		}

		JsonElement success = root.get("success");
		if (success == null || !success.getAsBoolean()) {
			return ItemResourceData.empty();
		}

		JsonArray items = root.getAsJsonArray("items");
		if (items == null) {
			return ItemResourceData.empty();
		}

		Map<String, Double> npcPrices = new HashMap<>();
		Map<String, Double> motesPrices = new HashMap<>();
		Map<String, Map<String, Integer>> baseStats = new HashMap<>();
		for (JsonElement element : items) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject item = element.getAsJsonObject();
			if (!item.has("id")) {
				continue;
			}

			String id = item.get("id").getAsString();
			if (id.isEmpty()) {
				continue;
			}

			putPositiveDouble(item, "npc_sell_price", id, npcPrices);
			putPositiveDouble(item, "motes_sell_price", id, motesPrices);
			putPositiveDouble(item, "motes_price", id, motesPrices);

			JsonObject stats = item.getAsJsonObject("stats");
			if (stats != null && !stats.isEmpty()) {
				Map<String, Integer> parsedStats = new HashMap<>();
				for (Map.Entry<String, JsonElement> stat : stats.entrySet()) {
					try {
						parsedStats.put(stat.getKey(), stat.getValue().getAsInt());
					} catch (NumberFormatException | UnsupportedOperationException ignored) {
					}
				}
				if (!parsedStats.isEmpty()) {
					baseStats.put(id, Map.copyOf(parsedStats));
				}
			}
		}

		return new ItemResourceData(Map.copyOf(npcPrices), Map.copyOf(motesPrices), Map.copyOf(baseStats));
	}

	private static void putPositiveDouble(JsonObject item, String key, String id, Map<String, Double> output) {
		if (!item.has(key)) {
			return;
		}

		try {
			double value = item.get(key).getAsDouble();
			if (value > 0.0D) {
				output.put(id, value);
			}
		} catch (NumberFormatException | UnsupportedOperationException ignored) {
		}
	}

	public record ItemResourceData(
		Map<String, Double> npcSellPrices,
		Map<String, Double> motesPrices,
		Map<String, Map<String, Integer>> baseStats
	) {
		public static ItemResourceData empty() {
			return new ItemResourceData(Map.of(), Map.of(), Map.of());
		}

		public boolean isEmpty() {
			return npcSellPrices.isEmpty() && motesPrices.isEmpty() && baseStats.isEmpty();
		}
	}
}
