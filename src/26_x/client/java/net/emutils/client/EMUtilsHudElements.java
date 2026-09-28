package net.emutils.client;

import net.emutils.client.emutils.hud.layout.HudElementId;
import net.emutils.client.emutils.util.EMUtilsTexts;

public final class EMUtilsHudElements {
	public static final HudElementId INFO_OVERLAY = HudElementId.of("info_overlay", EMUtilsTexts.HUD_ELEMENT_INFO_OVERLAY);
	public static final HudElementId SPOTIFY = HudElementId.of("spotify", EMUtilsTexts.HUD_ELEMENT_SPOTIFY);
	public static final HudElementId INVENTORY_PREVIEW = HudElementId.of("inventory_preview", EMUtilsTexts.HUD_ELEMENT_INVENTORY_PREVIEW);
	public static final HudElementId LOOK_AT_INFO = HudElementId.of("look_at_info", EMUtilsTexts.HUD_ELEMENT_LOOK_AT_INFO);

	private EMUtilsHudElements() {
	}
}
