package net.emutils.client.emskyblock.features.fishing.common;

import java.util.regex.Pattern;
import net.emutils.client.emskyblock.context.SkyblockFeatures;
import net.emutils.client.emskyblock.context.SkyblockTextUtils;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.features.fishing.profittracker.FishingProfitTrackerManager;
import net.emutils.client.emskyblock.features.fishing.seacreaturetracker.SeaCreatureTrackerManager;
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
