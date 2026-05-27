package net.emutils.client.skyblock.npc;

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
import net.emutils.client.skyblock.SkyblockContext;
import net.emutils.client.skyblock.SkyblockPriceFetchTask;
import net.emutils.client.skyblock.SkyblockPriceNeeds;
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
	private final SkyblockPriceFetchTask<Map<String, Double>> fetchTask = new SkyblockPriceFetchTask<>(
		LOGGER,
		"NPC",
		FETCH_INTERVAL_MS,
		NpcPriceFetcher::shouldRun,
		this::fetch,
		this::publish
	);

	private volatile Map<String, Double> npcSellPrices = Map.of();

	public Optional<Double> npcSellPrice(String itemId) {
		return Optional.ofNullable(npcSellPrices.get(itemId));
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
		fetchTask.clear();
	}

	private boolean publish(Map<String, Double> parsed) {
		if (parsed.isEmpty()) {
			return false;
		}

		npcSellPrices = parsed;
		LOGGER.info("Loaded {} NPC sell prices.", parsed.size());
		return true;
	}

	private Map<String, Double> fetch() {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
				.timeout(Duration.ofSeconds(45))
				.header("Accept", "application/json")
				.header("User-Agent", "mwsk75996/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)")
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				LOGGER.warn("NPC price request failed with HTTP {}.", response.statusCode());
				return Map.of();
			}

			return parseResponse(response.body());
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			LOGGER.debug("NPC price fetch failed.", exception);
			return Map.of();
		}
	}

	private static Map<String, Double> parseResponse(String body) {
		JsonObject root;
		try {
			root = GSON.fromJson(body, JsonObject.class);
		} catch (JsonParseException exception) {
			LOGGER.warn("NPC price API returned malformed JSON.", exception);
			return Map.of();
		}

		if (root == null) {
			return Map.of();
		}

		JsonElement success = root.get("success");
		if (success == null || !success.getAsBoolean()) {
			return Map.of();
		}

		JsonArray items = root.getAsJsonArray("items");
		if (items == null) {
			return Map.of();
		}

		Map<String, Double> result = new HashMap<>();
		for (JsonElement element : items) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject item = element.getAsJsonObject();
			if (!item.has("id") || !item.has("npc_sell_price")) {
				continue;
			}

			String id = item.get("id").getAsString();
			double price = item.get("npc_sell_price").getAsDouble();
			if (!id.isEmpty() && price > 0.0D) {
				result.put(id, price);
			}
		}

		return Map.copyOf(result);
	}
}
