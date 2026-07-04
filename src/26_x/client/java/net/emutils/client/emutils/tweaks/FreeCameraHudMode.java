package net.emutils.client.emutils.tweaks;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

public enum FreeCameraHudMode {
	SPECTATOR(EMUtilsTexts.OPTION_FREE_CAMERA_HUD_MODE_SPECTATOR),
	REGULAR(EMUtilsTexts.OPTION_FREE_CAMERA_HUD_MODE_REGULAR);

	private final String labelKey;

	FreeCameraHudMode(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public FreeCameraHudMode next() {
		FreeCameraHudMode[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static FreeCameraHudMode fromName(@Nullable String name) {
		if (name != null) {
			for (FreeCameraHudMode mode : values()) {
				if (mode.name().equals(name)) {
					return mode;
				}
			}
		}

		return SPECTATOR;
	}
}
