package net.emutils.client.emskyblock.features.fishing.seacreaturetracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.context.SkyblockTextUtils;
import org.jspecify.annotations.Nullable;

public final class SeaCreatureRegistry {
	private static final Map<String, SeaCreatureDefinition> BY_MESSAGE = new HashMap<>();
	private static final Map<String, SeaCreatureDefinition> BY_ID = new HashMap<>();
	private static final Map<String, List<String>> VARIANTS = new LinkedHashMap<>();
	private static boolean loaded;

	private SeaCreatureRegistry() {
	}

	public static void load() {
		if (loaded) {
			return;
		}

		BY_MESSAGE.clear();
		BY_ID.clear();
		VARIANTS.clear();
		try (InputStream stream = SeaCreatureRegistry.class.getResourceAsStream("/data/emutils/skyblock/sea_creatures.json")) {
			if (stream == null) {
				EMUtilsClient.LOGGER.warn("Missing sea_creatures.json");
				loaded = true;
				return;
			}

			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> categoryEntry : root.entrySet()) {
				String category = categoryEntry.getKey();
				JsonObject categoryObject = categoryEntry.getValue().getAsJsonObject();
				JsonElement chatColorElement = categoryObject.get("chat_color");
				String chatColor = chatColorElement == null ? "§b" : chatColorElement.getAsString();
				JsonObject creatures = categoryObject.getAsJsonObject("sea_creatures");
				if (creatures == null) {
					continue;
				}

				List<String> variantIds = new java.util.ArrayList<>();

				for (Map.Entry<String, JsonElement> creatureEntry : creatures.entrySet()) {
					String id = creatureEntry.getKey();
					JsonObject info = creatureEntry.getValue().getAsJsonObject();
					boolean rare = info.has("rare") && info.get("rare").getAsBoolean();
					SeaCreatureDefinition definition = new SeaCreatureDefinition(
						id,
						id,
						category,
						chatColor,
						rare
					);
					BY_ID.put(id, definition);
					variantIds.add(id);
					if (info.has("chat_message")) {
						registerMessage(info.get("chat_message").getAsString(), definition);
					}
					if (info.has("alternate_messages")) {
						for (JsonElement alternate : info.getAsJsonArray("alternate_messages")) {
							registerMessage(alternate.getAsString(), definition);
						}
					}
				}

				VARIANTS.put(category, List.copyOf(variantIds));
			}

			loaded = true;
			EMUtilsClient.LOGGER.info("Loaded {} sea creature chat patterns.", BY_MESSAGE.size());
		} catch (Exception exception) {
			BY_MESSAGE.clear();
			BY_ID.clear();
			VARIANTS.clear();
			loaded = false;
			EMUtilsClient.LOGGER.warn("Failed to load sea creature registry.", exception);
		}
	}

	private static void registerMessage(String message, SeaCreatureDefinition definition) {
		String stripped = SkyblockTextUtils.strip(message);
		if (!stripped.isEmpty()) {
			BY_MESSAGE.put(stripped, definition);
		}
	}

	@Nullable
	public static SeaCreatureDefinition fromChat(String message) {
		load();
		String stripped = SkyblockTextUtils.strip(message);
		return BY_MESSAGE.get(stripped);
	}

	@Nullable
	public static SeaCreatureDefinition byId(String id) {
		load();
		return BY_ID.get(id);
	}

	public static Map<String, List<String>> variants() {
		load();
		return Collections.unmodifiableMap(VARIANTS);
	}
}
