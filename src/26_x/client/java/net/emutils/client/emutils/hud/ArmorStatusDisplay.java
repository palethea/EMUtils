package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** How Armor Status (#44) writes an item's durability. */
public enum ArmorStatusDisplay {
	REMAINING(EMUtilsTexts.OPTION_ARMOR_STATUS_DISPLAY_REMAINING, "0000"),
	REMAINING_MAX(EMUtilsTexts.OPTION_ARMOR_STATUS_DISPLAY_REMAINING_MAX, "0000/0000"),
	PERCENT(EMUtilsTexts.OPTION_ARMOR_STATUS_DISPLAY_PERCENT, "100%");

	private final String labelKey;
	private final String widest;

	ArmorStatusDisplay(String labelKey, String widest) {
		this.labelKey = labelKey;
		this.widest = widest;
	}

	public String labelKey() {
		return labelKey;
	}

	/** The widest text this display writes, so the card keeps its width as durability changes. */
	String widest() {
		return widest;
	}

	String format(int remaining, int max) {
		return switch (this) {
			case REMAINING -> Integer.toString(remaining);
			case REMAINING_MAX -> remaining + "/" + max;
			case PERCENT -> Math.round(remaining * 100.0F / max) + "%";
		};
	}

	public static ArmorStatusDisplay fromName(@Nullable String name) {
		if (name != null) {
			for (ArmorStatusDisplay display : values()) {
				if (display.name().equals(name)) {
					return display;
				}
			}
		}
		return REMAINING;
	}
}
