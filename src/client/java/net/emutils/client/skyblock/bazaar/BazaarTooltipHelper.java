package net.emutils.client.skyblock.bazaar;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.emutils.client.skyblock.SkyblockPriceTooltipUtils;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class BazaarTooltipHelper {
	private BazaarTooltipHelper() {
	}

	public static List<Text> appendLines(ItemStack stack, List<Text> tooltip) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.skyblockEnabled() || !config.bazaarTooltipsEnabled()) {
			return tooltip;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!SkyblockFeatures.inSkyBlock(client)) {
			return tooltip;
		}

		String productId = SkyblockItemIds.bazaarId(stack);
		if (productId == null) {
			return tooltip;
		}

		BazaarProductPrice price = EMUtilsClient.bazaarPrices()
			.price(productId)
			.orElse(null);
		if (price == null) {
			return tooltip;
		}

		boolean stackTotal = SkyblockPriceTooltipUtils.isShiftDown(client);
		double multiplier = SkyblockPriceTooltipUtils.totalAmount(tooltip, stack, stackTotal);
		List<Text> lines = buildLines(price, multiplier);
		return SkyblockPriceTooltipUtils.appendSection(tooltip, lines);
	}

	private static List<Text> buildLines(BazaarProductPrice price, double multiplier) {
		List<Text> lines = new ArrayList<>(4);
		if (price.buyPrice() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.BAZAAR_BUY),
				Formatting.GRAY,
				price.buyPrice() * multiplier
			));
		}
		if (price.sellPrice() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.BAZAAR_SELL),
				Formatting.GRAY,
				price.sellPrice() * multiplier
			));
		}
		if (price.instantBuyPrice() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.BAZAAR_INSTANT_BUY),
				Formatting.GRAY,
				price.instantBuyPrice() * multiplier
			));
		}
		if (price.instantSellPrice() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.BAZAAR_INSTANT_SELL),
				Formatting.GRAY,
				price.instantSellPrice() * multiplier
			));
		}
		return lines;
	}
}
