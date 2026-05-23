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
}
