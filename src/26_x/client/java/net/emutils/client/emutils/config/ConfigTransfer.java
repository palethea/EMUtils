package net.emutils.client.emutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * {@code /emutils export} and {@code /emutils import} (#157): the active profile's settings, plus the
 * EMUtils keybinds. Keys are saved in Minecraft's {@code options.txt} rather than the config, and are the
 * same for every profile, so the export carries them in a {@code keybinds} object next to the settings.
 */
public final class ConfigTransfer {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String KEYBINDS = "keybinds";
	private static final String KEY_PREFIX = "key.emutils.";

	private ConfigTransfer() {
	}

	/** The active profile's settings and every EMUtils key, as text to copy. */
	public static String export() {
		JsonObject root = JsonParser.parseString(EMUtilsClient.config().toJson()).getAsJsonObject();
		JsonObject keybinds = new JsonObject();
		for (KeyMapping key : Minecraft.getInstance().options.keyMappings) {
			if (key.getName().startsWith(KEY_PREFIX)) {
				keybinds.addProperty(key.getName(), key.saveString());
			}
		}
		root.add(KEYBINDS, keybinds);
		return GSON.toJson(root);
	}

	/**
	 * Replaces the active profile's settings with exported ones and binds the exported keys; false if the
	 * text isn't an EMUtils config, and then nothing changes. How the menus look stays, as when switching
	 * profiles, and keys an older export doesn't list keep their binding.
	 */
	public static boolean importFrom(@Nullable String text) {
		EMUtilsConfig current = EMUtilsClient.config();
		EMUtilsConfig imported = current == null ? null : EMUtilsConfig.fromJson(text, current.file());
		if (imported == null) {
			return false;
		}
		imported.copyMenuSettingsFrom(current);
		EMUtilsClient.replaceConfig(imported);
		// Written now rather than after the save delay, so an import survives the game closing right away.
		imported.flush();
		applyKeybinds(text);
		return true;
	}

	private static void applyKeybinds(String text) {
		JsonObject keybinds;
		try {
			JsonElement found = JsonParser.parseString(text).getAsJsonObject().get(KEYBINDS);
			if (found == null || !found.isJsonObject()) {
				return;
			}
			keybinds = found.getAsJsonObject();
		} catch (JsonParseException | IllegalStateException ignored) {
			return;
		}
		boolean changed = false;
		for (Map.Entry<String, JsonElement> entry : keybinds.entrySet()) {
			KeyMapping key = entry.getKey().startsWith(KEY_PREFIX) ? KeyMapping.get(entry.getKey()) : null;
			if (key == null || !entry.getValue().isJsonPrimitive()) {
				continue;
			}
			try {
				key.setKey(InputConstants.getKey(entry.getValue().getAsString()));
				changed = true;
			} catch (IllegalArgumentException ignored) {
				// A key this game doesn't know; that binding is left as it is.
			}
		}
		if (changed) {
			KeyMapping.resetMapping();
			Minecraft.getInstance().options.save();
		}
	}
}
