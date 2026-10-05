package net.emutils.client.emutils.map;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
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
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
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
	/**
	 * Unpacks and samples chunks read from the world's files (#239), as many at once as the Loading Speed
	 * setting says; the importer's own thread only lists what's missing and hands out the reads.
	 */
	private static final java.util.concurrent.ThreadPoolExecutor WORKERS = new java.util.concurrent.ThreadPoolExecutor(
		1, 1, 30L, TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>(), runnable -> {
			Thread thread = new Thread(runnable, "EMUtils Map Import Worker");
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY);
			return thread;
		});
	private static volatile MapLoadSpeed speed = MapLoadSpeed.NORMAL;
	/**
	 * At most this many imported chunks wait for the client thread, which puts them on the map once a tick; the
	 * importer waits while it catches up. Sampled chunks are small, about 2 KB, and putting them is quick, so
	 * this is high enough not to hold the importer to what fits in a tick (#239).
	 */
	private static final int MAX_WAITING = 4096;
	/** From this many chunks of a region wanted, its whole file is read into memory at once. */
	private static final int WHOLE_FILE_CHUNKS = 64;
	/** A chunk that failed to read this many times is left for when you visit it. */
	private static final int MAX_FAILURES = 3;

	private final MapWorld world;
	private final ServerLevel level;
	private final Path regionFolder;
	private static final int CURRENT_DATA_VERSION = SharedConstants.getCurrentVersion().dataVersion().version();
	private final int dataVersion = CURRENT_DATA_VERSION;
	private final ConcurrentLinkedQueue<Imported> unpacked = new ConcurrentLinkedQueue<>();
	/** Chunks already on the map, brought in, or that can't be, so rescans skip them. Import thread only. */
	private final LongOpenHashSet done = new LongOpenHashSet();
	/**
	 * Chunks that weren't fully generated yet when read, such as those at the edge of what Chunky generated, by
	 * when the game last wrote them; they're read again only once it writes them again. Import thread only.
	 */
	private final Long2IntOpenHashMap unfinished = new Long2IntOpenHashMap();
	/** How often a chunk failed to read; it's tried again at the next scans, a few times. Import thread only. */
	private final Long2IntOpenHashMap failures = new Long2IntOpenHashMap();
	private volatile boolean stopped;
	private volatile int imported;
	/** Full passes over the world's region files done, for UI snapshot checks. */
	private volatile int passes;
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

	/**
	 * A chunk brought in from the world's files: sampled already, or, when it needed a block look the map
	 * hadn't made yet, unpacked for the client thread to sample.
	 */
	record Imported(int chunkX, int chunkZ, @Nullable MapChunk sampled, @Nullable SavedChunk saved) {
	}

	/** What reading one chunk came to, handed back to the importer's thread. */
	private record Outcome(long key, @Nullable Imported chunk, boolean settled, boolean failed) {
	}

	/** The next imported chunk for the client thread to put on the map, or null. */
	@Nullable Imported poll() {
		return unpacked.poll();
	}

	/** Follows the Loading Speed setting (#239): how many threads unpack and sample, and how many reads at once. */
	static void applySpeed(MapLoadSpeed wanted) {
		if (wanted == speed) {
			return;
		}
		speed = wanted;
		int threads = wanted.importWorkers();
		if (threads > WORKERS.getMaximumPoolSize()) {
			WORKERS.setMaximumPoolSize(threads);
			WORKERS.setCorePoolSize(threads);
		} else {
			WORKERS.setCorePoolSize(threads);
			WORKERS.setMaximumPoolSize(threads);
		}
	}

	int importedCount() {
		return imported;
	}

	int passes() {
		return passes;
	}

	/** For UI snapshot checks: imported chunks waiting for the client thread, and workers busy. */
	int[] queueForSnapshot() {
		return new int[] {unpacked.size(), WORKERS.getActiveCount()};
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
			passes++;
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
		// Where you are, read once: you move meanwhile, and a sort whose order changes under it fails (#235).
		int centerX = centerRegionX;
		int centerZ = centerRegionZ;
		files.sort(Comparator.comparingInt(path -> {
			int[] at = regionCoordinates(path);
			return at == null ? Integer.MAX_VALUE : Math.max(Math.abs(at[0] - centerX), Math.abs(at[1] - centerZ));
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
		int[] header = chunkHeader(file);
		if (header == null) {
			return;
		}
		// The map's regions are the same 32 x 32 chunks as the game's, so this is the one to look in. Which chunks
		// it has is read from its file's mask (#228), so a region the map has all of isn't read in at all.
		long[] mapped = null;
		List<Long> wanted = new ArrayList<>();
		for (int i = 0; i < MapRegion.CHUNKS * MapRegion.CHUNKS && !stopped; i++) {
			if (header[i] == 0) {
				continue;
			}
			int chunkX = at[0] * MapRegion.CHUNKS + (i & (MapRegion.CHUNKS - 1));
			int chunkZ = at[1] * MapRegion.CHUNKS + (i >> MapRegion.SHIFT);
			long key = ChunkPos.pack(chunkX, chunkZ);
			if (done.contains(key)) {
				continue;
			}
			if (mapped == null) {
				mapped = world.chunksIn(at[0], at[1]);
			}
			int index = MapRegion.index(chunkX, chunkZ);
			if ((mapped[index >> 6] & 1L << (index & 63)) != 0) {
				done.add(key);
				continue;
			}
			// Not fully generated when last read, and not written since: still not, so it isn't read again.
			if (unfinished.containsKey(key) && unfinished.get(key) == header[MapRegion.CHUNKS * MapRegion.CHUNKS + index]) {
				continue;
			}
			wanted.add(key);
		}
		if (wanted.isEmpty()) {
			return;
		}
		waitForRoom();
		// Several chunks at once, read straight from the region file and unpacked and sampled on the workers
		// (#239), instead of one by one through the game's storage, whose one thread reads and unpacks them all.
		// Most of a region wanted, as in a world just pre-generated: the whole file is read at once, and the workers
		// unpack from memory. Positional reads of one file wait for each other on Windows, so they'd take turns.
		byte[] whole = null;
		FileChannel channel = null;
		try {
			if (wanted.size() >= WHOLE_FILE_CHUNKS) {
				whole = Files.readAllBytes(file);
			} else {
				channel = FileChannel.open(file, StandardOpenOption.READ);
			}
		} catch (IOException exception) {
			// Read through the game's storage instead.
		}
		FileChannel direct = channel;
		byte[] inMemory = whole;
		ConcurrentLinkedQueue<Outcome> outcomes = new ConcurrentLinkedQueue<>();
		// A slot per read at once, given back the moment a read is done, so the next starts right away rather
		// than after a sleep, which on Windows can last 15 ms whatever it asks for.
		int slots = speed.importReadsInFlight();
		java.util.concurrent.Semaphore free = new java.util.concurrent.Semaphore(slots);
		for (long key : wanted) {
			if (!acquire(free, 1)) {
				break;
			}
			int chunkX = ChunkPos.getX(key);
			int chunkZ = ChunkPos.getZ(key);
			int location = header[MapRegion.index(chunkX, chunkZ)];
			java.util.concurrent.CompletableFuture
				.supplyAsync(() -> inMemory != null ? readDirect(inMemory, location) : direct == null ? null : readDirect(direct, location), WORKERS)
				.thenCompose(tag -> tag != null
					? java.util.concurrent.CompletableFuture.completedFuture(java.util.Optional.of(tag))
					: level.getChunkSource().chunkMap.read(new ChunkPos(chunkX, chunkZ)).orTimeout(10L, TimeUnit.SECONDS))
				.thenApplyAsync(tag -> unpack(key, chunkX, chunkZ, tag.orElse(null)), WORKERS)
				.whenComplete((outcome, error) -> {
					outcomes.add(error != null ? new Outcome(key, null, false, true) : outcome);
					free.release();
				});
			settle(outcomes, header);
		}
		// Every read back, also after a stop, so the region file isn't closed under one.
		try {
			while (!free.tryAcquire(slots, 20L, TimeUnit.MILLISECONDS)) {
				settle(outcomes, header);
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		settle(outcomes, header);
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException exception) {
				// Only read from.
			}
		}
	}

	/**
	 * A chunk read straight from its region file, as the game stores it there (#239): its sectors, then its
	 * length, how it's compressed and the compressed NBT. Null for a chunk kept in a file of its own, one
	 * compressed in a way this game doesn't know, or one that doesn't read cleanly, as when the game is writing
	 * it right then; those are read through the game's storage. Any thread: reads at a position share the file.
	 */
	private static @Nullable CompoundTag readDirect(FileChannel channel, int location) {
		long sector = location >>> 8;
		int sectors = location & 0xFF;
		if (sector < 2 || sectors == 0) {
			return null;
		}
		try {
			ByteBuffer buffer = ByteBuffer.allocate(sectors * 4096);
			long position = sector * 4096L;
			while (buffer.hasRemaining()) {
				int read = channel.read(buffer, position + buffer.position());
				if (read <= 0) {
					break;
				}
			}
			return decode(buffer.array(), 0, buffer.position());
		} catch (IOException | RuntimeException exception) {
			return null;
		}
	}

	/** The same, from the whole region file read into memory. */
	private static @Nullable CompoundTag readDirect(byte[] file, int location) {
		long sector = location >>> 8;
		int sectors = location & 0xFF;
		if (sector < 2 || sectors == 0 || sector * 4096L >= file.length) {
			return null;
		}
		int start = (int) (sector * 4096L);
		return decode(file, start, Math.min(sectors * 4096, file.length - start));
	}

	private static DataInputStream stream(RegionFileVersion version, byte[] bytes, int start, int length) throws IOException {
		return new DataInputStream(new BufferedInputStream(version.wrap(new ByteArrayInputStream(bytes, start + 5, length - 1))));
	}

	/** A chunk's stored bytes, starting at its length, unpacked into its NBT; null when they don't read cleanly. */
	private static @Nullable CompoundTag decode(byte[] bytes, int start, int available) {
		try {
			if (available < 5) {
				return null;
			}
			int length = ByteBuffer.wrap(bytes, start, 4).getInt();
			byte type = bytes[start + 4];
			if (length <= 1 || (type & 0x80) != 0 || length - 1 > available - 5) {
				return null;
			}
			RegionFileVersion version = RegionFileVersion.fromId(type);
			if (version == null) {
				return null;
			}
			// Only what the map reads: chunks are saved with these first, so the rest of the file isn't even
			// unpacked. One saved by an older game version is read whole, since upgrading it needs all of it.
			CollectFields wanted = new CollectFields(
				new FieldSelector(IntTag.TYPE, "DataVersion"),
				new FieldSelector(StringTag.TYPE, "Status"),
				new FieldSelector(IntTag.TYPE, "xPos"),
				new FieldSelector(IntTag.TYPE, "zPos"),
				new FieldSelector(ListTag.TYPE, "sections")
			);
			try (DataInputStream in = stream(version, bytes, start, length)) {
				NbtIo.parse(in, wanted, NbtAccounter.unlimitedHeap());
			}
			if (wanted.getResult() instanceof CompoundTag tag && tag.getIntOr("DataVersion", -1) == CURRENT_DATA_VERSION) {
				return tag;
			}
			try (DataInputStream in = stream(version, bytes, start, length)) {
				return NbtIo.read(in, NbtAccounter.unlimitedHeap());
			}
		} catch (IOException | RuntimeException exception) {
			return null;
		}
	}

	/**
	 * Hands chunks read to the client thread and keeps count of what's done. A chunk that isn't fully generated
	 * yet is read again once the game writes it again, one that failed to read at the next few scans, and one
	 * that can't be read (saved by a newer game version) not again. Importer thread.
	 */
	private void settle(ConcurrentLinkedQueue<Outcome> outcomes, int[] header) {
		Outcome outcome;
		while ((outcome = outcomes.poll()) != null) {
			if (outcome.failed()) {
				if (failures.addTo(outcome.key(), 1) + 1 >= MAX_FAILURES) {
					failures.remove(outcome.key());
					done.add(outcome.key());
				}
				continue;
			}
			int index = MapRegion.index(ChunkPos.getX(outcome.key()), ChunkPos.getZ(outcome.key()));
			if (outcome.settled()) {
				done.add(outcome.key());
				unfinished.remove(outcome.key());
			} else if (outcome.chunk() == null) {
				// When the game last wrote it, from the file's header: read again once it's written again.
				unfinished.put(outcome.key(), header[MapRegion.CHUNKS * MapRegion.CHUNKS + index]);
			}
			if (outcome.chunk() == null) {
				continue;
			}
			while (unpacked.size() >= MAX_WAITING && !stopped) {
				sleep(5L);
			}
			unpacked.add(outcome.chunk());
			imported++;
		}
	}

	/** Takes slots, waiting for them while the importer runs; false once it's stopped. */
	private boolean acquire(java.util.concurrent.Semaphore free, int count) {
		try {
			while (!stopped) {
				if (free.tryAcquire(count, 50L, TimeUnit.MILLISECONDS)) {
					return true;
				}
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		return false;
	}

	/**
	 * While the map holds many more regions than it keeps, waits for saved ones to be let go before bringing in
	 * chunks of another, which reads that one in.
	 */
	private void waitForRoom() {
		long crowded = System.currentTimeMillis() + 60_000L;
		while (world.loadedCount() > MapWorld.MAX_LOADED_REGIONS * 2 && !stopped && System.currentTimeMillis() < crowded) {
			sleep(50L);
		}
	}

	/**
	 * Unpacks a saved chunk read from the world's files and samples it where it can. Saved by a newer game version,
	 * or not there, it's settled with nothing; not fully generated yet, it's left for the next scan. Worker thread.
	 */
	private Outcome unpack(long key, int chunkX, int chunkZ, @Nullable CompoundTag read) {
		if (read == null) {
			return new Outcome(key, null, true, false);
		}
		CompoundTag tag = read;
		int version = tag.getIntOr("DataVersion", -1);
		if (version > dataVersion) {
			return new Outcome(key, null, true, false);
		}
		ChunkMap chunks = level.getChunkSource().chunkMap;
		if (version < dataVersion) {
			CompoundTag context = ChunkMap.getChunkDataFixContextTag(level.dimension(), level.getChunkSource().getGenerator().getTypeNameForDataFixer());
			tag = chunks.upgradeChunkTag(tag, -1, context, dataVersion);
		}
		int ceiling = level.dimensionType().hasCeiling() ? level.getMinY() + level.dimensionType().logicalHeight() - 1 : MapSampler.SURFACE;
		SavedChunk saved = SavedChunk.parse(tag, dataVersion, level.getMinY(), level.getHeight(), ceiling, world.biomes());
		if (saved == null) {
			return new Outcome(key, null, false, false);
		}
		MapChunk sampled = MapSampler.sampleOffThread(saved, world.startY());
		return new Outcome(key, new Imported(chunkX, chunkZ, sampled, sampled == null ? saved : null), true, false);
	}

	/**
	 * A region file's header: where each of its 1024 chunks is saved (its first sector and how many, or 0 for a
	 * chunk that isn't), then when each was last written, in seconds. Null when the file can't be read.
	 */
	private static int @Nullable [] chunkHeader(Path file) {
		int chunks = MapRegion.CHUNKS * MapRegion.CHUNKS;
		try (java.io.InputStream in = Files.newInputStream(file)) {
			byte[] bytes = in.readNBytes(chunks * 8);
			if (bytes.length < chunks * 4) {
				return null;
			}
			int[] header = new int[chunks * 2];
			ByteBuffer.wrap(bytes).asIntBuffer().get(header, 0, bytes.length / 4);
			return header;
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
