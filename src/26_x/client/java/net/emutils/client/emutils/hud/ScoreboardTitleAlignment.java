package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** Where the custom scoreboard's (#169) title sits above its lines. */
public enum ScoreboardTitleAlignment {
	CENTERED(EMUtilsTexts.OPTION_SCOREBOARD_TITLE_CENTERED),
	LEFT(EMUtilsTexts.OPTION_SCOREBOARD_TITLE_LEFT);

	private final String labelKey;

	ScoreboardTitleAlignment(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static ScoreboardTitleAlignment fromName(@Nullable String name) {
		if (name != null) {
			for (ScoreboardTitleAlignment alignment : values()) {
				if (alignment.name().equals(name)) {
					return alignment;
				}
			}
		}
		return CENTERED;
	}
}
