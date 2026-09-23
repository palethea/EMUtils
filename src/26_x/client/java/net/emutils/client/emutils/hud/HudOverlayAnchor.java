package net.emutils.client.emutils.hud;

import java.util.function.Supplier;
import net.emutils.client.emutils.util.EMHelpersTexts;

public enum HudOverlayAnchor {
	TOP_LEFT(EMHelpersTexts::hudAnchorTopLeft),
	TOP_CENTER(EMHelpersTexts::hudAnchorTopCenter),
	TOP_RIGHT(EMHelpersTexts::hudAnchorTopRight),
	BOTTOM_LEFT(EMHelpersTexts::hudAnchorBottomLeft),
	BOTTOM_CENTER(EMHelpersTexts::hudAnchorBottomCenter),
	BOTTOM_RIGHT(EMHelpersTexts::hudAnchorBottomRight);

	private final Supplier<String> labelKey;

	HudOverlayAnchor(Supplier<String> labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey.get();
	}

	public HudOverlayAnchor next() {
		HudOverlayAnchor[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public int x(int screenWidth, int panelWidth, int margin) {
		return switch (this) {
			case TOP_CENTER, BOTTOM_CENTER -> (screenWidth - panelWidth) / 2;
			case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - panelWidth - margin;
			case TOP_LEFT, BOTTOM_LEFT -> margin;
		};
	}

	public int y(int screenHeight, int panelHeight, int margin) {
		return switch (this) {
			case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - panelHeight - margin;
			case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> margin;
		};
	}

	public boolean isTop() {
		return switch (this) {
			case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> true;
			case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> false;
		};
	}

	public boolean isBottom() {
		return !isTop();
	}

	public static HudOverlayAnchor fromName(String name) {
		if (name != null) {
			for (HudOverlayAnchor anchor : values()) {
				if (anchor.name().equals(name)) {
					return anchor;
				}
			}
		}

		return TOP_LEFT;
	}
}
