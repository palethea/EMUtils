package net.emutils.client.emutils.tweaks;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

public enum AutoToolMode {
	LEGIT(EMUtilsTexts.OPTION_AUTO_TOOL_MODE_LEGIT),
	UNFAIR(EMUtilsTexts.OPTION_AUTO_TOOL_MODE_UNFAIR);

	private final String labelKey;

	AutoToolMode(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public AutoToolMode next() {
		AutoToolMode[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static AutoToolMode fromName(@Nullable String name) {
		if (name != null) {
			for (AutoToolMode mode : values()) {
				if (mode.name().equals(name)) {
					return mode;
				}
			}
		}

		return LEGIT;
	}
}
