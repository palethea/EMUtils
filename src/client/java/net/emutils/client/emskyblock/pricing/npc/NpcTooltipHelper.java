package net.emutils.client.emskyblock.pricing.npc;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.context.SkyblockFeatures;
import net.emutils.client.emskyblock.pricing.SkyblockPriceTooltipUtils;
import net.emutils.client.emskyblock.pricing.SkyblockSoulboundUtils;
import net.emutils.client.emskyblock.pricing.bazaar.SkyblockItemIds;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class NpcTooltipHelper {
	private NpcTooltipHelper() {
	}

	public static void collectLines(ItemStack stack, List<Text> tooltip, List<Text> target) {
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.npcSellPriceTooltipsEnabled()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!SkyblockFeatures.inSkyBlock(client)) {
			return;
		}
		if (SkyblockSoulboundUtils.shouldHide(EMSkyblockSettings.npcHideOnSoulbound(), tooltip)) {
			return;
		}

		String itemId = SkyblockItemIds.resolveItemId(stack);
		if (itemId == null) {
			return;
		}

		Double npcSellPrice = EMUtilsClient.skyblockPrices().npc()
			.npcSellPrice(itemId)
			.orElse(null);
		if (npcSellPrice == null || npcSellPrice <= 0.0D) {
			return;
		}

		boolean stackTotal = SkyblockPriceTooltipUtils.useStackTotal(client);
		double multiplier = SkyblockPriceTooltipUtils.totalAmount(tooltip, stack, stackTotal);
		target.add(SkyblockPriceTooltipUtils.priceLine(
			Text.translatable(EMUtilsTexts.NPC_SELL_PRICE),
			Formatting.GRAY,
			npcSellPrice * multiplier
		));
	}
}
