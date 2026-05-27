package net.emutils.client.skyblock;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.skyblock.auction.AuctionTooltipHelper;
import net.emutils.client.skyblock.bazaar.BazaarTooltipHelper;
import net.emutils.client.skyblock.npc.NpcTooltipHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class SkyblockTooltipPrices {
	private SkyblockTooltipPrices() {
	}

	public static List<Text> appendLines(ItemStack stack, List<Text> tooltip) {
		List<Text> addonLines = new ArrayList<>();
		BazaarTooltipHelper.collectLines(stack, tooltip, addonLines);
		AuctionTooltipHelper.collectLines(stack, tooltip, addonLines);
		NpcTooltipHelper.collectLines(stack, tooltip, addonLines);
		return SkyblockPriceTooltipUtils.appendAddonBlock(tooltip, addonLines);
	}
}
