package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** The font a custom server HUD element, the scoreboard (#169) or the tab list (#170), writes its text in. */
public enum HudFont {
	/** Minecraft's font, which keeps every server color and style exactly. */
	MINECRAFT(EMUtilsTexts.OPTION_HUD_FONT_MINECRAFT),
	/** The settings UI's font. Server colors and bold stay, italics and obfuscation don't. */
	EMUTILS(EMUtilsTexts.OPTION_HUD_FONT_EMUTILS);

	private final String labelKey;

	HudFont(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static HudFont fromName(@Nullable String name) {
		if (name != null) {
			for (HudFont font : values()) {
				if (font.name().equals(name)) {
					return font;
				}
			}
		}
		return MINECRAFT;
	}
}
