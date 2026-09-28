package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** The font the custom scoreboard (#169) writes its text in. */
public enum ScoreboardFont {
	/** Minecraft's font, which keeps every server color and style exactly. */
	MINECRAFT(EMUtilsTexts.OPTION_SCOREBOARD_FONT_MINECRAFT),
	/** The settings UI's font. Server colors and bold stay, italics and obfuscation don't. */
	EMUTILS(EMUtilsTexts.OPTION_SCOREBOARD_FONT_EMUTILS);

	private final String labelKey;

	ScoreboardFont(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static ScoreboardFont fromName(@Nullable String name) {
		if (name != null) {
			for (ScoreboardFont font : values()) {
				if (font.name().equals(name)) {
					return font;
				}
			}
		}
		return MINECRAFT;
	}
}
