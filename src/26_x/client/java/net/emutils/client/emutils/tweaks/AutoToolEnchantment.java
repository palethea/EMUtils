package net.emutils.client.emutils.tweaks;

import java.util.List;
import java.util.Set;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The enchantments Auto Tool can prefer (#50), in an order the player picks. Silk Touch and Fortune only
 * count on blocks where they change what drops; Efficiency always counts, since it's how fast a tool is.
 */
public enum AutoToolEnchantment {
	FORTUNE(EMUtilsTexts.OPTION_AUTO_TOOL_FORTUNE, Enchantments.FORTUNE),
	SILK_TOUCH(EMUtilsTexts.OPTION_AUTO_TOOL_SILK_TOUCH, Enchantments.SILK_TOUCH),
	EFFICIENCY(EMUtilsTexts.OPTION_AUTO_TOOL_EFFICIENCY, Enchantments.EFFICIENCY);

	public static final List<AutoToolEnchantment> DEFAULT_ORDER = List.of(FORTUNE, SILK_TOUCH, EFFICIENCY);

	/** Blocks outside the ore, glass and ice tags that drop something else, or nothing, without Silk Touch. */
	private static final Set<Block> SILK_TOUCH_BLOCKS = Set.of(
		Blocks.ENDER_CHEST,
		Blocks.GLOWSTONE,
		Blocks.SEA_LANTERN,
		Blocks.MELON,
		Blocks.AMETHYST_CLUSTER,
		Blocks.SCULK,
		Blocks.SCULK_VEIN,
		Blocks.SCULK_SENSOR,
		Blocks.CALIBRATED_SCULK_SENSOR,
		Blocks.SCULK_SHRIEKER,
		Blocks.SCULK_CATALYST,
		Blocks.TURTLE_EGG,
		Blocks.BEE_NEST,
		Blocks.CAMPFIRE,
		Blocks.SOUL_CAMPFIRE
	);
	/** Blocks outside the ore tags that drop more with Fortune. */
	private static final Set<Block> FORTUNE_BLOCKS = Set.of(
		Blocks.GLOWSTONE,
		Blocks.SEA_LANTERN,
		Blocks.MELON,
		Blocks.AMETHYST_CLUSTER
	);

	private final String labelKey;
	private final ResourceKey<Enchantment> key;

	AutoToolEnchantment(String labelKey, ResourceKey<Enchantment> key) {
		this.labelKey = labelKey;
		this.key = key;
	}

	public String labelKey() {
		return labelKey;
	}

	ResourceKey<Enchantment> key() {
		return key;
	}

	/** Whether this enchantment changes anything about breaking {@code state}. */
	boolean matters(BlockState state) {
		return switch (this) {
			case FORTUNE -> state.is(ConventionalBlockTags.ORES) || FORTUNE_BLOCKS.contains(state.getBlock());
			case SILK_TOUCH -> state.is(ConventionalBlockTags.ORES)
				|| state.is(ConventionalBlockTags.GLASS_BLOCKS)
				|| state.is(ConventionalBlockTags.GLASS_PANES)
				|| state.is(ConventionalBlockTags.BOOKSHELVES)
				|| state.is(BlockTags.ICE)
				|| SILK_TOUCH_BLOCKS.contains(state.getBlock());
			case EFFICIENCY -> true;
		};
	}

	public static @Nullable AutoToolEnchantment fromName(@Nullable String name) {
		if (name != null) {
			for (AutoToolEnchantment enchantment : values()) {
				if (enchantment.name().equals(name)) {
					return enchantment;
				}
			}
		}
		return null;
	}
}
