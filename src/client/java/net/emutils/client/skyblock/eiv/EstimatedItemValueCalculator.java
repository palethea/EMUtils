package net.emutils.client.skyblock.eiv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.skyblock.config.EMSkyblockSettings;
import net.emutils.client.skyblock.bazaar.SkyblockItemIds;
import net.emutils.client.skyblock.SkyblockPrices.PriceResult;
import net.emutils.client.skyblock.eiv.EstimatedItemValueData.EndCapEntry;
import net.emutils.client.skyblock.eiv.EstimatedItemValueData.ReforgeEntry;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.skyblock.eiv.EstimatedItemValueData.AlwaysActiveEnchant;
import net.emutils.client.skyblock.eiv.EstimatedItemValueResult.EstimatedItemValueLine;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class EstimatedItemValueCalculator {
	private static final String HOT_POTATO_BOOK = "HOT_POTATO_BOOK";
	private static final String FUMING_POTATO_BOOK = "FUMING_POTATO_BOOK";
	private static final String RECOMBOBULATOR = "RECOMBOBULATOR_3000";

	private EstimatedItemValueCalculator() {
	}

	public static EstimatedItemValueResult calculate(ItemStack stack, List<Text> tooltip) {
		if (stack.isEmpty()) {
			return EstimatedItemValueResult.empty();
		}

		List<EstimatedItemValueLine> lines = new ArrayList<>();
		lines.add(EstimatedItemValueLine.header("§aEstimated Item Value:"));

		double total = 0.0D;
		boolean hasExtras = false;

		String itemId = SkyblockItemAttributes.itemId(stack);
		boolean enchantedBook = "ENCHANTED_BOOK".equalsIgnoreCase(SkyblockItemIds.hypixelId(stack));

		ComponentResult stars = addStars(stack, itemId, tooltip);
		ComponentResult masterStars = addMasterStars(stack, tooltip);
		ComponentResult reforge = addReforge(stack, tooltip);
		ComponentResult rune = addRune(stack);

		double baseValue = 0.0D;
		if (!enchantedBook) {
			ComponentResult base = addBaseItem(stack, tooltip, itemId);
			if (base.line() != null) {
				lines.add(base.line());
			}
			baseValue = base.value();
			total += base.value();
		}

		hasExtras |= appendComponents(lines, stars);
		total += stars.value();
		hasExtras |= appendComponents(lines, masterStars);
		total += masterStars.value();
		hasExtras |= appendComponents(lines, reforge);
		total += reforge.value();
		hasExtras |= appendComponents(lines, rune);
		total += rune.value();

		ComponentResult recombob = addRecombobulator(stack);
		hasExtras |= appendComponents(lines, recombob);
		total += recombob.value();

		ComponentResult scroll = addPowerScroll(stack);
		hasExtras |= appendComponents(lines, scroll);
		total += scroll.value();

		ComponentResult abilityScrolls = addAbilityScrolls(stack);
		hasExtras |= appendComponents(lines, abilityScrolls);
		total += abilityScrolls.value();

		ComponentResult books = addHotPotatoBooks(stack);
		hasExtras |= appendComponents(lines, books);
		total += books.value();

		ComponentResult gemstones = addGemstones(stack);
		hasExtras |= appendComponents(lines, gemstones);
		total += gemstones.value();

		ComponentResult enchants = addEnchantments(stack, itemId);
		hasExtras |= appendComponents(lines, enchants);
		total += enchants.value();

		if (!hasExtras || lines.size() <= 1) {
			return EstimatedItemValueResult.empty();
		}

		String totalText = EivCoinFormat.compact(total);
		lines.add(EstimatedItemValueLine.of("§aTotal: §6§l" + totalText + " coins", total, true));

		return new EstimatedItemValueResult(List.copyOf(lines), baseValue, total);
	}

	private static ComponentResult addBaseItem(ItemStack stack, List<Text> tooltip, @Nullable String itemId) {
		String baseItemId = normalizeBaseItemId(itemId);
		if (baseItemId == null) {
			return ComponentResult.empty();
		}

		PriceResult price = EMUtilsClient.skyblockPrices().baseItemAuctionPrice(baseItemId);
		if (!price.known()) {
			return ComponentResult.empty();
		}

		double baseAmount = price.amount();
		String line = "§7Base item: " + baseItemDisplayName(stack, tooltip, baseItemId)
			+ " " + EivCoinFormat.hudCoinBracket(baseAmount);
		return ComponentResult.single(
			EstimatedItemValueLine.of(line, baseAmount, true),
			baseAmount
		);
	}

	@Nullable
	private static String normalizeBaseItemId(@Nullable String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return null;
		}

		String normalized = itemId.toUpperCase(Locale.ROOT);
		if (normalized.matches(".*_\\d+$") && !normalized.endsWith("_3000")) {
			normalized = normalized.replaceFirst("_\\d+$", "");
		}

		return normalized;
	}

	private static String baseItemDisplayName(ItemStack stack, List<Text> tooltip, String baseItemId) {
		SkyblockItemRarity rarity = SkyblockItemRarity.fromTooltip(tooltip, SkyblockItemAttributes.isRecombobulated(stack));
		String color = rarity != null ? rarity.colorCode() : "§f";
		return color + displayNameFromItemId(baseItemId);
	}

	private static String displayNameFromItemId(String itemId) {
		String[] parts = itemId.split("_");
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < parts.length; index++) {
			if (index > 0) {
				builder.append(' ');
			}

			builder.append(toTitle(parts[index].toLowerCase(Locale.ROOT)));
		}

		return builder.toString();
	}

	private static String priceSuffix(double amount) {
		return " " + EivCoinFormat.hudCoinBracket(amount);
	}

	private static ComponentResult addStars(ItemStack stack, @Nullable String itemId, List<Text> tooltip) {
		int totalStars = SkyblockItemAttributes.dungeonStarCount(stack, tooltip);
		if (totalStars <= 0) {
			return ComponentResult.empty();
		}

		int regularStars = SkyblockItemAttributes.regularStarCount(totalStars);
		if (regularStars <= 0) {
			return ComponentResult.empty();
		}

		return EivEssenceCosts.costsFor(itemId)
			.map(costs -> buildStarLines("Stars", costs, regularStars))
			.orElse(ComponentResult.empty());
	}

	private static ComponentResult addMasterStars(ItemStack stack, List<Text> tooltip) {
		int totalStars = SkyblockItemAttributes.dungeonStarCount(stack, tooltip);
		int masterStars = SkyblockItemAttributes.masterStarCount(totalStars);
		if (masterStars <= 0) {
			return ComponentResult.empty();
		}

		double total = EivEssenceCosts.masterStarsValue(masterStars);
		List<EstimatedItemValueLine> lines = new ArrayList<>();
		addSectionHeader(lines, "Master Stars (" + masterStars + "/5)", total);
		return new ComponentResult(lines, total);
	}

	private static ComponentResult buildStarLines(String label, EivEssenceCosts.ItemEssenceCosts costs, int totalStars) {
		EivEssenceCosts.PricedStarUpgrade priced = EivEssenceCosts.PricedStarUpgrade.price(costs, totalStars);
		if (priced.upgrade().appliedStars() <= 0 || priced.totalValue() <= 0.0D) {
			return ComponentResult.empty();
		}

		List<EstimatedItemValueLine> lines = new ArrayList<>();
		addSectionHeader(
			lines,
			label + " (" + priced.upgrade().appliedStars() + "/" + priced.upgrade().maxStars() + ")",
			priced.totalValue()
		);

		for (EivEssenceCosts.MaterialLine material : priced.materialLines()) {
			lines.add(EstimatedItemValueLine.of(
				" §7" + material.label() + priceSuffix(material.value()),
				material.value(),
				true
			));
		}

		return new ComponentResult(lines, priced.totalValue());
	}

	private static ComponentResult addRune(ItemStack stack) {
		String itemId = SkyblockItemAttributes.itemId(stack);
		if (itemId != null && itemId.toUpperCase(Locale.ROOT).contains("RUNE")) {
			return ComponentResult.empty();
		}

		SkyblockItemAttributes.AppliedRune rune = SkyblockItemAttributes.appliedRune(stack);
		if (rune == null) {
			return ComponentResult.empty();
		}

		PriceResult price = EMUtilsClient.skyblockPrices().price(rune.auctionProductId());
		double value = price.known() ? price.amount() : 0.0D;
		String label = formatRuneName(rune);
		return ComponentResult.single(
			EstimatedItemValueLine.of("§7Rune: §b" + label + priceSuffix(value), value, true),
			value
		);
	}

	private static String formatRuneName(SkyblockItemAttributes.AppliedRune rune) {
		String[] parts = rune.runeKey().toLowerCase(Locale.ROOT).split("_");
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < parts.length; index++) {
			if (index > 0) {
				builder.append(' ');
			}

			builder.append(toTitle(parts[index]));
		}

		builder.append(" Rune ").append(toRoman(rune.level()));
		return builder.toString();
	}

	private static ComponentResult addReforge(ItemStack stack, List<Text> tooltip) {
		String modifier = SkyblockItemAttributes.reforgeModifier(stack);
		ReforgeEntry reforge = EstimatedItemValueData.data().reforge(modifier);
		if (reforge == null) {
			return ComponentResult.empty();
		}

		PriceResult stonePrice = EMUtilsClient.skyblockPrices().price(reforge.stoneId());
		SkyblockItemRarity rarity = SkyblockItemRarity.fromTooltip(tooltip, SkyblockItemAttributes.isRecombobulated(stack));
		Long applyCost = reforge.costFor(rarity);
		double total = 0.0D;
		List<EstimatedItemValueLine> lines = new ArrayList<>();
		lines.add(EstimatedItemValueLine.header("§7Reforge: §9" + reforge.name()));

		double stoneValue = stonePrice.known() ? stonePrice.amount() : 0.0D;
		lines.add(EstimatedItemValueLine.of(
			" §7Stone: §e" + displayName(reforge.stoneId(), reforge.stoneId()) + priceSuffix(stoneValue),
			stoneValue,
			true
		));
		total += stoneValue;

		if (applyCost != null && applyCost > 0L) {
			lines.add(EstimatedItemValueLine.of(
				" §7Apply cost: " + EivCoinFormat.hudCoinBracket(applyCost.doubleValue()),
				applyCost.doubleValue(),
				true
			));
			total += applyCost.doubleValue();
		}

		return new ComponentResult(lines, total);
	}

	private static ComponentResult addRecombobulator(ItemStack stack) {
		if (!SkyblockItemAttributes.isRecombobulated(stack)) {
			return ComponentResult.empty();
		}

		PriceResult price = EMUtilsClient.skyblockPrices().price(RECOMBOBULATOR);
		double value = price.known() ? price.amount() : 0.0D;
		return ComponentResult.single(
			EstimatedItemValueLine.of("§7Recombobulated: §a✔" + priceSuffix(value), value, true),
			value
		);
	}

	private static ComponentResult addAbilityScrolls(ItemStack stack) {
		List<String> scrollIds = SkyblockItemAttributes.abilityScrollIds(stack);
		if (scrollIds.isEmpty()) {
			return ComponentResult.empty();
		}

		List<NamedPrice> priced = new ArrayList<>();
		double total = 0.0D;
		for (String scrollId : scrollIds) {
			PriceResult price = EMUtilsClient.skyblockPrices().price(scrollId);
			double amount = price.known() ? price.amount() : 0.0D;
			total += amount;
			priced.add(new NamedPrice(formatScrollName(scrollId), 1, amount));
		}

		priced.sort(Comparator.comparingDouble(NamedPrice::total).reversed());
		List<EstimatedItemValueLine> lines = new ArrayList<>();
		addSectionHeader(lines, "Ability Scrolls", total);

		for (NamedPrice entry : priced) {
			lines.add(EstimatedItemValueLine.of(
				" §5" + entry.name() + priceSuffix(entry.total()),
				entry.total(),
				true
			));
		}

		return new ComponentResult(lines, total);
	}

	private static ComponentResult addPowerScroll(ItemStack stack) {
		String scrollId = SkyblockItemAttributes.powerScrollId(stack);
		if (scrollId == null) {
			return ComponentResult.empty();
		}

		PriceResult price = EMUtilsClient.skyblockPrices().price(scrollId);
		double value = price.known() ? price.amount() : 0.0D;
		String label = displayName(scrollId, scrollId);
		return ComponentResult.single(
			EstimatedItemValueLine.of("§7" + label + ": §a§l✔" + priceSuffix(value), value, true),
			value
		);
	}

	private static ComponentResult addHotPotatoBooks(ItemStack stack) {
		Integer count = SkyblockItemAttributes.hotPotatoCount(stack);
		if (count == null || count <= 0) {
			return ComponentResult.empty();
		}

		int hpb = Math.min(count, 10);
		int fuming = Math.max(0, count - 10);
		double total = 0.0D;
		List<EstimatedItemValueLine> lines = new ArrayList<>();

		if (hpb > 0) {
			PriceResult hpbPrice = EMUtilsClient.skyblockPrices().price(HOT_POTATO_BOOK);
			double value = hpbPrice.known() ? hpbPrice.amount() * hpb : 0.0D;
			lines.add(EstimatedItemValueLine.of("§7HPB's: §e" + hpb + "§7/§e10" + priceSuffix(value), value, true));
			total += value;
		}

		if (fuming > 0) {
			PriceResult fumingPrice = EMUtilsClient.skyblockPrices().price(FUMING_POTATO_BOOK);
			double value = fumingPrice.known() ? fumingPrice.amount() * fuming : 0.0D;
			lines.add(EstimatedItemValueLine.of("§7Fuming: §e" + fuming + "§7/§e5" + priceSuffix(value), value, true));
			total += value;
		}

		return new ComponentResult(lines, total);
	}

	private static ComponentResult addGemstones(ItemStack stack) {
		List<SkyblockItemAttributes.GemstoneEntry> gemstones = SkyblockItemAttributes.gemstones(stack);
		if (gemstones.isEmpty()) {
			return ComponentResult.empty();
		}

		Map<String, Integer> counts = new LinkedHashMap<>();
		for (SkyblockItemAttributes.GemstoneEntry gemstone : gemstones) {
			counts.merge(gemstone.productId(), 1, Integer::sum);
		}

		List<NamedPrice> priced = new ArrayList<>();
		double total = 0.0D;
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			PriceResult price = EMUtilsClient.skyblockPrices().price(entry.getKey());
			double lineTotal = price.known() ? price.amount() * entry.getValue() : 0.0D;
			total += lineTotal;
			priced.add(new NamedPrice(EivGemstoneFormat.displayName(entry.getKey()), entry.getValue(), lineTotal));
		}

		priced.sort(Comparator.comparingDouble(NamedPrice::total).reversed());
		List<EstimatedItemValueLine> lines = new ArrayList<>();
		addSectionHeader(lines, "Gemstones Applied", total);

		for (NamedPrice entry : priced) {
			lines.add(EstimatedItemValueLine.of(
				" §7" + entry.amount() + "x " + entry.name() + priceSuffix(entry.total()),
				entry.total(),
				true
			));
		}

		return new ComponentResult(lines, total);
	}

	private static ComponentResult addEnchantments(ItemStack stack, @Nullable String baseItemId) {
		Map<String, Integer> enchantments = SkyblockItemAttributes.enchantments(stack);
		if (enchantments == null || enchantments.isEmpty()) {
			return ComponentResult.empty();
		}

		EstimatedItemValueData.LoadedData data = EstimatedItemValueData.data();
		Map<String, Integer> items = new LinkedHashMap<>();

		for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
			String rawName = data.renamedEnchants().getOrDefault(entry.getKey().toLowerCase(Locale.ROOT), entry.getKey());
			int rawLevel = entry.getValue();
			if ("efficiency".equalsIgnoreCase(rawName) && rawLevel <= 5) {
				continue;
			}

			AlwaysActiveEnchant alwaysActive = data.alwaysActiveEnchants().get(rawName.toLowerCase(Locale.ROOT));
			if (alwaysActive != null && alwaysActive.level() == rawLevel && baseItemId != null
				&& alwaysActive.items().contains(baseItemId)) {
				continue;
			}

			int level = rawLevel;
			int multiplier = 1;
			if (data.onlyTierOnePrices().contains(rawName) && level >= 2 && level <= 5) {
				multiplier = 1 << (level - 1);
				level = 1;
			} else if (data.onlyTierFivePrices().contains(rawName) && level >= 6 && level <= 10) {
				multiplier = 1 << (level - 5);
				level = 5;
			}

			List<EndCapEntry> endcaps = data.endcapEnchants().get(rawName);
			if (endcaps != null) {
				for (EndCapEntry endcap : endcaps) {
					if (rawLevel >= endcap.requiredLevel() + 1) {
						items.merge(endcap.endcapItem(), 1, Integer::sum);
					}
				}
			}

			String enchantId = enchantProductId(rawName, level);
			items.merge(enchantId, multiplier, Integer::sum);
		}

		if (items.isEmpty()) {
			return ComponentResult.empty();
		}

		List<NamedPrice> priced = new ArrayList<>();
		double total = 0.0D;
		for (Map.Entry<String, Integer> entry : items.entrySet()) {
			PriceResult price = EMUtilsClient.skyblockPrices().price(entry.getKey());
			double lineTotal = price.known() ? price.amount() * entry.getValue() : 0.0D;
			total += lineTotal;
			priced.add(new NamedPrice(formatEnchantName(entry.getKey()), entry.getValue(), lineTotal, entry.getKey()));
		}

		priced.sort(Comparator.comparingDouble(NamedPrice::total).reversed());
		int cap = Math.max(1, EMSkyblockSettings.estimatedItemValueEnchantmentsCap());
		List<EstimatedItemValueLine> lines = new ArrayList<>();
		addSectionHeader(lines, "Enchantments", total);

		for (int index = 0; index < priced.size() && index < cap; index++) {
			NamedPrice entry = priced.get(index);
			lines.add(EstimatedItemValueLine.of(
				enchantLine(entry),
				entry.total(),
				true
			));
		}

		if (priced.size() > cap) {
			lines.add(EstimatedItemValueLine.header(" §7§o" + (priced.size() - cap) + " more enchantments.."));
		}

		return new ComponentResult(lines, total);
	}

	private static String enchantProductId(String enchantName, int level) {
		return "ENCHANTMENT_" + enchantName.toUpperCase(Locale.ROOT).replace(' ', '_') + "_" + level;
	}

	private static String formatEnchantName(String productId) {
		if (!productId.startsWith("ENCHANTMENT_")) {
			return displayName(productId, productId);
		}

		String body = productId.substring("ENCHANTMENT_".length());
		int lastUnderscore = body.lastIndexOf('_');
		if (lastUnderscore <= 0) {
			return body.replace('_', ' ');
		}

		String name = body.substring(0, lastUnderscore).replace('_', ' ');
		int level = parseLevel(body.substring(lastUnderscore + 1));
		return formatEnchantTitle(name) + " " + toRoman(level);
	}

	private static String enchantLine(NamedPrice entry) {
		String color = entry.productId() != null && entry.productId().contains("ULTIMATE") ? "§d" : "§9";
		return " " + color + entry.name() + priceSuffix(entry.total());
	}

	private static String formatEnchantTitle(String rawName) {
		String[] parts = rawName.toLowerCase(Locale.ROOT).split(" ");
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < parts.length; index++) {
			if (index > 0) {
				builder.append(' ');
			}

			builder.append(toTitle(parts[index]));
		}

		return builder.toString();
	}

	private static int parseLevel(String level) {
		try {
			return Integer.parseInt(level);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

	private static String toRoman(int level) {
		return switch (level) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			case 6 -> "VI";
			case 7 -> "VII";
			case 8 -> "VIII";
			case 9 -> "IX";
			case 10 -> "X";
			default -> Integer.toString(level);
		};
	}

	private static String formatScrollName(String scrollId) {
		if ("IMPLOSION_SCROLL".equalsIgnoreCase(scrollId)) {
			return "Implosion";
		}
		if ("WITHER_SHIELD_SCROLL".equalsIgnoreCase(scrollId)) {
			return "Wither Shield";
		}
		if ("SHADOW_WARP_SCROLL".equalsIgnoreCase(scrollId)) {
			return "Shadow Warp";
		}

		return displayName(null, scrollId.replace("_SCROLL", ""));
	}

	private static void addSectionHeader(
		List<EstimatedItemValueLine> lines,
		String label,
		double total
	) {
		lines.add(EstimatedItemValueLine.of(
			"§7" + label + ": " + EivCoinFormat.hudSectionTotal(total),
			total,
			true
		));
	}

	private static String displayName(String preferred, String fallbackId) {
		if (preferred != null && !preferred.isBlank()) {
			return Formatting.strip(preferred);
		}

		return fallbackId.replace('_', ' ');
	}

	private static String toTitle(String value) {
		String lower = value.toLowerCase(Locale.ROOT);
		if (lower.isEmpty()) {
			return lower;
		}

		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}

	private static boolean appendComponents(List<EstimatedItemValueLine> lines, ComponentResult result) {
		if (result.lines().isEmpty()) {
			return false;
		}

		lines.addAll(result.lines());
		return true;
	}

	private record NamedPrice(String name, int amount, double total, @Nullable String productId) {
		NamedPrice(String name, int amount, double total) {
			this(name, amount, total, null);
		}
	}

	private record ComponentResult(List<EstimatedItemValueLine> lines, double value) {
		@Nullable
		EstimatedItemValueLine line() {
			return lines.isEmpty() ? null : lines.getFirst();
		}

		static ComponentResult empty() {
			return new ComponentResult(List.of(), 0.0D);
		}

		static ComponentResult single(EstimatedItemValueLine line, double value) {
			return new ComponentResult(List.of(line), value);
		}
	}
}
