package net.emutils.client.emutils.map;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.versioned.VersionedTextures;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/**
 * The map's tile textures (#212). Tiles are baked on background threads and uploaded on the render thread,
 * a few per frame, so the map never stalls a frame. A tile whose chunks changed keeps showing its old
 * picture until the new one is ready, and a redraw that came out unfinished, because a region was still
 * being read, never replaces a finished picture. New tiles fade in over whatever stood in for them.
 * Unused tiles are freed once there are too many.
 */
public final class MapTiles {
	/** About 64 MB of textures at most. */
	private static final int MAX_TILES = 256;
	private static final int MAX_BAKING = 4;
	private static final int UPLOADS_PER_FRAME = 3;
	/** How often a changed tile may be redrawn, per detail level: busy areas don't keep the baker busy. */
	private static final long[] REBAKE_MILLIS = {150L, 500L, 2000L, 1000L, 1000L, 1000L};
	/** An unfinished tile is tried again this soon, since what it waited for usually arrives quickly. */
	private static final long RETRY_MILLIS = 250L;
	/** How long a new tile takes to fade in over what stood in for it. */
	private static final float FADE_MILLIS = 160.0F;
	private static final ExecutorService BAKER = Executors.newFixedThreadPool(2, runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Map Baker");
		thread.setDaemon(true);
		thread.setPriority(Thread.NORM_PRIORITY - 1);
		return thread;
	});

	private final Map<Long, Tile> tiles = new LinkedHashMap<>(64, 0.75F, true);
	private final ConcurrentLinkedQueue<Baked> baked = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<MapRegion> overviewsDone = new ConcurrentLinkedQueue<>();
	private final AtomicInteger overviewsBaking = new AtomicInteger();
	private int baking;
	/** Tiles asked for this frame, and those of them that aren't finished yet, for the world map's loading sign. */
	private int asked;
	private int waiting;
	/** Counts up whenever all tiles are dropped, so bakes started before that are thrown away. */
	private int generation;
	private long frame;

	/** One tile: its texture once drawn, and whether that picture is finished. */
	static final class Tile {
		final int level;
		final int tileX;
		final int tileZ;
		@Nullable DynamicTexture texture;
		@Nullable NativeImage image;
		boolean baking;
		boolean dirty = true;
		/** The texture shows everything; false while some of it was still loading when it was drawn. */
		boolean complete;
		long bakedAt;
		/** When the tile first got a picture, for its fade-in. */
		long shownAt;
		long usedFrame;

		Tile(int level, int tileX, int tileZ) {
			this.level = level;
			this.tileX = tileX;
			this.tileZ = tileZ;
		}

		@Nullable DynamicTexture texture() {
			return texture;
		}

		/** Shows everything, faded in: nothing needs to be drawn under it. */
		boolean settled() {
			return texture != null && complete && fade() >= 1.0F;
		}

		/** How far the tile has faded in, 0 to 1. */
		float fade() {
			return shownAt == 0L ? 1.0F : Math.min(1.0F, (System.currentTimeMillis() - shownAt) / FADE_MILLIS);
		}
	}

	private record Baked(Tile tile, int generation, MapTileBaker.@Nullable Result result) {
	}

	private static long key(int level, int tileX, int tileZ) {
		return (long) level << 58 | ((long) tileX & 0x1FFFFFFFL) << 29 | (long) tileZ & 0x1FFFFFFFL;
	}

	/**
	 * A tile, asked to be drawn, or redrawn when its chunks changed; its texture is null until the first
	 * picture is ready. Render thread only.
	 */
	Tile tile(MapWorld world, int level, int tileX, int tileZ) {
		Tile tile = tiles.computeIfAbsent(key(level, tileX, tileZ), key -> new Tile(level, tileX, tileZ));
		tile.usedFrame = frame;
		asked++;
		long now = System.currentTimeMillis();
		long wait = tile.complete ? REBAKE_MILLIS[level] : RETRY_MILLIS;
		if (tile.dirty && !tile.baking && baking < MAX_BAKING && (tile.texture == null || now - tile.bakedAt >= wait)) {
			bake(world, tile);
		}
		if (!tile.complete || tile.texture == null) {
			waiting++;
		}
		return tile;
	}

	/** A tile that's already drawn, without asking for anything, to stand in for a missing one; null if none. */
	@Nullable Tile drawn(int level, int tileX, int tileZ) {
		Tile tile = tiles.get(key(level, tileX, tileZ));
		if (tile == null || tile.texture == null) {
			return null;
		}
		tile.usedFrame = frame;
		return tile;
	}

	/** The texture of a tile, or null while it's being drawn for the first time; asks for it like {@link #tile}. */
	public @Nullable DynamicTexture texture(MapWorld world, int level, int tileX, int tileZ) {
		return tile(world, level, tileX, tileZ).texture;
	}

	/** Whether tiles shown this frame, or the regions under them, are still being drawn or read. */
	public boolean busy() {
		return waiting > 0;
	}

	/** How much of what was shown this frame is finished, 0 to 1. */
	public float progress() {
		return asked == 0 ? 1.0F : (asked - waiting) / (float) asked;
	}

	private void bake(MapWorld world, Tile tile) {
		tile.baking = true;
		tile.dirty = false;
		baking++;
		int startedIn = generation;
		BAKER.execute(() -> {
			MapTileBaker.Result result = null;
			try {
				result = MapTileBaker.bake(world, tile.level, tile.tileX, tile.tileZ);
			} catch (RuntimeException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils map couldn't draw a tile", exception);
			}
			baked.add(new Baked(tile, startedIn, result));
		});
	}

	/** Uploads finished tiles and frees unused ones. Call once a frame before drawing, on the render thread. */
	public void beginFrame() {
		frame++;
		asked = 0;
		waiting = 0;
		for (int i = 0; i < UPLOADS_PER_FRAME; i++) {
			Baked done = baked.poll();
			if (done == null) {
				break;
			}
			baking--;
			Tile tile = done.tile();
			tile.baking = false;
			if (done.generation() != generation || done.result() == null) {
				continue;
			}
			boolean complete = done.result().complete();
			if (!complete) {
				// Drawn while some of it was still loading: drawn again once that had time to arrive.
				tile.dirty = true;
				if (tile.texture != null && tile.complete) {
					// The finished picture stays until a finished redraw replaces it.
					tile.bakedAt = System.currentTimeMillis();
					continue;
				}
			}
			boolean fresh = tile.texture == null;
			upload(tile, done.result().pixels());
			if (fresh) {
				tile.shownAt = System.currentTimeMillis();
			}
			tile.complete = complete;
		}
		if (tiles.size() > MAX_TILES) {
			Iterator<Tile> oldest = tiles.values().iterator();
			while (tiles.size() > MAX_TILES && oldest.hasNext()) {
				Tile tile = oldest.next();
				if (tile.usedFrame >= frame - 1 || tile.baking) {
					continue;
				}
				free(tile);
				oldest.remove();
			}
		}
	}

	private void upload(Tile tile, int[] pixels) {
		if (tile.texture == null) {
			tile.image = new NativeImage(MapTileBaker.TILE_PIXELS, MapTileBaker.TILE_PIXELS, false);
			String name = "EMUtils map tile " + tile.level + "/" + tile.tileX + "/" + tile.tileZ;
			tile.texture = VersionedTextures.mapTexture(() -> name, tile.image);
		}
		MemoryUtil.memIntBuffer(tile.image.getPointer(), pixels.length).put(pixels);
		tile.texture.upload();
		tile.bakedAt = System.currentTimeMillis();
	}

	/** Marks the tiles showing a chunk, and the columns next to it, to be redrawn. */
	public void markDirty(int chunkX, int chunkZ) {
		int minX = chunkX * MapChunk.SIZE;
		int minZ = chunkZ * MapChunk.SIZE;
		// The columns just east and south of the chunk are shaded by its heights, so their tiles change too.
		int maxX = minX + MapChunk.SIZE;
		int maxZ = minZ + MapChunk.SIZE;
		for (int level = 0; level < MapTileBaker.COLUMN_LEVELS; level++) {
			int blocks = MapTileBaker.blocksPerTile(level);
			for (int tileZ = Math.floorDiv(minZ, blocks); tileZ <= Math.floorDiv(maxZ, blocks); tileZ++) {
				for (int tileX = Math.floorDiv(minX, blocks); tileX <= Math.floorDiv(maxX, blocks); tileX++) {
					Tile tile = tiles.get(key(level, tileX, tileZ));
					if (tile != null) {
						tile.dirty = true;
					}
				}
			}
		}
	}

	/** Marks every tile showing part of a region to be redrawn, from {@code fromLevel} to the coarsest. */
	public void markRegionDirty(int regionX, int regionZ, int fromLevel) {
		int minX = regionX * MapRegion.BLOCKS;
		int minZ = regionZ * MapRegion.BLOCKS;
		int maxX = minX + MapRegion.BLOCKS;
		int maxZ = minZ + MapRegion.BLOCKS;
		for (int level = fromLevel; level < MapTileBaker.LEVELS; level++) {
			int blocks = MapTileBaker.blocksPerTile(level);
			for (Tile tile : tiles.values()) {
				if (tile.level == level
					&& (long) tile.tileX * blocks <= maxX && (long) (tile.tileX + 1) * blocks >= minX
					&& (long) tile.tileZ * blocks <= maxZ && (long) (tile.tileZ + 1) * blocks >= minZ) {
					tile.dirty = true;
				}
			}
		}
	}

	/**
	 * Draws a region's overview on the baker thread and hands it to the region; {@code done} runs on the
	 * render thread afterwards, through {@link #beginFrame}.
	 */
	public void bakeOverview(MapWorld world, MapRegion region, int fingerprint) {
		region.overviewStale = false;
		region.overviewBakedAt = System.currentTimeMillis();
		overviewsBaking.incrementAndGet();
		BAKER.execute(() -> {
			try {
				MapTileBaker.redrawOverview(world, region, fingerprint);
				overviewsDone.add(region);
			} catch (RuntimeException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils map couldn't draw a region overview", exception);
			} finally {
				overviewsBaking.decrementAndGet();
			}
		});
	}

	/** How many overviews are being drawn right now. */
	int overviewsBaking() {
		return overviewsBaking.get();
	}

	/** Regions whose overview was drawn since the last call. Render thread. */
	public @Nullable MapRegion pollOverviewDone() {
		return overviewsDone.poll();
	}

	/** Frees every tile, for a new world or a resource pack change. */
	public void clear() {
		List<Tile> all = new ArrayList<>(tiles.values());
		tiles.clear();
		for (Tile tile : all) {
			free(tile);
		}
		generation++;
	}

	int size() {
		return tiles.size();
	}

	private static void free(Tile tile) {
		if (tile.texture != null) {
			// Closing the texture closes its image too.
			tile.texture.close();
			tile.texture = null;
			tile.image = null;
		}
	}
}
