package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** When the HUD Overlay's text and icons get a shadow (#48). */
public enum HudTextShadow {
	/** Only while the background is under 50% opacity, where the world shows through. */
	AUTO(EMUtilsTexts.OPTION_HUD_TEXT_SHADOW_AUTO),
	ON(EMUtilsTexts.OPTION_HUD_TEXT_SHADOW_ON),
	OFF(EMUtilsTexts.OPTION_HUD_TEXT_SHADOW_OFF);

	private final String labelKey;

	HudTextShadow(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public boolean shadow(int opacityPercent, int autoBelowPercent) {
		return switch (this) {
			case AUTO -> opacityPercent < autoBelowPercent;
			case ON -> true;
			case OFF -> false;
		};
	}

	public static HudTextShadow fromName(@Nullable String name) {
		if (name != null) {
			for (HudTextShadow mode : values()) {
				if (mode.name().equals(name)) {
					return mode;
				}
			}
		}
		return AUTO;
	}
}
