package net.emutils.client.skyblock.eiv;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.emutils.client.skyblock.SkyblockPriceTooltipUtils;
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
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.skyblockEnabled() || !config.estimatedItemValueHudEnabled()) {
			return tooltip;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!SkyblockFeatures.inSkyBlock(client)) {
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
		return insertAfterLowestBin(tooltip, line);
	}

	private static List<Text> insertAfterLowestBin(List<Text> tooltip, Text line) {
		String lowestBinPrefix = Text.translatable(EMUtilsTexts.AUCTION_LOWEST_BIN).getString();
		for (int index = 0; index < tooltip.size(); index++) {
			if (SkyblockTextUtils.strip(tooltip.get(index)).startsWith(lowestBinPrefix)) {
				List<Text> combined = new ArrayList<>(tooltip.size() + 1);
				combined.addAll(tooltip.subList(0, index + 1));
				combined.add(line);
				combined.addAll(tooltip.subList(index + 1, tooltip.size()));
				return combined;
			}
		}

		return SkyblockPriceTooltipUtils.appendSection(tooltip, List.of(line));
	}
}
