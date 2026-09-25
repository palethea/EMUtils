package net.emutils.client.emutils.gui.ui;

/**
 * A global opacity for everything the UI toolkit draws, so a whole panel can fade in and out together.
 * Set it before drawing the panel and reset it afterwards.
 */
public final class UiOpacity {
	private static float value = 1.0F;

	private UiOpacity() {
	}

	public static void set(float opacity) {
		value = Math.clamp(opacity, 0.0F, 1.0F);
	}

	public static float get() {
		return value;
	}

	public static void reset() {
		value = 1.0F;
	}

	/** {@code color} with its alpha multiplied by the current opacity. */
	public static int apply(int color) {
		if (value >= 1.0F) {
			return color;
		}
		int alpha = Math.round(((color >>> 24) & 0xFF) * value);
		return (alpha << 24) | (color & 0x00FFFFFF);
	}
}
