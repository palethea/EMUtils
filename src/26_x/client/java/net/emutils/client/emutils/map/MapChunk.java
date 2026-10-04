package net.emutils.client.emutils.map;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.jspecify.annotations.Nullable;

/**
 * What the map knows about one chunk (#212): for each of its 16 x 16 columns, the block seen from above and
 * its height, the solid block under it when that block lets you see through (water, glass, plants), and the
 * biome. Block states and biomes are ids, not colors, so textures, tints and shading are applied when a tile
 * is drawn and follow the resource pack.
 *
 * <p>Kept small, as the map holds tens of thousands (#228): a chunk has few different blocks and biomes, so
 * columns store a byte each, an index into the chunk's own list of them, and only a chunk with more than
 * 256 different blocks keeps their ids whole. That's about 1.9 KB a chunk instead of 3.6 KB.
 *
 * <p>Samples are never changed once made: a new sample replaces the whole chunk, so the tile baker can read
 * them from its own thread.
 */
public final class MapChunk {
	public static final int SIZE = 16;
	public static final int AREA = SIZE * SIZE;
	/** Air's block state id, also used for "no floor". */
	public static final int NONE = 0;
	private static final int BYTE_PALETTE = 256;

	/** The blocks in the chunk, which {@link #tops} and {@link #floors} point into; null when they hold ids. */
	private final int @Nullable [] blocks;
	private final byte @Nullable [] tops;
	private final byte @Nullable [] floors;
	/** The blocks' ids themselves, for a chunk with too many different ones for a byte. */
	private final int @Nullable [] topIds;
	private final int @Nullable [] floorIds;
	private final short[] topY;
	private final short[] floorY;
	private final int[] biomes;
	private final byte[] biome;
	private final boolean empty;

	MapChunk(int[] top, short[] topY, int[] floor, short[] floorY, short[] biome) {
		this.topY = topY;
		this.floorY = floorY;
		Int2IntOpenHashMap index = new Int2IntOpenHashMap();
		int[] palette = new int[BYTE_PALETTE];
		byte[] tops = new byte[AREA];
		byte[] floors = new byte[AREA];
		boolean fits = true;
		for (int c = 0; c < AREA && fits; c++) {
			int t = indexOf(index, palette, top[c]);
			int f = t < 0 ? -1 : indexOf(index, palette, floor[c]);
			if (t < 0 || f < 0) {
				fits = false;
			} else {
				tops[c] = (byte) t;
				floors[c] = (byte) f;
			}
		}
		if (fits) {
			this.blocks = java.util.Arrays.copyOf(palette, index.size());
			this.tops = tops;
			this.floors = floors;
			this.topIds = null;
			this.floorIds = null;
		} else {
			this.blocks = null;
			this.tops = null;
			this.floors = null;
			this.topIds = top;
			this.floorIds = floor;
		}
		// A chunk is in one or a few biomes, never more than a byte's worth: there are only 256 columns.
		Int2IntOpenHashMap biomeIndex = new Int2IntOpenHashMap();
		int[] biomePalette = new int[BYTE_PALETTE];
		this.biome = new byte[AREA];
		for (int c = 0; c < AREA; c++) {
			this.biome[c] = (byte) indexOf(biomeIndex, biomePalette, biome[c]);
		}
		this.biomes = java.util.Arrays.copyOf(biomePalette, biomeIndex.size());
		boolean empty = true;
		for (int c = 0; c < AREA && empty; c++) {
			empty = top[c] == NONE;
		}
		this.empty = empty;
	}

	/** A value's place in a palette, adding it if it's new; -1 when the palette is full. */
	private static int indexOf(Int2IntOpenHashMap index, int[] palette, int value) {
		int known = index.getOrDefault(value, -1);
		if (known >= 0) {
			return known;
		}
		if (index.size() >= palette.length) {
			return -1;
		}
		int at = index.size();
		palette[at] = value;
		index.put(value, at);
		return at;
	}

	static int index(int localX, int localZ) {
		return localZ * SIZE + localX;
	}

	/** Nothing in it is drawn: all air, as in a void world. */
	boolean isEmpty() {
		return empty;
	}

	/** The block state id seen from above, or {@link #NONE} when the column is empty. */
	public int top(int index) {
		return blocks != null ? blocks[tops[index] & 0xFF] : topIds[index];
	}

	public int topY(int index) {
		return topY[index];
	}

	/** The solid block under a see-through top, or {@link #NONE}. */
	public int floor(int index) {
		return blocks != null ? blocks[floors[index] & 0xFF] : floorIds[index];
	}

	public int floorY(int index) {
		return floorY[index];
	}

	/** The biome's id in the level's biome registry. */
	public int biome(int index) {
		return biomes[biome[index] & 0xFF];
	}

	/** About how many bytes the chunk takes in memory, for UI snapshot checks. */
	int bytesForSnapshot() {
		int arrays = topY.length * 2 + floorY.length * 2 + biome.length + biomes.length * 4;
		arrays += blocks != null ? tops.length + floors.length + blocks.length * 4 : topIds.length * 4 + floorIds.length * 4;
		// Each array has a header of about 16 bytes, and the chunk itself about 40.
		return arrays + 16 * 7 + 40;
	}
}
