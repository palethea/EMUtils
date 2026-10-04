package net.emutils.client.emutils.map;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
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
 *
 * <p>On a server, the dimension's map is first kept in memory while the chunks around you tell which of the
 * server's worlds you're in (#219); then it's given that world's folder, or a new one. Underground, and
 * always in a dimension with a ceiling like the Nether, a second map of the cave layer at your height is kept
 * next to the surface's (#222), and the minimap shows that one.
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
	/** On a server, which world you're in is worked out once this many chunks with something in them are sampled... */
	private static final int RESOLVE_CHUNKS = 24;
	/** ...or after this many ticks, whichever comes first. */
	private static final int RESOLVE_TICKS = 100;
	/** Underground means the sky's light where your eyes are is at most this; you're out again from {@link #CAVE_LEAVE_SKY}. */
	private static final int CAVE_ENTER_SKY = 2;
	private static final int CAVE_LEAVE_SKY = 8;
	/** You stay on a cave layer until you're this many blocks past its edge, so walking along an edge doesn't flip it. */
	private static final int LAYER_SLACK = 2;
	/** Before the server says where its spawn is, the game puts it here. */
	private static final BlockPos NO_SPAWN = new BlockPos(8, 64, 8);

	private static final LongLinkedOpenHashSet LOADED = new LongLinkedOpenHashSet();
	private static final LongLinkedOpenHashSet PENDING = new LongLinkedOpenHashSet();
	/** Chunks the cave layer's map still has to sample, as it changes more often than the surface's. */
	private static final LongLinkedOpenHashSet CAVE_PENDING = new LongLinkedOpenHashSet();
	private static final MapTiles TILES = new MapTiles();
	private static final MapTiles CAVE_TILES = new MapTiles();
	private static @Nullable ClientLevel loadedLevel;
	private static @Nullable MapWorld world;
	private static @Nullable MapWorld cave;
	/** The worlds kept for the dimension you're in, or null when this world keeps no maps. */
	private static @Nullable MapWorlds worlds;
	/** The folder with the maps of the server or save you're on, and its dimensions' worlds, looked up once. */
	private static @Nullable Path serverFolder;
	private static final Map<String, MapWorlds> CATALOGS = new HashMap<>();
	/** Chunks with something in them sampled into a map whose world isn't known yet. */
	private static int pendingSamples;
	private static int pendingTicks;
	private static @Nullable Match match;

	/** Working out which world a map is, in the background (#219). */
	private static final class Match {
		final MapWorld forWorld;
		final int @Nullable [] spawn;
		volatile boolean done;
		volatile @Nullable String id;

		Match(MapWorld forWorld, int @Nullable [] spawn) {
			this.forWorld = forWorld;
			this.spawn = spawn;
		}
	}

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
			closeAll();
			MapWorld.flush();
		});
	}

	/** True while something shows the map, so the world is sampled and saved. */
	public static boolean active() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && (config.minimap() || config.worldMap());
	}

	/** The surface map's tiles. */
	public static MapTiles tiles() {
		return TILES;
	}

	/** The map of the surface of the level you are in, or null while no map is shown. */
	public static @Nullable MapWorld world() {
		return world;
	}

	/** The map of the cave layer you're in (#222), or null above ground. */
	static @Nullable MapWorld cave() {
		return cave;
	}

	static MapTiles caveTiles() {
		return CAVE_TILES;
	}

	/** The map the minimap shows: the cave layer's underground, the surface's otherwise. */
	public static @Nullable MapWorld shownWorld() {
		return cave != null ? cave : world;
	}

	public static MapTiles shownTiles() {
		return cave != null ? CAVE_TILES : TILES;
	}

	private static void onChunkLoad(ClientLevel level, LevelChunk chunk) {
		if (level != loadedLevel) {
			// Chunks of a new level arrive before the next tick notices it; the old level's list is stale.
			LOADED.clear();
			PENDING.clear();
			CAVE_PENDING.clear();
			loadedLevel = level;
		}
		long key = chunk.getPos().pack();
		LOADED.add(key);
		PENDING.add(key);
		if (cave != null) {
			CAVE_PENDING.add(key);
		}
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
			if (cave != null) {
				CAVE_PENDING.add(key);
			}
		}
	}

	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		LocalPlayer player = client.player;
		if (level == null || player == null || !active()) {
			drop();
			return;
		}
		if (MapBlockLooks.refreshIfReloaded(client)) {
			// New resource packs: every block may look different, and see-through blocks may have changed.
			for (MapWorld map : new MapWorld[] {world, cave}) {
				if (map != null) {
					for (MapRegion region : map.loadedRegions()) {
						region.overviewStale = true;
					}
					map.packsChanged();
				}
			}
			TILES.clear();
			CAVE_TILES.clear();
			PENDING.addAll(LOADED);
			CAVE_PENDING.addAll(LOADED);
		}
		if (world == null || world.level() != level) {
			openLevel(client, level);
		}
		MapWorld surface = world;
		resolve(surface, level);
		updateCave(level, player);

		prepare(surface, TILES);
		if (cave != null) {
			prepare(cave, CAVE_TILES);
		}

		int playerChunkX = player.getBlockX() >> 4;
		int playerChunkZ = player.getBlockZ() >> 4;
		long deadline = System.nanoTime() + SAMPLE_BUDGET_NANOS;
		long[] batch = new long[NEAREST_BATCH];
		while (!PENDING.isEmpty() && System.nanoTime() < deadline) {
			int count = nearest(PENDING, batch, playerChunkX, playerChunkZ);
			for (int i = 0; i < count; i++) {
				long key = batch[i];
				PENDING.remove(key);
				boolean caveToo = cave != null && CAVE_PENDING.remove(key);
				sample(level, ChunkPos.getX(key), ChunkPos.getZ(key), true, caveToo);
			}
		}
		while (cave != null && !CAVE_PENDING.isEmpty() && System.nanoTime() < deadline) {
			int count = nearest(CAVE_PENDING, batch, playerChunkX, playerChunkZ);
			for (int i = 0; i < count; i++) {
				long key = batch[i];
				CAVE_PENDING.remove(key);
				sample(level, ChunkPos.getX(key), ChunkPos.getZ(key), false, true);
			}
		}

		if (surface.importer != null) {
			surface.importer.center(player.getX(), player.getZ());
		}
		importSaved(surface, TILES, deadline);
	}

	/**
	 * Starts the map of a level you just entered. In singleplayer it's the save's map right away; on a
	 * server it's kept in memory until it's clear which world this is.
	 */
	private static void openLevel(Minecraft client, ClientLevel level) {
		closeAll();
		TILES.clear();
		CAVE_TILES.clear();
		pendingSamples = 0;
		pendingTicks = 0;
		match = null;
		Path server = serverFolder(client);
		String dimension = WaypointManager.dimensionId(level);
		worlds = server == null ? null : catalog(dimension);
		boolean ceiling = level.dimensionType().hasCeiling();
		if (worlds != null && client.getSingleplayerServer() != null) {
			// A save is one world.
			MapWorlds.Entry entry = worlds.latest() != null ? worlds.latest() : worlds.create();
			worlds.seen(entry, spawn(level), level.getMinY(), level.getHeight());
			world = new MapWorld(level, worlds.folder(entry.id()), dimension, level.getMinY(), ceiling, entry.id(), MapSampler.SURFACE);
			world.importer = MapImporter.start(client, world, level.dimension());
		} else {
			world = new MapWorld(level, null, dimension, level.getMinY(), ceiling, null, MapSampler.SURFACE);
		}
		if (level == loadedLevel) {
			PENDING.addAll(LOADED);
		}
	}

	/**
	 * On a server, works out which world a map kept in memory is (#219), once enough of it was sampled,
	 * and gives it that world's folder, or a new world's.
	 */
	private static void resolve(MapWorld surface, ClientLevel level) {
		if (!surface.pending() || worlds == null) {
			return;
		}
		pendingTicks++;
		Match current = match;
		if (current == null) {
			if (pendingSamples < RESOLVE_CHUNKS && pendingTicks < RESOLVE_TICKS) {
				return;
			}
			Match started = new Match(surface, spawn(level));
			match = started;
			List<MapWorldMatcher.Candidate> candidates = candidates(worlds);
			List<MapWorldMatcher.Sample> samples = surface.samples();
			int minY = level.getMinY();
			int height = level.getHeight();
			MapWorld.runIo(() -> {
				started.id = MapWorldMatcher.match(candidates, samples, started.spawn, minY, height, surface.biomes());
				started.done = true;
			});
			return;
		}
		if (!current.done || current.forWorld != surface) {
			return;
		}
		MapWorlds.Entry entry = worlds.get(current.id);
		if (entry == null) {
			entry = worlds.create();
		}
		worlds.seen(entry, current.spawn, level.getMinY(), level.getHeight());
		surface.attach(worlds.folder(entry.id()), entry.id());
		if (cave != null && cave.pending()) {
			cave.attach(worlds.caveFolder(entry.id(), cave.cave), entry.id());
		}
		EMUtilsClient.LOGGER.info("EMUtils map: {} in {} is {}", serverFolder == null ? "?" : serverFolder.getFileName(), worlds.dimension(), entry.name());
		match = null;
	}

	private static List<MapWorldMatcher.Candidate> candidates(MapWorlds catalog) {
		List<MapWorldMatcher.Candidate> candidates = new ArrayList<>();
		for (MapWorlds.Entry entry : catalog.worlds()) {
			candidates.add(new MapWorldMatcher.Candidate(entry.id, catalog.folder(entry.id), entry.spawn, entry.minY, entry.height, entry.lastSeen));
		}
		return candidates;
	}

	/** Where the server says its spawn is, or null before it said. */
	private static int @Nullable [] spawn(ClientLevel level) {
		BlockPos pos = level.getRespawnData().pos();
		return pos.equals(NO_SPAWN) ? null : new int[] {pos.getX(), pos.getY(), pos.getZ()};
	}

	/**
	 * Keeps the map of the cave layer at your height (#222): underground, or always in a dimension with a
	 * ceiling, when the cave view is on; none otherwise.
	 */
	private static void updateCave(ClientLevel level, LocalPlayer player) {
		int wanted = wantedLayer(level, player);
		int current = cave == null ? MapSampler.SURFACE : cave.cave;
		if (wanted == current) {
			return;
		}
		if (cave != null) {
			cave.close();
			cave = null;
		}
		CAVE_TILES.clear();
		CAVE_PENDING.clear();
		if (wanted == MapSampler.SURFACE || world == null) {
			return;
		}
		String id = world.worldId;
		Path folder = worlds == null || id == null ? null : worlds.caveFolder(id, wanted);
		cave = new MapWorld(level, folder, world.dimension(), level.getMinY(), level.dimensionType().hasCeiling(), id, wanted);
		CAVE_PENDING.addAll(LOADED);
	}

	/** The cave layer you're in, or {@link MapSampler#SURFACE} above ground or with the cave view off. */
	private static int wantedLayer(ClientLevel level, LocalPlayer player) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.mapCaves()) {
			return MapSampler.SURFACE;
		}
		int current = cave == null ? MapSampler.SURFACE : cave.cave;
		if (!level.dimensionType().hasCeiling()) {
			int sky = level.getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition()));
			if (current == MapSampler.SURFACE ? sky > CAVE_ENTER_SKY : sky >= CAVE_LEAVE_SKY) {
				return MapSampler.SURFACE;
			}
		}
		// A little above your feet, so a layer starts just over your head in a cave of the usual height.
		int y = player.getBlockY() + 2;
		if (current != MapSampler.SURFACE && y >= current * MapWorld.LAYER_BLOCKS - LAYER_SLACK && y < (current + 1) * MapWorld.LAYER_BLOCKS + LAYER_SLACK) {
			return current;
		}
		return Math.floorDiv(y, MapWorld.LAYER_BLOCKS);
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
			if (world.chunk(chunk.chunkX, chunk.chunkZ) == null) {
				sampled = MapSampler.sample(chunk, world.startY());
				world.put(chunk.chunkX, chunk.chunkZ, sampled);
				tiles.markDirty(chunk.chunkX, chunk.chunkZ);
			}
		}
	}

	/**
	 * Keeps a map's regions ready to draw, once a tick: makes the looks of blocks just read from disk, has
	 * the tiles of loaded regions drawn, redraws changed overviews, saves and lets go of idle regions. The
	 * world map calls it for another map it shows.
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
	 * since reading a region in again changes nothing on them, unless saved chunks were read in under ones
	 * sampled before the map knew its world.
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
			if (region.merged) {
				region.merged = false;
				tiles.markRegionDirty(region.regionX, region.regionZ, 0);
			}
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

	/** Samples a loaded chunk into the surface's map and, with {@code caveToo}, the cave layer's. */
	private static void sample(ClientLevel level, int chunkX, int chunkZ, boolean surface, boolean caveToo) {
		LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
		if (chunk == null) {
			return;
		}
		if (surface && world != null && sampleInto(world, TILES, level, chunk, chunkX, chunkZ) && world.pending()) {
			pendingSamples++;
		}
		if (caveToo && cave != null) {
			sampleInto(cave, CAVE_TILES, level, chunk, chunkX, chunkZ);
		}
	}

	/** Samples a chunk into a map; returns whether it had something in it. */
	private static boolean sampleInto(MapWorld map, MapTiles tiles, ClientLevel level, LevelChunk chunk, int chunkX, int chunkZ) {
		MapChunk sampled = MapSampler.sample(level, chunk, map.biomes(), map.startY());
		// An empty chunk is kept as void (#220). On a server it doesn't replace what the map has there, as it
		// may be another world's void that the map took for this one; in singleplayer it really was emptied.
		if (sampled.isEmpty() && map.chunk(chunkX, chunkZ) != null && Minecraft.getInstance().getSingleplayerServer() == null) {
			return false;
		}
		map.put(chunkX, chunkZ, sampled);
		tiles.markDirty(chunkX, chunkZ);
		return !sampled.isEmpty();
	}

	/**
	 * The worlds kept for a dimension of the server or save you're on (#219), or null when it keeps no maps.
	 * Client thread.
	 */
	static @Nullable MapWorlds worlds(String dimension) {
		return serverFolder == null ? null : catalog(dimension);
	}

	/** The folder with the maps of the server or save you're on, as last looked up, or null. */
	static @Nullable Path serverFolder() {
		return serverFolder;
	}

	private static MapWorlds catalog(String dimension) {
		return CATALOGS.computeIfAbsent(dimension, id -> MapWorlds.of(dimensionFolder(serverFolder, id), id));
	}

	/**
	 * Which world of a dimension the map is in (#219): yours for your dimension, else the one seen last; null
	 * in singleplayer, where a save is one world, or while it isn't known.
	 */
	public static @Nullable String worldIdFor(Minecraft client, String dimension) {
		if (client.getSingleplayerServer() != null || client.level == null || !active()) {
			return null;
		}
		if (world != null && world.dimension().equals(dimension)) {
			return world.worldId;
		}
		MapWorlds catalog = worlds(dimension);
		MapWorlds.Entry latest = catalog == null ? null : catalog.latest();
		return latest == null ? null : latest.id();
	}

	/**
	 * From now on, maps where you are as {@code id}, or as a new world when it's null (#219): for when the map
	 * took you for being in another world than you are.
	 */
	static void useWorld(@Nullable String id) {
		MapWorld surface = world;
		if (surface == null || worlds == null) {
			return;
		}
		ClientLevel level = surface.level();
		MapWorlds.Entry entry = id == null ? null : worlds.get(id);
		if (entry == null) {
			entry = worlds.create();
		}
		closeAll();
		TILES.clear();
		CAVE_TILES.clear();
		match = null;
		worlds.seen(entry, spawn(level), level.getMinY(), level.getHeight());
		world = new MapWorld(level, worlds.folder(entry.id()), surface.dimension(), level.getMinY(), level.dimensionType().hasCeiling(), entry.id(), MapSampler.SURFACE);
		Minecraft client = Minecraft.getInstance();
		world.importer = MapImporter.start(client, world, level.dimension());
		PENDING.addAll(LOADED);
	}

	/**
	 * The folder with the maps of the server or save you're on, or null when there's none. Servers are told
	 * apart like waypoints do; singleplayer worlds by their save folder instead of their name, so two worlds
	 * called the same don't draw over each other's map. Looked up once per server or save.
	 */
	static @Nullable Path serverFolder(Minecraft client) {
		Path folder = worldFolder(client);
		if (folder == null ? serverFolder != null : !folder.equals(serverFolder)) {
			serverFolder = folder;
			CATALOGS.clear();
		}
		return serverFolder;
	}

	private static @Nullable Path worldFolder(Minecraft client) {
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

	/** The folder with the maps of one dimension, in a server's or save's map folder. */
	static Path dimensionFolder(Path server, String dimension) {
		return named(server, dimension);
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
	private static int nearest(LongLinkedOpenHashSet pending, long[] batch, int centerX, int centerZ) {
		int count = 0;
		int[] distances = new int[batch.length];
		LongIterator iterator = pending.iterator();
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

	private static void closeAll() {
		if (world != null) {
			world.close();
			world = null;
		}
		if (cave != null) {
			cave.close();
			cave = null;
		}
	}

	/** Saves the map and lets go of its memory while no map is shown or you left the world; it comes back when one is. */
	private static void drop() {
		if (world != null || cave != null) {
			closeAll();
			TILES.clear();
			CAVE_TILES.clear();
			PENDING.addAll(LOADED);
			CAVE_PENDING.clear();
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

	/** For UI snapshot checks: the cave layer the minimap shows, or null above ground. */
	public static @Nullable Integer caveLayerForSnapshot() {
		return cave == null ? null : cave.cave;
	}

	/** For UI snapshot checks: whether the cave layer's map sampled the chunk you're in, with a floor under you. */
	public static String caveColumnForSnapshot(Minecraft client) {
		if (cave == null || client.player == null) {
			return "no cave map";
		}
		MapChunk chunk = cave.chunk(client.player.getBlockX() >> 4, client.player.getBlockZ() >> 4);
		if (chunk == null) {
			return "the chunk you're in isn't sampled";
		}
		int c = MapChunk.index(client.player.getBlockX() & 15, client.player.getBlockZ() & 15);
		return chunk.top(c) == MapChunk.NONE ? "no floor" : "floor at " + chunk.topY(c);
	}

	/** For UI snapshot checks: the world the map took this for, by name, or null when it has none. */
	public static @Nullable String worldNameForSnapshot() {
		if (world == null || worlds == null || world.worldId == null) {
			return null;
		}
		MapWorlds.Entry entry = worlds.get(world.worldId);
		return entry == null ? null : entry.name();
	}

	/** For UI snapshot checks: how many worlds are kept for the dimension you're in. */
	public static int worldCountForSnapshot() {
		return worlds == null ? 0 : worlds.worlds().size();
	}

	/**
	 * For UI snapshot checks: which saved world the chunks sampled now match, worked out right away, by name,
	 * or null when they'd make a new one.
	 */
	public static @Nullable String matchForSnapshot(Minecraft client) {
		if (world == null || worlds == null || client.level == null) {
			return null;
		}
		String id = MapWorldMatcher.match(candidates(worlds), world.samples(), spawn(client.level), client.level.getMinY(), client.level.getHeight(), world.biomes());
		MapWorlds.Entry entry = worlds.get(id);
		return entry == null ? null : entry.name();
	}

	/** For UI snapshot checks: saves what changed and waits until it's written. */
	public static void saveForSnapshot() {
		if (world != null) {
			world.saveChanged();
		}
		MapWorld.flush();
	}

	/** For UI snapshot checks: deletes the world of that name and its map. */
	public static void deleteWorldForSnapshot(String name) {
		if (worlds == null) {
			return;
		}
		for (MapWorlds.Entry entry : worlds.worlds()) {
			if (entry.name().equals(name)) {
				worlds.delete(entry.id());
			}
		}
	}

	/** For UI snapshot checks: maps where you are as a new world, or as the world of that name. */
	public static void useWorldForSnapshot(@Nullable String name) {
		String id = null;
		if (name != null && worlds != null) {
			for (MapWorlds.Entry entry : worlds.worlds()) {
				if (entry.name().equals(name)) {
					id = entry.id();
				}
			}
		}
		useWorld(id);
	}

	/**
	 * For UI snapshot checks: samples made-up columns with the surface, Nether roof and cave layer rules
	 * (#221, #222) and says what differs from what they should give, or returns an empty text.
	 */
	public static String samplingRulesForSnapshot() {
		return MapSampler.checkRules();
	}
}
