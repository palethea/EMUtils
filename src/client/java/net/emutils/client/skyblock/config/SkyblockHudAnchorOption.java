package net.emutils.client.skyblock.config;

import net.emutils.client.hud.HudOverlayAnchor;

public enum SkyblockHudAnchorOption {
	TOP_LEFT,
	TOP_CENTER,
	TOP_RIGHT,
	BOTTOM_LEFT,
	BOTTOM_CENTER,
	BOTTOM_RIGHT;

	public HudOverlayAnchor toAnchor() {
		return HudOverlayAnchor.valueOf(name());
	}

	public static SkyblockHudAnchorOption fromAnchor(HudOverlayAnchor anchor) {
		if (anchor == null) {
			return TOP_LEFT;
		}

		return valueOf(anchor.name());
	}
}
