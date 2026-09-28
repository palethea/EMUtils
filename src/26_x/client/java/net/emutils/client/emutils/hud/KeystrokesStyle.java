package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** The shape of the Keystrokes overlay's keys (#43). */
public enum KeystrokesStyle {
	ROUNDED(EMUtilsTexts.OPTION_KEYSTROKES_STYLE_ROUNDED, 6),
	SQUARE(EMUtilsTexts.OPTION_KEYSTROKES_STYLE_SQUARE, 2);

	private final String labelKey;
	private final int radius;

	KeystrokesStyle(String labelKey, int radius) {
		this.labelKey = labelKey;
		this.radius = radius;
	}

	public String labelKey() {
		return labelKey;
	}

	public int radius() {
		return radius;
	}

	public static KeystrokesStyle fromName(@Nullable String name) {
		if (name != null) {
			for (KeystrokesStyle style : values()) {
				if (style.name().equals(name)) {
					return style;
				}
			}
		}
		return ROUNDED;
	}
}
