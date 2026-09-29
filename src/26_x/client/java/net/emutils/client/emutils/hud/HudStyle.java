package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** How a custom server HUD element, the scoreboard (#169) or the tab list (#170), is drawn. */
public enum HudStyle {
	/** A card in the settings UI's look, like the HUD Overlay. */
	CARD(EMUtilsTexts.OPTION_HUD_STYLE_CARD),
	/** The dark translucent bars vanilla draws behind the sidebar. */
	VANILLA(EMUtilsTexts.OPTION_HUD_STYLE_VANILLA);

	private final String labelKey;

	HudStyle(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static HudStyle fromName(@Nullable String name) {
		if (name != null) {
			for (HudStyle style : values()) {
				if (style.name().equals(name)) {
					return style;
				}
			}
		}
		return CARD;
	}
}
