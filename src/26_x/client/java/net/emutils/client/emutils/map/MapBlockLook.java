package net.emutils.client.emutils.map;

import net.minecraft.client.color.block.BlockTintSource;
import org.jspecify.annotations.Nullable;

/**
 * How one block state looks on the map (#212): its top as seen from above and its south side, taken from
 * the block's model and textures, so the map follows the resource pack. Each is kept at 16, 4 and 1 pixels
 * per block for the map's detail levels.
 */
public final class MapBlockLook {
	public static final MapBlockLook INVISIBLE = new MapBlockLook(Kind.INVISIBLE, Layer.EMPTY, null, null, false);

	public enum Kind {
		/** Not drawn at all: air, barriers, light blocks. The map looks past it. */
		INVISIBLE,
		/** Covers the column, so nothing under it shows. */
		OPAQUE,
		/** Drawn over the solid block under it: plants, glass, fences, torches. */
		SEE_THROUGH,
		/** Water and lava: drawn over the block under it, darker the deeper it is. */
		FLUID
	}

	private final Kind kind;
	private final Layer top;
	private final @Nullable Layer side;
	private final @Nullable BlockTintSource tint;
	private final boolean solid;
	private final Part part;

	/** What a block is part of, for the tilted view (#217): trees' leaves float over the ground on their trunk. */
	public enum Part {
		BLOCK,
		/** Leaves: the map looks past them for the ground under the tree. */
		CANOPY,
		/** Logs and stems, drawn as the trunk under a canopy. */
		TRUNK
	}

	MapBlockLook(Kind kind, Layer top, @Nullable Layer side, @Nullable BlockTintSource tint, boolean solid) {
		this(kind, top, side, tint, solid, Part.BLOCK);
	}

	MapBlockLook(Kind kind, Layer top, @Nullable Layer side, @Nullable BlockTintSource tint, boolean solid, Part part) {
		this.kind = kind;
		this.top = top;
		this.side = side;
		this.tint = tint;
		this.solid = solid;
		this.part = part;
	}

	public Part part() {
		return part;
	}

	public Kind kind() {
		return kind;
	}

	public Layer top() {
		return top;
	}

	/** The block's south side, drawn where the map shows it standing above the ground south of it, or null. */
	public @Nullable Layer side() {
		return side;
	}

	/** Where the tinted pixels get their color from (grass, foliage, water), or null when nothing is tinted. */
	public @Nullable BlockTintSource tint() {
		return tint;
	}

	/** It fills its whole block, so its height counts for the map's shading; plants and torches don't. */
	public boolean solid() {
		return solid;
	}

	/**
	 * A square picture at the map's three detail levels: 16 x 16, 4 x 4 and 1 x 1 pixels, as ARGB with
	 * straight alpha, plus which pixels take the block's tint.
	 */
	public static final class Layer {
		static final int[] RESOLUTIONS = {16, 4, 1};
		static final Layer EMPTY = new Layer(new int[MapChunk.AREA], new boolean[MapChunk.AREA]);

		private final int[][] pixels = new int[RESOLUTIONS.length][];
		private final boolean[][] tinted = new boolean[RESOLUTIONS.length][];

		Layer(int[] pixels16, boolean[] tinted16) {
			pixels[0] = pixels16;
			tinted[0] = tinted16;
			for (int level = 1; level < RESOLUTIONS.length; level++) {
				downsample(pixels16, tinted16, RESOLUTIONS[level], level);
			}
		}

		/** The picture at a detail level: 0 is 16 x 16, 1 is 4 x 4, 2 is 1 x 1. */
		public int[] pixels(int level) {
			return pixels[level];
		}

		public boolean[] tinted(int level) {
			return tinted[level];
		}

		/** The average color of the 16 x 16 picture's covered pixels, opaque. */
		public int average() {
			return pixels[RESOLUTIONS.length - 1][0] | 0xFF000000;
		}

		/** Averages blocks of pixels, weighting colors by their alpha so see-through pixels don't darken the result. */
		private void downsample(int[] source, boolean[] sourceTinted, int size, int level) {
			int step = 16 / size;
			int[] out = new int[size * size];
			boolean[] outTinted = new boolean[size * size];
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					long a = 0;
					long r = 0;
					long g = 0;
					long b = 0;
					int tintedCount = 0;
					int covered = 0;
					for (int dy = 0; dy < step; dy++) {
						for (int dx = 0; dx < step; dx++) {
							int i = (y * step + dy) * 16 + x * step + dx;
							int pixel = source[i];
							int alpha = pixel >>> 24;
							a += alpha;
							r += (long) ((pixel >> 16) & 0xFF) * alpha;
							g += (long) ((pixel >> 8) & 0xFF) * alpha;
							b += (long) (pixel & 0xFF) * alpha;
							if (alpha > 0) {
								covered++;
								if (sourceTinted[i]) {
									tintedCount++;
								}
							}
						}
					}
					int count = step * step;
					int index = y * size + x;
					if (a > 0) {
						out[index] = (int) (a / count) << 24 | (int) (r / a) << 16 | (int) (g / a) << 8 | (int) (b / a);
					}
					outTinted[index] = tintedCount * 2 > covered;
				}
			}
			pixels[level] = out;
			tinted[level] = outTinted;
		}
	}
}
