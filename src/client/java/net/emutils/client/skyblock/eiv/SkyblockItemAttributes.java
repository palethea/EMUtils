package net.emutils.client.skyblock.eiv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.NbtList;
import net.emutils.client.skyblock.bazaar.SkyblockItemIds;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.emutils.client.skyblock.SkyblockTextUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class SkyblockItemAttributes {
	private SkyblockItemAttributes() {
	}

	@Nullable
	public static NbtCompound extraAttributes(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}

		NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (customData == null || customData.isEmpty()) {
			return null;
		}

		return readExtraAttributes(customData.copyNbt());
	}

	@Nullable
	private static NbtCompound readExtraAttributes(@Nullable NbtCompound compound) {
		if (compound == null || compound.isEmpty()) {
			return null;
		}

		if (compound.contains("id") || compound.contains("petInfo") || compound.contains("enchantments")) {
			return compound;
		}

		if (compound.contains("ExtraAttributes")) {
			NbtCompound extra = compound.getCompound("ExtraAttributes").orElse(null);
			if (extra != null && !extra.isEmpty()) {
				return extra;
			}
		}

		if (compound.contains("tag")) {
			return readExtraAttributes(compound.getCompound("tag").orElse(null));
		}

		return null;
	}

	@Nullable
	public static String itemId(ItemStack stack) {
		NbtCompound extra = extraAttributes(stack);
		if (extra == null) {
			return SkyblockItemIds.resolveItemId(stack);
		}

		String id = extra.getString("id").orElse("");
		if (id.isEmpty()) {
			return SkyblockItemIds.resolveItemId(stack);
		}

		if ("ENCHANTED_BOOK".equalsIgnoreCase(id)) {
			return enchantedBookId(extra);
		}

		return id;
	}

	@Nullable
	private static String enchantedBookId(NbtCompound extra) {
		NbtCompound enchants = extra.getCompound("enchantments").orElse(null);
		if (enchants == null || enchants.isEmpty()) {
			return null;
		}

		for (String enchant : enchants.getKeys()) {
			int level = enchants.getInt(enchant).orElse(0);
			if (level > 0) {
				return "ENCHANTMENT_" + enchant.toUpperCase(Locale.ROOT) + "_" + level;
			}
		}

		return null;
	}

	@Nullable
	public static String reforgeModifier(ItemStack stack) {
		return getString(extraAttributes(stack), "modifier");
	}

	public static boolean isRecombobulated(ItemStack stack) {
		return getInt(extraAttributes(stack), "rarity_upgrades") > 0;
	}

	public static boolean isDungeonItem(java.util.List<Text> tooltip) {
		for (Text line : tooltip) {
			if (SkyblockTextUtils.strip(line).toUpperCase(Locale.ROOT).contains("DUNGEON")) {
				return true;
			}
		}

		return false;
	}

	public static int dungeonStarCount(ItemStack stack, java.util.List<Text> tooltip) {
		NbtCompound extra = extraAttributes(stack);
		if (extra == null) {
			return 0;
		}

		int dungeonLevel = extra.getInt("dungeon_item_level").orElse(0);
		if (dungeonLevel > 0) {
			return dungeonLevel;
		}

		int upgradeLevel = extra.getInt("upgrade_level").orElse(0);
		if (upgradeLevel > 0) {
			return upgradeLevel;
		}

		return isDungeonItem(tooltip) ? 0 : 0;
	}

	public static int regularStarCount(int totalStars) {
		return Math.min(5, Math.max(0, totalStars));
	}

	public static int masterStarCount(int totalStars) {
		return Math.max(0, Math.min(5, totalStars - 5));
	}

	@Nullable
	public static String powerScrollId(ItemStack stack) {
		return getString(extraAttributes(stack), "power_ability_scroll");
	}

	public static List<String> abilityScrollIds(ItemStack stack) {
		NbtCompound extra = extraAttributes(stack);
		if (extra == null || !extra.contains("ability_scroll")) {
			return List.of();
		}

		NbtList scrollList = extra.getList("ability_scroll").orElse(null);
		if (scrollList == null || scrollList.isEmpty()) {
			return List.of();
		}

		Set<String> scrolls = new LinkedHashSet<>();
		for (int index = 0; index < scrollList.size(); index++) {
			String scrollId = scrollList.getString(index).orElse("");
			if (scrollId.isBlank()) {
				continue;
			}

			if ("ULTIMATE_WITHER_SCROLL".equalsIgnoreCase(scrollId)) {
				scrolls.add("IMPLOSION_SCROLL");
				scrolls.add("WITHER_SHIELD_SCROLL");
				scrolls.add("SHADOW_WARP_SCROLL");
				continue;
			}

			scrolls.add(scrollId.toUpperCase(Locale.ROOT));
		}

		return List.copyOf(scrolls);
	}

	@Nullable
	public static Integer hotPotatoCount(ItemStack stack) {
		return getIntOrNull(extraAttributes(stack), "hot_potato_count");
	}

	@Nullable
	public static Map<String, Integer> enchantments(ItemStack stack) {
		NbtCompound extra = extraAttributes(stack);
		if (extra == null || !extra.contains("enchantments")) {
			return null;
		}

		NbtCompound enchants = extra.getCompound("enchantments").orElse(null);
		if (enchants == null || enchants.isEmpty()) {
			return null;
		}

		Map<String, Integer> result = new LinkedHashMap<>();
		for (String key : enchants.getKeys()) {
			int level = enchants.getInt(key).orElse(0);
			if (level > 0) {
				result.put(key, level);
			}
		}

		return result.isEmpty() ? null : Map.copyOf(result);
	}

	@Nullable
	public static AppliedRune appliedRune(ItemStack stack) {
		NbtCompound extra = extraAttributes(stack);
		if (extra == null) {
			return null;
		}

		NbtCompound runes = extra.getCompound("runes").orElse(null);
		if (runes == null || runes.isEmpty()) {
			return null;
		}

		for (String runeKey : runes.getKeys()) {
			int level = runes.getInt(runeKey).orElse(0);
			if (level > 0) {
				return new AppliedRune(runeKey, level);
			}
		}

		return null;
	}

	public static List<GemstoneEntry> gemstones(ItemStack stack) {
		NbtCompound extra = extraAttributes(stack);
		if (extra == null || !extra.contains("gems")) {
			return List.of();
		}

		NbtCompound gems = extra.getCompound("gems").orElse(null);
		if (gems == null || gems.isEmpty()) {
			return List.of();
		}

		List<GemstoneEntry> result = new java.util.ArrayList<>();
		for (String key : gems.getKeys()) {
			if ("unlocked_slots".equals(key) || key.endsWith("_gem")) {
				continue;
			}

			String quality = gems.getString(key).orElse("");
			if (quality.isEmpty()) {
				NbtCompound compound = gems.getCompound(key).orElse(null);
				if (compound != null) {
					quality = compound.getString("quality").orElse("");
				}
			}

			if (quality.isEmpty()) {
				continue;
			}

			String type = key.split("_")[0].toUpperCase(Locale.ROOT);
			if (type.isEmpty()) {
				continue;
			}

			String altType = gems.getString(key + "_gem").orElse("");
			if (!altType.isEmpty()) {
				type = altType.toUpperCase(Locale.ROOT);
			}

			result.add(new GemstoneEntry(type, quality.toUpperCase(Locale.ROOT)));
		}

		return List.copyOf(result);
	}

	public static String gemstoneProductId(String type, String quality) {
		return quality + "_" + type + "_GEM";
	}

	@Nullable
	private static String getString(@Nullable NbtCompound extra, String key) {
		if (extra == null) {
			return null;
		}

		String value = extra.getString(key).orElse("");
		return value.isBlank() ? null : value;
	}

	private static int getInt(@Nullable NbtCompound extra, String key) {
		Integer value = getIntOrNull(extra, key);
		return value == null ? 0 : value;
	}

	@Nullable
	private static Integer getIntOrNull(@Nullable NbtCompound extra, String key) {
		if (extra == null || !extra.contains(key)) {
			return null;
		}

		int value = extra.getInt(key).orElse(0);
		return value == 0 ? null : value;
	}

	public record GemstoneEntry(String type, String quality) {
		public String productId() {
			return gemstoneProductId(type, quality);
		}
	}

	public record AppliedRune(String runeKey, int level) {
		public String auctionProductId() {
			return "RUNE-" + runeKey.toUpperCase(Locale.ROOT) + "-" + level;
		}
	}
}
