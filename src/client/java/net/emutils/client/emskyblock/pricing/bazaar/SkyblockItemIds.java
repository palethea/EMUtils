package net.emutils.client.emskyblock.pricing.bazaar;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class SkyblockItemIds {
	private static final Gson GSON = new Gson();
	private static final Map<String, String> AUCTION_ID_ALIASES = Map.of(
		"MAGMA_LORD_NECKLACE", "MAGMA_LORD_GAUNTLET"
	);
	private static final Map<String, String> DISPLAY_NAME_ALIASES = Map.ofEntries(
		Map.entry("CLAY", "CLAY_BALL"),
		Map.entry("INK_SAC", "INK_SACK"),
		Map.entry("INK_SACK", "INK_SACK"),
		Map.entry("LILY_PAD", "WATER_LILY"),
		Map.entry("MAGMAFISH", "MAGMA_FISH"),
		Map.entry("MAGMA_FISH", "MAGMA_FISH"),
		Map.entry("SILVER_MAGMAFISH", "MAGMA_FISH_SILVER"),
		Map.entry("SILVER_MAGMA_FISH", "MAGMA_FISH_SILVER"),
		Map.entry("MYCELIUM", "MYCEL"),
		Map.entry("PUFFERFISH", "RAW_FISH-3"),
		Map.entry("RAW_COD", "RAW_FISH"),
		Map.entry("RAW_FISH", "RAW_FISH"),
		Map.entry("RAW_SALMON", "RAW_FISH-1"),
		Map.entry("RED_SAND", "SAND-1"),
		Map.entry("SULPHUR", "SULPHUR_ORE"),
		Map.entry("TROPICAL_FISH", "RAW_FISH-2"),
		Map.entry("CLOWNFISH", "RAW_FISH-2"),
		Map.entry("PRISMARINE_CRYSTAL", "PRISMARINE_CRYSTALS"),
		Map.entry("ENCHANTED_PRISMARINE_CRYSTAL", "ENCHANTED_PRISMARINE_CRYSTALS")
	);

	private SkyblockItemIds() {
	}

	@Nullable
	public static String bazaarId(ItemStack stack) {
		return resolveItemId(stack);
	}

	@Nullable
	public static String hypixelId(ItemStack stack) {
		NbtCompound extra = readExtraAttributes(stack);
		return extra == null ? null : resolveId(extra);
	}

	@Nullable
	public static String resolveItemId(ItemStack stack) {
		String id = hypixelId(stack);
		if (id != null) {
			return id;
		}

		return guessIdFromDisplayName(stack);
	}

	public static List<String> auctionLookupIds(ItemStack stack) {
		Set<String> candidates = new LinkedHashSet<>();
		NbtCompound extra = readExtraAttributes(stack);
		if (extra != null) {
			String baseId = resolveId(extra);
			if (baseId != null) {
				addAuctionCandidate(candidates, baseId);
				addAuctionVariants(candidates, extra, baseId);
			}
		}

		String guessed = guessIdFromDisplayName(stack);
		if (guessed != null) {
			addAuctionCandidate(candidates, guessed);
		}

		return List.copyOf(candidates);
	}

	private static void addAuctionCandidate(Set<String> candidates, String itemId) {
		candidates.add(itemId);
		String alias = AUCTION_ID_ALIASES.get(itemId);
		if (alias != null) {
			candidates.add(alias);
		}
	}

	@Nullable
	public static String guessFromDisplayName(String displayName) {
		if (displayName == null) {
			return null;
		}

		String stripped = Formatting.strip(displayName).trim();
		if (stripped.isEmpty()) {
			return null;
		}

		String guessed = stripped.toUpperCase(Locale.ROOT).replace(' ', '_');
		return DISPLAY_NAME_ALIASES.getOrDefault(guessed, guessed);
	}

	@Nullable
	private static String guessIdFromDisplayName(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}

		String stripped = Formatting.strip(stack.getName().getString()).trim();
		if (stripped.isEmpty()) {
			return null;
		}

		return stripped.toUpperCase(Locale.ROOT).replace(' ', '_');
	}

	@Nullable
	private static NbtCompound readExtraAttributes(ItemStack stack) {
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

	private static void addAuctionVariants(Set<String> candidates, NbtCompound extra, String id) {
		switch (id) {
			case "PET" -> addPetIds(candidates, extra);
			case "ATTRIBUTE_SHARD" -> addAttributeShardIds(candidates, extra);
			case "ENCHANTED_BOOK" -> addEnchantedBookIds(candidates, extra);
			case "POTION" -> addPotionIds(candidates, extra);
			case "RUNE", "UNIQUE_RUNE" -> addRuneIds(candidates, extra);
			case "NEW_YEAR_CAKE" -> addNewYearCakeIds(candidates, extra);
			default -> {
			}
		}
	}

	@Nullable
	private static String resolveId(NbtCompound extra) {
		String id = extra.getString("id").orElse("");
		if (id.isEmpty()) {
			return null;
		}

		if (id.startsWith("ENCHANTMENT_")) {
			return id;
		}

		if ("ENCHANTED_BOOK".equalsIgnoreCase(id)) {
			return enchantmentBookId(extra);
		}

		return id;
	}

	@Nullable
	private static String enchantmentBookId(NbtCompound extra) {
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

	private static void addPetIds(Set<String> candidates, NbtCompound extra) {
		String petInfo = extra.getString("petInfo").orElse("");
		if (!petInfo.startsWith("{")) {
			return;
		}

		try {
			JsonObject json = GSON.fromJson(petInfo, JsonObject.class);
			if (json == null || !json.has("type") || !json.has("tier")) {
				return;
			}

			String type = json.get("type").getAsString();
			String tier = json.get("tier").getAsString();
			if (type.isEmpty() || tier.isEmpty()) {
				return;
			}

			String base = "PET-" + type + "-" + tier;
			candidates.add(base);
			if (json.has("level") && json.get("level").getAsInt() >= 100) {
				candidates.add(base + "-100");
			}
		} catch (JsonParseException | IllegalStateException | NumberFormatException ignored) {
		}
	}

	private static void addAttributeShardIds(Set<String> candidates, NbtCompound extra) {
		NbtCompound attributes = extra.getCompound("attributes").orElse(null);
		if (attributes == null || attributes.isEmpty()) {
			return;
		}

		for (String attribute : attributes.getKeys()) {
			int level = attributes.getInt(attribute).orElse(0);
			if (level > 0) {
				candidates.add("ATTRIBUTE_SHARD-" + attribute.toUpperCase(Locale.ROOT) + "-" + level);
			}
		}
	}

	private static void addEnchantedBookIds(Set<String> candidates, NbtCompound extra) {
		NbtCompound enchants = extra.getCompound("enchantments").orElse(null);
		if (enchants == null || enchants.isEmpty()) {
			return;
		}

		for (String enchant : enchants.getKeys()) {
			int level = enchants.getInt(enchant).orElse(0);
			if (level > 0) {
				candidates.add("ENCHANTED_BOOK-" + enchant.toUpperCase(Locale.ROOT) + "-" + level);
			}
		}
	}

	private static void addPotionIds(Set<String> candidates, NbtCompound extra) {
		String potion = extra.getString("potion").orElse("");
		int level = extra.getInt("potion_level").orElse(0);
		if (potion.isEmpty() || level <= 0) {
			return;
		}

		StringBuilder id = new StringBuilder("POTION-")
			.append(potion.toUpperCase(Locale.ROOT))
			.append('-')
			.append(level);
		if (extra.contains("enhanced")) {
			id.append("-ENHANCED");
		}
		if (extra.contains("extended")) {
			id.append("-EXTENDED");
		}
		if (extra.contains("splash")) {
			id.append("-SPLASH");
		}

		candidates.add(id.toString());
	}

	private static void addRuneIds(Set<String> candidates, NbtCompound extra) {
		NbtCompound runes = extra.getCompound("runes").orElse(null);
		if (runes == null || runes.isEmpty()) {
			return;
		}

		for (String rune : runes.getKeys()) {
			int level = runes.getInt(rune).orElse(0);
			if (level > 0) {
				candidates.add("RUNE-" + rune.toUpperCase(Locale.ROOT) + "-" + level);
			}
		}
	}

	private static void addNewYearCakeIds(Set<String> candidates, NbtCompound extra) {
		int year = extra.getInt("new_years_cake").orElse(0);
		if (year > 0) {
			candidates.add("NEW_YEAR_CAKE-" + year);
		}
	}
}
