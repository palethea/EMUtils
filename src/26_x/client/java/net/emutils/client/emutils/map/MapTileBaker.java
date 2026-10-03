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
 * gets: 16 (its full texture), 4, or 1 (its average color), so one tile covers 16, 64 or 256 blocks.
 */
final class MapTileBaker {
	static final int TILE_PIXELS = 256;
	static final int LEVELS = MapBlockLook.Layer.RESOLUTIONS.length;
	/** Where nothing is known under see-through blocks, such as the open ocean's floor. */
	private static final int DEEP = 0xFF0B1426;
	/** How strongly each block of height difference brightens or darkens a column, per detail level. */
	private static final float[] SLOPE_SHADE = {0.03F, 0.035F, 0.05F};
	private static final int MAX_SLOPE = 6;
	private static final float PLANT_ALPHA = 0.55F;
	/** The depth water is drawn at when its floor is too deep to be known. */
	private static final int NO_FLOOR_DEPTH = 24;
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
		return TILE_PIXELS / pixelsPerBlock(level);
	}

	/** The tile's pixels as ABGR, the byte order of a texture's memory, ready to upload. Missing columns are clear. */
	static int[] bake(MapWorld world, int level, int tileX, int tileZ) {
		int res = pixelsPerBlock(level);
		int blocks = blocksPerTile(level);
		int originX = tileX * blocks;
		int originZ = tileZ * blocks;
		Grid grid = new Grid(world, originX - 1, originZ - 1, blocks + 2);
		Tints tints = new Tints(world, grid);
		int[] out = new int[TILE_PIXELS * TILE_PIXELS];
		int stripMax = res / 4 + res / 8;
		for (int bz = 0; bz < blocks; bz++) {
			for (int bx = 0; bx < blocks; bx++) {
				int g = grid.index(bx + 1, bz + 1);
				if (!grid.present[g] || grid.top[g] == MapChunk.NONE) {
					continue;
				}
				MapBlockLook top = MapBlockLooks.get(grid.top[g]);
				if (top == null) {
					continue;
				}
				MapBlockLook floor = grid.floor[g] == MapChunk.NONE ? null : MapBlockLooks.get(grid.floor[g]);
				int topTint = tints.color(top, grid.top[g], g);
				int floorTint = floor == null ? 0xFFFFFFFF : tints.color(floor, grid.floor[g], g);
				int height = grid.shadeHeight(g, top);
				int north = grid.index(bx + 1, bz);
				int west = grid.index(bx, bz + 1);
				int northHeight = grid.present[north] ? grid.shadeHeight(north) : height;
				int westHeight = grid.present[west] ? grid.shadeHeight(west) : height;
				int slope = Math.clamp((height - northHeight) + (height - westHeight), -MAX_SLOPE, MAX_SLOPE);
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
				int shadowRows = rise > 0 ? Math.max(1, res / 8) : 0;

				int depth = floor == null ? NO_FLOOR_DEPTH : grid.topY[g] - grid.floorY[g];
				for (int py = 0; py < res; py++) {
					for (int px = 0; px < res; px++) {
						int p = py * res + px;
						int color = surface(top, floor, level, p, topTint, floorTint, depth);
						if (py < strip && side != null) {
							int sidePixel = pixel(side, level, py * res + px, sideTint);
							if ((sidePixel >>> 24) != 0) {
								color = scale(sidePixel, SIDE_SHADE);
							}
						} else if (py < strip + shadowRows) {
							color = scale(color, SHADOW_SHADE);
						}
						color = scale(color, shade);
						int x = bx * res + px;
						int y = bz * res + py;
						out[y * TILE_PIXELS + x] = toAbgr(color);
					}
				}
			}
		}
		return out;
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

	/** The columns a tile needs, plus a one-block border for shading and side strips, copied from the chunks. */
	private static final class Grid {
		final int originX;
		final int originZ;
		final int size;
		final boolean[] present;
		final int[] top;
		final int[] topY;
		final int[] floor;
		final int[] floorY;
		final int[] biome;

		Grid(MapWorld world, int originX, int originZ, int size) {
			this.originX = originX;
			this.originZ = originZ;
			this.size = size;
			int area = size * size;
			present = new boolean[area];
			top = new int[area];
			topY = new int[area];
			floor = new int[area];
			floorY = new int[area];
			biome = new int[area];
			int firstChunkX = Math.floorDiv(originX, MapChunk.SIZE);
			int firstChunkZ = Math.floorDiv(originZ, MapChunk.SIZE);
			int lastChunkX = Math.floorDiv(originX + size - 1, MapChunk.SIZE);
			int lastChunkZ = Math.floorDiv(originZ + size - 1, MapChunk.SIZE);
			for (int chunkZ = firstChunkZ; chunkZ <= lastChunkZ; chunkZ++) {
				for (int chunkX = firstChunkX; chunkX <= lastChunkX; chunkX++) {
					MapChunk chunk = world.chunk(chunkX, chunkZ);
					if (chunk == null) {
						continue;
					}
					int fromX = Math.max(originX, chunkX * MapChunk.SIZE);
					int toX = Math.min(originX + size, chunkX * MapChunk.SIZE + MapChunk.SIZE);
					int fromZ = Math.max(originZ, chunkZ * MapChunk.SIZE);
					int toZ = Math.min(originZ + size, chunkZ * MapChunk.SIZE + MapChunk.SIZE);
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

		/** The id of the block whose height counts for shading: the top, unless it's a plant or torch standing on the floor. */
		int shadeId(int g) {
			MapBlockLook look = MapBlockLooks.get(top[g]);
			return look == null || usesTop(look) || floor[g] == MapChunk.NONE ? top[g] : floor[g];
		}

		@Nullable MapBlockLook shadeLook(int g) {
			return MapBlockLooks.get(shadeId(g));
		}

		int shadeHeight(int g) {
			MapBlockLook look = MapBlockLooks.get(top[g]);
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
					if (x < 0 || z < 0 || x >= grid.size || z >= grid.size) {
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
