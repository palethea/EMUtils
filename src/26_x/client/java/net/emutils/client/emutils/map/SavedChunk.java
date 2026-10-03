package net.emutils.client.emutils.map;

import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A chunk read from a singleplayer world's files (#215), such as one Chunky generated that you never went
 * near. Only its blocks and biomes are unpacked from the saved sections, off the client thread; the map's
 * sampler then reads it like a loaded chunk.
 */
final class SavedChunk implements MapSampler.Columns {
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	final int chunkX;
	final int chunkZ;
	private final int minY;
	private final int minSection;
	private final Section[] sections;

	private record Section(BlockState[] palette, boolean[] air, long @Nullable [] data, int bits, int[] biomePalette, long @Nullable [] biomeData, int biomeBits) {
		BlockState state(int index) {
			return palette[unpack(data, bits, index)];
		}

		boolean isAir(int index) {
			return air[unpack(data, bits, index)];
		}

		int biome(int index) {
			return biomePalette[unpack(biomeData, biomeBits, index)];
		}
	}

	private SavedChunk(int chunkX, int chunkZ, int minY, int minSection, Section[] sections) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.minY = minY;
		this.minSection = minSection;
		this.sections = sections;
	}

	/**
	 * Unpacks a saved chunk, or returns null when it isn't fully generated or was saved by another game
	 * version, whose layout may differ; those show up once you visit them.
	 */
	static @Nullable SavedChunk parse(CompoundTag tag, int dataVersion, int minY, int height, Registry<Biome> biomes) {
		if (tag.getIntOr("DataVersion", -1) != dataVersion || !tag.getStringOr("Status", "").equals("minecraft:full")) {
			return null;
		}
		int minSection = minY >> 4;
		Section[] sections = new Section[height >> 4];
		ListTag list = tag.getListOrEmpty("sections");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag section = list.getCompoundOrEmpty(i);
			int index = section.getByteOr("Y", (byte) 0) - minSection;
			if (index < 0 || index >= sections.length) {
				continue;
			}
			sections[index] = section(section, biomes);
		}
		return new SavedChunk(tag.getIntOr("xPos", 0), tag.getIntOr("zPos", 0), minY, minSection, sections);
	}

	private static @Nullable Section section(CompoundTag tag, Registry<Biome> biomes) {
		CompoundTag states = tag.getCompoundOrEmpty("block_states");
		ListTag paletteTags = states.getListOrEmpty("palette");
		if (paletteTags.isEmpty()) {
			return null;
		}
		BlockState[] palette = new BlockState[paletteTags.size()];
		boolean[] air = new boolean[palette.length];
		boolean allAir = true;
		for (int i = 0; i < palette.length; i++) {
			palette[i] = paletteState(paletteTags, i);
			air[i] = palette[i].isAir();
			allAir &= air[i];
		}
		if (allAir) {
			return null;
		}
		long[] data = palette.length > 1 ? states.getLongArray("data").orElse(null) : null;
		int bits = Math.max(4, Mth.ceillog2(palette.length));

		CompoundTag biomeTag = tag.getCompoundOrEmpty("biomes");
		ListTag biomeNames = biomeTag.getListOrEmpty("palette");
		int[] biomePalette = new int[Math.max(1, biomeNames.size())];
		biomePalette[0] = -1;
		for (int i = 0; i < biomeNames.size(); i++) {
			Identifier id = Identifier.tryParse(biomeNames.getStringOr(i, ""));
			Biome biome = id == null ? null : biomes.getValue(id);
			biomePalette[i] = biome == null ? -1 : biomes.getId(biome);
		}
		long[] biomeData = biomePalette.length > 1 ? biomeTag.getLongArray("data").orElse(null) : null;
		int biomeBits = Mth.ceillog2(biomePalette.length);
		return new Section(palette, air, data, bits, biomePalette, biomeData, biomeBits);
	}

	/**
	 * A block state in a saved palette: a compound with its name and properties, or, for a block saved in its
	 * default state, just its name.
	 */
	private static BlockState paletteState(ListTag palette, int index) {
		Optional<String> name = palette.getString(index);
		if (name.isPresent()) {
			Identifier id = Identifier.tryParse(name.get());
			Block block = id == null ? null : BuiltInRegistries.BLOCK.getValue(id);
			return block == null ? AIR : block.defaultBlockState();
		}
		return NbtUtils.readBlockState(BuiltInRegistries.BLOCK, palette.getCompoundOrEmpty(index));
	}

	/** The palette index at {@code index} in a packed array whose values don't span longs, as the game saves them. */
	private static int unpack(long @Nullable [] data, int bits, int index) {
		if (data == null || bits == 0) {
			return 0;
		}
		int perLong = 64 / bits;
		int word = index / perLong;
		if (word >= data.length) {
			return 0;
		}
		return (int) (data[word] >>> (index % perLong) * bits & (1L << bits) - 1);
	}

	@Override
	public int minY() {
		return minY;
	}

	@Override
	public int highest(int localX, int localZ) {
		for (int s = sections.length - 1; s >= 0; s--) {
			Section section = sections[s];
			if (section == null) {
				continue;
			}
			for (int y = 15; y >= 0; y--) {
				if (!section.isAir((y * 16 + localZ) * 16 + localX)) {
					return minY + s * 16 + y;
				}
			}
		}
		return minY - 1;
	}

	@Override
	public BlockState state(int localX, int y, int localZ) {
		int s = (y >> 4) - minSection;
		if (s < 0 || s >= sections.length || sections[s] == null) {
			return AIR;
		}
		return sections[s].state(((y & 15) * 16 + localZ) * 16 + localX);
	}

	@Override
	public int biome(int localX, int y, int localZ) {
		int s = (y >> 4) - minSection;
		if (s < 0 || s >= sections.length || sections[s] == null) {
			return -1;
		}
		return sections[s].biome((((y & 15) >> 2) * 4 + (localZ >> 2)) * 4 + (localX >> 2));
	}
}
