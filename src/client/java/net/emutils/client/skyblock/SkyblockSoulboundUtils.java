package net.emutils.client.skyblock;

import java.util.List;
import net.minecraft.text.Text;

public final class SkyblockSoulboundUtils {
	private SkyblockSoulboundUtils() {
	}

	public static boolean isCoopOrSoloSoulbound(List<Text> tooltip) {
		for (Text line : tooltip) {
			String normalized = SkyblockTextUtils.normalizeKey(SkyblockTextUtils.strip(line));
			if (normalized.contains("co-op soulbound") || normalized.contains("solo soulbound")) {
				return true;
			}
		}

		return false;
	}

	public static boolean shouldHide(boolean hideOnSoulbound, List<Text> tooltip) {
		return hideOnSoulbound && isCoopOrSoloSoulbound(tooltip);
	}
}
