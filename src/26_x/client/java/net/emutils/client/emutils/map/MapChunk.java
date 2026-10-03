package net.emutils.client.emutils.map;

/**
 * What the map knows about one chunk (#212): for each of its 16 x 16 columns, the block seen from above and
 * its height, the solid block under it when that block lets you see through (water, glass, plants), and the
 * biome. Block states and biomes are ids, not colors, so textures, tints and shading are applied when a tile
 * is drawn and follow the resource pack.
 *
 * <p>Samples are never changed once made: a new sample replaces the whole chunk, so the tile baker can read
 * them from its own thread.
 */
public final class MapChunk {
	public static final int SIZE = 16;
	public static final int AREA = SIZE * SIZE;
	/** Air's block state id, also used for "no floor". */
	public static final int NONE = 0;

	private final int[] top;
	private final short[] topY;
	private final int[] floor;
	private final short[] floorY;
	private final short[] biome;

	MapChunk(int[] top, short[] topY, int[] floor, short[] floorY, short[] biome) {
		this.top = top;
		this.topY = topY;
		this.floor = floor;
		this.floorY = floorY;
		this.biome = biome;
	}

	static int index(int localX, int localZ) {
		return localZ * SIZE + localX;
	}

	/** The block state id seen from above, or {@link #NONE} when the column is empty. */
	public int top(int index) {
		return top[index];
	}

	public int topY(int index) {
		return topY[index];
	}

	/** The solid block under a see-through top, or {@link #NONE}. */
	public int floor(int index) {
		return floor[index];
	}

	public int floorY(int index) {
		return floorY[index];
	}

	/** The biome's id in the level's biome registry. */
	public int biome(int index) {
		return biome[index];
	}
}
