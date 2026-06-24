package net.emutils.client.emutils.gui.hub;

import net.emutils.client.emutils.util.EMUtilsTexts;

public enum HubCategory {
	CHAT(EMUtilsTexts.HUB_CHAT_FEATURES),
	DEATH_WAYPOINTS(EMUtilsTexts.HUB_WAYPOINTS),
	AUTO_RECONNECT(EMUtilsTexts.HUB_AUTO_RECONNECT),
	SCREENSHOT(EMUtilsTexts.HUB_SCREENSHOT_HELPER),
	SCREENSHOT_GALLERY(EMUtilsTexts.SCREEN_SCREENSHOT_GALLERY),
	MANAGERS(EMUtilsTexts.HUB_MANAGERS),
	HUD_OVERLAY(EMUtilsTexts.HUB_HUD_OVERLAY),
	FOOD_HUD(EMUtilsTexts.HUB_FOOD_HUD),
	ZOOM(EMUtilsTexts.HUB_ZOOM),
	FULLBRIGHT(EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT),
	CLEAR_WEATHER(EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER),
	TWEAKS(EMUtilsTexts.HUB_TWEAKS),
	AUTO_FLIGHT(EMUtilsTexts.OPTION_AUTO_FLIGHT_GEAR),
	AUTO_TOOL(EMUtilsTexts.OPTION_AUTO_TOOL),
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
