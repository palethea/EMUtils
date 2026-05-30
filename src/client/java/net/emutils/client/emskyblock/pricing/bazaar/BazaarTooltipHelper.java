package net.emutils.client.emskyblock.pricing.bazaar;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.context.SkyblockFeatures;
import net.emutils.client.emskyblock.pricing.SkyblockPriceTooltipUtils;
import net.emutils.client.emskyblock.pricing.SkyblockSoulboundUtils;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class BazaarTooltipHelper {
	private BazaarTooltipHelper() {
	}

	public static void collectLines(ItemStack stack, List<Text> tooltip, List<Text> target) {
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.bazaarTooltipsEnabled()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!SkyblockFeatures.inSkyBlock(client)) {
			return;
		}
		if (SkyblockSoulboundUtils.shouldHide(EMSkyblockSettings.bazaarHideOnSoulbound(), tooltip)) {
			return;
		}
		if (!EMUtilsClient.skyblockPrices().bazaar().isListed(stack)) {
			return;
		}

		String productId = SkyblockItemIds.bazaarId(stack);
		if (productId == null) {
			return;
		}

		BazaarProductPrice price = EMUtilsClient.skyblockPrices().bazaar()
			.price(productId)
			.orElse(null);
		if (price == null || !price.hasAny()) {
			return;
		}

		boolean stackTotal = SkyblockPriceTooltipUtils.useStackTotal(client);
		double multiplier = SkyblockPriceTooltipUtils.totalAmount(tooltip, stack, stackTotal);
		target.addAll(buildLines(price, multiplier));
	}

	private static List<Text> buildLines(BazaarProductPrice price, double multiplier) {
		List<Text> lines = new ArrayList<>(4);
		if (EMSkyblockSettings.bazaarShowBuyOrder() && price.buyPrice() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.BAZAAR_BUY_ORDER),
				Formatting.GRAY,
				price.buyPrice() * multiplier
			));
		}
		if (EMSkyblockSettings.bazaarShowSellOrder() && price.sellPrice() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.BAZAAR_SELL_ORDER),
				Formatting.GRAY,
				price.sellPrice() * multiplier
			));
		}
		if (EMSkyblockSettings.bazaarShowInstantBuy() && price.instantBuyPrice() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.BAZAAR_INSTANT_BUY),
				Formatting.GRAY,
				price.instantBuyPrice() * multiplier
			));
		}
		if (EMSkyblockSettings.bazaarShowInstantSell() && price.instantSellPrice() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.BAZAAR_INSTANT_SELL),
				Formatting.GRAY,
				price.instantSellPrice() * multiplier
			));
		}
		if (EMSkyblockSettings.bazaarShowAverage24h() && price.average24h() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.BAZAAR_AVERAGE_24H),
				Formatting.GRAY,
				price.average24h() * multiplier
			));
		}
		return lines;
	}
}
