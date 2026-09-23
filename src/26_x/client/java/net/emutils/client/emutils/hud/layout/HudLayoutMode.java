package net.emutils.client.emutils.hud.layout;

import java.util.function.Supplier;
import net.emutils.client.emutils.util.EMHelpersTexts;

public enum HudLayoutMode {
	ANCHOR(EMHelpersTexts::hudLayoutModeAnchor),
	CUSTOM(EMHelpersTexts::hudLayoutModeCustom);

	private final Supplier<String> labelKey;

	HudLayoutMode(Supplier<String> labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey.get();
	}

	public HudLayoutMode next() {
		HudLayoutMode[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static HudLayoutMode fromName(String name) {
		if (name != null) {
			for (HudLayoutMode mode : values()) {
				if (mode.name().equals(name)) {
					return mode;
				}
			}
		}

		return ANCHOR;
	}
}
