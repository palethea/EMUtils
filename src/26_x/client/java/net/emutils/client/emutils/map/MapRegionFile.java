package net.emutils.client.emutils.map;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Reads and writes one region of the map (#215). The file is compressed and starts with the region's
 * overview, so the world map's far zoom levels can read just that. Block states and biomes are written by
 * name through a palette per region, so files keep working when mods or game versions change their ids;
 * a block that no longer exists is read as air. Each chunk's columns are written as separate planes (all
 * tops, then all heights, ...), which compress far better than whole columns one after another.
 *
 * <p>Runs on the map's IO thread.
 */
final class MapRegionFile {
	private static final int MAGIC = 0x454D4D50;
	private static final int VERSION = 1;

	private MapRegionFile() {
	}

	/** What a region file holds: its chunks by index (null where none), and its overview if it has one. */
	record Contents(MapChunk @Nullable [] chunks, int @Nullable [] overview, int overviewFingerprint) {
	}

	static Path path(Path folder, int regionX, int regionZ) {
		return folder.resolve("r." + regionX + "." + regionZ + ".emap");
	}

	static void write(Path file, MapRegion region, Registry<Biome> biomes, int minY) throws IOException {
		MapChunk[] chunks = new MapChunk[MapRegion.CHUNKS * MapRegion.CHUNKS];
		long[] present = new long[chunks.length / 64];
		Int2IntOpenHashMap blockIndex = new Int2IntOpenHashMap();
		IntArrayList blockPalette = new IntArrayList();
		Int2IntOpenHashMap biomeIndex = new Int2IntOpenHashMap();
		IntArrayList biomePalette = new IntArrayList();
		for (int i = 0; i < chunks.length; i++) {
			MapChunk chunk = region.chunk(i);
			if (chunk == null) {
				continue;
			}
			chunks[i] = chunk;
			present[i >> 6] |= 1L << (i & 63);
			for (int c = 0; c < MapChunk.AREA; c++) {
				addToPalette(blockIndex, blockPalette, chunk.top(c));
				addToPalette(blockIndex, blockPalette, chunk.floor(c));
				addToPalette(biomeIndex, biomePalette, chunk.biome(c));
			}
		}

		Files.createDirectories(file.getParent());
		Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
			new DeflaterOutputStream(Files.newOutputStream(temporary), new Deflater(Deflater.DEFAULT_COMPRESSION), 1 << 16), 1 << 16))) {
			out.writeInt(MAGIC);
			out.writeInt(VERSION);
			// Heights are kept above the dimension's bottom, which the file names so any dimension's map can be read.
			out.writeInt(minY);
			int[] overview = region.overview;
			out.writeBoolean(overview != null);
			if (overview != null) {
				out.writeInt(region.overviewFingerprint);
				for (int pixel : overview) {
					out.writeByte(pixel >>> 24);
					out.writeByte(pixel >> 16);
					out.writeByte(pixel >> 8);
					out.writeByte(pixel);
				}
			}
			for (long bits : present) {
				out.writeLong(bits);
			}
			out.writeInt(blockPalette.size());
			for (int i = 0; i < blockPalette.size(); i++) {
				out.writeUTF(BlockStateParser.serialize(Block.stateById(blockPalette.getInt(i))));
			}
			out.writeInt(biomePalette.size());
			for (int i = 0; i < biomePalette.size(); i++) {
				Biome biome = biomes.byId(biomePalette.getInt(i));
				Identifier key = biome == null ? null : biomes.getKey(biome);
				out.writeUTF(key == null ? "" : key.toString());
			}
			boolean wideBlocks = blockPalette.size() > 256;
			boolean wideBiomes = biomePalette.size() > 256;
			for (MapChunk chunk : chunks) {
				if (chunk == null) {
					continue;
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					writeIndex(out, blockIndex.get(chunk.top(c)), wideBlocks);
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					out.writeShort(chunk.topY(c) - minY);
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					writeIndex(out, blockIndex.get(chunk.floor(c)), wideBlocks);
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					// How far under the top the floor is; it's never more than the sampler's depth limit.
					out.writeByte(chunk.floor(c) == MapChunk.NONE ? 0 : Math.clamp(chunk.topY(c) - chunk.floorY(c), 0, 255));
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					writeIndex(out, biomeIndex.get(chunk.biome(c)), wideBiomes);
				}
			}
		}
		Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	/** Reads only the overview at the start of a region file, or null when there is none. */
	static @Nullable Contents readOverview(Path file) throws IOException {
		if (!Files.isRegularFile(file)) {
			return null;
		}
		try (DataInputStream in = open(file)) {
			if (!readHeader(in)) {
				return null;
			}
			in.readInt();
			int fingerprint = 0;
			int[] overview = null;
			if (in.readBoolean()) {
				fingerprint = in.readInt();
				overview = readOverviewPixels(in);
			}
			return new Contents(null, overview, fingerprint);
		}
	}

	static @Nullable Contents read(Path file, Registry<Biome> biomes) throws IOException {
		if (!Files.isRegularFile(file)) {
			return null;
		}
		try (DataInputStream in = open(file)) {
			if (!readHeader(in)) {
				return null;
			}
			int minY = in.readInt();
			int fingerprint = 0;
			int[] overview = null;
			if (in.readBoolean()) {
				fingerprint = in.readInt();
				overview = readOverviewPixels(in);
			}
			long[] present = new long[MapRegion.CHUNKS * MapRegion.CHUNKS / 64];
			for (int i = 0; i < present.length; i++) {
				present[i] = in.readLong();
			}
			int[] blockPalette = new int[in.readInt()];
			for (int i = 0; i < blockPalette.length; i++) {
				blockPalette[i] = Block.getId(parseState(in.readUTF()));
			}
			int[] biomePalette = new int[in.readInt()];
			for (int i = 0; i < biomePalette.length; i++) {
				Identifier key = Identifier.tryParse(in.readUTF());
				Biome biome = key == null ? null : biomes.getValue(key);
				biomePalette[i] = biome == null ? -1 : biomes.getId(biome);
			}
			boolean wideBlocks = blockPalette.length > 256;
			boolean wideBiomes = biomePalette.length > 256;
			MapChunk[] chunks = new MapChunk[MapRegion.CHUNKS * MapRegion.CHUNKS];
			for (int i = 0; i < chunks.length; i++) {
				if ((present[i >> 6] & 1L << (i & 63)) == 0) {
					continue;
				}
				int[] top = new int[MapChunk.AREA];
				short[] topY = new short[MapChunk.AREA];
				int[] floor = new int[MapChunk.AREA];
				short[] floorY = new short[MapChunk.AREA];
				short[] biome = new short[MapChunk.AREA];
				for (int c = 0; c < MapChunk.AREA; c++) {
					top[c] = blockPalette[readIndex(in, wideBlocks)];
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					topY[c] = (short) (in.readUnsignedShort() + minY);
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					floor[c] = blockPalette[readIndex(in, wideBlocks)];
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					floorY[c] = (short) (topY[c] - in.readUnsignedByte());
				}
				for (int c = 0; c < MapChunk.AREA; c++) {
					biome[c] = (short) biomePalette[readIndex(in, wideBiomes)];
				}
				chunks[i] = new MapChunk(top, topY, floor, floorY, biome);
			}
			return new Contents(chunks, overview, fingerprint);
		}
	}

	private static DataInputStream open(Path file) throws IOException {
		InputStream raw = Files.newInputStream(file);
		return new DataInputStream(new BufferedInputStream(new InflaterInputStream(raw), 1 << 16));
	}

	private static boolean readHeader(DataInputStream in) throws IOException {
		return in.readInt() == MAGIC && in.readInt() == VERSION;
	}

	private static int[] readOverviewPixels(DataInputStream in) throws IOException {
		int[] pixels = new int[MapRegion.OVERVIEW_SIZE * MapRegion.OVERVIEW_SIZE];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = in.readInt();
		}
		return pixels;
	}

	private static BlockState parseState(String text) {
		try {
			return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, text, false).blockState();
		} catch (CommandSyntaxException | RuntimeException exception) {
			return Block.stateById(MapChunk.NONE);
		}
	}

	private static void addToPalette(Int2IntOpenHashMap index, IntArrayList palette, int id) {
		if (!index.containsKey(id)) {
			index.put(id, palette.size());
			palette.add(id);
		}
	}

	private static void writeIndex(OutputStream out, int index, boolean wide) throws IOException {
		if (wide) {
			out.write(index >> 8);
		}
		out.write(index);
	}

	private static int readIndex(DataInputStream in, boolean wide) throws IOException {
		return wide ? in.readUnsignedShort() : in.readUnsignedByte();
	}
}
