package net.emutils.client.emutils.gui.ui;

/**
 * Extra scale that text and icons are rasterized at, for things drawn under a scaled pose, such as a
 * HUD element at 200%: with it they get as many texels as screen pixels instead of being stretched.
 * Set it around the scaled drawing and reset it afterwards; only for scales that stay put, since
 * every new scale renders its own textures.
 */
public final class UiRasterScale {
	private static float value = 1.0F;

	private UiRasterScale() {
	}

	public static void set(float scale) {
		// Rounded, so nearby scales share textures.
		value = Math.max(0.1F, Math.round(scale * 20.0F) / 20.0F);
	}

	public static float get() {
		return value;
	}

	public static void reset() {
		value = 1.0F;
	}
}
