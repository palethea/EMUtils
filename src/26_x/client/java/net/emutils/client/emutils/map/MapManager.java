package net.emutils.client.emutils.map;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

/**
 * Keeps the map's picture of the world up to date (#212): remembers which chunks the client has loaded,
 * samples them a few at a time on the client thread, nearest first, and again when their blocks change,
 * and tells the tiles showing them to redraw. Only works while a map is turned on.
 */
public final class MapManager {
	/** At most this long per tick is spent sampling chunks, so joining a world doesn't hitch. */
	private static final long SAMPLE_BUDGET_NANOS = 3_000_000L;
	/** How many of the nearest waiting chunks are picked per pass. */
	private static final int NEAREST_BATCH = 16;
	private static final int FORGET_EVERY_TICKS = 200;

	private static final LongLinkedOpenHashSet LOADED = new LongLinkedOpenHashSet();
	private static final LongLinkedOpenHashSet PENDING = new LongLinkedOpenHashSet();
	private static final MapTiles TILES = new MapTiles();
	private static @Nullable ClientLevel loadedLevel;
	private static @Nullable MapWorld world;
	private static int ticks;

	private MapManager() {
	}

	public static void register() {
		ClientChunkEvents.CHUNK_LOAD.register(MapManager::onChunkLoad);
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			if (level == loadedLevel) {
				LOADED.remove(chunk.getPos().pack());
			}
		});
	}

	/** True while something shows the map, so the world is sampled. */
	public static boolean active() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.minimap();
	}

	public static MapTiles tiles() {
		return TILES;
	}

	/** The map of the level you are in, or null while no map is shown. */
	public static @Nullable MapWorld world() {
		return world;
	}

	private static void onChunkLoad(ClientLevel level, LevelChunk chunk) {
		if (level != loadedLevel) {
			// Chunks of a new level arrive before the next tick notices it; the old level's list is stale.
			LOADED.clear();
			PENDING.clear();
			loadedLevel = level;
		}
		long key = chunk.getPos().pack();
		LOADED.add(key);
		PENDING.add(key);
	}

	/** A block changed at these coordinates; its chunk is sampled again. Safe to call from any thread. */
	public static void onBlockChanged(int blockX, int blockZ) {
		Minecraft client = Minecraft.getInstance();
		if (!client.isSameThread()) {
			return;
		}
		long key = ChunkPos.pack(blockX >> 4, blockZ >> 4);
		if (LOADED.contains(key)) {
			PENDING.add(key);
		}
	}

	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null || client.player == null || !active()) {
			drop();
			return;
		}
		if (MapBlockLooks.refreshIfReloaded(client) && world != null) {
			// New resource packs: every block may look different, and see-through blocks may have changed.
			TILES.clear();
			PENDING.addAll(LOADED);
		}
		if (world == null || world.level() != level) {
			TILES.clear();
			world = new MapWorld(level);
			if (level == loadedLevel) {
				PENDING.addAll(LOADED);
			}
		}

		int playerChunkX = client.player.getBlockX() >> 4;
		int playerChunkZ = client.player.getBlockZ() >> 4;
		long deadline = System.nanoTime() + SAMPLE_BUDGET_NANOS;
		long[] batch = new long[NEAREST_BATCH];
		while (!PENDING.isEmpty() && System.nanoTime() < deadline) {
			int count = nearest(batch, playerChunkX, playerChunkZ);
			for (int i = 0; i < count; i++) {
				long key = batch[i];
				PENDING.remove(key);
				sample(level, ChunkPos.getX(key), ChunkPos.getZ(key));
			}
		}

		if (++ticks % FORGET_EVERY_TICKS == 0) {
			world.forgetFarFrom(playerChunkX, playerChunkZ);
		}
	}

	private static void sample(ClientLevel level, int chunkX, int chunkZ) {
		LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
		if (chunk == null || world == null) {
			return;
		}
		world.put(chunkX, chunkZ, MapSampler.sample(level, chunk, world.biomes()));
		TILES.markDirty(chunkX, chunkZ);
	}

	/** Fills {@code batch} with the waiting chunks nearest the player, and returns how many it found. */
	private static int nearest(long[] batch, int centerX, int centerZ) {
		int count = 0;
		int[] distances = new int[batch.length];
		LongIterator iterator = PENDING.iterator();
		while (iterator.hasNext()) {
			long key = iterator.nextLong();
			int distance = Math.max(Math.abs(ChunkPos.getX(key) - centerX), Math.abs(ChunkPos.getZ(key) - centerZ));
			if (count < batch.length) {
				batch[count] = key;
				distances[count] = distance;
				count++;
				continue;
			}
			int farthest = 0;
			for (int i = 1; i < count; i++) {
				if (distances[i] > distances[farthest]) {
					farthest = i;
				}
			}
			if (distance < distances[farthest]) {
				batch[farthest] = key;
				distances[farthest] = distance;
			}
		}
		return count;
	}

	/** Lets go of the map's memory while no map is shown; it is sampled again when one is. */
	private static void drop() {
		if (world != null) {
			world = null;
			TILES.clear();
			PENDING.addAll(LOADED);
		}
	}

	/** For UI snapshot checks: how many chunks are sampled and how many are still waiting. */
	public static int[] countsForSnapshot() {
		return new int[] {world == null ? 0 : world.size(), PENDING.size(), TILES.size()};
	}
}
