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
 * Screenshot images for the new gallery (#106), loaded in the background at the size they're drawn and
 * kept for the most recently used ones. The grid and the large preview ask for different sizes of the
 * same screenshot.
 */
final class GalleryThumbnails implements AutoCloseable {
	private static final int MAX_CACHED = 48;

	private final Minecraft client;
	private final ScreenshotThumbnailLoader loader;
	private final LinkedHashMap<Key, LoadedThumbnail> cache = new LinkedHashMap<>(16, 0.75F, true);

	GalleryThumbnails(Minecraft client) {
		this.client = client;
		this.loader = new ScreenshotThumbnailLoader(client, this::loaded);
	}

	private void loaded(Path path, LoadedThumbnail thumbnail) {
		LoadedThumbnail previous = cache.put(new Key(path, thumbnail.targetWidth(), thumbnail.targetHeight()), thumbnail);
		if (previous != null && !previous.id().equals(thumbnail.id())) {
			client.getTextureManager().release(previous.id());
		}
		Iterator<LoadedThumbnail> oldest = cache.values().iterator();
		while (cache.size() > MAX_CACHED && oldest.hasNext()) {
			client.getTextureManager().release(oldest.next().id());
			oldest.remove();
		}
	}

	/** The image at this size, or null while it loads (the load starts here) or when it can't be read. */
	@Nullable LoadedThumbnail get(ScreenshotEntry screenshot, int targetWidth, int targetHeight) {
		LoadedThumbnail thumbnail = cache.get(new Key(screenshot.path(), targetWidth, targetHeight));
		if (thumbnail == null && !loader.hasFailed(screenshot, targetWidth, targetHeight)) {
			loader.request(screenshot, targetWidth, targetHeight);
		}
		return thumbnail;
	}

	boolean failed(ScreenshotEntry screenshot, int targetWidth, int targetHeight) {
		return loader.hasFailed(screenshot, targetWidth, targetHeight);
	}

	/** Drops every size of a screenshot, for example after it was deleted. */
	void forget(Path path) {
		Iterator<Map.Entry<Key, LoadedThumbnail>> iterator = cache.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Key, LoadedThumbnail> entry = iterator.next();
			if (entry.getKey().path().equals(path)) {
				client.getTextureManager().release(entry.getValue().id());
				iterator.remove();
			}
		}
	}

	@Override
	public void close() {
		loader.close();
		for (LoadedThumbnail thumbnail : cache.values()) {
			client.getTextureManager().release(thumbnail.id());
		}
		cache.clear();
	}

	private record Key(Path path, int targetWidth, int targetHeight) {
	}
}
