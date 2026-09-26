package net.emutils.client.emutils.gui.ui;

import org.jspecify.annotations.Nullable;

/** How the menus animate (#149): as designed, twice as fast, or not at all. */
public enum UiMotion {
	NORMAL,
	FAST,
	OFF;

	public static UiMotion byName(@Nullable String name) {
		for (UiMotion motion : values()) {
			if (motion.name().equalsIgnoreCase(name)) {
				return motion;
			}
		}
		return NORMAL;
	}
}
