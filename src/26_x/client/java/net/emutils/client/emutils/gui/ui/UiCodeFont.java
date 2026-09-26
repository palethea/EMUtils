package net.emutils.client.emutils.gui.ui;

import org.jspecify.annotations.Nullable;

/** The monospaced fonts code can be set in, such as the Script Manager's editor (#120). */
public enum UiCodeFont {
	JETBRAINS_MONO("JetBrains Mono", "jetbrains_mono_regular.ttf", false),
	FIRA_CODE("Fira Code", "fira_code.ttf", true),
	CASCADIA_CODE("Cascadia Code", "cascadia_code.ttf", true);

	private final String displayName;
	private final String file;
	private final boolean variable;

	UiCodeFont(String displayName, String file, boolean variable) {
		this.displayName = displayName;
		this.file = file;
		this.variable = variable;
	}

	public String displayName() {
		return displayName;
	}

	String file() {
		return file;
	}

	boolean variable() {
		return variable;
	}

	public static UiCodeFont byName(@Nullable String name) {
		for (UiCodeFont font : values()) {
			if (font.name().equalsIgnoreCase(name)) {
				return font;
			}
		}
		return JETBRAINS_MONO;
	}
}
