package net.emutils.client.hud;

import net.emutils.client.EMUtilsClient;
import net.minecraft.util.Identifier;

public record HudOverlayLine(String labelKey, String value, Identifier icon) {
	public static Identifier icon(String name) {
		return Identifier.of(EMUtilsClient.MOD_ID, "textures/gui/hud/" + name + ".png");
	}
}
