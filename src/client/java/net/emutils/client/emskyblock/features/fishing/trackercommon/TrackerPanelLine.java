package net.emutils.client.emskyblock.features.fishing.trackercommon;

import java.util.Locale;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public record TrackerPanelLine(
	Text text,
	@Nullable TrackerHeaderParts headerParts
) {
	public static TrackerPanelLine of(String legacy) {
		return new TrackerPanelLine(Text.literal(legacy), null);
	}

	public static TrackerPanelLine header(TrackerHeaderParts parts) {
		return new TrackerPanelLine(Text.empty(), parts);
	}

	public boolean isHeader() {
		return headerParts != null;
	}

	public record TrackerHeaderParts(
		String prefixLegacy,
		String modeLegacy,
		String clickLegacy
	) {
		public static TrackerHeaderParts fishingProfit(TrackerDisplayMode mode) {
			return new TrackerHeaderParts(
				"§aFishing Profit §7| ",
				"§7" + mode.displayName() + " ",
				""
			);
		}

		public static TrackerHeaderParts seaCreature(TrackerDisplayMode mode) {
			return new TrackerHeaderParts(
				"§aSea Creature Tracker §7| ",
				"§7" + mode.displayName() + " ",
				""
			);
		}

		public static TrackerHeaderParts slayerProfit(TrackerDisplayMode mode) {
			return new TrackerHeaderParts(
				"§cSlayer Profit §7| ",
				"§7" + mode.displayName() + " ",
				""
			);
		}

		public static String tooltipLine(TrackerDisplayMode mode) {
			return "§7Click: switch mode §8| §7Right-click: reset " + mode.displayName().toLowerCase(Locale.ROOT);
		}
	}
}
