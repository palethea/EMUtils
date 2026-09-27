package net.emutils.client.emutils.tweaks;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** What Anti Durability Break's Protect At and Warn At values count: durability points left, or percent of the maximum. */
public enum AntiDurabilityUnit {
	DURABILITY(EMUtilsTexts.OPTION_ANTI_DURABILITY_UNIT_DURABILITY, ""),
	PERCENT(EMUtilsTexts.OPTION_ANTI_DURABILITY_UNIT_PERCENT, EMUtilsTexts.SUFFIX_PERCENT);

	private final String labelKey;
	private final String suffixKey;

	AntiDurabilityUnit(String labelKey, String suffixKey) {
		this.labelKey = labelKey;
		this.suffixKey = suffixKey;
	}

	public String labelKey() {
		return labelKey;
	}

	/** The suffix the threshold sliders show after their value. */
	public String suffixKey() {
		return suffixKey;
	}

	/** Whether an item with {@code remaining} of {@code max} durability left is at or below {@code threshold}. */
	public boolean atOrBelow(int remaining, int max, int threshold) {
		return switch (this) {
			case DURABILITY -> remaining <= threshold;
			case PERCENT -> remaining * 100L <= (long) threshold * max;
		};
	}

	public static AntiDurabilityUnit fromName(@Nullable String name) {
		if (name != null) {
			for (AntiDurabilityUnit unit : values()) {
				if (unit.name().equals(name)) {
					return unit;
				}
			}
		}

		return DURABILITY;
	}
}
