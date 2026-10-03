package net.emutils.client.emutils.map;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Draws one map tile's pixels from the sampled chunks (#212), on the baker's thread. Each column shows its
 * block's top texture with the biome's grass, foliage or water color, water gets darker with depth, and
 * height changes are shaded as if lit from the north-west. Where a block stands higher than the one south of
 * it, a strip of its side shows, which gives the map a slight 3D look.
 *
 * <p>Tiles are always {@link #TILE_PIXELS} pixels square. The detail level sets how many pixels a block
 * gets: 16 (its full texture), 4, or 1 (its average color), so one tile covers 16, 64 or 256 blocks. The
 * world map's far levels (#215) give a pixel to 4, 16 or 64 blocks and are put together from the regions'
 * overviews instead of their columns.
 */
final class MapTileBaker {
	static final int TILE_PIXELS = 256;
	/** The levels drawn from columns: 16, 4 and 1 pixels per block. */
	static final int COLUMN_LEVELS = MapBlockLook.Layer.RESOLUTIONS.length;
	/** Blocks a tile covers at each level; the last three are drawn from overviews. */
	private static final int[] BLOCKS_PER_TILE = {16, 64, 256, 1024, 4096, 16384};
	static final int LEVELS = BLOCKS_PER_TILE.length;
	/** Where nothing is known under see-through blocks, such as the open ocean's floor. */
	private static final int DEEP = 0xFF0B1426;
	/** How strongly each block of height difference brightens or darkens a column, per detail level. */
	private static final float[] SLOPE_SHADE = {0.03F, 0.035F, 0.05F};
	private static final int MAX_SLOPE = 6;
	private static final float PLANT_ALPHA = 0.55F;
	/** The depth water is drawn at when its floor is too deep to be known. */
	private static final int NO_FLOOR_DEPTH = 24;
	/** How far up the map a block of height lifts a column in the tilted view, in blocks: looking from about 60 degrees up. */
	static final double TILT = 0.6D;
	/** The tilted view lifts columns at most this many blocks up the map, and sinks them at most this many. */
	static final int TILT_UP = 128;
	static final int TILT_DOWN = 32;
	/** Tilted, the south faces that show are lit like the game lights them: a fifth darker than tops. */
	private static final float SOUTH_FACE_LIGHT = 0.8F;
	/** Tilted, ground at the foot of a wall or under a canopy is this much darker, like the game's smooth lighting. */
	private static final float CONTACT_SHADE = 0.82F;
	private static final float UNDER_CANOPY_SHADE = 0.78F;
	/** Tilted, a tree's leaves are drawn this many blocks thick, floating over the ground the map found under them. */
	private static final int CANOPY_DEPTH = 3;
	/** A side strip is this much darker than the block's top, like a side facing away from the light. */
	private static final float SIDE_SHADE = 0.68F;
	/** The ground just south of a raised block is slightly darker, as if in its shadow. */
	private static final float SHADOW_SHADE = 0.86F;

	private MapTileBaker() {
	}

	static int pixelsPerBlock(int level) {
		return MapBlockLook.Layer.RESOLUTIONS[level];
	}

	static int blocksPerTile(int level) {
		return BLOCKS_PER_TILE[level];
	}

	/**
	 * A drawn tile: its pixels as ABGR, the byte order of a texture's memory, ready to upload, with missing
	 * columns clear. Not complete when some of what it shows was still loading; it is drawn again later.
	 */
	record Result(int[] pixels, boolean complete) {
	}

	/** A tile, drawn top-down or, with {@code tilted}, seen slightly from the south (#217). */
	static Result bake(MapWorld world, int level, int tileX, int tileZ, boolean tilted) {
		if (level >= COLUMN_LEVELS) {
			return bakeOverviews(world, level, tileX, tileZ);
		}
		return tilted ? bakeTilted(world, level, tileX, tileZ) : bakeColumns(world, level, tileX, tileZ, null);
	}

	/**
	 * A tile drawn from columns. With {@code only}, just that region's chunks are read in and waited for; the
	 * neighbours' chunks at its border are used if they're in memory, as a region's overview needs.
	 */
	private static Result bakeColumns(MapWorld world, int level, int tileX, int tileZ, @Nullable MapRegion only) {
		int blocks = blocksPerTile(level);
		Flat flat = flat(world, level, tileX * blocks, tileZ * blocks, blocks, blocks, only, true);
		int[] out = new int[TILE_PIXELS * TILE_PIXELS];
		for (int i = 0; i < out.length; i++) {
			int color = flat.pixels()[i];
			out[i] = color == 0 ? 0 : toAbgr(color);
		}
		return new Result(out, flat.complete());
	}

	/**
	 * Columns drawn top-down over an area {@code width} by {@code depth} blocks: the ARGB pixels of each
	 * column's top, clear where nothing is known, and for the tilted view each block's height
	 * ({@link Integer#MIN_VALUE} where nothing is known) and side. Under a tree's leaves, the ground or trunk
	 * the map found there gets a picture, height and side of its own ({@code under}), so the canopy can float.
	 */
	private record Flat(
		int[] pixels, int[] heights, MapBlockLook.@Nullable Layer[] sides, int[] sideTints,
		int[] under, int[] underHeights, MapBlockLook.@Nullable Layer[] underSides, int[] underTints, boolean complete
	) {
	}

	/**
	 * The rows of the area a tilted tile shows, {@code from} to {@code to} (exclusive), counted from the
	 * area's north edge: columns whose lifted top and front wall land elsewhere needn't be drawn.
	 */
	private record Window(int seaLevel, int from, int to) {
		boolean shows(int row, int height, int lowest) {
			double top = row - lift(seaLevel, height);
			double bottom = row + 1 - lift(seaLevel, Math.min(height, lowest));
			return bottom > from && top < to;
		}
	}

	private static Flat flat(MapWorld world, int level, int originX, int originZ, int width, int depth, @Nullable MapRegion only, boolean strips) {
		return flat(world, level, originX, originZ, width, depth, only, strips, null);
	}

	/**
	 * Draws columns top-down. Without a {@code window}, it's the top-down view: heights are shaded as if lit
	 * from the north-west, and a strip of a raised block's side and its shadow show south of it ({@code
	 * strips}). With one, it's the input of the tilted view, which shows heights itself and lights faces like
	 * the game does; only the columns the tile shows are drawn, though every height is known.
	 */
	private static Flat flat(MapWorld world, int level, int originX, int originZ, int width, int depth, @Nullable MapRegion only, boolean strips, @Nullable Window window) {
		int res = pixelsPerBlock(level);
		int rowPixels = width * res;
		int blocks = width * depth;
		Grid grid = new Grid(world, originX - 1, originZ - 1, width + 2, depth + 2, only);
		Tints tints = new Tints(world, grid);
		int[] out = new int[rowPixels * depth * res];
		int[] heights = new int[blocks];
		java.util.Arrays.fill(heights, Integer.MIN_VALUE);
		MapBlockLook.Layer[] sides = new MapBlockLook.Layer[blocks];
		int[] sideTints = new int[blocks];
		boolean tilted = window != null;
		int[] under = tilted ? new int[out.length] : new int[0];
		int[] underHeights = new int[tilted ? blocks : 0];
		java.util.Arrays.fill(underHeights, Integer.MIN_VALUE);
		MapBlockLook.Layer[] underSides = new MapBlockLook.Layer[tilted ? blocks : 0];
		int[] underTints = new int[tilted ? blocks : 0];
		int stripMax = strips ? res / 4 + res / 8 : 0;
		for (int bz = 0; bz < depth; bz++) {
			for (int bx = 0; bx < width; bx++) {
				int g = grid.index(bx + 1, bz + 1);
				if (!grid.present[g] || grid.top[g] == MapChunk.NONE) {
					continue;
				}
				MapBlockLook top = grid.look(grid.top[g]);
				if (top == null) {
					continue;
				}
				int b = bz * width + bx;
				int height = grid.shadeHeight(g, top);
				heights[b] = height;
				MapBlockLook floor = grid.floor[g] == MapChunk.NONE ? null : grid.look(grid.floor[g]);
				boolean canopy = tilted && top.part() == MapBlockLook.Part.CANOPY && floor != null;
				if (canopy) {
					underHeights[b] = grid.floorY[g];
				}
				if (tilted) {
					int south = grid.index(bx + 1, bz + 2);
					int lowest = grid.present[south] ? grid.groundHeight(south) : height;
					if (!window.shows(bz, height, canopy ? Math.min(lowest, grid.floorY[g]) : lowest)) {
						continue;
					}
				}
				int topTint = tints.color(top, grid.top[g], g);
				int floorTint = floor == null ? 0xFFFFFFFF : tints.color(floor, grid.floor[g], g);
				MapBlockLook standing = grid.shadeLook(g);
				if (standing != null) {
					sides[b] = standing.side() != null ? standing.side() : standing.top();
					sideTints[b] = tints.color(standing, grid.shadeId(g), g);
				}
				if (canopy) {
					underSides[b] = floor.side() != null ? floor.side() : floor.top();
					underTints[b] = floorTint;
				}
				int north = grid.index(bx + 1, bz);
				int west = grid.index(bx, bz + 1);
				int northHeight = grid.present[north] ? grid.shadeHeight(north) : height;
				int westHeight = grid.present[west] ? grid.shadeHeight(west) : height;
				// Tilted, heights show for themselves; top-down, they're shaded as if lit from the north-west.
				int slope = tilted ? 0 : Math.clamp((height - northHeight) + (height - westHeight), -MAX_SLOPE, MAX_SLOPE);
				float shade = 1.0F + slope * SLOPE_SHADE[level];

				// A strip of the north neighbor's side where it stands above this column.
				int strip = 0;
				MapBlockLook.Layer side = null;
				int sideTint = 0xFFFFFFFF;
				int rise = northHeight - height;
				if (rise > 0 && stripMax > 0) {
					strip = Math.min(stripMax, Math.max(1, rise * res / 8));
					MapBlockLook northLook = grid.shadeLook(north);
					if (northLook != null) {
						side = northLook.side() != null ? northLook.side() : northLook.top();
						sideTint = tints.color(northLook, grid.shadeId(north), north);
					}
				}
				int shadowRows = rise > 0 && (strips || tilted) ? Math.max(1, res / 8) : 0;
				// Tilted, the ground at the foot of a wall is a little darker, as the game's smooth lighting has it.
				float shadow = tilted ? CONTACT_SHADE : SHADOW_SHADE;
				// Ground the map found under leaves is in the canopy's shade, and its own foot by trunks too.
				int groundRise = canopy && grid.present[north] ? grid.groundHeight(north) - grid.floorY[g] : 0;
				int groundShadowRows = groundRise > 0 ? Math.max(1, res / 8) : 0;

				int waterDepth = floor == null ? NO_FLOOR_DEPTH : grid.topY[g] - grid.floorY[g];
				for (int py = 0; py < res; py++) {
					for (int px = 0; px < res; px++) {
						int p = py * res + px;
						int color = surface(top, floor, level, p, topTint, floorTint, waterDepth);
						if (py < strip && side != null) {
							int sidePixel = pixel(side, level, py * res + px, sideTint);
							if ((sidePixel >>> 24) != 0) {
								color = scale(sidePixel, SIDE_SHADE);
							}
						} else if (py < strip + shadowRows) {
							color = scale(color, shadow);
						}
						color = scale(color, shade);
						int x = bx * res + px;
						int y = bz * res + py;
						out[y * rowPixels + x] = color | 0xFF000000;
						if (canopy) {
							int ground = scale(pixel(floor.top(), level, p, floorTint), UNDER_CANOPY_SHADE);
							under[y * rowPixels + x] = (py < groundShadowRows ? scale(ground, CONTACT_SHADE) : ground) | 0xFF000000;
						}
					}
				}
			}
		}
		return new Flat(out, heights, sides, sideTints, under, underHeights, underSides, underTints, grid.complete);
	}

	/**
	 * A tile seen slightly from the south, like a map tilted in 3D (#217). Each surface is lifted up the screen
	 * by its height above the sea: a column's top, and where ground rises toward the north, the raised block's
	 * south face as a wall in its side's picture. Under a tree's leaves the map knows the ground or trunk, so
	 * the canopy is drawn as a few blocks of leaves floating over them. Faces are lit like the game lights
	 * them: tops in full, south faces a little darker.
	 *
	 * <p>Surfaces are drawn from the nearest (south) to the farthest, each pixel only where nothing nearer
	 * covers it yet, so tall ground hides what's behind it and the ground shows under canopies. The area drawn
	 * reaches past the tile, south for tall ground lifted into it and north for low ground sunk into it.
	 */
	private static Result bakeTilted(MapWorld world, int level, int tileX, int tileZ) {
		int res = pixelsPerBlock(level);
		int blocks = blocksPerTile(level);
		int depth = TILT_DOWN + blocks + TILT_UP;
		int base = world.seaLevel();
		Flat flat = flat(world, level, tileX * blocks, tileZ * blocks - TILT_DOWN, blocks, depth, null, false, new Window(base, TILT_DOWN, TILT_DOWN + blocks));
		int[] heights = flat.heights();
		int[] underHeights = flat.underHeights();
		int[] lifts = new int[heights.length];
		int[] underLifts = new int[heights.length];
		for (int i = 0; i < lifts.length; i++) {
			lifts[i] = heights[i] == Integer.MIN_VALUE ? 0 : liftPixels(base, heights[i], res);
			underLifts[i] = underHeights[i] == Integer.MIN_VALUE ? 0 : liftPixels(base, underHeights[i], res);
		}
		int rows = depth * res;
		int[] out = new int[TILE_PIXELS * TILE_PIXELS];
		boolean[] covered = new boolean[TILE_PIXELS];
		for (int px = 0; px < TILE_PIXELS; px++) {
			int bx = px / res;
			java.util.Arrays.fill(covered, false);
			int left = TILE_PIXELS;
			for (int sy = rows - 1; sy >= 0 && left > 0; sy--) {
				int block = (sy / res) * blocks + bx;
				int color = flat.pixels()[sy * TILE_PIXELS + px];
				if (color == 0 || heights[block] == Integer.MIN_VALUE) {
					continue;
				}
				// Only a block's south edge row has a wall: its south face.
				boolean edge = sy % res == res - 1;
				int south = block + blocks;
				boolean southKnown = south < heights.length && heights[south] != Integer.MIN_VALUE;
				int southGround = !southKnown ? 0 : underHeights[south] != Integer.MIN_VALUE ? underLifts[south] : lifts[south];
				int y = sy - TILT_DOWN * res;
				if (underHeights[block] != Integer.MIN_VALUE) {
					int canopyBottom = Math.max(underHeights[block] + 1, heights[block] - (CANOPY_DEPTH - 1));
					int leaves = edge ? lifts[block] - liftPixels(base, canopyBottom - 1, res) : 0;
					left -= span(out, covered, px, y - lifts[block], leaves, color, flat.sides()[block], flat.sideTints()[block], level, res, false);
					int trunk = edge && southKnown ? Math.max(0, underLifts[block] - southGround) : 0;
					left -= span(out, covered, px, y - underLifts[block], trunk, flat.under()[sy * TILE_PIXELS + px], flat.underSides()[block], flat.underTints()[block], level, res, true);
				} else {
					int wall = edge && southKnown ? Math.max(0, lifts[block] - southGround) : 0;
					left -= span(out, covered, px, y - lifts[block], wall, color, flat.sides()[block], flat.sideTints()[block], level, res, true);
				}
			}
		}
		return new Result(out, flat.complete());
	}

	/**
	 * Draws one surface of a tilted tile into a column of pixels where nothing nearer covers it: its top's
	 * pixel at {@code y}, then {@code wall} rows of its south face, the side's picture repeating once per
	 * block of height. The face is lit like the game lights a south face; a wall standing on the ground is a
	 * little darker at its foot. Returns how many pixels it covered.
	 */
	private static int span(int[] out, boolean[] covered, int px, int y, int wall, int top, MapBlockLook.@Nullable Layer side, int sideTint, int level, int res, boolean grounded) {
		int drawn = 0;
		int last = Math.min(TILE_PIXELS - 1, y + wall);
		int foot = Math.max(1, res / 4);
		for (int row = Math.max(0, y); row <= last; row++) {
			if (covered[row]) {
				continue;
			}
			int shown = top;
			if (row > y) {
				int textureRow = (int) ((row - y - 1) / TILT) % res;
				int sidePixel = side == null ? 0 : pixel(side, level, textureRow * res + px % res, sideTint);
				float light = SOUTH_FACE_LIGHT;
				if (grounded && y + wall - row < foot) {
					light *= CONTACT_SHADE;
				}
				shown = scale((sidePixel >>> 24) == 0 ? top : sidePixel | 0xFF000000, light);
			}
			out[row * TILE_PIXELS + px] = toAbgr(shown);
			covered[row] = true;
			drawn++;
		}
		return drawn;
	}

	private static int liftPixels(int seaLevel, int height, int res) {
		return (int) Math.round(lift(seaLevel, height) * res);
	}

	/**
	 * How far the tilted view (#217) lifts something at height {@code y} up the map, in blocks, with the
	 * sea's surface staying put. Tiles, waypoints and the player's arrow all use it.
	 */
	static double lift(int seaLevel, double y) {
		return Math.clamp((y - seaLevel) * TILT, -TILT_DOWN, TILT_UP);
	}

	/** A far tile, put together from the overviews of the regions it covers, each shrunk to fit. */
	private static Result bakeOverviews(MapWorld world, int level, int tileX, int tileZ) {
		int blocks = blocksPerTile(level);
		int regionsAcross = blocks / MapRegion.BLOCKS;
		int regionPixels = TILE_PIXELS / regionsAcross;
		int shrink = MapRegion.OVERVIEW_SIZE / regionPixels;
		int firstRegionX = tileX * regionsAcross;
		int firstRegionZ = tileZ * regionsAcross;
		int[] out = new int[TILE_PIXELS * TILE_PIXELS];
		boolean complete = true;
		for (int rz = 0; rz < regionsAcross; rz++) {
			for (int rx = 0; rx < regionsAcross; rx++) {
				MapRegion region = world.overview(firstRegionX + rx, firstRegionZ + rz);
				if (region == null) {
					continue;
				}
				// A region in memory with chunks but no overview yet gets one now, rather than leave a hole.
				if (region.loaded && region.overview == null && region.states == null && region.count() > 0) {
					redrawOverview(world, region, MapBlockLooks.fingerprint());
				}
				int[] overview = region.overview;
				// A far region saved without an overview, or with one from other resource packs, is read in
				// full and drawn again; the old picture shows meanwhile.
				if (region.loaded && (overview == null || region.overviewFingerprint != MapBlockLooks.fingerprint())) {
					world.requestRedraw(region.regionX, region.regionZ);
				}
				if (overview == null) {
					complete &= region.loaded && !region.overviewStale;
					continue;
				}
				for (int py = 0; py < regionPixels; py++) {
					for (int px = 0; px < regionPixels; px++) {
						int color = averageAt(overview, MapRegion.OVERVIEW_SIZE, px * shrink, py * shrink, shrink);
						if ((color >>> 24) != 0) {
							out[(rz * regionPixels + py) * TILE_PIXELS + rx * regionPixels + px] = toAbgr(color);
						}
					}
				}
			}
		}
		return new Result(out, complete);
	}

	/**
	 * Draws a region's overview and hands it to the region, to be saved with it. One that came out unfinished
	 * keeps the old picture where the new one has nothing yet, and is drawn again soon. Baker thread.
	 */
	static void redrawOverview(MapWorld world, MapRegion region, int fingerprint) {
		region.overviewStale = false;
		Result result = overview(world, region);
		int[] pixels = result.pixels();
		int[] old = region.overview;
		if (!result.complete()) {
			if (old != null) {
				for (int i = 0; i < pixels.length; i++) {
					if ((pixels[i] >>> 24) == 0) {
						pixels[i] = old[i];
					}
				}
			}
			region.overviewStale = true;
			region.overviewBakedAt = 0L;
		}
		region.overview = pixels;
		region.overviewFingerprint = fingerprint;
		// Saved with the region, so the far zoom levels have it next time without the chunks.
		region.dirty = true;
	}

	/**
	 * Draws a region's overview from its columns: a pixel per {@link MapRegion#OVERVIEW_BLOCKS} blocks, as
	 * ARGB, clear where nothing was explored. Not complete while some of its looks were still missing.
	 */
	static Result overview(MapWorld world, MapRegion region) {
		int[] out = new int[MapRegion.OVERVIEW_SIZE * MapRegion.OVERVIEW_SIZE];
		int tilesAcross = MapRegion.BLOCKS / blocksPerTile(2);
		int tilePixels = TILE_PIXELS / MapRegion.OVERVIEW_BLOCKS;
		boolean complete = true;
		for (int tz = 0; tz < tilesAcross; tz++) {
			for (int tx = 0; tx < tilesAcross; tx++) {
				Result tile = bakeColumns(world, 2, region.regionX * tilesAcross + tx, region.regionZ * tilesAcross + tz, region);
				complete &= tile.complete();
				int[] argb = tile.pixels();
				for (int i = 0; i < argb.length; i++) {
					argb[i] = fromAbgr(argb[i]);
				}
				for (int py = 0; py < tilePixels; py++) {
					for (int px = 0; px < tilePixels; px++) {
						int color = averageAt(argb, TILE_PIXELS, px * MapRegion.OVERVIEW_BLOCKS, py * MapRegion.OVERVIEW_BLOCKS, MapRegion.OVERVIEW_BLOCKS);
						out[(tz * tilePixels + py) * MapRegion.OVERVIEW_SIZE + tx * tilePixels + px] = color;
					}
				}
			}
		}
		return new Result(out, complete);
	}

	/** The average of a square of ARGB pixels; clear pixels are unexplored ground. */
	private static int averageAt(int[] pixels, int width, int x, int y, int size) {
		if (size == 1) {
			return pixels[y * width + x];
		}
		long r = 0;
		long g = 0;
		long b = 0;
		int count = 0;
		for (int dy = 0; dy < size; dy++) {
			for (int dx = 0; dx < size; dx++) {
				int pixel = pixels[(y + dy) * width + x + dx];
				if ((pixel >>> 24) == 0) {
					continue;
				}
				r += (pixel >> 16) & 0xFF;
				g += (pixel >> 8) & 0xFF;
				b += pixel & 0xFF;
				count++;
			}
		}
		// Mostly unexplored squares stay clear, so the explored area keeps its edge instead of smearing.
		if (count * 2 < size * size) {
			return 0;
		}
		return 0xFF000000 | (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
	}

	/** A column's color at one pixel, before shading: its top over whatever shows under it. */
	private static int surface(MapBlockLook top, @Nullable MapBlockLook floor, int level, int p, int topTint, int floorTint, int depth) {
		int over = pixel(top.top(), level, p, topTint);
		if (top.kind() == MapBlockLook.Kind.OPAQUE) {
			return over;
		}
		int base = floor == null ? DEEP : pixel(floor.top(), level, p, floorTint);
		if (top.kind() == MapBlockLook.Kind.FLUID) {
			if (top.tint() == null) {
				// Lava and other untinted fluids hide what's under them.
				return over | 0xFF000000;
			}
			base = scale(base, Math.max(0.45F, 1.0F - depth * 0.045F));
			float alpha = Math.clamp(0.5F + depth * 0.045F, 0.5F, 0.88F);
			return mix(base, over | 0xFF000000, alpha);
		}
		float alpha = (over >>> 24) / 255.0F;
		if (!top.solid() && top.tint() != null) {
			// Grass, ferns and bushes cover most of the ground; drawn lighter, the ground under them still reads.
			alpha *= PLANT_ALPHA;
		}
		return mix(base, over | 0xFF000000, alpha);
	}

	private static int pixel(MapBlockLook.Layer layer, int level, int p, int tint) {
		int pixel = layer.pixels(level)[p];
		if (!layer.tinted(level)[p] || tint == 0xFFFFFFFF) {
			return pixel;
		}
		int r = ((pixel >> 16) & 0xFF) * ((tint >> 16) & 0xFF) / 255;
		int g = ((pixel >> 8) & 0xFF) * ((tint >> 8) & 0xFF) / 255;
		int b = (pixel & 0xFF) * (tint & 0xFF) / 255;
		return (pixel & 0xFF000000) | r << 16 | g << 8 | b;
	}

	private static int mix(int from, int to, float t) {
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	private static int scale(int color, float factor) {
		if (factor == 1.0F) {
			return color;
		}
		int r = Math.min(255, Math.round(((color >> 16) & 0xFF) * factor));
		int g = Math.min(255, Math.round(((color >> 8) & 0xFF) * factor));
		int b = Math.min(255, Math.round((color & 0xFF) * factor));
		return (color & 0xFF000000) | r << 16 | g << 8 | b;
	}

	private static int toAbgr(int argb) {
		return 0xFF000000 | (argb & 0xFF) << 16 | (argb & 0xFF00) | (argb >> 16) & 0xFF;
	}

	/** Back from a drawn pixel to ARGB; pixels that were never drawn stay clear. */
	private static int fromAbgr(int abgr) {
		if (abgr == 0) {
			return 0;
		}
		return 0xFF000000 | (abgr & 0xFF) << 16 | (abgr & 0xFF00) | (abgr >> 16) & 0xFF;
	}

	/** The columns a tile needs, plus a one-block border for shading and side strips, copied from the chunks. */
	private static final class Grid {
		final int originX;
		final int originZ;
		/** Columns across (west to east) and down (north to south). */
		final int size;
		final int depth;
		final boolean[] present;
		final int[] top;
		final int[] topY;
		final int[] floor;
		final int[] floorY;
		final int[] biome;
		/** False when a chunk's region was still loading or a block had no look yet. */
		boolean complete = true;

		Grid(MapWorld world, int originX, int originZ, int size, int depth, @Nullable MapRegion only) {
			this.originX = originX;
			this.originZ = originZ;
			this.size = size;
			this.depth = depth;
			int area = size * depth;
			present = new boolean[area];
			top = new int[area];
			topY = new int[area];
			floor = new int[area];
			floorY = new int[area];
			biome = new int[area];
			int firstChunkX = Math.floorDiv(originX, MapChunk.SIZE);
			int firstChunkZ = Math.floorDiv(originZ, MapChunk.SIZE);
			int lastChunkX = Math.floorDiv(originX + size - 1, MapChunk.SIZE);
			int lastChunkZ = Math.floorDiv(originZ + depth - 1, MapChunk.SIZE);
			for (int chunkZ = firstChunkZ; chunkZ <= lastChunkZ; chunkZ++) {
				for (int chunkX = firstChunkX; chunkX <= lastChunkX; chunkX++) {
					boolean outside = only != null && (chunkX >> MapRegion.SHIFT != only.regionX || chunkZ >> MapRegion.SHIFT != only.regionZ);
					MapChunk chunk = outside ? world.loadedChunk(chunkX, chunkZ) : world.chunk(chunkX, chunkZ);
					if (chunk == null) {
						if (outside) {
							continue;
						}
						if (world.loading(chunkX, chunkZ)) {
							complete = false;
						}
						continue;
					}
					int fromX = Math.max(originX, chunkX * MapChunk.SIZE);
					int toX = Math.min(originX + size, chunkX * MapChunk.SIZE + MapChunk.SIZE);
					int fromZ = Math.max(originZ, chunkZ * MapChunk.SIZE);
					int toZ = Math.min(originZ + depth, chunkZ * MapChunk.SIZE + MapChunk.SIZE);
					for (int z = fromZ; z < toZ; z++) {
						for (int x = fromX; x < toX; x++) {
							int source = MapChunk.index(x - chunkX * MapChunk.SIZE, z - chunkZ * MapChunk.SIZE);
							int g = (z - originZ) * size + (x - originX);
							present[g] = true;
							top[g] = chunk.top(source);
							topY[g] = chunk.topY(source);
							floor[g] = chunk.floor(source);
							floorY[g] = chunk.floorY(source);
							biome[g] = chunk.biome(source);
						}
					}
				}
			}
		}

		int index(int x, int z) {
			return z * size + x;
		}

		/** A state's look; one the map has no look for yet is asked for, and the tile is drawn again later. */
		@Nullable MapBlockLook look(int stateId) {
			MapBlockLook look = MapBlockLooks.get(stateId);
			if (look == null && stateId != MapChunk.NONE) {
				MapBlockLooks.request(stateId);
				complete = false;
			}
			return look;
		}

		/** The id of the block whose height counts for shading: the top, unless it's a plant or torch standing on the floor. */
		int shadeId(int g) {
			MapBlockLook look = look(top[g]);
			return look == null || usesTop(look) || floor[g] == MapChunk.NONE ? top[g] : floor[g];
		}

		@Nullable MapBlockLook shadeLook(int g) {
			return look(shadeId(g));
		}

		/** The height of the ground at a column: under a tree's leaves, what the map found there. */
		int groundHeight(int g) {
			MapBlockLook look = look(top[g]);
			if (look != null && look.part() == MapBlockLook.Part.CANOPY && floor[g] != MapChunk.NONE) {
				return floorY[g];
			}
			return shadeHeight(g);
		}

		int shadeHeight(int g) {
			MapBlockLook look = look(top[g]);
			return look == null ? topY[g] : shadeHeight(g, look);
		}

		int shadeHeight(int g, MapBlockLook topLook) {
			return usesTop(topLook) || floor[g] == MapChunk.NONE ? topY[g] : floorY[g];
		}

		private static boolean usesTop(MapBlockLook look) {
			return look.kind() == MapBlockLook.Kind.FLUID || look.solid();
		}
	}

	/**
	 * Biome colors for the tile, blended over the 3 x 3 columns around each one like the game blends them.
	 * Block tint sources ask it for colors as if it were the level, so every block, modded ones too, gets
	 * its own tint.
	 */
	private static final class Tints implements BlockAndTintGetter {
		private final MapWorld world;
		private final Grid grid;
		private final Int2ObjectOpenHashMap<Biome> biomes = new Int2ObjectOpenHashMap<>();
		private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		private int column;

		Tints(MapWorld world, Grid grid) {
			this.world = world;
			this.grid = grid;
		}

		/** The tint of a block at a grid column, or white when it has none. */
		int color(MapBlockLook look, int stateId, int g) {
			BlockTintSource source = look.tint();
			if (source == null) {
				return 0xFFFFFFFF;
			}
			column = g;
			int x = grid.originX + g % grid.size;
			int z = grid.originZ + g / grid.size;
			BlockState state = Block.stateById(stateId);
			try {
				return source.colorInWorld(state, this, pos.set(x, grid.topY[g], z)) | 0xFF000000;
			} catch (RuntimeException exception) {
				return source.color(state) | 0xFF000000;
			}
		}

		@Override
		public int getBlockTint(BlockPos at, ColorResolver resolver) {
			int centerX = column % grid.size;
			int centerZ = column / grid.size;
			int r = 0;
			int g = 0;
			int b = 0;
			int count = 0;
			for (int dz = -1; dz <= 1; dz++) {
				for (int dx = -1; dx <= 1; dx++) {
					int x = centerX + dx;
					int z = centerZ + dz;
					if (x < 0 || z < 0 || x >= grid.size || z >= grid.depth) {
						continue;
					}
					int index = grid.index(x, z);
					if (!grid.present[index]) {
						continue;
					}
					Biome biome = biome(grid.biome[index]);
					if (biome == null) {
						continue;
					}
					int color = resolver.getColor(biome, grid.originX + x, grid.originZ + z);
					r += (color >> 16) & 0xFF;
					g += (color >> 8) & 0xFF;
					b += color & 0xFF;
					count++;
				}
			}
			if (count == 0) {
				return 0xFFFFFFFF;
			}
			return 0xFF000000 | (r / count) << 16 | (g / count) << 8 | (b / count);
		}

		private @Nullable Biome biome(int id) {
			Biome biome = biomes.get(id);
			if (biome == null && !biomes.containsKey(id)) {
				biome = world.biomes().byId(id);
				biomes.put(id, biome);
			}
			return biome;
		}

		@Override
		public CardinalLighting cardinalLighting() {
			return CardinalLighting.DEFAULT;
		}

		@Override
		public LevelLightEngine getLightEngine() {
			return LevelLightEngine.EMPTY;
		}

		@Override
		public @Nullable BlockEntity getBlockEntity(BlockPos at) {
			return null;
		}

		@Override
		public BlockState getBlockState(BlockPos at) {
			return Blocks.AIR.defaultBlockState();
		}

		@Override
		public FluidState getFluidState(BlockPos at) {
			return Fluids.EMPTY.defaultFluidState();
		}

		@Override
		public int getHeight() {
			return world.level().getHeight();
		}

		@Override
		public int getMinY() {
			return world.level().getMinY();
		}
	}
}
