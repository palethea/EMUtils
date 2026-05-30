package net.emutils.client.emskyblock.pricing.auction;

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

public final class AuctionTooltipHelper {
	private AuctionTooltipHelper() {
	}

	public static void collectLines(ItemStack stack, List<Text> tooltip, List<Text> target) {
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.auctionTooltipsEnabled()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!SkyblockFeatures.inSkyBlock(client)) {
			return;
		}
		if (SkyblockSoulboundUtils.shouldHide(EMSkyblockSettings.auctionHideOnSoulbound(), tooltip)) {
			return;
		}
		if (EMUtilsClient.skyblockPrices().bazaar().isListed(stack)) {
			return;
		}

		AuctionProductPrice price = EMUtilsClient.skyblockPrices().auction()
			.price(stack)
			.orElse(null);
		if (price == null || !price.hasAny()) {
			return;
		}

		boolean stackTotal = SkyblockPriceTooltipUtils.useStackTotal(client);
		double multiplier = SkyblockPriceTooltipUtils.totalAmount(tooltip, stack, stackTotal);
		target.addAll(buildLines(price, multiplier));
	}

	private static List<Text> buildLines(AuctionProductPrice price, double multiplier) {
		List<Text> lines = new ArrayList<>(2);
		if (EMSkyblockSettings.auctionShowLowestBin() && price.lowestBin() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.AUCTION_LOWEST_BIN),
				Formatting.GRAY,
				price.lowestBin() * multiplier
			));
		}
		if (EMSkyblockSettings.auctionShowAverage24h() && price.average24h() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.AUCTION_AVERAGE_24H),
				Formatting.GRAY,
				price.average24h() * multiplier
			));
		}
		return lines;
	}
}
