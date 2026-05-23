package net.emutils.client.screenshot;

import java.io.File;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;

public final class ScreenshotPaths {
	private ScreenshotPaths() {
	}

	public static Path screenshotsDir(MinecraftClient client) {
		File runDirectory = client.runDirectory;
		return runDirectory.toPath().resolve("screenshots");
	}
}
