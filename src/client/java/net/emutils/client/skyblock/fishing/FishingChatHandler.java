package net.emutils.client.skyblock.fishing;

import java.util.regex.Pattern;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.emutils.client.skyblock.SkyblockTextUtils;
import net.emutils.client.skyblock.config.EMSkyblockSettings;
import net.emutils.client.skyblock.fishing.tracker.profit.FishingProfitTrackerManager;
import net.emutils.client.skyblock.fishing.tracker.seacreature.SeaCreatureTrackerManager;
import net.minecraft.text.Text;

public final class FishingChatHandler {
	private static final Pattern DOUBLE_HOOK = Pattern.compile(
		"^It's a Double Hook!.*",
		Pattern.CASE_INSENSITIVE
	);

	private FishingChatHandler() {
	}

	public static void onChat(Text message) {
		if (!SkyblockFeatures.inSkyBlock() || !EMSkyblockSettings.skyblockEnabled()) {
			return;
		}

		String stripped = SkyblockTextUtils.strip(message);
		if (stripped.isEmpty()) {
			return;
		}

		if (DOUBLE_HOOK.matcher(stripped).matches()) {
			SeaCreatureTrackerManager.onDoubleHookChat();
			return;
		}

		boolean profitTrackerEnabled = EMSkyblockSettings.fishingProfitTrackerEnabled();
		boolean seaCreatureTrackerEnabled = EMSkyblockSettings.seaCreatureTrackerEnabled();

		if (profitTrackerEnabled) {
			FishingProfitTrackerManager.onCoinsChat(stripped);
		}

		if (seaCreatureTrackerEnabled || profitTrackerEnabled) {
			boolean seaCreatureCaught = SeaCreatureTrackerManager.onSeaCreatureChat(stripped, seaCreatureTrackerEnabled);
			if (seaCreatureCaught && profitTrackerEnabled) {
				FishingProfitTrackerManager.recordCatch();
			}
		}
	}
}
