package net.emutils.client.emutils.map;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.nio.file.Path;
import java.util.Locale;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * Keeps the map's picture of the world up to date (#212, #215): remembers which chunks the client has
 * loaded, samples them a few at a time on the client thread, nearest first, and again when their blocks
 * change, and tells the tiles showing them to redraw. What was sampled is saved per world and dimension
 * and comes back next time. Only works while a map is turned on.
 */
public final class MapManager {
	/** At most this long per tick is spent sampling chunks, so joining a world doesn't hitch. */
	private static final long SAMPLE_BUDGET_NANOS = 3_000_000L;
	/** How many of the nearest waiting chunks are picked per pass. */
	private static final int NEAREST_BATCH = 16;
	private static final int SAVE_EVERY_TICKS = 600;
	private static final int UNLOAD_EVERY_TICKS = 100;
	private static final int OVERVIEWS_EVERY_TICKS = 40;
	/** A region's overview is redrawn at most this often while you explore it. */
	private static final long OVERVIEW_MIN_MILLIS = 10_000L;

	private static final LongLinkedOpenHashSet LOADED = new LongLinkedOpenHashSet();
	private static final LongLinkedOpenHashSet PENDING = new LongLinkedOpenHashSet();
	private static final MapTiles TILES = new MapTiles();
	private static @Nullable ClientLevel loadedLevel;
	private static @Nullable MapWorld world;

	private MapManager() {
	}

	public static void register() {
		ClientChunkEvents.CHUNK_LOAD.register(MapManager::onChunkLoad);
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			if (level == loadedLevel) {
				LOADED.remove(chunk.getPos().pack());
			}
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			if (world != null) {
				world.close();
				world = null;
			}
			MapWorld.flush();
		});
	}

	/** True while something shows the map, so the world is sampled and saved. */
	public static boolean active() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && (config.minimap() || config.worldMap());
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
			for (MapRegion region : world.loadedRegions()) {
				region.overviewStale = true;
			}
		}
		if (world == null || world.level() != level) {
			if (world != null) {
				world.close();
			}
			TILES.clear();
			world = new MapWorld(level, folder(client, level));
			if (level == loadedLevel) {
				PENDING.addAll(LOADED);
			}
		}

		prepare(world, TILES);

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

		if (world.ticks % SAVE_EVERY_TICKS == 0) {
			world.saveChanged();
		}
	}

	/**
	 * Keeps a map's regions ready to draw, once a tick: makes the looks of blocks just read from disk, has
	 * the tiles of loaded regions drawn, redraws changed overviews and lets go of idle regions. The world map
	 * calls it for another dimension's map it shows.
	 */
	static void prepare(MapWorld world, MapTiles tiles) {
		prepareLoaded(world, tiles);
		// Tiles that missed these looks were left unfinished and are drawn again on their own.
		MapBlockLooks.makeRequested();
		world.ticks++;
		if (world.ticks % OVERVIEWS_EVERY_TICKS == 0) {
			redrawOverviews(world, tiles);
		}
		if (world.ticks % UNLOAD_EVERY_TICKS == 0) {
			world.unloadIdle();
		}
	}

	/** Makes the looks of blocks in regions just read from disk, and has their tiles drawn. */
	private static void prepareLoaded(MapWorld world, MapTiles tiles) {
		MapRegion region;
		while ((region = world.pollLoaded()) != null) {
			IntOpenHashSet states = new IntOpenHashSet();
			for (int i = 0; i < MapRegion.CHUNKS * MapRegion.CHUNKS; i++) {
				MapChunk chunk = region.chunk(i);
				if (chunk == null) {
					continue;
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					states.add(chunk.top(c));
					states.add(chunk.floor(c));
				}
			}
			states.forEach(id -> MapBlockLooks.ensure(Block.stateById(id), id));
			tiles.markRegionDirty(region.regionX, region.regionZ);
		}
		while ((region = world.pollOverviewRead()) != null) {
			tiles.markRegionDirty(region.regionX, region.regionZ);
		}
		while ((region = tiles.pollOverviewDone()) != null) {
			tiles.markRegionDirty(region.regionX, region.regionZ);
		}
	}

	/** Redraws the overviews of regions that changed, or were drawn with other resource packs. */
	private static void redrawOverviews(MapWorld world, MapTiles tiles) {
		long now = System.currentTimeMillis();
		if (now - world.lastOverviewRound < OVERVIEW_MIN_MILLIS) {
			return;
		}
		world.lastOverviewRound = now;
		int fingerprint = MapBlockLooks.fingerprint();
		for (MapRegion region : world.loadedRegions()) {
			if (region.loaded && region.count() > 0
				&& (region.overviewStale || region.overview != null && region.overviewFingerprint != fingerprint)) {
				tiles.bakeOverview(world, region, fingerprint);
			}
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

	/**
	 * Where the map of this dimension is saved: a folder per world or server, as waypoints tell them apart,
	 * and one per dimension in it. Null when the world can't be told apart, so nothing is saved.
	 */
	private static @Nullable Path folder(Minecraft client, ClientLevel level) {
		Path world = worldFolder(client);
		return world == null ? null : world.resolve(safeName(WaypointManager.dimensionId(level)));
	}

	/**
	 * The folder with the maps of every dimension of the world or server you are in, or null when there's
	 * none. Servers are told apart like waypoints do; singleplayer worlds by their save folder instead of
	 * their name, so two worlds called the same don't draw over each other's map.
	 */
	static @Nullable Path worldFolder(Minecraft client) {
		IntegratedServer server = client.getSingleplayerServer();
		if (server != null) {
			Path save = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
			if (save != null) {
				return EMUtilsPaths.mapsDir().resolve(safeName("singleplayer_" + save));
			}
		}
		String worldKey = WaypointManager.worldKey(client);
		return worldKey.isBlank() ? null : EMUtilsPaths.mapsDir().resolve(safeName(worldKey));
	}

	/** A file name for any text: letters, digits, dots and dashes stay, everything else becomes an underscore. */
	static String safeName(String text) {
		String name = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.\\-]", "_");
		// "." and ".." aren't folder names one can use.
		return name.replace(".", "").isEmpty() ? "_" + name : name;
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

	/** Saves the map and lets go of its memory while no map is shown or you left the world; it comes back when one is. */
	private static void drop() {
		if (world != null) {
			world.close();
			world = null;
			TILES.clear();
			PENDING.addAll(LOADED);
		}
	}

	/** For UI snapshot checks: saves the region you stand in and reads it back; empty when it matches. */
	public static String roundTripForSnapshot(Minecraft client) {
		if (world == null || client.player == null) {
			return "no map";
		}
		return world.roundTripForSnapshot(client.player.getBlockX() >> 9, client.player.getBlockZ() >> 9);
	}

	/** For UI snapshot checks: chunks in memory, chunks still waiting, tiles, explored regions known, and regions in memory. */
	public static int[] countsForSnapshot() {
		return new int[] {
			world == null ? 0 : world.chunkCount(),
			PENDING.size(),
			TILES.size(),
			world == null ? 0 : world.knownRegions().size(),
			world == null ? 0 : world.loadedCount()
		};
	}
}
