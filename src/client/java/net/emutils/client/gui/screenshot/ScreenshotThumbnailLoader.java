package net.emutils.client.gui.screenshot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.screenshot.ScreenshotRepository.ScreenshotEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class ScreenshotThumbnailLoader implements AutoCloseable {
	private record LoadKey(Path path, int targetWidth, int targetHeight, long modifiedMillis) {
		static LoadKey of(ScreenshotEntry entry, int targetWidth, int targetHeight) {
			return new LoadKey(entry.path(), targetWidth, targetHeight, entry.modifiedMillis());
		}
	}

	public record LoadedThumbnail(Identifier id, int width, int height, int targetWidth, int targetHeight) {
	}

	private final MinecraftClient client;
	private final ThumbnailConsumer onLoaded;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(thread -> {
		Thread workerThread = new Thread(thread, "EMUtils-Screenshot-Thumbnails");
		workerThread.setDaemon(true);
		return workerThread;
	});
	private final Map<LoadKey, CompletableFuture<NativeImage>> inFlight = new ConcurrentHashMap<>();
	private final Set<LoadKey> active = ConcurrentHashMap.newKeySet();
	private final Set<LoadKey> failed = ConcurrentHashMap.newKeySet();
	private final AtomicBoolean closed = new AtomicBoolean();

	public ScreenshotThumbnailLoader(MinecraftClient client, ThumbnailConsumer onLoaded) {
		this.client = client;
		this.onLoaded = onLoaded;
	}

	public void request(ScreenshotEntry entry, int targetWidth, int targetHeight) {
		if (closed.get()) {
			return;
		}

		LoadKey key = LoadKey.of(entry, targetWidth, targetHeight);
		if (failed.contains(key) || active.contains(key) || inFlight.containsKey(key)) {
			return;
		}

		active.add(key);
		CompletableFuture<NativeImage> future = CompletableFuture.supplyAsync(() -> decodeThumbnail(entry.path(), targetWidth, targetHeight), worker);
		inFlight.put(key, future);
		future.whenComplete((image, error) -> client.execute(() -> completeLoad(key, entry, targetWidth, targetHeight, image, error)));
	}

	public boolean isActive(ScreenshotEntry entry, int targetWidth, int targetHeight) {
		LoadKey key = LoadKey.of(entry, targetWidth, targetHeight);
		return active.contains(key) || inFlight.containsKey(key);
	}

	public boolean hasFailed(ScreenshotEntry entry, int targetWidth, int targetHeight) {
		return failed.contains(LoadKey.of(entry, targetWidth, targetHeight));
	}

	public void clearFailures() {
		failed.clear();
	}

	@Override
	public void close() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}

		active.clear();
		inFlight.clear();
		failed.clear();
		worker.shutdownNow();
	}

	private void completeLoad(LoadKey key, ScreenshotEntry entry, int targetWidth, int targetHeight, NativeImage image, Throwable error) {
		inFlight.remove(key);
		active.remove(key);

		if (closed.get()) {
			closeImage(image);
			return;
		}

		if (error != null || image == null) {
			failed.add(key);
			if (error != null) {
				EMUtilsClient.LOGGER.warn("Failed to load screenshot thumbnail {}.", entry.path(), error);
			}
			return;
		}

		try {
			Identifier id = Identifier.of(
				EMUtilsClient.MOD_ID,
				"screenshot_gallery/" + Integer.toUnsignedString(entry.path().toString().hashCode(), 16) + "_" + entry.modifiedMillis()
			);
			client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> entry.filename(), image));
			onLoaded.onLoaded(
				entry.path(),
				new LoadedThumbnail(id, image.getWidth(), image.getHeight(), targetWidth, targetHeight)
			);
		} catch (RuntimeException exception) {
			closeImage(image);
			failed.add(key);
			EMUtilsClient.LOGGER.warn("Failed to register screenshot thumbnail {}.", entry.path(), exception);
		}
	}

	private static NativeImage decodeThumbnail(Path path, int targetWidth, int targetHeight) {
		try (InputStream stream = Files.newInputStream(path)) {
			NativeImage image = NativeImage.read(stream);
			NativeImage scaled = scaleToPreviewSize(image, targetWidth, targetHeight);
			if (scaled != image) {
				image.close();
			}
			return scaled;
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static NativeImage scaleToPreviewSize(NativeImage image, int maxWidth, int maxHeight) {
		int width = image.getWidth();
		int height = image.getHeight();
		if (width <= maxWidth && height <= maxHeight) {
			return image;
		}

		double scale = Math.min(maxWidth / (double) width, maxHeight / (double) height);
		int targetWidth = Math.max(1, (int) Math.round(width * scale));
		int targetHeight = Math.max(1, (int) Math.round(height * scale));
		NativeImage scaled = new NativeImage(targetWidth, targetHeight, false);
		image.resizeSubRectTo(0, 0, width, height, scaled);
		return scaled;
	}

	private static void closeImage(NativeImage image) {
		if (image != null) {
			image.close();
		}
	}

	@FunctionalInterface
	public interface ThumbnailConsumer {
		void onLoaded(Path path, LoadedThumbnail thumbnail);
	}
}
