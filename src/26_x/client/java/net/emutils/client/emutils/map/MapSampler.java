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
 * Reads what the map shows of a loaded chunk (#212): for each column the first block from the top that is
 * drawn, and when that one lets you see through it (water, glass, plants), the first solid block under it.
 * Runs on the client thread, which owns the chunks; the result is handed to the tile baker.
 */
final class MapSampler {
	/** How far down under water or glass the map looks for the floor. Deeper floors are left out, like the open ocean's. */
	private static final int MAX_FLOOR_DEPTH = 64;

	private MapSampler() {
	}

	static MapChunk sample(ClientLevel level, LevelChunk chunk, Registry<Biome> biomes) {
		int[] top = new int[MapChunk.AREA];
		short[] topY = new short[MapChunk.AREA];
		int[] floor = new int[MapChunk.AREA];
		short[] floorY = new short[MapChunk.AREA];
		short[] biome = new short[MapChunk.AREA];
		int minY = level.getMinY();
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int localZ = 0; localZ < MapChunk.SIZE; localZ++) {
			for (int localX = 0; localX < MapChunk.SIZE; localX++) {
				int index = MapChunk.index(localX, localZ);
				int x = baseX + localX;
				int z = baseZ + localZ;
				int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
				int topId = MapChunk.NONE;
				int columnTopY = minY;
				int floorId = MapChunk.NONE;
				int columnFloorY = minY;
				for (; y >= minY; y--) {
					BlockState state = chunk.getBlockState(pos.set(x, y, z));
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
					if (columnTopY - y > MAX_FLOOR_DEPTH) {
						break;
					}
				}
				top[index] = topId;
				topY[index] = (short) columnTopY;
				floor[index] = floorId;
				floorY[index] = (short) columnFloorY;
				Biome columnBiome = chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(columnTopY), QuartPos.fromBlock(z)).value();
				biome[index] = (short) biomes.getId(columnBiome);
			}
		}
		return new MapChunk(top, topY, floor, floorY, biome);
	}
}
