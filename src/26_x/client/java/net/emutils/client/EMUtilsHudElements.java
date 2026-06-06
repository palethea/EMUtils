package net.emutils.client;

import net.emhelpers.client.hud.layout.HudElementId;
import net.emutils.client.emutils.util.EMUtilsTexts;

public final class EMUtilsHudElements {
	public static final HudElementId INFO_OVERLAY = HudElementId.of("info_overlay", EMUtilsTexts.HUD_ELEMENT_INFO_OVERLAY);
	public static final HudElementId SPOTIFY = HudElementId.of("spotify", EMUtilsTexts.HUD_ELEMENT_SPOTIFY);
	public static final HudElementId INVENTORY_PREVIEW = HudElementId.of("inventory_preview", EMUtilsTexts.HUD_ELEMENT_INVENTORY_PREVIEW);

	private EMUtilsHudElements() {
	}
}
