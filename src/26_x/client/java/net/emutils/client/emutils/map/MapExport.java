package net.emutils.client.emutils.map;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.screenshot.ScreenshotMessage;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Saves a map as a PNG image (#225), like Xaero's map export: the dimension, world and layer the world map
 * shows, everything explored. A pixel per block while the map fits in {@link #MAX_PIXELS} pixels across,
 * else a pixel per 4, 16 or 64 blocks, drawn from the regions' overviews. Unexplored ground and void are
 * left clear. The tiles are drawn like the world map's, on a thread of its own, one row of tiles at a time,
 * and written as they're done, so a big map doesn't need the whole picture in memory. Saved maps are read
 * from their files by a map of its own, which the client thread keeps ready to draw while it runs; the
 * picture is in the screenshots folder, and chat says so with buttons to open it, like a screenshot's.
 */
public final class MapExport {
	/** The picture is at most this many pixels across; a bigger map is saved with more blocks to a pixel. */
	private static final int MAX_PIXELS = 16384;
	/** The levels a picture is drawn at: a pixel per block, then per 4, 16 and 64 blocks. */
	private static final int FIRST_LEVEL = 2;
	/** How long a tile is waited for while its regions are read; it's saved as it is after that. */
	private static final long TILE_WAIT_MILLIS = 20_000L;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
	private static final ExecutorService THREAD = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Map Export");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});

	private static @Nullable MapExport running;
	/** The last picture saved, for UI snapshot checks. */
	private static @Nullable Path lastSaved;

	private final MapWorld world;
	/** The map was opened for the export, so it's kept ready and closed by it. */
	private final boolean owned;
	/** Never drawn; the export's map needs them to have its overviews redrawn. */
	private final MapTiles tiles = new MapTiles();
	private final Path file;
	private volatile int done;
	private volatile int total = 1;
	private volatile boolean finished;
	private volatile @Nullable Component failure;
	private volatile boolean cancelled;

	private MapExport(MapWorld world, boolean owned, Path file) {
		this.world = world;
		this.owned = owned;
		this.file = file;
	}

	/** Whether a picture is being saved. */
	public static boolean running() {
		return running != null;
	}

	/** How far the picture being saved got, 0 to 1. */
	static float progress() {
		MapExport export = running;
		return export == null ? 1.0F : Math.clamp(export.done / (float) Math.max(1, export.total), 0.0F, 1.0F);
	}

	/**
	 * Starts saving a picture of a map, as the world map shows it; returns false while another is saved.
	 * Client thread.
	 */
	static boolean start(Minecraft client, MapWorld shown) {
		if (running != null || client.level == null) {
			return false;
		}
		MapWorld world = shown;
		boolean owned = false;
		Path folder = shown.folder();
		if (folder != null) {
			// What changed lately is written first, then read back with the rest by a map that's only read.
			shown.saveChanged();
			world = new MapWorld(client.level, folder, shown.dimension(), Integer.MIN_VALUE, shown.ceiling(), shown.worldId, shown.cave);
			owned = true;
		}
		MapExport export = new MapExport(world, owned, client.gameDirectory.toPath().resolve("screenshots").resolve(fileName(shown)));
		running = export;
		THREAD.execute(export::run);
		return true;
	}

	/** Client thread, every tick: keeps the export's map ready to draw, and says when the picture is saved. */
	static void tick(Minecraft client) {
		MapExport export = running;
		if (export == null) {
			return;
		}
		if (client.level == null || export.world.level() != client.level) {
			// You left the world; the map it reads from goes with it.
			export.cancelled = true;
		}
		if (export.owned && !export.finished) {
			MapManager.prepare(export.world, export.tiles);
		}
		if (!export.finished) {
			return;
		}
		running = null;
		if (export.owned) {
			export.world.close();
			export.tiles.clear();
		}
		if (client.gui == null || export.cancelled) {
			return;
		}
		Component failure = export.failure;
		if (failure == null) {
			lastSaved = export.file;
		}
		MinecraftClientCompat.chat(client).addClientSystemMessage(failure != null
			? Component.empty().append(EMUtilsTexts.greenPrefix()).append(failure.copy().withStyle(ChatFormatting.RED))
			: ScreenshotMessage.saved(export.file.toFile(), EMUtilsTexts.CHAT_MAP_EXPORTED));
	}

	/** For UI snapshot checks: the last picture saved, or null. */
	public static @Nullable Path lastSavedForSnapshot() {
		return lastSaved;
	}

	/** Named after the dimension and layer, and when it was saved, like a screenshot. */
	private static String fileName(MapWorld shown) {
		String dimension = shown.dimension();
		String name = dimension.substring(dimension.indexOf(':') + 1).replaceAll("[^a-z0-9_.-]", "_");
		String layer = shown.cave == MapSampler.SURFACE ? "" : "_caves-" + shown.cave * MapWorld.LAYER_BLOCKS;
		return "emutils-map_" + name + layer + "_" + LocalDateTime.now().format(DATE) + ".png";
	}

	/** Export thread. */
	private void run() {
		try {
			// The regions the map knows are listed, and what was just saved written, before reading on.
			MapWorld.flush();
			List<Long> known = new ArrayList<>(world.knownRegions());
			if (known.isEmpty()) {
				failure = Component.translatable(EMUtilsTexts.CHAT_MAP_EXPORT_EMPTY);
				return;
			}
			int minX = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxZ = Integer.MIN_VALUE;
			for (long key : known) {
				int regionX = (int) (key >> 32);
				int regionZ = (int) key;
				minX = Math.min(minX, regionX);
				maxX = Math.max(maxX, regionX);
				minZ = Math.min(minZ, regionZ);
				maxZ = Math.max(maxZ, regionZ);
			}
			int level = FIRST_LEVEL;
			long across = (long) Math.max(maxX - minX + 1, maxZ - minZ + 1) * MapRegion.BLOCKS;
			while (level < MapTileBaker.LEVELS - 1 && across * MapTileBaker.TILE_PIXELS / MapTileBaker.blocksPerTile(level) > MAX_PIXELS) {
				level++;
			}
			int blocks = MapTileBaker.blocksPerTile(level);
			int firstTileX = Math.floorDiv(minX * MapRegion.BLOCKS, blocks);
			int lastTileX = Math.floorDiv((maxX + 1) * MapRegion.BLOCKS - 1, blocks);
			int firstTileZ = Math.floorDiv(minZ * MapRegion.BLOCKS, blocks);
			int lastTileZ = Math.floorDiv((maxZ + 1) * MapRegion.BLOCKS - 1, blocks);
			int tilesAcross = lastTileX - firstTileX + 1;
			int tilesDown = lastTileZ - firstTileZ + 1;
			total = tilesAcross * tilesDown;
			int width = tilesAcross * MapTileBaker.TILE_PIXELS;
			int[] row = new int[width * MapTileBaker.TILE_PIXELS];
			Files.createDirectories(file.getParent());
			try (Png png = new Png(file, width, tilesDown * MapTileBaker.TILE_PIXELS)) {
				for (int tileZ = firstTileZ; tileZ <= lastTileZ && !cancelled; tileZ++) {
					java.util.Arrays.fill(row, 0);
					for (int tileX = firstTileX; tileX <= lastTileX && !cancelled; tileX++) {
						if (explored(tileX, tileZ, blocks)) {
							int[] pixels = bake(level, tileX, tileZ);
							int left = (tileX - firstTileX) * MapTileBaker.TILE_PIXELS;
							for (int py = 0; py < MapTileBaker.TILE_PIXELS; py++) {
								System.arraycopy(pixels, py * MapTileBaker.TILE_PIXELS, row, py * width + left, MapTileBaker.TILE_PIXELS);
							}
						}
						done++;
					}
					png.rows(row, MapTileBaker.TILE_PIXELS);
				}
			}
			if (cancelled) {
				Files.deleteIfExists(file);
			}
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't save the picture {}", file, exception);
			failure = Component.translatable(EMUtilsTexts.CHAT_MAP_EXPORT_FAILED);
		} finally {
			finished = true;
		}
	}

	/** Whether any region a tile covers was explored. */
	private boolean explored(int tileX, int tileZ, int blocks) {
		int firstX = Math.floorDiv(tileX * blocks, MapRegion.BLOCKS);
		int firstZ = Math.floorDiv(tileZ * blocks, MapRegion.BLOCKS);
		int lastX = Math.floorDiv((tileX + 1) * blocks - 1, MapRegion.BLOCKS);
		int lastZ = Math.floorDiv((tileZ + 1) * blocks - 1, MapRegion.BLOCKS);
		for (int regionZ = firstZ; regionZ <= lastZ; regionZ++) {
			for (int regionX = firstX; regionX <= lastX; regionX++) {
				if (world.known(regionX, regionZ)) {
					return true;
				}
			}
		}
		return false;
	}

	/** A tile, drawn again until all it shows is read in, or as far as it got after a while. */
	private int[] bake(int level, int tileX, int tileZ) {
		long until = System.currentTimeMillis() + TILE_WAIT_MILLIS;
		MapTileBaker.Result result = MapTileBaker.bake(world, level, tileX, tileZ);
		while (!result.complete() && !cancelled && System.currentTimeMillis() < until) {
			try {
				Thread.sleep(50L);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				break;
			}
			result = MapTileBaker.bake(world, level, tileX, tileZ);
		}
		return result.pixels();
	}

	/**
	 * A PNG written a few rows at a time, in color with alpha. Each row is stored as the difference to the pixel
	 * on its left, which shrinks maps' runs of the same ground a lot.
	 */
	private static final class Png implements AutoCloseable {
		private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
		private final DataOutputStream out;
		private final Deflater deflater = new Deflater(6);
		private final byte[] compressed = new byte[1 << 16];
		private final byte[] line;
		private final int width;

		Png(Path file, int width, int height) throws IOException {
			this.width = width;
			this.line = new byte[1 + width * 4];
			out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file), 1 << 16));
			out.write(SIGNATURE);
			byte[] header = new byte[13];
			putInt(header, 0, width);
			putInt(header, 4, height);
			header[8] = 8;
			// Color with alpha; the default compression, filtering and no interlacing.
			header[9] = 6;
			chunk("IHDR", header, header.length);
		}

		/** Writes {@code count} rows of pixels in a texture's ABGR order. */
		void rows(int[] pixels, int count) throws IOException {
			for (int y = 0; y < count; y++) {
				// Filter 1: each byte minus the same byte of the pixel to the left.
				line[0] = 1;
				int previous = 0;
				for (int x = 0; x < width; x++) {
					int pixel = pixels[y * width + x];
					int at = 1 + x * 4;
					line[at] = (byte) ((pixel & 0xFF) - (previous & 0xFF));
					line[at + 1] = (byte) ((pixel >> 8 & 0xFF) - (previous >> 8 & 0xFF));
					line[at + 2] = (byte) ((pixel >> 16 & 0xFF) - (previous >> 16 & 0xFF));
					line[at + 3] = (byte) ((pixel >>> 24) - (previous >>> 24));
					previous = pixel;
				}
				deflater.setInput(line);
				while (!deflater.needsInput()) {
					drain(Deflater.NO_FLUSH);
				}
			}
		}

		private void drain(int flush) throws IOException {
			int length = deflater.deflate(compressed, 0, compressed.length, flush);
			if (length > 0) {
				chunk("IDAT", compressed, length);
			}
		}

		private void chunk(String type, byte[] data, int length) throws IOException {
			byte[] name = type.getBytes(StandardCharsets.US_ASCII);
			CRC32 crc = new CRC32();
			crc.update(name);
			crc.update(data, 0, length);
			out.writeInt(length);
			out.write(name);
			out.write(data, 0, length);
			out.writeInt((int) crc.getValue());
		}

		private static void putInt(byte[] bytes, int at, int value) {
			bytes[at] = (byte) (value >>> 24);
			bytes[at + 1] = (byte) (value >>> 16);
			bytes[at + 2] = (byte) (value >>> 8);
			bytes[at + 3] = (byte) value;
		}

		@Override
		public void close() throws IOException {
			try (OutputStream ignored = out) {
				deflater.finish();
				while (!deflater.finished()) {
					drain(Deflater.NO_FLUSH);
				}
				chunk("IEND", new byte[0], 0);
			} finally {
				deflater.end();
			}
		}
	}
}
