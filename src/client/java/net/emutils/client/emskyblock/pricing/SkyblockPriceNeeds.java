package net.emutils.client.emskyblock.pricing;

import net.emutils.client.emskyblock.config.EMSkyblockSettings;

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
