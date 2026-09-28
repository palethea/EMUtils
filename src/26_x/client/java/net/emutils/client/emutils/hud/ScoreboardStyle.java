package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** How the custom scoreboard (#169) is drawn. */
public enum ScoreboardStyle {
	/** A card in the settings UI's look, like the HUD Overlay. */
	CARD(EMUtilsTexts.OPTION_SCOREBOARD_STYLE_CARD),
	/** The dark translucent bars vanilla draws behind the sidebar. */
	VANILLA(EMUtilsTexts.OPTION_SCOREBOARD_STYLE_VANILLA);

	private final String labelKey;

	ScoreboardStyle(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static ScoreboardStyle fromName(@Nullable String name) {
		if (name != null) {
			for (ScoreboardStyle style : values()) {
				if (style.name().equals(name)) {
					return style;
				}
			}
		}
		return CARD;
	}
}
