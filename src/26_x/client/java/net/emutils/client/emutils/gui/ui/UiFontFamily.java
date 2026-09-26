package net.emutils.client.emutils.gui.ui;

import org.jspecify.annotations.Nullable;

/**
 * The fonts the settings UI can be set in (#120). Nunito ships one file per weight; the others are
 * variable fonts from Google Fonts, one file with every weight in it. Minecraft uses the game's own
 * font and none of these files. Every bundled font has its OFL license next to it.
 */
public enum UiFontFamily {
	NUNITO("Nunito", "nunito_semibold.ttf", "nunito_extrabold.ttf", "nunito_black.ttf", false),
	INTER("Inter", "inter.ttf", "inter.ttf", "inter.ttf", true),
	RUBIK("Rubik", "rubik.ttf", "rubik.ttf", "rubik.ttf", true),
	FIGTREE("Figtree", "figtree.ttf", "figtree.ttf", "figtree.ttf", true),
	MINECRAFT("Minecraft", null, null, null, false);

	private final String displayName;
	private final @Nullable String semibold;
	private final @Nullable String extrabold;
	private final @Nullable String black;
	private final boolean variable;

	UiFontFamily(String displayName, @Nullable String semibold, @Nullable String extrabold, @Nullable String black, boolean variable) {
		this.displayName = displayName;
		this.semibold = semibold;
		this.extrabold = extrabold;
		this.black = black;
		this.variable = variable;
	}

	public String displayName() {
		return displayName;
	}

	/** Whether text is drawn with Minecraft's own font instead of a bundled one. */
	public boolean minecraft() {
		return this == MINECRAFT;
	}

	/** The file for a weight, or null for Minecraft's font. */
	@Nullable String file(UiFontRenderer.Weight weight) {
		return switch (weight) {
			case SEMIBOLD -> semibold;
			case EXTRABOLD -> extrabold;
			case BLACK -> black;
			case MONO -> null;
		};
	}

	/** Whether one variable file holds every weight, so the weight has to be picked when loading. */
	boolean variable() {
		return variable;
	}

	public static UiFontFamily byName(@Nullable String name) {
		for (UiFontFamily family : values()) {
			if (family.name().equalsIgnoreCase(name)) {
				return family;
			}
		}
		return NUNITO;
	}
}
