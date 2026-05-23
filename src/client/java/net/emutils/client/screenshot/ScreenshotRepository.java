package net.emutils.client.screenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;

public final class ScreenshotRepository {
	private static final int MAX_SCREENSHOTS = 200;

	private ScreenshotRepository() {
	}

	public static List<ScreenshotEntry> list(MinecraftClient client) {
		Path screenshotsDir = ScreenshotPaths.screenshotsDir(client);
		if (!Files.isDirectory(screenshotsDir)) {
			return List.of();
		}

		try (var stream = Files.list(screenshotsDir)) {
			return stream
				.filter(ScreenshotRepository::isReadablePng)
				.map(ScreenshotRepository::entry)
				.sorted(Comparator.comparingLong(ScreenshotEntry::modifiedMillis).reversed())
				.limit(MAX_SCREENSHOTS)
				.toList();
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to list screenshots.", exception);
			return List.of();
		}
	}

	private static boolean isReadablePng(Path path) {
		return Files.isRegularFile(path)
			&& Files.isReadable(path)
			&& path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png");
	}

	private static ScreenshotEntry entry(Path path) {
		long modifiedMillis;
		try {
			modifiedMillis = Files.getLastModifiedTime(path).toMillis();
		} catch (IOException exception) {
			modifiedMillis = 0L;
		}

		return new ScreenshotEntry(path, modifiedMillis);
	}

	public record ScreenshotEntry(Path path, long modifiedMillis) {
		public String filename() {
			return path.getFileName().toString();
		}
	}
}
