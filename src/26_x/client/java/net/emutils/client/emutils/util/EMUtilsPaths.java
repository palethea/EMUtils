package net.emutils.client.emutils.util;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

public final class EMUtilsPaths {
	private EMUtilsPaths() {
	}

	public static Path configDir() {
		return FabricLoader.getInstance().getConfigDir().resolve("emutils");
	}

	public static Path configFile() {
		return configDir().resolve("config.json");
	}

	/** The list of config profiles (#89); the Default profile's settings stay in {@link #configFile()}. */
	public static Path profilesFile() {
		return configDir().resolve("profiles.json");
	}

	/** Where every profile but Default keeps its settings. */
	public static Path profilesDir() {
		return configDir().resolve("profiles");
	}

	public static Path deathWaypointFile() {
		return configDir().resolve("last-death.json");
	}

	public static Path waypointFile() {
		return configDir().resolve("waypoints.json");
	}

	public static Path packInstallIndexFile() {
		return configDir().resolve("pack-index.json");
	}

	public static Path minescriptKeybindFile() {
		return configDir().resolve("minescript-keybinds.json");
	}

	public static Path commandShortcutsFile() {
		return configDir().resolve("command-shortcuts.json");
	}

	public static Path inventoryToolsFile() {
		return configDir().resolve("inventory-tools.json");
	}

	public static Path massDropFile() {
		return configDir().resolve("mass-drop.json");
	}

	public static Path debugDir() {
		return configDir().resolve("debug");
	}
}
