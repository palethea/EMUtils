package net.emutils.client.emutils.gui.ui;

/**
 * Scales Minecraft's menu background blur, so a screen can fade the blur in and out with itself
 * instead of it switching on and off in one frame. The blur radius is a whole number of pixels, so it
 * ramps up in small steps.
 */
public final class UiBlur {
	private static float factor = 1.0F;

	private UiBlur() {
	}

	public static void set(float value) {
		factor = Math.clamp(value, 0.0F, 1.0F);
	}

	public static float get() {
		return factor;
	}

	public static void reset() {
		factor = 1.0F;
	}

	/** The blur radius from the options, scaled by the current factor. */
	public static int apply(int radius) {
		return factor >= 1.0F ? radius : Math.round(radius * factor);
	}
}
