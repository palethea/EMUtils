package net.emutils.client.hud.layout;

import net.emutils.client.util.EMUtilsTexts;

public enum HudElementId {
	INFO_OVERLAY(EMUtilsTexts.HUD_ELEMENT_INFO_OVERLAY),
	SPOTIFY(EMUtilsTexts.HUD_ELEMENT_SPOTIFY),
	INVENTORY_PREVIEW(EMUtilsTexts.HUD_ELEMENT_INVENTORY_PREVIEW),
	SKYBLOCK_STATS(EMUtilsTexts.HUD_ELEMENT_SKYBLOCK_STATS),
	ESTIMATED_ITEM_VALUE(EMUtilsTexts.HUD_ELEMENT_ESTIMATED_ITEM_VALUE);

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
