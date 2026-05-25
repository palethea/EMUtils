package net.emutils.client.hud;

import net.emutils.client.util.EMUtilsTexts;

public enum HudOverlayAnchor {
	TOP_LEFT(EMUtilsTexts.HUD_ANCHOR_TOP_LEFT),
	TOP_CENTER(EMUtilsTexts.HUD_ANCHOR_TOP_CENTER),
	TOP_RIGHT(EMUtilsTexts.HUD_ANCHOR_TOP_RIGHT),
	BOTTOM_LEFT(EMUtilsTexts.HUD_ANCHOR_BOTTOM_LEFT),
	BOTTOM_CENTER(EMUtilsTexts.HUD_ANCHOR_BOTTOM_CENTER),
	BOTTOM_RIGHT(EMUtilsTexts.HUD_ANCHOR_BOTTOM_RIGHT);

	private final String labelKey;

	HudOverlayAnchor(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
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
