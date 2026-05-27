package net.emutils.client.skyblock.eiv;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.skyblock.config.EMSkyblockSettings;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.emutils.client.skyblock.SkyblockPriceTooltipUtils;
import net.emutils.client.skyblock.SkyblockSoulboundUtils;
import net.emutils.client.skyblock.SkyblockTextUtils;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class EstimatedItemValueTooltipHelper {
	private EstimatedItemValueTooltipHelper() {
	}

	public static List<Text> appendLine(ItemStack stack, List<Text> tooltip, EstimatedItemValueResult result) {
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.estimatedItemValueTooltipEnabled()) {
			return tooltip;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!SkyblockFeatures.inSkyBlock(client)) {
			return tooltip;
		}
		if (SkyblockSoulboundUtils.shouldHide(EMSkyblockSettings.estimatedItemValueHideOnSoulbound(), tooltip)) {
			return tooltip;
		}

		if (result.isEmpty() || result.totalValue() <= result.baseValue() + 0.5D) {
			return tooltip;
		}

		Text line = SkyblockPriceTooltipUtils.priceLine(
			Text.translatable(EMUtilsTexts.ESTIMATED_ITEM_VALUE_TOTAL),
			Formatting.GRAY,
			result.totalValue()
		);
		return appendEstimatedValueLine(tooltip, line);
	}

	private static List<Text> appendEstimatedValueLine(List<Text> tooltip, Text line) {
		String lowestBinPrefix = Text.translatable(EMUtilsTexts.AUCTION_LOWEST_BIN).getString();
		for (int index = 0; index < tooltip.size(); index++) {
			if (SkyblockTextUtils.strip(tooltip.get(index)).startsWith(lowestBinPrefix)) {
				List<Text> combined = new ArrayList<>(tooltip.size() + 1);
				combined.addAll(tooltip.subList(0, index));
				combined.add(line);
				combined.addAll(tooltip.subList(index, tooltip.size()));
				return combined;
			}
		}

		return SkyblockPriceTooltipUtils.appendAddonLines(tooltip, List.of(line), !hasPriceAddonLines(tooltip));
	}

	private static boolean hasPriceAddonLines(List<Text> tooltip) {
		String bazaarPrefix = Text.translatable(EMUtilsTexts.BAZAAR_BUY_ORDER).getString();
		String lowestBinPrefix = Text.translatable(EMUtilsTexts.AUCTION_LOWEST_BIN).getString();
		String averagePrefix = Text.translatable(EMUtilsTexts.AUCTION_AVERAGE_24H).getString();
		String npcPrefix = Text.translatable(EMUtilsTexts.NPC_SELL_PRICE).getString();
		for (Text line : tooltip) {
			String stripped = SkyblockTextUtils.strip(line);
			if (stripped.startsWith(bazaarPrefix)
				|| stripped.startsWith("Bazaar ")
				|| stripped.startsWith(lowestBinPrefix)
				|| stripped.startsWith(averagePrefix)
				|| stripped.startsWith(npcPrefix)) {
				return true;
			}
		}

		return false;
	}
}
