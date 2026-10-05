package net.emutils.client.emutils.map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * One map (#212, #215): of one world (#219) in one dimension, at the surface or one cave layer (#222). Its
 * regions are loaded from disk as the map needs them and saved back when they change. Lives as long as you
 * are there. Chunks and overviews are read by the tile baker from its own thread, and regions load and save
 * on the map's IO thread. On a server, which world you're in is only known after a few chunks, so the map
 * starts without a folder, in memory, and is given one when it's known ({@link #attach}).
 */
public final class MapWorld {
	/** Regions unused for this long are let go once saved. */
	private static final long UNLOAD_AFTER_MILLIS = 45_000L;
	/** At most this many regions keep their chunks in memory; the least used go first. */
	static final int MAX_LOADED_REGIONS = 48;
	/**
	 * At most this many overviews of regions without their chunks stay in memory, about 64 MB; the least
	 * used go first and are read from their files again when needed.
	 */
	private static final int MAX_OVERVIEWS = 1024;
	static final String DIMENSION_FILE = "dimension.txt";
	/** How many blocks tall a cave layer is (#222). */
	static final int LAYER_BLOCKS = 16;
	private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Map IO");
		thread.setDaemon(true);
		return thread;
	});
	/**
	 * Reads overviews for the far zoom levels, apart from {@link #IO} so they don't wait behind saves and whole
	 * regions, with as many threads as the Loading Speed setting says (#239). Only reads: whole regions are
	 * read in on IO, in order with their saves.
	 */
	private static final ThreadPoolExecutor OVERVIEW_READS = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Map Overview Reader");
		thread.setDaemon(true);
		return thread;
	});

	private final ClientLevel level;
	private final String dimension;
	private final int minY;
	/** The dimension has a ceiling, like the Nether's roof, which old files show instead of the ground (#221). */
	private final boolean ceiling;
	/** The cave layer this map shows, or {@link MapSampler#SURFACE}. */
	final int cave;
	private final Registry<Biome> biomes;
	private volatile @Nullable Path folder;
	/** Which of the server's or save's worlds this is (#219), or null while that isn't known yet. */
	volatile @Nullable String worldId;
	private final ConcurrentHashMap<Long, MapRegion> regions = new ConcurrentHashMap<>();
	/** Overviews of regions whose chunks aren't loaded, read from their files for the far zoom levels. */
	private final ConcurrentHashMap<Long, MapRegion> overviews = new ConcurrentHashMap<>();
	/** Every region with a file or with chunks, so the world map knows what was explored. */
	private final Set<Long> known = ConcurrentHashMap.newKeySet();
	private final ConcurrentLinkedQueue<MapRegion> justLoaded = new ConcurrentLinkedQueue<>();
	/** Regions whose saved overview is missing or from other resource packs, to be read in full and drawn again. */
	private final Set<Long> redraws = ConcurrentHashMap.newKeySet();
	/** Regions already asked for again since the last resource pack change, so none is asked for twice. */
	private final Set<Long> redrawn = ConcurrentHashMap.newKeySet();
	/** Regions read in by {@link #loadRedraws} whose overview isn't redrawn yet. Client thread only. */
	private final Set<Long> redrawing = new HashSet<>();
	private volatile boolean closed;
	/** Brings in the chunks a singleplayer world generated away from you, or null elsewhere. */
	@Nullable MapImporter importer;
	/** When this map's overviews were last looked over for redrawing. */
	int ticks;
	/** The region whose block looks the client thread is making, and how far it got. */
	@Nullable MapRegion preparing;
	int preparingAt;

	/**
	 * A map, which may be of another dimension or world than yours, as the world map shows them. Its bottom
	 * is needed to save its heights; without one ({@link Integer#MIN_VALUE}) the map is only read, never
	 * saved. Without a folder it's kept in memory until {@link #attach} gives it one.
	 */
	MapWorld(ClientLevel level, @Nullable Path folder, String dimension, int minY, boolean ceiling, @Nullable String worldId, int cave) {
		this.level = level;
		this.dimension = dimension;
		this.minY = minY;
		this.ceiling = ceiling;
		this.worldId = worldId;
		this.cave = cave;
		this.biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		this.folder = folder;
		if (folder != null) {
			IO.execute(this::listKnownRegions);
		}
	}

	String dimension() {
		return dimension;
	}

	/** The folder the map is saved in, or null while it's kept in memory. */
	@Nullable Path folder() {
		return folder;
	}

	/** Whether the dimension has a ceiling, like the Nether. */
	boolean ceiling() {
		return ceiling;
	}

	/** Where sampling starts: {@link MapSampler#SURFACE}, or the top of the cave layer. */
	int startY() {
		return cave == MapSampler.SURFACE ? MapSampler.SURFACE : cave * LAYER_BLOCKS + LAYER_BLOCKS - 1;
	}

	/** Which world this is isn't known yet, so nothing is saved. */
	boolean pending() {
		return worldId == null;
	}

	/**
	 * Gives a map kept in memory its world and folder (#219). What it sampled meanwhile stays, the saved
	 * chunks fill in around it, and from now on it's saved like any other.
	 */
	void attach(Path folder, String worldId) {
		if (this.folder != null || closed) {
			return;
		}
		this.folder = folder;
		this.worldId = worldId;
		IO.execute(this::listKnownRegions);
		for (MapRegion region : regions.values()) {
			IO.execute(() -> merge(region));
		}
	}

	/**
	 * Reads a region's file under what was sampled before the map had a folder. IO thread. Also once the map
	 * is closed: closing saves the region after this, and without the file read in that save would keep only
	 * what was sampled since you arrived.
	 */
	private void merge(MapRegion region) {
		try {
			MapRegionFile.Contents contents = read(region);
			if (contents != null && contents.chunks() != null) {
				IntOpenHashSet states = new IntOpenHashSet();
				MapChunk[] chunks = contents.chunks();
				for (int i = 0; i < chunks.length; i++) {
					if (chunks[i] != null) {
						region.putLoaded(i, chunks[i]);
						for (int c = 0; c < MapChunk.AREA; c++) {
							states.add(chunks[i].top(c));
							states.add(chunks[i].floor(c));
						}
					}
				}
				region.states = states.toIntArray();
				region.overviewStale = true;
				region.dirty = true;
				region.merged = true;
			}
		} catch (IOException | RuntimeException exception) {
			region.unreadable = true;
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't read region {}, {}; it won't be saved over", region.regionX, region.regionZ, exception);
		}
		justLoaded.add(region);
	}

	/**
	 * A region's file, with its chunks and overview left out when they were saved by an older build that
	 * drew a ceiling's roof instead of the ground under it (#221).
	 */
	private MapRegionFile.@Nullable Contents read(MapRegion region) throws IOException {
		return MapRegionFile.readFor(MapRegionFile.path(folder, region.regionX, region.regionZ), biomes, ceiling);
	}

	public ClientLevel level() {
		return level;
	}

	public Registry<Biome> biomes() {
		return biomes;
	}

	/** The sampled or saved chunk, or null when there is none or its region is still loading. Safe from any thread. */
	public @Nullable MapChunk chunk(int chunkX, int chunkZ) {
		MapRegion region = region(chunkX >> MapRegion.SHIFT, chunkZ >> MapRegion.SHIFT);
		region.lastUsed = System.currentTimeMillis();
		return region.chunk(MapRegion.index(chunkX, chunkZ));
	}

	/** The chunk if its region is in memory already, without reading the region in; null otherwise. Any thread. */
	@Nullable MapChunk loadedChunk(int chunkX, int chunkZ) {
		MapRegion region = regions.get(MapRegion.key(chunkX >> MapRegion.SHIFT, chunkZ >> MapRegion.SHIFT));
		return region == null || !region.loaded ? null : region.chunk(MapRegion.index(chunkX, chunkZ));
	}

	/** True when the region of this chunk is still being read from disk, so a missing chunk may still come. */
	boolean loading(int chunkX, int chunkZ) {
		MapRegion region = regions.get(MapRegion.key(chunkX >> MapRegion.SHIFT, chunkZ >> MapRegion.SHIFT));
		return region == null || !region.loaded;
	}

	void put(int chunkX, int chunkZ, MapChunk chunk) {
		MapRegion region = region(chunkX >> MapRegion.SHIFT, chunkZ >> MapRegion.SHIFT);
		region.put(MapRegion.index(chunkX, chunkZ), chunk);
		region.lastUsed = System.currentTimeMillis();
		known.add(MapRegion.key(region.regionX, region.regionZ));
	}

	/** The region, made and queued to load from disk if it isn't in memory. */
	MapRegion region(int regionX, int regionZ) {
		long key = MapRegion.key(regionX, regionZ);
		MapRegion region = regions.get(key);
		if (region != null) {
			return region;
		}
		MapRegion created = new MapRegion(regionX, regionZ);
		region = regions.putIfAbsent(key, created);
		if (region != null) {
			return region;
		}
		MapRegion overviewOnly = overviews.remove(key);
		if (overviewOnly != null && overviewOnly.overview != null) {
			created.overview = overviewOnly.overview;
			created.overviewFingerprint = overviewOnly.overviewFingerprint;
		}
		if (folder == null) {
			created.loaded = true;
		} else {
			IO.execute(() -> load(created));
		}
		return created;
	}

	private void load(MapRegion region) {
		// Once closed, nothing waits for it; close() reads in the regions that hold new samples itself.
		if (closed) {
			return;
		}
		readInto(region);
		justLoaded.add(region);
	}

	/** Reads a region's file into it, keeping what was sampled meanwhile, and marks it loaded. IO thread. */
	private void readInto(MapRegion region) {
		if (region.loaded) {
			return;
		}
		try {
			MapRegionFile.Contents contents = read(region);
			if (contents != null) {
				MapChunk[] chunks = contents.chunks();
				IntOpenHashSet states = new IntOpenHashSet();
				for (int i = 0; chunks != null && i < chunks.length; i++) {
					if (chunks[i] != null) {
						region.putLoaded(i, chunks[i]);
						for (int c = 0; c < MapChunk.AREA; c++) {
							states.add(chunks[i].top(c));
							states.add(chunks[i].floor(c));
						}
					}
				}
				// Gathered here, so the client thread only makes the looks, a few at a time.
				region.states = states.toIntArray();
				if (region.overview == null && contents.overview() != null) {
					region.overview = contents.overview();
					region.overviewFingerprint = contents.overviewFingerprint();
				}
				// The saved overview is right for the saved chunks unless something newer was sampled.
				region.overviewStale = region.dirty || contents.overview() == null;
			}
		} catch (IOException | RuntimeException exception) {
			region.unreadable = true;
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't read region {}, {}; it won't be saved over", region.regionX, region.regionZ, exception);
		}
		region.loaded = true;
	}

	/**
	 * The overview of a region for the far zoom levels: from memory, or read from its file in the background
	 * (null until then). Safe from any thread.
	 */
	@Nullable MapRegion overview(int regionX, int regionZ) {
		long key = MapRegion.key(regionX, regionZ);
		MapRegion region = regions.get(key);
		if (region != null) {
			return region;
		}
		if (folder == null || !known.contains(key)) {
			return null;
		}
		MapRegion overviewOnly = overviews.get(key);
		if (overviewOnly != null) {
			overviewOnly.lastUsed = System.currentTimeMillis();
			return overviewOnly;
		}
		MapRegion created = new MapRegion(regionX, regionZ);
		if (overviews.putIfAbsent(key, created) == null) {
			OVERVIEW_READS.execute(() -> {
				try {
					MapRegionFile.Contents contents = MapRegionFile.readOverview(MapRegionFile.path(folder, regionX, regionZ));
					if (contents != null && contents.overview() != null && !(ceiling && contents.version() < 2)) {
						created.overview = contents.overview();
						created.overviewFingerprint = contents.overviewFingerprint();
					}
				} catch (IOException | RuntimeException exception) {
					EMUtilsClient.LOGGER.warn("EMUtils map couldn't read the overview of region {}, {}", regionX, regionZ, exception);
				}
				created.loaded = true;
			});
		}
		return created;
	}

	/**
	 * Asks for a region's overview to be drawn again from its chunks, because the saved one is missing or was
	 * drawn with other resource packs. Each region is asked for once per pack change. Safe from any thread.
	 */
	void requestRedraw(int regionX, int regionZ) {
		long key = MapRegion.key(regionX, regionZ);
		if (redrawn.add(key)) {
			redraws.add(key);
		}
	}

	/** Whether a region has chunks but no overview that's up to date with them and the resource packs. */
	static boolean needsOverview(MapRegion region) {
		return region.count() > 0
			&& (region.overviewStale || region.overview == null || region.overviewFingerprint != MapBlockLooks.fingerprint());
	}

	/**
	 * Reads in regions asked for by {@link #requestRedraw}; their overviews are then redrawn. At most
	 * {@code max} are waiting for that at a time, on top of the regions the map keeps anyway, so they come in
	 * only as fast as they're drawn.
	 */
	void loadRedraws(int max) {
		// The regions around you are redrawn all the time as you explore; only those read in here count.
		redrawing.removeIf(key -> {
			MapRegion region = regions.get(key);
			if (region != null && region.loaded && !needsOverview(region)) {
				// Saved right away, so it can be let go instead of waiting for the next save.
				saveNow(region);
				return true;
			}
			return region == null;
		});
		max -= redrawing.size();
		Iterator<Long> iterator = redraws.iterator();
		while (max > 0 && iterator.hasNext()) {
			long key = iterator.next();
			iterator.remove();
			if (!regions.containsKey(key)) {
				region((int) (key >> 32), (int) key);
				redrawing.add(key);
				max--;
			}
		}
	}

	/** New resource packs: every region may be asked for again. */
	void packsChanged() {
		redrawn.clear();
		redraws.clear();
		redrawing.clear();
	}

	/**
	 * Which chunks of a region the map has (#228), as a mask of {@link MapRegion#index} bits: from memory when
	 * the region is in, or else from the mask in its file, without reading the region in. Any thread.
	 */
	long[] chunksIn(int regionX, int regionZ) {
		long[] present = new long[MapRegion.CHUNKS * MapRegion.CHUNKS / 64];
		MapRegion region = regions.get(MapRegion.key(regionX, regionZ));
		Path where = folder;
		if (region == null || !region.loaded) {
			if (where != null) {
				try {
					long[] saved = MapRegionFile.readPresence(MapRegionFile.path(where, regionX, regionZ), ceiling);
					if (saved != null) {
						System.arraycopy(saved, 0, present, 0, present.length);
					}
				} catch (IOException | RuntimeException exception) {
					EMUtilsClient.LOGGER.warn("EMUtils map couldn't read which chunks region {}, {} has", regionX, regionZ, exception);
				}
			}
		}
		if (region != null) {
			// What was sampled since, and everything once it's read in.
			for (int i = 0; i < MapRegion.CHUNKS * MapRegion.CHUNKS; i++) {
				if (region.chunk(i) != null) {
					present[i >> 6] |= 1L << (i & 63);
				}
			}
		}
		return present;
	}

	/**
	 * For UI snapshot checks (#228): whether the chunk mask read from a region's file, without reading the
	 * region in, matches the chunks it has in memory; says what differs, or returns an empty text.
	 */
	String presenceForSnapshot(int regionX, int regionZ) {
		MapRegion region = regions.get(MapRegion.key(regionX, regionZ));
		if (folder == null || region == null || !region.loaded) {
			return "region " + regionX + ", " + regionZ + " isn't loaded";
		}
		try {
			long[] saved = MapRegionFile.readPresence(MapRegionFile.path(folder, regionX, regionZ), ceiling);
			if (saved == null) {
				return "the region has no file";
			}
			for (int i = 0; i < MapRegion.CHUNKS * MapRegion.CHUNKS; i++) {
				boolean inFile = (saved[i >> 6] & 1L << (i & 63)) != 0;
				if (inFile != (region.chunk(i) != null)) {
					return "chunk " + i + " is " + (inFile ? "in the file but not in memory" : "in memory but not in the file");
				}
			}
			return "";
		} catch (IOException | RuntimeException exception) {
			return "reading the mask failed: " + exception;
		}
	}

	/** Follows the Loading Speed setting (#239): how many threads read overviews. */
	static void applySpeed(MapLoadSpeed speed) {
		int threads = speed.overviewReaders();
		if (threads > OVERVIEW_READS.getMaximumPoolSize()) {
			OVERVIEW_READS.setMaximumPoolSize(threads);
			OVERVIEW_READS.setCorePoolSize(threads);
		} else if (threads < OVERVIEW_READS.getMaximumPoolSize()) {
			OVERVIEW_READS.setCorePoolSize(threads);
			OVERVIEW_READS.setMaximumPoolSize(threads);
		}
	}

	/** For UI snapshot checks: regions waiting to be read in or drawn only for their overviews. */
	int redrawsForSnapshot() {
		return redraws.size() + redrawing.size();
	}

	/** For UI snapshot checks: waits for everything queued to read or save to be done. */
	static void awaitIoForSnapshot() {
		try {
			IO.submit(() -> {
			}).get(60L, java.util.concurrent.TimeUnit.SECONDS);
		} catch (Exception exception) {
			EMUtilsClient.LOGGER.warn("EMUtils UI snapshot gave up waiting for the map's IO", exception);
		}
	}

	/** For UI snapshot checks: regions in memory, and of those, the ones whose overview is still to be drawn. */
	int[] regionsForSnapshot() {
		int loaded = 0;
		int needing = 0;
		for (MapRegion region : regions.values()) {
			if (region.loaded) {
				loaded++;
				if (needsOverview(region)) {
					needing++;
				}
			}
		}
		return new int[] {loaded, needing, known.size()};
	}

	/** For UI snapshot checks: looks for region files again, as when the map opens. */
	void relistForSnapshot() {
		IO.execute(this::listKnownRegions);
	}

	/** Some regions or overviews are still being read from disk. */
	boolean busy() {
		for (MapRegion region : regions.values()) {
			if (!region.loaded) {
				return true;
			}
		}
		for (MapRegion region : overviews.values()) {
			if (!region.loaded) {
				return true;
			}
		}
		return false;
	}

	/** Regions read from disk since the last call, for the client thread to prepare. */
	@Nullable MapRegion pollLoaded() {
		return justLoaded.poll();
	}


	boolean known(int regionX, int regionZ) {
		return known.contains(MapRegion.key(regionX, regionZ));
	}

	/** Every explored region, as {@link MapRegion#key} values. */
	Set<Long> knownRegions() {
		return known;
	}

	List<MapRegion> loadedRegions() {
		return new ArrayList<>(regions.values());
	}

	/** The map can be saved: it has a folder, and its bottom is known. */
	private boolean writable() {
		return folder != null && minY != Integer.MIN_VALUE;
	}

	/** Saves the regions that changed, in the background. */
	void saveChanged() {
		if (!writable()) {
			return;
		}
		for (MapRegion region : regions.values()) {
			if (region.dirty && region.loaded) {
				region.dirty = false;
				if (region.unreadable) {
					continue;
				}
				// Regions the map only looked into, with nothing explored, get no file.
				if (region.count() > 0) {
					IO.execute(() -> save(region));
				}
			}
		}
	}

	/** Saves one region now, in the background, if it changed. */
	private void saveNow(MapRegion region) {
		if (writable() && region.dirty && region.loaded) {
			region.dirty = false;
			if (region.count() > 0 && !region.unreadable) {
				IO.execute(() -> save(region));
			}
		}
	}

	private void save(MapRegion region) {
		try {
			MapRegionFile.write(MapRegionFile.path(folder, region.regionX, region.regionZ), region, biomes, minY);
		} catch (IOException | RuntimeException exception) {
			region.dirty = true;
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't save region {}, {}", region.regionX, region.regionZ, exception);
		}
	}

	/** The dimension id the map wrote in a folder, or null when there's none. */
	static @Nullable String dimensionOf(Path folder) {
		try {
			Path file = folder.resolve(DIMENSION_FILE);
			return Files.isRegularFile(file) ? Files.readString(file).trim() : null;
		} catch (IOException exception) {
			return null;
		}
	}

	/**
	 * Lets go of regions that were saved and haven't been used for a while, and the least used past the limit.
	 * A map that's only read has nothing to save, so its regions go whether they changed or not. Overviews
	 * past their limit go too, least used first.
	 */
	void unloadIdle() {
		long now = System.currentTimeMillis();
		boolean writable = writable();
		List<MapRegion> idle = new ArrayList<>();
		for (MapRegion region : regions.values()) {
			// One whose overview is still to be drawn stays until it is, or far away it would have none.
			if (region.loaded && (!region.dirty || !writable) && !needsOverview(region)) {
				idle.add(region);
			}
		}
		idle.sort((a, b) -> Long.compare(a.lastUsed, b.lastUsed));
		int excess = regions.size() - MAX_LOADED_REGIONS;
		for (MapRegion region : idle) {
			if (now - region.lastUsed < UNLOAD_AFTER_MILLIS && excess <= 0) {
				break;
			}
			long key = MapRegion.key(region.regionX, region.regionZ);
			if (regions.remove(key, region)) {
				excess--;
				// The overview stays for the far zoom levels; the chunks go.
				if (region.overview != null) {
					MapRegion overviewOnly = new MapRegion(region.regionX, region.regionZ);
					overviewOnly.overview = region.overview;
					overviewOnly.overviewFingerprint = region.overviewFingerprint;
					overviewOnly.loaded = true;
					overviews.put(key, overviewOnly);
				}
			}
		}
		if (overviews.size() > MAX_OVERVIEWS) {
			List<MapRegion> old = new ArrayList<>();
			for (MapRegion region : overviews.values()) {
				if (region.loaded) {
					old.add(region);
				}
			}
			old.sort((a, b) -> Long.compare(a.lastUsed, b.lastUsed));
			int overviewExcess = overviews.size() - MAX_OVERVIEWS;
			for (MapRegion region : old) {
				if (overviewExcess <= 0) {
					break;
				}
				if (overviews.remove(MapRegion.key(region.regionX, region.regionZ), region)) {
					overviewExcess--;
				}
			}
		}
	}

	/** Saves everything that changed and stops loading; the map of another dimension or world takes over. */
	void close() {
		if (importer != null) {
			importer.stop();
		}
		closed = true;
		if (!writable()) {
			return;
		}
		for (MapRegion region : regions.values()) {
			if (region.dirty) {
				region.dirty = false;
				// A region still being read holds only what was sampled meanwhile, so its file is read in
				// first; saving just the new samples would lose the rest.
				IO.execute(() -> {
					readInto(region);
					if (region.count() > 0 && !region.unreadable) {
						save(region);
					}
				});
			}
		}
	}

	/** Runs something on the map's IO thread, after what's queued there. */
	static void runIo(Runnable task) {
		IO.execute(task);
	}

	/** Every chunk sampled into the map so far, for working out which world it is (#219). */
	List<MapWorldMatcher.Sample> samples() {
		List<MapWorldMatcher.Sample> samples = new ArrayList<>();
		for (MapRegion region : regions.values()) {
			for (int i = 0; i < MapRegion.CHUNKS * MapRegion.CHUNKS; i++) {
				MapChunk chunk = region.chunk(i);
				if (chunk != null) {
					int chunkX = region.regionX * MapRegion.CHUNKS + (i & (MapRegion.CHUNKS - 1));
					int chunkZ = region.regionZ * MapRegion.CHUNKS + (i >> MapRegion.SHIFT);
					samples.add(new MapWorldMatcher.Sample(chunkX, chunkZ, chunk));
				}
			}
		}
		return samples;
	}

	/** Waits until the saves queued so far are written, for when the game closes. */
	static void flush() {
		try {
			IO.submit(() -> { }).get();
		} catch (Exception exception) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * For UI snapshot checks: writes a region to its file right away, reads it back, and says what differs
	 * from the chunks in memory, or returns an empty text when they match.
	 */
	String roundTripForSnapshot(int regionX, int regionZ) {
		MapRegion region = regions.get(MapRegion.key(regionX, regionZ));
		if (folder == null || region == null || !region.loaded) {
			return "region " + regionX + ", " + regionZ + " isn't loaded";
		}
		try {
			Path file = MapRegionFile.path(folder, regionX, regionZ);
			MapRegionFile.write(file, region, biomes, minY);
			MapRegionFile.Contents contents = MapRegionFile.read(file, biomes);
			if (contents == null || contents.chunks() == null) {
				return "the file couldn't be read back";
			}
			int compared = 0;
			for (int i = 0; i < contents.chunks().length; i++) {
				MapChunk saved = region.chunk(i);
				MapChunk read = contents.chunks()[i];
				if ((saved == null) != (read == null)) {
					return "chunk " + i + " is " + (saved == null ? "extra" : "missing") + " in the file";
				}
				if (saved == null) {
					continue;
				}
				compared++;
				for (int c = 0; c < MapChunk.AREA; c++) {
					if (saved.top(c) != read.top(c) || saved.topY(c) != read.topY(c) || saved.floor(c) != read.floor(c)
						|| saved.biome(c) != read.biome(c) || saved.floor(c) != MapChunk.NONE && saved.floorY(c) != read.floorY(c)) {
						return "chunk " + i + " column " + c + " differs after reading it back";
					}
				}
			}
			return compared == 0 ? "the region had no chunks" : "";
		} catch (IOException | RuntimeException exception) {
			return "saving or reading failed: " + exception;
		}
	}

	int loadedCount() {
		return regions.size();
	}

	int chunkCount() {
		int count = 0;
		for (MapRegion region : regions.values()) {
			count += region.count();
		}
		return count;
	}

	private void listKnownRegions() {
		if (folder == null || !Files.isDirectory(folder)) {
			return;
		}
		try (Stream<Path> files = Files.list(folder)) {
			files.forEach(file -> {
				String[] parts = file.getFileName().toString().split("\\.");
				if (parts.length == 4 && parts[0].equals("r") && parts[3].equals("emap")) {
					try {
						known.add(MapRegion.key(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
					} catch (NumberFormatException ignored) {
						// Not one of ours.
					}
				}
			});
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't list {}", folder, exception);
		}
	}
}
