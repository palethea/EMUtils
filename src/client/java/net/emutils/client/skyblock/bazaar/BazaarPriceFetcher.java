package net.emutils.client.skyblock.bazaar;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
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
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BazaarPriceFetcher {
	private static final Logger LOGGER = LoggerFactory.getLogger("emutils.bazaar");
	private static final String API_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
	private static final long FETCH_INTERVAL_MS = 120_000L;
	private static final Gson GSON = new Gson();

	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	private volatile Map<String, BazaarProductPrice> products = Map.of();
	private long lastFetchAttemptMs;
	private long fetchStartedMs;
	private boolean fetching;

	public int productCount() {
		return products.size();
	}

	public Optional<BazaarProductPrice> price(String productId) {
		return Optional.ofNullable(products.get(productId));
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
		products = Map.of();
		lastFetchAttemptMs = 0L;
		fetchStartedMs = 0L;
		fetching = false;
	}

	private void recoverStuckFetch() {
		if (!fetching || fetchStartedMs <= 0L) {
			return;
		}

		if (System.currentTimeMillis() - fetchStartedMs > 45_000L) {
			LOGGER.warn("Bazaar price fetch timed out; allowing retry.");
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
		CompletableFuture.runAsync(this::fetch, SkyblockPriceExecutor.EXECUTOR)
			.whenComplete((ignored, throwable) -> {
				fetching = false;
				fetchStartedMs = 0L;
				if (throwable != null) {
					LOGGER.warn("Bazaar price fetch failed.", throwable);
				}
			});
	}

	private void fetch() {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
				.timeout(Duration.ofSeconds(30))
				.header("Accept", "application/json")
				.header("User-Agent", "mwsk75996/EMUtils/" + EMUtilsClient.MOD_ID + " (Minecraft client mod)")
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				LOGGER.warn("Bazaar API request failed with HTTP {}.", response.statusCode());
				return;
			}

			Map<String, BazaarProductPrice> parsed = parseResponse(response.body());
			if (!parsed.isEmpty()) {
				products = parsed;
				LOGGER.info("Loaded {} bazaar products.", parsed.size());
			} else {
				LOGGER.warn("Bazaar API returned no products.");
				lastFetchAttemptMs = System.currentTimeMillis() - FETCH_INTERVAL_MS + 5_000L;
			}
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			LOGGER.warn("Bazaar API fetch failed.", exception);
			lastFetchAttemptMs = System.currentTimeMillis() - FETCH_INTERVAL_MS + 5_000L;
		}
	}

	private static Map<String, BazaarProductPrice> parseResponse(String body) {
		Map<String, BazaarProductPrice> streamed = parseResponseStreaming(body);
		if (!streamed.isEmpty()) {
			return streamed;
		}

		JsonObject root;
		try {
			root = GSON.fromJson(body, JsonObject.class);
		} catch (JsonParseException exception) {
			LOGGER.warn("Bazaar API returned malformed JSON.", exception);
			return Map.of();
		}

		if (root == null || !root.get("success").getAsBoolean()) {
			return Map.of();
		}

		JsonObject productsObject = root.getAsJsonObject("products");
		if (productsObject == null) {
			return Map.of();
		}

		Map<String, BazaarProductPrice> result = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : productsObject.entrySet()) {
			BazaarProductPrice price = parseProduct(entry.getValue().getAsJsonObject());
			if (price != null) {
				result.put(entry.getKey(), price);
			}
		}
		return Map.copyOf(result);
	}

	private static Map<String, BazaarProductPrice> parseResponseStreaming(String body) {
		Map<String, BazaarProductPrice> result = new HashMap<>();
		try (JsonReader reader = new JsonReader(new StringReader(body))) {
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if ("success".equals(name)) {
					if (!reader.nextBoolean()) {
						return Map.of();
					}
					continue;
				}

				if ("products".equals(name)) {
					reader.beginObject();
					while (reader.hasNext()) {
						String productId = reader.nextName();
						BazaarProductPrice price = parseProductStreaming(reader);
						if (price != null) {
							result.put(productId, price);
						}
					}
					reader.endObject();
					continue;
				}

				reader.skipValue();
			}
			reader.endObject();
		} catch (IOException | IllegalStateException exception) {
			LOGGER.debug("Streaming Bazaar parse failed, falling back to full parse.", exception);
			return Map.of();
		}

		return Map.copyOf(result);
	}

	@Nullable
	private static BazaarProductPrice parseProductStreaming(JsonReader reader) throws IOException {
		double buy = 0.0D;
		double sell = 0.0D;
		double instantBuy = 0.0D;
		double instantSell = 0.0D;

		reader.beginObject();
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "quick_status" -> {
					reader.beginObject();
					while (reader.hasNext()) {
						switch (reader.nextName()) {
							case "buyPrice" -> buy = reader.nextDouble();
							case "sellPrice" -> sell = reader.nextDouble();
							default -> reader.skipValue();
						}
					}
					reader.endObject();
				}
				case "buy_summary" -> instantBuy = minPriceFromSummary(reader);
				case "sell_summary" -> instantSell = maxPriceFromSummary(reader);
				default -> reader.skipValue();
			}
		}
		reader.endObject();

		if (buy <= 0.0D && sell <= 0.0D && instantBuy <= 0.0D && instantSell <= 0.0D) {
			return null;
		}

		if (instantBuy <= 0.0D) {
			instantBuy = buy;
		}
		if (instantSell <= 0.0D) {
			instantSell = sell;
		}

		return new BazaarProductPrice(buy, sell, instantBuy, instantSell);
	}

	private static double minPriceFromSummary(JsonReader reader) throws IOException {
		if (reader.peek() != JsonToken.BEGIN_ARRAY) {
			reader.skipValue();
			return 0.0D;
		}

		double min = Double.MAX_VALUE;
		reader.beginArray();
		while (reader.hasNext()) {
			double price = 0.0D;
			reader.beginObject();
			while (reader.hasNext()) {
				if ("pricePerUnit".equals(reader.nextName())) {
					price = reader.nextDouble();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
			if (price > 0.0D && price < min) {
				min = price;
			}
		}
		reader.endArray();
		return min == Double.MAX_VALUE ? 0.0D : min;
	}

	private static double maxPriceFromSummary(JsonReader reader) throws IOException {
		if (reader.peek() != JsonToken.BEGIN_ARRAY) {
			reader.skipValue();
			return 0.0D;
		}

		double max = 0.0D;
		reader.beginArray();
		while (reader.hasNext()) {
			double price = 0.0D;
			reader.beginObject();
			while (reader.hasNext()) {
				if ("pricePerUnit".equals(reader.nextName())) {
					price = reader.nextDouble();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
			if (price > max) {
				max = price;
			}
		}
		reader.endArray();
		return max;
	}

	@Nullable
	private static BazaarProductPrice parseProduct(JsonObject product) {
		JsonObject quick = product.getAsJsonObject("quick_status");
		if (quick == null) {
			return null;
		}

		double buy = quick.get("buyPrice").getAsDouble();
		double sell = quick.get("sellPrice").getAsDouble();
		double instantBuy = minPrice(product.getAsJsonArray("buy_summary"));
		double instantSell = maxPrice(product.getAsJsonArray("sell_summary"));
		if (instantBuy <= 0.0D) {
			instantBuy = buy;
		}
		if (instantSell <= 0.0D) {
			instantSell = sell;
		}

		return new BazaarProductPrice(buy, sell, instantBuy, instantSell);
	}

	private static double minPrice(@Nullable JsonArray summary) {
		if (summary == null || summary.isEmpty()) {
			return 0.0D;
		}

		double min = Double.MAX_VALUE;
		for (JsonElement element : summary) {
			double price = element.getAsJsonObject().get("pricePerUnit").getAsDouble();
			if (price > 0.0D && price < min) {
				min = price;
			}
		}

		return min == Double.MAX_VALUE ? 0.0D : min;
	}

	private static double maxPrice(@Nullable JsonArray summary) {
		if (summary == null || summary.isEmpty()) {
			return 0.0D;
		}

		double max = 0.0D;
		for (JsonElement element : summary) {
			double price = element.getAsJsonObject().get("pricePerUnit").getAsDouble();
			if (price > max) {
				max = price;
			}
		}

		return max;
	}
}
