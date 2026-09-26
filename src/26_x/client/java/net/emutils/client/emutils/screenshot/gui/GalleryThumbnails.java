package net.emutils.client.emutils.screenshot.gui;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.emutils.client.emutils.screenshot.ScreenshotRepository.ScreenshotEntry;
import net.emutils.client.emutils.screenshot.gui.ScreenshotThumbnailLoader.LoadedThumbnail;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * Screenshot images for the gallery (#106), loaded in the background at the size they're drawn. The grid
 * and the large preview ask for different sizes of the same screenshot.
 *
 * <p>One cache is shared by every gallery screen and outlives them (#133), so reopening the gallery shows
 * the thumbnails right away instead of loading them again. The least recently used images are freed once
 * they take more texture memory than {@link #MAX_BYTES}, and everything is freed when leaving the world.
 */
public final class GalleryThumbnails {
	/** Texture memory the cached images may use: a few hundred grid thumbnails, or a handful of previews. */
	private static final long MAX_BYTES = 96L * 1024 * 1024;
	private static @Nullable GalleryThumbnails shared;

	private final Minecraft client;
	private final ScreenshotThumbnailLoader loader;
	private final LinkedHashMap<Key, LoadedThumbnail> cache = new LinkedHashMap<>(16, 0.75F, true);
	private long bytes;

	private GalleryThumbnails(Minecraft client) {
		this.client = client;
		this.loader = new ScreenshotThumbnailLoader(client, this::loaded);
	}

	static GalleryThumbnails shared() {
		if (shared == null) {
			shared = new GalleryThumbnails(Minecraft.getInstance());
		}
		return shared;
	}

	/**
	 * Frees every cached image, for example when leaving the world. Safe to call from any thread: the
	 * disconnect event runs on the network thread, and textures can only be freed on the render thread.
	 */
	public static void freeShared() {
		Minecraft client = Minecraft.getInstance();
		if (!client.isSameThread()) {
			client.execute(GalleryThumbnails::freeShared);
			return;
		}
		if (shared != null) {
			shared.close();
			shared = null;
		}
	}

	private void loaded(Path path, long modifiedMillis, LoadedThumbnail thumbnail) {
		LoadedThumbnail previous = cache.put(new Key(path, modifiedMillis, thumbnail.targetWidth(), thumbnail.targetHeight()), thumbnail);
		bytes += bytes(thumbnail);
		if (previous != null) {
			bytes -= bytes(previous);
			if (!previous.id().equals(thumbnail.id())) {
				client.getTextureManager().release(previous.id());
			}
		}
		Iterator<LoadedThumbnail> oldest = cache.values().iterator();
		// Never frees the image that just loaded, even if it alone is over the budget.
		while (bytes > MAX_BYTES && cache.size() > 1 && oldest.hasNext()) {
			LoadedThumbnail evicted = oldest.next();
			client.getTextureManager().release(evicted.id());
			bytes -= bytes(evicted);
			oldest.remove();
		}
	}

	/** The image at this size, or null while it loads (the load starts here) or when it can't be read. */
	@Nullable LoadedThumbnail get(ScreenshotEntry screenshot, int targetWidth, int targetHeight) {
		LoadedThumbnail thumbnail = cache.get(new Key(screenshot.path(), screenshot.modifiedMillis(), targetWidth, targetHeight));
		if (thumbnail == null && !loader.hasFailed(screenshot, targetWidth, targetHeight)) {
			loader.request(screenshot, targetWidth, targetHeight);
		}
		return thumbnail;
	}

	boolean failed(ScreenshotEntry screenshot, int targetWidth, int targetHeight) {
		return loader.hasFailed(screenshot, targetWidth, targetHeight);
	}

	/** Lets screenshots that couldn't be read try again, for example one that was still being written. */
	void retryFailed() {
		loader.clearFailures();
	}

	/** Drops every size of a screenshot, for example after it was deleted. */
	void forget(Path path) {
		Iterator<Map.Entry<Key, LoadedThumbnail>> iterator = cache.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Key, LoadedThumbnail> entry = iterator.next();
			if (entry.getKey().path().equals(path)) {
				client.getTextureManager().release(entry.getValue().id());
				bytes -= bytes(entry.getValue());
				iterator.remove();
			}
		}
	}

	private void close() {
		loader.close();
		for (LoadedThumbnail thumbnail : cache.values()) {
			client.getTextureManager().release(thumbnail.id());
		}
		cache.clear();
		bytes = 0;
	}

	private static long bytes(LoadedThumbnail thumbnail) {
		return (long) thumbnail.width() * thumbnail.height() * 4;
	}

	/** Includes the modified time, so a screenshot that was overwritten loads again. */
	private record Key(Path path, long modifiedMillis, int targetWidth, int targetHeight) {
	}
}
