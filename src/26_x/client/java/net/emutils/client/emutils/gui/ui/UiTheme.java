package net.emutils.client.emutils.gui.ui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.HubFeature;

/** Colors of the settings UI, as ARGB. Dark is the default. */
public record UiTheme(
	int dim,
	int panel,
	int surface,
	int surfaceHover,
	int surfaceAlt,
	int border,
	int segmentBackground,
	int segmentSelected,
	int text,
	int textSecondary,
	int muted,
	int placeholder,
	int line,
	int switchOff,
	int hover,
	int selectedBackground,
	int selectedText,
	int shadow,
	int accent,
	int accentHover,
	int devBackground,
	int devText,
	int render,
	int hud,
	int utility,
	int management,
	int qol,
	int overlay
) {
	public static final UiTheme DARK = new UiTheme(
		0x8C060908,
		0xF2121615,
		0xFF1D2321,
		0xFF242B28,
		0xFF252C29,
		0x0DFFFFFF,
		0xFF151A18,
		0xFF353E3A,
		0xFFE9EEEA,
		0xFFC6CFC9,
		0xFF9AA59F,
		0xFF7E8983,
		0xFF2D3531,
		0xFF3A433F,
		0x0FFFFFFF,
		0xFFE9EEEA,
		0xFF141817,
		0x59000000,
		0xFF16A058,
		0xFF1BB866,
		0x2EFFB84D,
		0xFFFFC56E,
		0xFF9CC2FF,
		0xFF6BE0A4,
		0xFFFFC56E,
		0xFFC9AEFF,
		0xFFFFA3BC,
		0x80000000
	);

	public static final UiTheme LIGHT = new UiTheme(
		0x400C1210,
		0xF2F3F5F1,
		0xFFFFFFFF,
		0xFFF7F9F6,
		0xFFF3F5F1,
		0x00000000,
		0xFFE3E8E4,
		0xFFFFFFFF,
		0xFF1B2320,
		0xFF3E4A44,
		0xFF56615B,
		0xFF6B766F,
		0xFFE6EBE7,
		0xFFD5DBD7,
		0x0F1B2320,
		0xFF1B2320,
		0xFFFFFFFF,
		0x261B2320,
		0xFF16A058,
		0xFF12894B,
		0xFFFFE7B8,
		0xFF7A4A00,
		0xFF2F5FB3,
		0xFF17804A,
		0xFF8A5200,
		0xFF6B45B8,
		0xFFA8345A,
		0x521B2320
	);

	public static UiTheme current() {
		return EMUtilsClient.config() == null || EMUtilsClient.config().settingsUiDark() ? DARK : LIGHT;
	}

	public int groupColor(HubFeature.Group group) {
		return switch (group) {
			case RENDER -> render;
			case HUD -> hud;
			case UTILITY -> utility;
			case MANAGEMENT -> management;
			case QOL -> qol;
		};
	}

	/** Blends two ARGB colors; {@code t} = 0 gives {@code from}, 1 gives {@code to}. */
	public static int mix(int from, int to, float t) {
		float clamped = Math.clamp(t, 0.0F, 1.0F);
		int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * clamped);
		int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * clamped);
		int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * clamped);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * clamped);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/** The same color with its alpha multiplied by {@code factor}. */
	public static int fade(int color, float factor) {
		int alpha = Math.round(((color >>> 24) & 0xFF) * Math.clamp(factor, 0.0F, 1.0F));
		return (alpha << 24) | (color & 0x00FFFFFF);
	}
}
