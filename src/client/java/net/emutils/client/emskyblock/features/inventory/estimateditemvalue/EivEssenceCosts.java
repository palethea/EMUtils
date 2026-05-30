package net.emutils.client.emskyblock.features.inventory.estimateditemvalue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.pricing.SkyblockPrices.PriceResult;
import org.jspecify.annotations.Nullable;

public final class EivEssenceCosts {
	private static final Gson GSON = new Gson();
	private static final String[] MASTER_STAR_IDS = {
		"FIRST_MASTER_STAR",
		"SECOND_MASTER_STAR",
		"THIRD_MASTER_STAR",
		"FOURTH_MASTER_STAR",
		"FIFTH_MASTER_STAR",
	};

	private static volatile Map<String, ItemEssenceCosts> costsByItemId = Map.of();

	private EivEssenceCosts() {
	}

	public static void load() {
		try (InputStream stream = EivEssenceCosts.class.getResourceAsStream("/data/emutils/skyblock/essence_costs.json")) {
			if (stream == null) {
				EMUtilsClient.LOGGER.warn("Missing essence cost data for EIV.");
				costsByItemId = Map.of();
				return;
			}

			JsonObject root = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
			Map<String, ItemEssenceCosts> parsed = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				if (!entry.getValue().isJsonObject()) {
					continue;
				}

				ItemEssenceCosts costs = ItemEssenceCosts.parse(entry.getKey(), entry.getValue().getAsJsonObject());
				if (costs != null) {
					parsed.put(entry.getKey(), costs);
				}
			}

			costsByItemId = Map.copyOf(parsed);
		} catch (IOException | JsonParseException exception) {
			EMUtilsClient.LOGGER.warn("Failed to load essence cost data for EIV.", exception);
			costsByItemId = Map.of();
		}
	}

	public static Optional<ItemEssenceCosts> costsFor(@Nullable String itemId) {
		String resolved = resolveEssenceItemId(itemId);
		if (resolved == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(costsByItemId.get(resolved));
	}

	@Nullable
	public static String resolveEssenceItemId(@Nullable String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return null;
		}

		String normalized = itemId.toUpperCase(Locale.ROOT);
		if (costsByItemId.containsKey(normalized)) {
			return normalized;
		}

		if (normalized.endsWith("_SWORD")) {
			String shortened = normalized.substring(0, normalized.length() - "_SWORD".length());
			if (costsByItemId.containsKey(shortened)) {
				return shortened;
			}
		}

		return null;
	}

	public static double masterStarsValue(int masterStars) {
		if (masterStars <= 0) {
			return 0.0D;
		}

		double total = 0.0D;
		for (int index = 0; index < MASTER_STAR_IDS.length && index < masterStars; index++) {
			PriceResult price = EMUtilsClient.skyblockPrices().price(MASTER_STAR_IDS[index]);
			if (price.known()) {
				total += price.amount();
			}
		}

		return total;
	}

	public record ItemEssenceCosts(String itemId, String essenceType, Map<Integer, StarTierCost> tiers) {
		@Nullable
		static ItemEssenceCosts parse(String itemId, JsonObject object) {
			String essenceType = object.has("type") && object.get("type").isJsonPrimitive()
				? object.get("type").getAsString()
				: null;
			if (essenceType == null || essenceType.isBlank()) {
				return null;
			}

			Map<Integer, StarTierCost> tiers = new LinkedHashMap<>();
			JsonObject itemCosts = object.has("items") && object.get("items").isJsonObject()
				? object.getAsJsonObject("items")
				: new JsonObject();

			for (int tier = 1; tier <= 15; tier++) {
				String tierKey = Integer.toString(tier);
				if (!object.has(tierKey) || !object.get(tierKey).isJsonPrimitive()) {
					continue;
				}

				int essenceAmount = object.get(tierKey).getAsInt();
				long coins = 0L;
				Map<String, Integer> materials = new LinkedHashMap<>();
				if (itemCosts.has(tierKey) && itemCosts.get(tierKey).isJsonArray()) {
					for (JsonElement element : itemCosts.getAsJsonArray(tierKey)) {
						if (!element.isJsonPrimitive()) {
							continue;
						}

						coins += parseMaterialEntry(element.getAsString(), materials);
					}
				}

				tiers.put(tier, new StarTierCost(essenceAmount, coins, Map.copyOf(materials)));
			}

			if (tiers.isEmpty()) {
				return null;
			}

			return new ItemEssenceCosts(itemId, essenceType, Map.copyOf(tiers));
		}

		private static long parseMaterialEntry(String entry, Map<String, Integer> materials) {
			int separator = entry.indexOf(':');
			if (separator <= 0) {
				return 0L;
			}

			String materialId = entry.substring(0, separator).trim().toUpperCase(Locale.ROOT);
			String amountText = entry.substring(separator + 1).trim();
			if (materialId.isEmpty() || amountText.isEmpty()) {
				return 0L;
			}

			if ("SKYBLOCK_COIN".equals(materialId)) {
				return parseLongAmount(amountText);
			}

			materials.merge(materialId, parseAmount(amountText), Integer::sum);
			return 0L;
		}

		private static int parseAmount(String amountText) {
			try {
				return (int) Math.min(Integer.MAX_VALUE, Long.parseLong(amountText.replace(",", "")));
			} catch (NumberFormatException ignored) {
				return 0;
			}
		}

		private static long parseLongAmount(String amountText) {
			try {
				return Long.parseLong(amountText.replace(",", ""));
			} catch (NumberFormatException ignored) {
				return 0L;
			}
		}

		public StarUpgradeValue valueForStars(int totalStars) {
			if (totalStars <= 0) {
				return StarUpgradeValue.EMPTY;
			}

			int essenceTotal = 0;
			long coins = 0L;
			Map<String, Integer> materials = new LinkedHashMap<>();
			int maxTier = tiers.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
			int appliedStars = Math.min(totalStars, maxTier);

			for (Map.Entry<Integer, StarTierCost> entry : tiers.entrySet()) {
				if (entry.getKey() > appliedStars) {
					continue;
				}

				StarTierCost tier = entry.getValue();
				essenceTotal += tier.essenceAmount();
				coins += tier.coins();
				for (Map.Entry<String, Integer> material : tier.materials().entrySet()) {
					materials.merge(material.getKey(), material.getValue(), Integer::sum);
				}
			}

			return new StarUpgradeValue(appliedStars, maxTier, essenceType, essenceTotal, coins, Map.copyOf(materials));
		}
	}

	public record StarTierCost(int essenceAmount, long coins, Map<String, Integer> materials) {
	}

	public record StarUpgradeValue(
		int appliedStars,
		int maxStars,
		String essenceType,
		int essenceAmount,
		long coins,
		Map<String, Integer> materials
	) {
		public static final StarUpgradeValue EMPTY = new StarUpgradeValue(0, 0, "", 0, 0L, Map.of());

		public String essenceItemId() {
			return "ESSENCE_" + essenceType.toUpperCase(Locale.ROOT);
		}
	}

	public record PricedStarUpgrade(double totalValue, StarUpgradeValue upgrade, List<MaterialLine> materialLines) {
		public static final PricedStarUpgrade EMPTY = new PricedStarUpgrade(0.0D, StarUpgradeValue.EMPTY, List.of());

		public static PricedStarUpgrade price(ItemEssenceCosts costs, int totalStars) {
			StarUpgradeValue upgrade = costs.valueForStars(totalStars);
			if (upgrade.appliedStars() <= 0) {
				return EMPTY;
			}

			double total = 0.0D;
			List<MaterialLine> lines = new ArrayList<>();

			if (upgrade.essenceAmount() > 0) {
				PriceResult essencePrice = EMUtilsClient.skyblockPrices().price(upgrade.essenceItemId());
				double lineTotal = essencePrice.known() ? essencePrice.amount() * upgrade.essenceAmount() : 0.0D;
				total += lineTotal;
				lines.add(new MaterialLine(
					upgrade.essenceAmount() + "x " + formatEssenceName(upgrade.essenceType()) + " Essence",
					lineTotal
				));
			}

			if (upgrade.coins() > 0L) {
				total += upgrade.coins();
				lines.add(new MaterialLine("Coins", upgrade.coins()));
			}

			for (Map.Entry<String, Integer> entry : upgrade.materials().entrySet()) {
				PriceResult materialPrice = EMUtilsClient.skyblockPrices().price(entry.getKey());
				double lineTotal = materialPrice.known() ? materialPrice.amount() * entry.getValue() : 0.0D;
				total += lineTotal;
				lines.add(new MaterialLine(entry.getValue() + "x " + formatMaterialName(entry.getKey()), lineTotal));
			}

			return new PricedStarUpgrade(total, upgrade, List.copyOf(lines));
		}

		private static String formatEssenceName(String essenceType) {
			return toTitle(essenceType.toLowerCase(Locale.ROOT));
		}

		private static String formatMaterialName(String materialId) {
			return toTitle(materialId.toLowerCase(Locale.ROOT).replace('_', ' '));
		}

		private static String toTitle(String value) {
			if (value.isEmpty()) {
				return value;
			}

			return Character.toUpperCase(value.charAt(0)) + value.substring(1);
		}
	}

	public record MaterialLine(String label, double value) {
	}
}
