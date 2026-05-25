package net.emutils.client.skyblock;

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
		return NpcTooltipHelper.appendLines(
			stack,
			AuctionTooltipHelper.appendLines(stack, BazaarTooltipHelper.appendLines(stack, tooltip))
		);
	}
}
