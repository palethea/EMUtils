package net.emutils.client.emutils.map;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * Puts the chunks a singleplayer world has generated on the map (#215), also those you never went near, such
 * as the ones Chunky pre-generates. The client only gets the chunks around you, but in singleplayer the world's
 * files are on this computer: the importer lists the chunks in the world's region files, reads the ones the map
 * doesn't have through the server's own chunk storage (which also knows chunks not written to disk yet), and
 * unpacks them on its own thread. Chunks saved by an older game version are upgraded first, as the game does
 * when it loads them. The client thread samples them like loaded chunks. It looks again every so often, so
 * chunks generated while you play show up too, and chunks that weren't finished or couldn't be read are tried
 * again then.
 */
final class MapImporter {
	private static final ScheduledExecutorService THREAD = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Map Import");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});
	private static final long RESCAN_SECONDS = 30L;
	/** At most this many unpacked chunks wait for the client thread; the importer waits while it catches up. */
	private static final int MAX_WAITING = 64;
	private static final long REGION_LOAD_TIMEOUT_MILLIS = 5_000L;
	/** A chunk that failed to read this many times is left for when you visit it. */
	private static final int MAX_FAILURES = 3;

	private final MapWorld world;
	private final ServerLevel level;
	private final Path regionFolder;
	private final int dataVersion = SharedConstants.getCurrentVersion().dataVersion().version();
	private final ConcurrentLinkedQueue<SavedChunk> unpacked = new ConcurrentLinkedQueue<>();
	/** Chunks already on the map, brought in, or that can't be, so rescans skip them. Import thread only. */
	private final LongOpenHashSet done = new LongOpenHashSet();
	/** How often a chunk failed to read; it's tried again at the next scans, a few times. Import thread only. */
	private final Long2IntOpenHashMap failures = new Long2IntOpenHashMap();
	private volatile boolean stopped;
	private volatile int imported;
	private volatile @Nullable ScheduledFuture<?> nextScan;
	/** The region the map is looking at, so the regions around it are imported first. */
	private volatile int centerRegionX;
	private volatile int centerRegionZ;

	private MapImporter(MapWorld world, ServerLevel level, Path regionFolder) {
		this.world = world;
		this.level = level;
		this.regionFolder = regionFolder;
	}

	/** An importer for this dimension of the singleplayer world you're in, started; null outside singleplayer. */
	static @Nullable MapImporter start(Minecraft client, MapWorld world, ResourceKey<Level> dimension) {
		ServerLevel level = serverLevel(client, dimension);
		IntegratedServer server = client.getSingleplayerServer();
		if (server == null || level == null) {
			return null;
		}
		Path folder = DimensionType.getStorageFolder(dimension, server.getWorldPath(LevelResource.ROOT)).resolve("region");
		MapImporter importer = new MapImporter(world, level, folder);
		THREAD.execute(importer::scan);
		return importer;
	}

	/** The singleplayer world's level of a dimension, or null outside singleplayer or when it has none. */
	static @Nullable ServerLevel serverLevel(Minecraft client, ResourceKey<Level> dimension) {
		IntegratedServer server = client.getSingleplayerServer();
		return server == null ? null : server.getLevel(dimension);
	}

	void stop() {
		stopped = true;
	}

	/** Where you are, or where the world map looks, in blocks; the next scan starts around there. */
	void center(double blockX, double blockZ) {
		centerRegionX = (int) Math.floor(blockX) >> 9;
		centerRegionZ = (int) Math.floor(blockZ) >> 9;
	}

	/** The next unpacked chunk for the client thread to sample, or null. */
	@Nullable SavedChunk poll() {
		return unpacked.poll();
	}

	int importedCount() {
		return imported;
	}

	private void scan() {
		if (!stopped) {
			step(regionFiles(), 0);
		}
	}

	/**
	 * Imports one region file and queues the next one behind whatever else waits on this thread, so another
	 * dimension's importer gets its turn instead of waiting for a whole dimension.
	 */
	private void step(List<Path> files, int next) {
		if (stopped) {
			return;
		}
		if (next >= files.size()) {
			nextScan = THREAD.schedule(this::scan, RESCAN_SECONDS, TimeUnit.SECONDS);
			return;
		}
		try {
			importRegion(files.get(next));
		} catch (RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't read the world's generated chunks in {}", files.get(next), exception);
		}
		THREAD.execute(() -> step(files, next + 1));
	}

	/** Looks for new chunks now instead of at the next scan; for UI snapshot checks. */
	void rescanNow() {
		ScheduledFuture<?> next = nextScan;
		if (next != null && next.cancel(false)) {
			THREAD.execute(this::scan);
		}
	}

	/** The world's region files, nearest where you are first, so what's around you comes in first. */
	private List<Path> regionFiles() {
		List<Path> files = new ArrayList<>();
		if (!Files.isDirectory(regionFolder)) {
			return files;
		}
		try (Stream<Path> list = Files.list(regionFolder)) {
			list.filter(path -> regionCoordinates(path) != null).forEach(files::add);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't list {}", regionFolder, exception);
		}
		files.sort(Comparator.comparingInt(path -> {
			int[] at = regionCoordinates(path);
			return at == null ? Integer.MAX_VALUE : Math.max(Math.abs(at[0] - centerRegionX), Math.abs(at[1] - centerRegionZ));
		}));
		return files;
	}

	private static int @Nullable [] regionCoordinates(Path file) {
		String[] parts = file.getFileName().toString().split("\\.");
		if (parts.length != 4 || !parts[0].equals("r") || !parts[3].equals("mca")) {
			return null;
		}
		try {
			return new int[] {Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private void importRegion(Path file) {
		int[] at = regionCoordinates(file);
		if (at == null) {
			return;
		}
		boolean[] present = presentChunks(file);
		if (present == null) {
			return;
		}
		// The map's regions are the same 32 x 32 chunks as the game's, so this is the one to look in.
		MapRegion region = null;
		for (int i = 0; i < present.length && !stopped; i++) {
			if (!present[i]) {
				continue;
			}
			int chunkX = at[0] * MapRegion.CHUNKS + (i & (MapRegion.CHUNKS - 1));
			int chunkZ = at[1] * MapRegion.CHUNKS + (i >> MapRegion.SHIFT);
			long key = ChunkPos.pack(chunkX, chunkZ);
			if (done.contains(key)) {
				continue;
			}
			if (region == null) {
				region = waitForRegion(at[0], at[1]);
				if (region == null) {
					return;
				}
			}
			if (region.chunk(MapRegion.index(chunkX, chunkZ)) != null) {
				done.add(key);
				continue;
			}
			SavedChunk chunk = read(key, chunkX, chunkZ);
			if (chunk == null) {
				continue;
			}
			done.add(key);
			while (unpacked.size() >= MAX_WAITING && !stopped) {
				sleep(20L);
			}
			unpacked.add(chunk);
			imported++;
		}
	}

	/**
	 * The map region, once it's read from the map's own files, so chunks already on the map are skipped. While
	 * the map holds many more regions than it keeps, it waits for saved ones to be let go first.
	 */
	private @Nullable MapRegion waitForRegion(int regionX, int regionZ) {
		long crowded = System.currentTimeMillis() + 60_000L;
		while (world.loadedCount() > MapWorld.MAX_LOADED_REGIONS * 2 && !stopped && System.currentTimeMillis() < crowded) {
			sleep(50L);
		}
		MapRegion region = world.region(regionX, regionZ);
		long deadline = System.currentTimeMillis() + REGION_LOAD_TIMEOUT_MILLIS;
		while (!region.loaded && !stopped && System.currentTimeMillis() < deadline) {
			sleep(10L);
		}
		return region.loaded ? region : null;
	}

	/**
	 * Reads and unpacks a saved chunk, or returns null. A chunk that isn't fully generated yet is tried again
	 * at the next scan, one that failed to read a few more times, and one that can't be read (saved by a newer
	 * game version) not again.
	 */
	private @Nullable SavedChunk read(long key, int chunkX, int chunkZ) {
		try {
			ChunkMap chunks = level.getChunkSource().chunkMap;
			CompoundTag tag = chunks.read(new ChunkPos(chunkX, chunkZ)).get(10L, TimeUnit.SECONDS).orElse(null);
			if (tag == null) {
				done.add(key);
				return null;
			}
			int version = tag.getIntOr("DataVersion", -1);
			if (version > dataVersion) {
				done.add(key);
				return null;
			}
			if (version < dataVersion) {
				CompoundTag context = ChunkMap.getChunkDataFixContextTag(level.dimension(), level.getChunkSource().getGenerator().getTypeNameForDataFixer());
				tag = chunks.upgradeChunkTag(tag, -1, context, dataVersion);
			}
			int ceiling = level.dimensionType().hasCeiling() ? level.getMinY() + level.dimensionType().logicalHeight() - 1 : MapSampler.SURFACE;
			return SavedChunk.parse(tag, dataVersion, level.getMinY(), level.getHeight(), ceiling, world.biomes());
		} catch (Exception exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			if (failures.addTo(key, 1) + 1 >= MAX_FAILURES) {
				failures.remove(key);
				done.add(key);
			}
			return null;
		}
	}

	/**
	 * Which of a region file's 1024 chunks are saved, from its header: a chunk with a place in the file has a
	 * non-zero location. Null when the file can't be read.
	 */
	private static boolean @Nullable [] presentChunks(Path file) {
		try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {
			if (in.length() < 4096) {
				return null;
			}
			boolean[] present = new boolean[MapRegion.CHUNKS * MapRegion.CHUNKS];
			for (int i = 0; i < present.length; i++) {
				present[i] = in.readInt() != 0;
			}
			return present;
		} catch (IOException exception) {
			return null;
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}
}
