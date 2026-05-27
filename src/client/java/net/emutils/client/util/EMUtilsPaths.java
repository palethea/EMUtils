package net.emutils.client.util;

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

	public static Path packInstallIndexFile() {
		return configDir().resolve("pack-index.json");
	}

	public static Path minescriptKeybindFile() {
		return configDir().resolve("minescript-keybinds.json");
	}

	public static Path inventoryToolsFile() {
		return configDir().resolve("inventory-tools.json");
	}

	public static Path storagePreviewFile() {
		return configDir().resolve("skyblock-storage.json");
	}

	public static Path emSkyblockConfigFile() {
		return configDir().resolve("emskyblock.json");
	}

	public static Path debugDir() {
		return configDir().resolve("debug");
	}
}
