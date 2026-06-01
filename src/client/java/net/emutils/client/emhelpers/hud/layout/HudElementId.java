package net.emutils.client.emhelpers.hud.layout;

import net.emutils.client.emhelpers.util.EMUtilsTexts;

public enum HudElementId {
	INFO_OVERLAY(EMUtilsTexts.HUD_ELEMENT_INFO_OVERLAY),
	SPOTIFY(EMUtilsTexts.HUD_ELEMENT_SPOTIFY),
	INVENTORY_PREVIEW(EMUtilsTexts.HUD_ELEMENT_INVENTORY_PREVIEW),
	SKYBLOCK_STATS(EMUtilsTexts.HUD_ELEMENT_SKYBLOCK_STATS),
	ESTIMATED_ITEM_VALUE(EMUtilsTexts.HUD_ELEMENT_ESTIMATED_ITEM_VALUE),
	BAZAAR_BEST_SELL_METHOD(EMUtilsTexts.HUD_ELEMENT_BAZAAR_BEST_SELL_METHOD),
	BAZAAR_MAX_PURSE_ITEMS(EMUtilsTexts.HUD_ELEMENT_BAZAAR_MAX_PURSE_ITEMS),
	BAZAAR_DAILY_LIMIT(EMUtilsTexts.HUD_ELEMENT_BAZAAR_DAILY_LIMIT),
	BAZAAR_CRAFT_MATERIAL_COLLECTOR(EMUtilsTexts.HUD_ELEMENT_BAZAAR_CRAFT_MATERIAL_COLLECTOR),
	FISHING_HOOK(EMUtilsTexts.HUD_ELEMENT_FISHING_HOOK),
	SEA_CREATURE_TRACKER(EMUtilsTexts.HUD_ELEMENT_SEA_CREATURE_TRACKER),
	FISHING_PROFIT_TRACKER(EMUtilsTexts.HUD_ELEMENT_FISHING_PROFIT_TRACKER),
	SLAYER_PROFIT_TRACKER(EMUtilsTexts.HUD_ELEMENT_SLAYER_PROFIT_TRACKER);

	private final String labelKey;

	HudElementId(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public String configKey() {
		return name().toLowerCase();
	}

	public static HudElementId fromConfigKey(String key) {
		if (key != null) {
			for (HudElementId id : values()) {
				if (id.configKey().equals(key)) {
					return id;
				}
			}
		}

		return INFO_OVERLAY;
	}
}
