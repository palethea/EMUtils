package net.emutils.client.skyblock;

import net.emutils.client.skyblock.config.EMSkyblockSettings;

public final class SkyblockPriceNeeds {
	private SkyblockPriceNeeds() {
	}

	public static boolean anyEnabled() {
		if (!EMSkyblockSettings.skyblockEnabled()) {
			return false;
		}

		return EMSkyblockSettings.bazaarTooltipsEnabled()
			|| EMSkyblockSettings.auctionTooltipsEnabled()
			|| EMSkyblockSettings.npcSellPriceTooltipsEnabled()
			|| EMSkyblockSettings.estimatedItemValueHudEnabled();
	}
}
