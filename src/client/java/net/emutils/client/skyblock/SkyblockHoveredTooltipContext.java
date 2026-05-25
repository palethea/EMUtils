package net.emutils.client.skyblock;

import java.util.List;
import net.minecraft.text.Text;

public final class SkyblockHoveredTooltipContext {
	private static final ThreadLocal<List<Text>> TOOLTIP = new ThreadLocal<>();

	private SkyblockHoveredTooltipContext() {
	}

	public static void set(List<Text> tooltip) {
		TOOLTIP.set(tooltip);
	}

	public static List<Text> get() {
		List<Text> tooltip = TOOLTIP.get();
		return tooltip == null ? List.of() : tooltip;
	}

	public static void clear() {
		TOOLTIP.remove();
	}
}
