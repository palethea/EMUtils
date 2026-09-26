package net.emutils.client.emutils.gui.ui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;

/**
 * The menu settings that change how the UI toolkit draws (#120, #149), with their defaults while the
 * config isn't loaded yet.
 */
public final class UiStyle {
	private UiStyle() {
	}

	private static EMUtilsConfig config() {
		return EMUtilsClient.config();
	}

	/** Menu text size as a factor, 1 at 100%; code has its own size. */
	public static float textScale() {
		return config() == null ? 1.0F : config().uiTextSize() / 100.0F;
	}

	public static UiMotion motion() {
		return config() == null ? UiMotion.NORMAL : config().uiMotion();
	}

	/** How strongly the world behind menus is blurred, from 0 to 1. */
	public static float blur() {
		return config() == null ? 1.0F : config().uiBackgroundBlur() / 100.0F;
	}

	/** How strongly the world behind menus is darkened, from 0 to 1. */
	public static float dim() {
		return config() == null ? 1.0F : config().uiBackgroundDim() / 100.0F;
	}

	/** The panel's opacity, from 0 to 1, on top of the theme's own. */
	public static float panelOpacity() {
		return config() == null ? 1.0F : config().uiPanelOpacity() / 100.0F;
	}

	/** Corner radii as a factor, from 0 (square) to 1 (as designed). */
	public static float roundness() {
		return config() == null ? 1.0F : config().uiCornerRoundness() / 100.0F;
	}

	public static boolean compactCards() {
		return config() != null && config().uiCompactCards();
	}

	public static boolean categoryColors() {
		return config() == null || config().uiCategoryColors();
	}

	public static boolean highContrast() {
		return config() != null && config().uiHighContrast();
	}
}
