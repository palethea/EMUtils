package net.emutils.client.emskyblock.features.fishing.profittracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.emutils.client.EMUtilsClient;

public final class FishingProfitItemRegistry {
	public static final String SKYBLOCK_COIN = "SKYBLOCK_COIN";

	private static final Set<String> ALLOWED = new HashSet<>();
	private static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>();
	private static boolean loaded;

	private FishingProfitItemRegistry() {
	}

	public static void load() {
		if (loaded) {
			return;
		}

		loaded = true;
		try (InputStream stream = FishingProfitItemRegistry.class.getResourceAsStream("/data/emutils/skyblock/fishing_profit_items.json")) {
			if (stream == null) {
				EMUtilsClient.LOGGER.warn("Missing fishing_profit_items.json");
				return;
			}

			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject categories = root.getAsJsonObject("categories");
			for (Map.Entry<String, JsonElement> entry : categories.entrySet()) {
				List<String> items = new java.util.ArrayList<>();
				for (JsonElement item : entry.getValue().getAsJsonArray()) {
					String id = item.getAsString();
					items.add(id);
					ALLOWED.add(id);
				}

				CATEGORIES.put(entry.getKey(), List.copyOf(items));
			}

			EMUtilsClient.LOGGER.info("Loaded {} fishing profit item ids.", ALLOWED.size());
		} catch (Exception exception) {
			EMUtilsClient.LOGGER.warn("Failed to load fishing profit item registry.", exception);
		}
	}

	public static boolean isAllowed(String itemId) {
		load();
		return ALLOWED.contains(itemId);
	}

	public static Map<String, List<String>> categories() {
		load();
		return Collections.unmodifiableMap(CATEGORIES);
	}
}
