package net.emutils.client.emutils.map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Reads what the map shows of a chunk (#212): for each column the first block from the top that is drawn,
 * and when that one lets you see through it (water, glass, plants), the first solid block under it. Works on
 * chunks the client loaded and on chunks read from a singleplayer world's files (#215). Runs on the client
 * thread, which owns the block looks; the result is handed to the tile baker.
 */
final class MapSampler {
	/** How far down under water or glass the map looks for the floor. Deeper floors are left out, like the open ocean's. */
	private static final int MAX_FLOOR_DEPTH = 64;

	private MapSampler() {
	}

	/** A chunk's blocks and biomes, wherever they come from. */
	interface Columns {
		int minY();

		/** The y of the highest block that isn't air in a column, or below {@link #minY()} when there is none. */
		int highest(int localX, int localZ);

		BlockState state(int localX, int y, int localZ);

		/** The biome's id in the level's biome registry, or -1 when unknown. */
		int biome(int localX, int y, int localZ);
	}

	static MapChunk sample(ClientLevel level, LevelChunk chunk, Registry<Biome> biomes) {
		return sample(new LoadedColumns(level, chunk, biomes));
	}

	static MapChunk sample(Columns columns) {
		int[] top = new int[MapChunk.AREA];
		short[] topY = new short[MapChunk.AREA];
		int[] floor = new int[MapChunk.AREA];
		short[] floorY = new short[MapChunk.AREA];
		short[] biome = new short[MapChunk.AREA];
		int minY = columns.minY();
		for (int localZ = 0; localZ < MapChunk.SIZE; localZ++) {
			for (int localX = 0; localX < MapChunk.SIZE; localX++) {
				int index = MapChunk.index(localX, localZ);
				int topId = MapChunk.NONE;
				int columnTopY = minY;
				int floorId = MapChunk.NONE;
				int columnFloorY = minY;
				for (int y = columns.highest(localX, localZ); y >= minY; y--) {
					if (topId != MapChunk.NONE && columnTopY - y > MAX_FLOOR_DEPTH) {
						// Glass over a ravine, or plants on an island over the void: no floor near enough.
						break;
					}
					BlockState state = columns.state(localX, y, localZ);
					int id = Block.getId(state);
					MapBlockLook.Kind kind = MapBlockLooks.ensure(state, id).kind();
					if (kind == MapBlockLook.Kind.INVISIBLE) {
						continue;
					}
					if (topId == MapChunk.NONE) {
						topId = id;
						columnTopY = y;
						if (kind == MapBlockLook.Kind.OPAQUE) {
							break;
						}
						continue;
					}
					if (kind == MapBlockLook.Kind.OPAQUE) {
						floorId = id;
						columnFloorY = y;
						break;
					}
				}
				top[index] = topId;
				topY[index] = (short) columnTopY;
				floor[index] = floorId;
				floorY[index] = (short) columnFloorY;
				biome[index] = (short) columns.biome(localX, columnTopY, localZ);
			}
		}
		return new MapChunk(top, topY, floor, floorY, biome);
	}

	/** A chunk the client has loaded. */
	private record LoadedColumns(ClientLevel level, LevelChunk chunk, Registry<Biome> biomes, BlockPos.MutableBlockPos pos) implements Columns {
		LoadedColumns(ClientLevel level, LevelChunk chunk, Registry<Biome> biomes) {
			this(level, chunk, biomes, new BlockPos.MutableBlockPos());
		}

		@Override
		public int minY() {
			return level.getMinY();
		}

		@Override
		public int highest(int localX, int localZ) {
			return chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
		}

		@Override
		public BlockState state(int localX, int y, int localZ) {
			return chunk.getBlockState(pos.set(chunk.getPos().getMinBlockX() + localX, y, chunk.getPos().getMinBlockZ() + localZ));
		}

		@Override
		public int biome(int localX, int y, int localZ) {
			int x = chunk.getPos().getMinBlockX() + localX;
			int z = chunk.getPos().getMinBlockZ() + localZ;
			return biomes.getId(chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)).value());
		}
	}
}
