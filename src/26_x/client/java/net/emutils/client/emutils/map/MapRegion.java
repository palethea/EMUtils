package net.emutils.client.emutils.map;

import java.util.concurrent.atomic.AtomicReferenceArray;
import org.jspecify.annotations.Nullable;

/**
 * 32 x 32 chunks of the map (#215), the unit the map is saved and loaded in. Chunks sampled before the
 * region finished loading from disk are kept over the saved ones, since they are newer.
 *
 * <p>Besides the chunks, a region has an overview: a small picture of it at {@link #OVERVIEW_BLOCKS} blocks
 * per pixel, which the world map's far zoom levels are built from, so they never need the full data of
 * every region in view.
 */
public final class MapRegion {
	public static final int CHUNKS = 32;
	public static final int SHIFT = 5;
	public static final int BLOCKS = CHUNKS * MapChunk.SIZE;
	public static final int OVERVIEW_BLOCKS = 4;
	public static final int OVERVIEW_SIZE = BLOCKS / OVERVIEW_BLOCKS;

	final int regionX;
	final int regionZ;
	private final AtomicReferenceArray<MapChunk> chunks = new AtomicReferenceArray<>(CHUNKS * CHUNKS);
	/** The saved chunks are in, or there were none. */
	volatile boolean loaded;
	/** Some chunks changed since the last save. */
	volatile boolean dirty;
	/** The overview no longer matches the chunks; set when a chunk is sampled or read without an overview. */
	volatile boolean overviewStale;
	volatile long lastUsed = System.currentTimeMillis();
	/** The overview's pixels as ARGB, or null when there is none yet. */
	volatile int @Nullable [] overview;
	/** The resource packs the overview was drawn with; another fingerprint means it is redrawn. */
	volatile int overviewFingerprint;
	/** When the overview was last drawn, so a region being explored isn't redrawn all the time. */
	volatile long overviewBakedAt;
	/** The block states of the chunks just read from disk, whose looks the client thread still has to make. */
	volatile int @Nullable [] states;

	MapRegion(int regionX, int regionZ) {
		this.regionX = regionX;
		this.regionZ = regionZ;
	}

	static int index(int chunkX, int chunkZ) {
		return (chunkZ & (CHUNKS - 1)) * CHUNKS + (chunkX & (CHUNKS - 1));
	}

	static long key(int regionX, int regionZ) {
		return (long) regionX << 32 | regionZ & 0xFFFFFFFFL;
	}

	@Nullable MapChunk chunk(int index) {
		return chunks.get(index);
	}

	void put(int index, MapChunk chunk) {
		chunks.set(index, chunk);
		dirty = true;
		overviewStale = true;
	}

	/** Puts a chunk read from disk where nothing newer was sampled meanwhile. */
	void putLoaded(int index, MapChunk chunk) {
		chunks.compareAndSet(index, null, chunk);
	}

	int count() {
		int count = 0;
		for (int i = 0; i < chunks.length(); i++) {
			if (chunks.get(i) != null) {
				count++;
			}
		}
		return count;
	}
}
