package net.emutils.client.emutils.gui.hub;

import net.emutils.client.emutils.util.EMUtilsTexts;

public enum HubCategory {
	CHAT(EMUtilsTexts.HUB_CHAT_FEATURES),
	DEATH_WAYPOINTS(EMUtilsTexts.HUB_WAYPOINTS),
	AUTO_RECONNECT(EMUtilsTexts.HUB_AUTO_RECONNECT),
	SCREENSHOT(EMUtilsTexts.HUB_SCREENSHOT_HELPER),
	MANAGERS(EMUtilsTexts.HUB_MANAGERS),
	HUD_OVERLAY(EMUtilsTexts.HUB_HUD_OVERLAY),
	ZOOM(EMUtilsTexts.HUB_ZOOM),
	TWEAKS(EMUtilsTexts.HUB_TWEAKS),
	CAPES(EMUtilsTexts.HUB_CAPES),
	INVENTORY(EMUtilsTexts.HUB_INVENTORY_TOOLS),
	SPOTIFY(EMUtilsTexts.HUB_SPOTIFY_PLAYER);

	private final String titleKey;

	HubCategory(String titleKey) {
		this.titleKey = titleKey;
	}

	public String titleKey() {
		return titleKey;
	}
}
