package net.emutils.client.emutils.hud;

import net.emutils.client.EMUtilsClient;
import net.minecraft.resources.Identifier;

public record HudOverlayLine(String labelKey, String value, Identifier icon) {
	public static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "textures/gui/hud/" + name + ".png");
	}
}
