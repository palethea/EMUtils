package net.emutils.client.emutils.map;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
	/** Often, so regions read in only to draw far tiles are let go soon after. */
	private static final int UNLOAD_EVERY_TICKS = 20;
	private static final int OVERVIEWS_EVERY_TICKS = 10;
	/** A region's overview is redrawn at most this often while you explore it. */
	private static final long OVERVIEW_MIN_MILLIS = 10_000L;
	/**
	 * At most this many overviews are drawn at once, and only one while tiles on screen are waiting, so they
	 * don't wait behind overviews.
	 */
	private static final int MAX_OVERVIEWS_BAKING = 2;
	/** At most this many regions are read in at a time to redraw an overview that's missing or outdated. */
	private static final int REDRAW_LOADS = 4;
	/** At most this long per tick is spent making the looks of blocks just read from disk. */
	private static final long PREPARE_BUDGET_NANOS = 2_000_000L;

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
			world.packsChanged();
		}
		if (world == null || world.level() != level) {
			if (world != null) {
				world.close();
			}
			TILES.clear();
			world = new MapWorld(level, folder(client, level));
			world.importer = MapImporter.start(client, world, level.dimension());
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

		importSaved(world, TILES, deadline);
	}

	/**
	 * Samples chunks the importer read from a singleplayer world's files, with what's left of the tick's
	 * time. Chunks the client loaded meanwhile were sampled live and are newer, so those are skipped.
	 */
	static void importSaved(MapWorld world, MapTiles tiles, long deadline) {
		MapImporter importer = world.importer;
		if (importer == null) {
			return;
		}
		SavedChunk chunk;
		while (System.nanoTime() < deadline && (chunk = importer.poll()) != null) {
			MapChunk sampled;
			if (world.chunk(chunk.chunkX, chunk.chunkZ) == null && !(sampled = MapSampler.sample(chunk)).isEmpty()) {
				world.put(chunk.chunkX, chunk.chunkZ, sampled);
				tiles.markDirty(chunk.chunkX, chunk.chunkZ);
			}
		}
	}

	/**
	 * Keeps a map's regions ready to draw, once a tick: makes the looks of blocks just read from disk, has
	 * the tiles of loaded regions drawn, redraws changed overviews, saves and lets go of idle regions. The
	 * world map calls it for another dimension's map it shows.
	 */
	static void prepare(MapWorld world, MapTiles tiles) {
		prepareLoaded(world, tiles);
		// Tiles that missed these looks were left unfinished and are drawn again on their own.
		MapBlockLooks.makeRequested();
		world.ticks++;
		if (world.ticks % OVERVIEWS_EVERY_TICKS == 0) {
			redrawOverviews(world, tiles);
		}
		if (world.ticks % SAVE_EVERY_TICKS == 0) {
			world.saveChanged();
		}
		if (world.ticks % UNLOAD_EVERY_TICKS == 0) {
			world.unloadIdle();
		}
	}

	/**
	 * Makes the looks of blocks in regions just read from disk, within a budget so a burst of regions doesn't
	 * stall a frame, and has the far tiles redrawn for overviews just drawn. Tiles drawn while a region was
	 * still being read were left unfinished and are drawn again on their own; finished ones aren't touched,
	 * since reading a region in again changes nothing on them.
	 */
	private static void prepareLoaded(MapWorld world, MapTiles tiles) {
		long deadline = System.nanoTime() + PREPARE_BUDGET_NANOS;
		MapRegion region;
		while (System.nanoTime() < deadline && (region = world.preparing != null ? world.preparing : world.pollLoaded()) != null) {
			world.preparing = region;
			int[] states = region.states;
			while (states != null && world.preparingAt < states.length && System.nanoTime() < deadline) {
				int id = states[world.preparingAt++];
				MapBlockLooks.ensure(Block.stateById(id), id);
			}
			if (states != null && world.preparingAt < states.length) {
				break;
			}
			region.states = null;
			world.preparing = null;
			world.preparingAt = 0;
		}
		while ((region = tiles.pollOverviewDone()) != null) {
			tiles.markRegionDirty(region.regionX, region.regionZ, MapTileBaker.COLUMN_LEVELS);
		}
	}

	/**
	 * Redraws the overviews of regions that changed, have none, or were drawn with other resource packs, a few
	 * at a time, and reads in far regions whose saved overview needs the same.
	 */
	private static void redrawOverviews(MapWorld world, MapTiles tiles) {
		long now = System.currentTimeMillis();
		int fingerprint = MapBlockLooks.fingerprint();
		List<MapRegion> waiting = new ArrayList<>();
		for (MapRegion region : world.loadedRegions()) {
			// Not before the looks of its blocks are made, or it would come out empty.
			if (region.loaded && region.states == null && now - region.overviewBakedAt >= OVERVIEW_MIN_MILLIS && MapWorld.needsOverview(region)) {
				waiting.add(region);
			}
		}
		// Regions with no picture far away go first, then changed ones, then ones only drawn with other packs.
		waiting.sort(Comparator.comparingInt(region -> region.overview == null ? 0 : region.overviewStale ? 1 : 2));
		for (MapRegion region : waiting) {
			if (tiles.overviewsBaking() >= (tiles.busy() ? 1 : MAX_OVERVIEWS_BAKING)) {
				return;
			}
			tiles.bakeOverview(world, region, fingerprint);
		}
		world.loadRedraws(REDRAW_LOADS);
	}

	private static void sample(ClientLevel level, int chunkX, int chunkZ) {
		LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
		if (chunk == null || world == null) {
			return;
		}
		MapChunk sampled = MapSampler.sample(level, chunk, world.biomes());
		// An empty chunk shows nothing, and on servers whose worlds share a dimension (lobbies, SkyBlock
		// islands) it's often another world's void over this one's map, so it's neither kept nor saved.
		if (sampled.isEmpty()) {
			return;
		}
		world.put(chunkX, chunkZ, sampled);
		TILES.markDirty(chunkX, chunkZ);
	}

	/**
	 * Where the map of this dimension is saved: a folder per world or server, as waypoints tell them apart,
	 * and one per dimension in it. Null when the world can't be told apart, so nothing is saved.
	 */
	private static @Nullable Path folder(Minecraft client, ClientLevel level) {
		Path world = worldFolder(client);
		return world == null ? null : dimensionFolder(world, WaypointManager.dimensionId(level));
	}

	/** The folder with the map of one dimension, in a world's map folder. */
	static Path dimensionFolder(Path world, String dimension) {
		return named(world, dimension);
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
				return named(EMUtilsPaths.mapsDir(), "singleplayer_" + save);
			}
		}
		String worldKey = WaypointManager.worldKey(client);
		return worldKey.isBlank() ? null : named(EMUtilsPaths.mapsDir(), worldKey);
	}

	/**
	 * The folder for a text in {@code parent}. A map kept under the name earlier builds gave it is moved
	 * there, so it isn't lost.
	 */
	private static Path named(Path parent, String text) {
		Path folder = parent.resolve(safeName(text));
		Path legacy = parent.resolve(legacyName(text));
		if (!legacy.equals(folder) && !Files.exists(folder) && Files.isDirectory(legacy)) {
			try {
				Files.move(legacy, folder);
			} catch (IOException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils map couldn't move {} to {}", legacy, folder, exception);
			}
		}
		return folder;
	}

	/**
	 * A file name for any text: lowercase letters, digits, dots, dashes and underscores stay, everything else
	 * becomes an underscore. Different texts could then come out the same ("World 1" and "World_1"), so a
	 * name that had to change ends in a hash of the text.
	 */
	static String safeName(String text) {
		String name = legacyName(text);
		return name.equals(text) ? name : name + "-" + Integer.toHexString(text.hashCode());
	}

	/** The name earlier builds gave a folder, which could be the same for different texts. */
	private static String legacyName(String text) {
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

	/** For UI snapshot checks: has the importer look for new chunks in the world's files now. */
	public static void rescanForSnapshot() {
		if (world != null && world.importer != null) {
			world.importer.rescanNow();
		}
	}

	/** For UI snapshot checks: whether the map has a chunk. */
	public static boolean hasChunkForSnapshot(int chunkX, int chunkZ) {
		return world != null && world.chunk(chunkX, chunkZ) != null;
	}

	/** For UI snapshot checks: how many chunks the importer brought in from the world's files. */
	public static int importedForSnapshot() {
		return world == null || world.importer == null ? -1 : world.importer.importedCount();
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
