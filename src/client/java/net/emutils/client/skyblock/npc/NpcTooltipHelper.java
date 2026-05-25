package net.emutils.client.skyblock.npc;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.emutils.client.skyblock.SkyblockPriceTooltipUtils;
import net.emutils.client.skyblock.bazaar.SkyblockItemIds;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class NpcTooltipHelper {
	private NpcTooltipHelper() {
	}

	public static List<Text> appendLines(ItemStack stack, List<Text> tooltip) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.skyblockEnabled() || !config.npcSellPriceTooltipsEnabled()) {
			return tooltip;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!SkyblockFeatures.inSkyBlock(client)) {
			return tooltip;
		}

		String itemId = SkyblockItemIds.resolveItemId(stack);
		if (itemId == null) {
			return tooltip;
		}

		Double npcSellPrice = EMUtilsClient.npcPrices()
			.npcSellPrice(itemId)
			.orElse(null);
		if (npcSellPrice == null || npcSellPrice <= 0.0D) {
			return tooltip;
		}

		boolean stackTotal = SkyblockPriceTooltipUtils.isShiftDown(client);
		double multiplier = SkyblockPriceTooltipUtils.totalAmount(tooltip, stack, stackTotal);
		List<Text> lines = List.of(SkyblockPriceTooltipUtils.priceLine(
			Text.translatable(EMUtilsTexts.NPC_SELL_PRICE),
			Formatting.GRAY,
			npcSellPrice * multiplier
		));
		return SkyblockPriceTooltipUtils.appendSection(tooltip, lines);
	}
}
