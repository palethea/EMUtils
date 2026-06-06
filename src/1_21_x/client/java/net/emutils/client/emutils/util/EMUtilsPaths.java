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

	public static Path debugDir() {
		return configDir().resolve("debug");
	}
}
