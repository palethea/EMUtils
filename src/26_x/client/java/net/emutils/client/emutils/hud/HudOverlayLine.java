package net.emutils.client.emutils.hud;

import net.emutils.client.EMUtilsClient;
import net.minecraft.resources.Identifier;

/** One line of the HUD Overlay; {@code tone} colors the value, such as the TPS line's health. */
public record HudOverlayLine(String labelKey, String value, Identifier icon, Tone tone) {
	/** The value's color, taken from the current theme so it reads in dark and light. */
	public enum Tone {
		NORMAL,
		GOOD,
		SLOW,
		BAD
	}

	public HudOverlayLine(String labelKey, String value, Identifier icon) {
		this(labelKey, value, icon, Tone.NORMAL);
	}

	public static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "textures/gui/hud/" + name + ".png");
	}
}
