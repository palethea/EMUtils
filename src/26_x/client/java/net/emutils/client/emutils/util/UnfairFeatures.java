package net.emutils.client.emutils.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** What the Unfair Features switch (#213) tells you when a key asks for one of them while they're off. */
public final class UnfairFeatures {
	private UnfairFeatures() {
	}

	/** Says above the hotbar that a feature's key does nothing because the Unfair Features are off. */
	public static void tellOff(Minecraft client, Component feature) {
		client.gui.hud.setOverlayMessage(Component.translatable(EMUtilsTexts.UI_UNFAIR_OFF_MESSAGE, feature), false);
	}
}
