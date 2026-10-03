package net.emutils.client.emutils.map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * The map of one dimension of one world (#212, #215): its regions, loaded from disk as the map needs them
 * and saved back when they change. Lives as long as you are in that dimension. Chunks and overviews are
 * read by the tile baker from its own thread, and regions load and save on the map's IO thread.
 */
public final class MapWorld {
	/** Regions unused for this long are let go once saved. */
	private static final long UNLOAD_AFTER_MILLIS = 45_000L;
	/** At most this many regions keep their chunks in memory; the least used go first. */
	private static final int MAX_LOADED_REGIONS = 48;
	private static final String DIMENSION_FILE = "dimension.txt";
	private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Map IO");
		thread.setDaemon(true);
		return thread;
	});

	private final ClientLevel level;
	private final Registry<Biome> biomes;
	private final @Nullable Path folder;
	private final ConcurrentHashMap<Long, MapRegion> regions = new ConcurrentHashMap<>();
	/** Overviews of regions whose chunks aren't loaded, read from their files for the far zoom levels. */
	private final ConcurrentHashMap<Long, MapRegion> overviews = new ConcurrentHashMap<>();
	/** Every region with a file or with chunks, so the world map knows what was explored. */
	private final Set<Long> known = ConcurrentHashMap.newKeySet();
	private final ConcurrentLinkedQueue<MapRegion> justLoaded = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<MapRegion> overviewsRead = new ConcurrentLinkedQueue<>();
	private volatile boolean closed;
	/** When this map's overviews were last looked over for redrawing. */
	long lastOverviewRound;
	int ticks;

	MapWorld(ClientLevel level, @Nullable Path folder) {
		this.level = level;
		this.biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		this.folder = folder;
		if (folder != null) {
			IO.execute(this::listKnownRegions);
		}
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
		if (closed) {
			return;
		}
		try {
			MapRegionFile.Contents contents = MapRegionFile.read(MapRegionFile.path(folder, region.regionX, region.regionZ), biomes);
			if (contents != null) {
				MapChunk[] chunks = contents.chunks();
				for (int i = 0; chunks != null && i < chunks.length; i++) {
					if (chunks[i] != null) {
						region.putLoaded(i, chunks[i]);
					}
				}
				if (region.overview == null && contents.overview() != null) {
					region.overview = contents.overview();
					region.overviewFingerprint = contents.overviewFingerprint();
				}
				// The saved overview is right for the saved chunks unless something newer was sampled.
				region.overviewStale = region.dirty || contents.overview() == null;
			}
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't read region {}, {}", region.regionX, region.regionZ, exception);
		}
		region.loaded = true;
		justLoaded.add(region);
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
			return overviewOnly;
		}
		MapRegion created = new MapRegion(regionX, regionZ);
		if (overviews.putIfAbsent(key, created) == null) {
			IO.execute(() -> {
				try {
					MapRegionFile.Contents contents = MapRegionFile.readOverview(MapRegionFile.path(folder, regionX, regionZ));
					if (contents != null && contents.overview() != null) {
						created.overview = contents.overview();
						created.overviewFingerprint = contents.overviewFingerprint();
					}
				} catch (IOException | RuntimeException exception) {
					EMUtilsClient.LOGGER.warn("EMUtils map couldn't read the overview of region {}, {}", regionX, regionZ, exception);
				}
				created.loaded = true;
				overviewsRead.add(created);
			});
		}
		return created;
	}

	/** Regions read from disk since the last call, for the client thread to prepare. */
	@Nullable MapRegion pollLoaded() {
		return justLoaded.poll();
	}

	@Nullable MapRegion pollOverviewRead() {
		return overviewsRead.poll();
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

	/** Saves the regions that changed, in the background. */
	void saveChanged() {
		if (folder == null) {
			return;
		}
		for (MapRegion region : regions.values()) {
			if (region.dirty && region.loaded) {
				region.dirty = false;
				// Regions the map only looked into, with nothing explored, get no file.
				if (region.count() > 0) {
					IO.execute(() -> save(region));
				}
			}
		}
	}

	private void save(MapRegion region) {
		try {
			writeDimension();
			MapRegionFile.write(MapRegionFile.path(folder, region.regionX, region.regionZ), region, biomes, level.getMinY());
		} catch (IOException | RuntimeException exception) {
			region.dirty = true;
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't save region {}, {}", region.regionX, region.regionZ, exception);
		}
	}

	/** The dimension's id, written once beside its regions, so the world map can name the folder. */
	private void writeDimension() throws IOException {
		Path file = folder.resolve(DIMENSION_FILE);
		if (!Files.exists(file)) {
			Files.createDirectories(folder);
			Files.writeString(file, WaypointManager.dimensionId(level));
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

	/** Lets go of regions that were saved and haven't been used for a while, and the least used past the limit. */
	void unloadIdle() {
		long now = System.currentTimeMillis();
		List<MapRegion> idle = new ArrayList<>();
		for (MapRegion region : regions.values()) {
			if (region.loaded && !region.dirty) {
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
	}

	/** Saves everything that changed and stops loading; the map of another dimension or world takes over. */
	void close() {
		saveChanged();
		closed = true;
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
			MapRegionFile.write(file, region, biomes, level.getMinY());
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
