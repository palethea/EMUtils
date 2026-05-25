package net.emutils.client.skyblock.auction;

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

public final class AuctionTooltipHelper {
	private AuctionTooltipHelper() {
	}

	public static List<Text> appendLines(ItemStack stack, List<Text> tooltip) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.skyblockEnabled() || !config.auctionTooltipsEnabled()) {
			return tooltip;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!SkyblockFeatures.inSkyBlock(client)) {
			return tooltip;
		}

		AuctionProductPrice price = EMUtilsClient.auctionPrices()
			.price(stack)
			.orElse(null);
		if (price == null || !price.hasAny()) {
			return tooltip;
		}

		boolean stackTotal = SkyblockPriceTooltipUtils.isShiftDown(client);
		double multiplier = SkyblockPriceTooltipUtils.totalAmount(tooltip, stack, stackTotal);
		List<Text> lines = buildLines(price, multiplier);
		return SkyblockPriceTooltipUtils.appendSection(tooltip, lines);
	}

	private static List<Text> buildLines(AuctionProductPrice price, double multiplier) {
		List<Text> lines = new ArrayList<>(2);
		if (price.lowestBin() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.AUCTION_LOWEST_BIN),
				Formatting.GRAY,
				price.lowestBin() * multiplier
			));
		}
		if (price.average24h() > 0.0D) {
			lines.add(SkyblockPriceTooltipUtils.priceLine(
				Text.translatable(EMUtilsTexts.AUCTION_AVERAGE_24H),
				Formatting.GRAY,
				price.average24h() * multiplier
			));
		}
		return lines;
	}
}
