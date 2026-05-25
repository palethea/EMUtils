package net.emutils.client.skyblock;

import net.emutils.client.config.EMUtilsConfig;
import org.jspecify.annotations.Nullable;

public final class SkyblockPriceNeeds {
	private SkyblockPriceNeeds() {
	}

	public static boolean anyEnabled(@Nullable EMUtilsConfig config) {
		if (config == null || !config.skyblockEnabled()) {
			return false;
		}

		return config.bazaarTooltipsEnabled()
			|| config.auctionTooltipsEnabled()
			|| config.npcSellPriceTooltipsEnabled()
			|| config.estimatedItemValueHudEnabled();
	}
}
