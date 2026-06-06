package net.emutils.client.emutils.screenshot;

import java.io.File;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

public final class ScreenshotPaths {
	private ScreenshotPaths() {
	}

	public static Path screenshotsDir(Minecraft client) {
		File runDirectory = client.gameDirectory;
		return runDirectory.toPath().resolve("screenshots");
	}
}
