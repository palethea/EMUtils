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
 * and when that one lets you see through it (water, glass, plants), the first solid block under it. Under a
 * tree's leaves, that's the ground or its trunk, kept for a 3D view of the map (#218).
 *
 * <p>Under a ceiling, like the Nether's bedrock roof (#221), and for a cave layer (#222), it starts lower: from
 * a height inside the rock it goes down to the first open space and shows the floor under it. Works on
 * chunks the client loaded and on chunks read from a singleplayer world's files (#215). Runs on the client
 * thread, which owns the block looks; the result is handed to the tile baker.
 */
final class MapSampler {
	/** How far down under water or glass the map looks for the floor. Deeper floors are left out, like the open ocean's. */
	private static final int MAX_FLOOR_DEPTH = 64;
	/** Sampled from the top, or under a dimension's ceiling from just below it: the surface map. */
	static final int SURFACE = Integer.MIN_VALUE;
	/** How far under where it starts the map looks for the floor: under a roof, its thickness and the open space below. */
	private static final int CAVE_DEPTH = 64;
	/**
	 * How far down a cave layer looks through rock for open space: a little past its own 16 blocks, so it
	 * shows the caves in it, not ones far below.
	 */
	private static final int CAVE_SKIP = 24;

	private MapSampler() {
	}

	/** A chunk's blocks and biomes, wherever they come from. */
	interface Columns {
		int minY();

		/** The y of the highest block that isn't air in a column, or below {@link #minY()} when there is none. */
		int highest(int localX, int localZ);

		/** The y of the top of the dimension's ceiling, like the Nether's bedrock roof, or {@link #SURFACE} without one. */
		int ceiling();

		BlockState state(int localX, int y, int localZ);

		/** The biome's id in the level's biome registry, or -1 when unknown. */
		int biome(int localX, int y, int localZ);
	}

	/** Samples a loaded chunk, from {@code startY} down for a cave layer or from the top for {@link #SURFACE}. */
	static MapChunk sample(ClientLevel level, LevelChunk chunk, Registry<Biome> biomes, int startY) {
		return sample(new LoadedColumns(level, chunk, biomes), startY);
	}

	static MapChunk sample(Columns columns, int startY) {
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
				int highest = columns.highest(localX, localZ);
				int start = startY == SURFACE ? columns.ceiling() : startY;
				// Starting inside the rock (or the roof), the map looks down to the first open space first.
				boolean underground = start != SURFACE && start <= highest;
				int from = underground ? start : highest;
				int lowest = underground ? Math.max(minY, from - CAVE_DEPTH) : minY;
				int skipTo = startY == SURFACE ? lowest : Math.max(lowest, from - CAVE_SKIP);
				while (underground && from >= skipTo) {
					BlockState state = columns.state(localX, from, localZ);
					if (MapBlockLooks.ensure(state, Block.getId(state)).kind() == MapBlockLook.Kind.INVISIBLE) {
						break;
					}
					from--;
				}
				// No open space near enough: solid rock, which the map shows as nothing.
				boolean open = !underground || from >= skipTo;
				for (int y = from; open && y >= lowest; y--) {
					if (topId != MapChunk.NONE && columnTopY - y > MAX_FLOOR_DEPTH) {
						// Glass over a ravine, or plants on an island over the void: no floor near enough.
						break;
					}
					BlockState state = columns.state(localX, y, localZ);
					int id = Block.getId(state);
					MapBlockLook look = MapBlockLooks.ensure(state, id);
					MapBlockLook.Kind kind = look.kind();
					if (kind == MapBlockLook.Kind.INVISIBLE) {
						continue;
					}
					MapBlockLook.Part part = look.part();
					if (topId == MapChunk.NONE) {
						topId = id;
						columnTopY = y;
						if (kind == MapBlockLook.Kind.OPAQUE && part != MapBlockLook.Part.CANOPY) {
							break;
						}
						continue;
					}
					if (kind == MapBlockLook.Kind.OPAQUE && part != MapBlockLook.Part.CANOPY) {
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

	/**
	 * Samples made-up columns with each rule and says what came out wrong, or returns an empty text; for UI
	 * snapshot checks. Under a roof the ground below shows, a cave layer shows the floor of the cave it starts
	 * above, and one that starts in the open air shows the surface.
	 */
	static String checkRules() {
		BlockState bedrock = net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState();
		BlockState netherrack = net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState();
		BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
		BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		// The Nether: a roof down to 100, open space down to 70, then the ground.
		Columns nether = column(127, y -> y >= 123 ? bedrock : y >= 100 ? netherrack : y >= 70 ? air : netherrack);
		// The Overworld: ground at 80, a cave from 39 down to 30.
		Columns overworld = column(SURFACE, y -> y > 80 ? air : y >= 40 ? stone : y >= 30 ? air : stone);
		String problem = expect(sample(nether, SURFACE), 69, "the Nether's surface");
		if (problem.isEmpty()) {
			problem = expect(sample(overworld, SURFACE), 80, "the surface");
		}
		if (problem.isEmpty()) {
			problem = expect(sample(overworld, 47), 29, "a cave layer starting in the rock above a cave");
		}
		if (problem.isEmpty()) {
			problem = expect(sample(overworld, 95), 80, "a cave layer starting in the open air");
		}
		if (problem.isEmpty() && sample(overworld, 79).top(0) != MapChunk.NONE) {
			problem = "a cave layer in solid rock, with no cave near enough, wasn't left empty";
		}
		return problem;
	}

	private static String expect(MapChunk chunk, int topY, String what) {
		return chunk.top(0) != MapChunk.NONE && chunk.topY(0) == topY ? "" : what + " came out at " + chunk.topY(0) + " instead of " + topY;
	}

	/** Made-up columns, all alike, from y 0 up to 127, with a ceiling at {@code ceiling} or none. */
	private static Columns column(int ceiling, java.util.function.IntFunction<BlockState> blocks) {
		return new Columns() {
			@Override
			public int minY() {
				return 0;
			}

			@Override
			public int highest(int localX, int localZ) {
				for (int y = 127; y >= 0; y--) {
					if (!blocks.apply(y).isAir()) {
						return y;
					}
				}
				return -1;
			}

			@Override
			public int ceiling() {
				return ceiling;
			}

			@Override
			public BlockState state(int localX, int y, int localZ) {
				return y < 0 || y > 127 ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : blocks.apply(y);
			}

			@Override
			public int biome(int localX, int y, int localZ) {
				return -1;
			}
		};
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
		public int ceiling() {
			return level.dimensionType().hasCeiling() ? level.getMinY() + level.dimensionType().logicalHeight() - 1 : SURFACE;
		}

		/**
		 * The heightmap's top, unless it's missing or points at air, as some servers send it (Hypixel's islands):
		 * then the blocks are looked through from the highest section that has any.
		 */
		@Override
		public int highest(int localX, int localZ) {
			int minY = level.getMinY();
			int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
			if (height >= minY && !state(localX, height, localZ).isAir()) {
				return height;
			}
			int section = chunk.getHighestFilledSectionIndex();
			for (int y = section < 0 ? minY - 1 : minY + section * 16 + 15; y >= minY; y--) {
				if (!state(localX, y, localZ).isAir()) {
					return y;
				}
			}
			return minY - 1;
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
