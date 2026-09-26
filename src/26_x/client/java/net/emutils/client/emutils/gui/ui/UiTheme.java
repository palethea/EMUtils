package net.emutils.client.emutils.gui.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
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
	int overlay,
	int warning,
	/** Text and icons drawn on the accent color, such as a primary button's label. */
	int onAccent
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
		0x80000000,
		0xFFFF9B85,
		0xFFFFFFFF
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
		0x521B2320,
		0xFFC2412D,
		0xFFFFFFFF
	);

	private static final RecordComponent[] COMPONENTS = UiTheme.class.getRecordComponents();
	private static final Constructor<UiTheme> CONSTRUCTOR = canonicalConstructor();

	private static Constructor<UiTheme> canonicalConstructor() {
		try {
			return UiTheme.class.getDeclaredConstructor(Arrays.stream(COMPONENTS).map(RecordComponent::getType).toArray(Class<?>[]::new));
		} catch (NoSuchMethodException exception) {
			throw new IllegalStateException(exception);
		}
	}

	/** The theme picked in the menu settings, with its accent color. */
	public static UiTheme current() {
		boolean dark = EMUtilsClient.config() == null || EMUtilsClient.config().settingsUiDark();
		return (dark ? DARK : LIGHT).withAccent(accentColor(), dark ? 0.0F : 1.0F).withStyle();
	}

	/** Whether the menus are in the dark theme, without the accent applied. */
	public static boolean dark() {
		return EMUtilsClient.config() == null || EMUtilsClient.config().settingsUiDark();
	}

	/**
	 * The accent picked in the menu settings (#120), or the active profile's color when the menus follow
	 * the profile.
	 */
	public static int accentColor() {
		if (EMUtilsClient.config() == null) {
			return DARK.accent;
		}
		if (EMUtilsClient.config().uiAccentFromProfile() && EMUtilsClient.profiles() != null) {
			return EMUtilsClient.profiles().active().color().argb();
		}
		return EMUtilsClient.config().uiAccent();
	}

	/**
	 * This theme with another accent color. {@code lightness} (0 dark, 1 light, between while the theme
	 * crossfades) decides whether hovering lightens the accent, on dark panels, or darkens it, on light
	 * ones. Text on the accent turns dark when the accent is light, so it stays readable.
	 */
	public UiTheme withAccent(int color, float lightness) {
		int newAccent = color | 0xFF000000;
		int newAccentHover = mix(mix(newAccent, 0xFFFFFFFF, 0.12F), mix(newAccent, 0xFF000000, 0.14F), lightness);
		int newOnAccent = luminance(newAccent) > 0.6F ? 0xFF141817 : 0xFFFFFFFF;
		if (newAccent == accent && newAccentHover == accentHover && newOnAccent == onAccent) {
			return this;
		}
		return new UiTheme(
			dim, panel, surface, surfaceHover, surfaceAlt, border, segmentBackground, segmentSelected, text, textSecondary, muted,
			placeholder, line, switchOff, hover, selectedBackground, selectedText, shadow, newAccent, newAccentHover, devBackground, devText,
			render, hud, utility, management, qol, overlay, warning, newOnAccent
		);
	}

	/**
	 * This theme with the High contrast menu setting (#149) applied: secondary text, borders and
	 * dividers moved towards the main text color, so they stand out more.
	 */
	public UiTheme withStyle() {
		if (!UiStyle.highContrast()) {
			return this;
		}
		return new UiTheme(
			dim, panel, surface, surfaceHover, surfaceAlt, fade(text, 0.3F), segmentBackground, segmentSelected, text,
			mix(textSecondary, text, 0.6F), mix(muted, text, 0.45F), mix(placeholder, text, 0.35F), mix(line, text, 0.3F),
			mix(switchOff, text, 0.2F), hover, selectedBackground, selectedText, shadow, accent, accentHover, devBackground, devText,
			render, hud, utility, management, qol, overlay, warning, onAccent
		);
	}

	/** Relative luminance from 0 (black) to 1 (white), as WCAG defines it. */
	public static float luminance(int color) {
		return 0.2126F * channel(color >>> 16) + 0.7152F * channel(color >>> 8) + 0.0722F * channel(color);
	}

	private static float channel(int value) {
		float c = (value & 0xFF) / 255.0F;
		return c <= 0.03928F ? c / 12.92F : (float) Math.pow((c + 0.055F) / 1.055F, 2.4F);
	}

	/**
	 * Every color blended from {@code from} to {@code to}; {@code t} = 0 gives {@code from}, 1 gives
	 * {@code to}. Used to crossfade when switching between dark and light.
	 */
	public static UiTheme blend(UiTheme from, UiTheme to, float t) {
		if (t <= 0.0F) {
			return from;
		}
		if (t >= 1.0F) {
			return to;
		}
		try {
			Object[] colors = new Object[COMPONENTS.length];
			for (int i = 0; i < COMPONENTS.length; i++) {
				Method accessor = COMPONENTS[i].getAccessor();
				colors[i] = mix((int) accessor.invoke(from), (int) accessor.invoke(to), t);
			}
			return CONSTRUCTOR.newInstance(colors);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not blend UI themes", exception);
		}
	}

	/** A category's color, or the muted text color with Category colors turned off (#149). */
	public int groupColor(HubFeature.Group group) {
		if (!UiStyle.categoryColors()) {
			return muted;
		}
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
