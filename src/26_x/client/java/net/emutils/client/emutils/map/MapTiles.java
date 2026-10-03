package net.emutils.client.emutils.map;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.versioned.VersionedTextures;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/**
 * The map's tile textures (#212). Tiles are baked on a background thread and uploaded on the render thread,
 * a few per frame, so the map never stalls a frame. A tile whose chunks changed keeps showing its old
 * picture until the new one is ready. Unused tiles are freed once there are too many.
 */
public final class MapTiles {
	/** About 64 MB of textures at most. */
	private static final int MAX_TILES = 256;
	private static final int MAX_BAKING = 3;
	private static final int UPLOADS_PER_FRAME = 3;
	/** How often a changed tile may be redrawn, per detail level: busy areas don't keep the baker busy. */
	private static final long[] REBAKE_MILLIS = {150L, 500L, 2000L};
	private static final ExecutorService BAKER = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Map Baker");
		thread.setDaemon(true);
		thread.setPriority(Thread.NORM_PRIORITY - 1);
		return thread;
	});

	private final Map<Long, Tile> tiles = new LinkedHashMap<>(64, 0.75F, true);
	private final ConcurrentLinkedQueue<Baked> baked = new ConcurrentLinkedQueue<>();
	private int baking;
	/** Counts up whenever all tiles are dropped, so bakes started before that are thrown away. */
	private int generation;
	private long frame;

	private static final class Tile {
		final int level;
		final int tileX;
		final int tileZ;
		@Nullable DynamicTexture texture;
		@Nullable NativeImage image;
		boolean baking;
		boolean dirty = true;
		long bakedAt;
		long usedFrame;

		Tile(int level, int tileX, int tileZ) {
			this.level = level;
			this.tileX = tileX;
			this.tileZ = tileZ;
		}
	}

	private record Baked(Tile tile, int generation, int @Nullable [] pixels) {
	}

	private static long key(int level, int tileX, int tileZ) {
		return (long) level << 58 | ((long) tileX & 0x1FFFFFFFL) << 29 | (long) tileZ & 0x1FFFFFFFL;
	}

	/**
	 * The texture of a tile, or null while it's being drawn for the first time. Asks for the tile to be
	 * drawn, or redrawn when its chunks changed. Render thread only.
	 */
	public @Nullable DynamicTexture texture(MapWorld world, int level, int tileX, int tileZ) {
		Tile tile = tiles.computeIfAbsent(key(level, tileX, tileZ), key -> new Tile(level, tileX, tileZ));
		tile.usedFrame = frame;
		long now = System.currentTimeMillis();
		if (tile.dirty && !tile.baking && baking < MAX_BAKING
			&& (tile.texture == null || now - tile.bakedAt >= REBAKE_MILLIS[level])) {
			bake(world, tile);
		}
		return tile.texture;
	}

	private void bake(MapWorld world, Tile tile) {
		tile.baking = true;
		tile.dirty = false;
		baking++;
		int startedIn = generation;
		BAKER.execute(() -> {
			int[] pixels = null;
			try {
				pixels = MapTileBaker.bake(world, tile.level, tile.tileX, tile.tileZ);
			} catch (RuntimeException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils map couldn't draw a tile", exception);
			}
			baked.add(new Baked(tile, startedIn, pixels));
		});
	}

	/** Uploads finished tiles and frees unused ones. Call once a frame before drawing, on the render thread. */
	public void beginFrame() {
		frame++;
		for (int i = 0; i < UPLOADS_PER_FRAME; i++) {
			Baked done = baked.poll();
			if (done == null) {
				break;
			}
			baking--;
			Tile tile = done.tile();
			tile.baking = false;
			if (done.generation() != generation || done.pixels() == null) {
				continue;
			}
			upload(tile, done.pixels());
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
		for (int level = 0; level < MapTileBaker.LEVELS; level++) {
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
