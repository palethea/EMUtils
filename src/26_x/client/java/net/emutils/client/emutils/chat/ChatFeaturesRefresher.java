package net.emutils.client.emutils.chat;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;

public final class ChatFeaturesRefresher {
	private ChatFeaturesRefresher() {
	}

	public static void refreshDisplayedMessages() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.gui == null) {
			return;
		}

		ChatComponent chatHud = net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client);
		if (chatHud instanceof ChatHudAccess access) {
			access.emutils$refreshDisplayedMessages();
		}
	}

	public static void onTimestampSettingsChanged() {
		if (EMUtilsClient.config() == null) {
			return;
		}

		refreshDisplayedMessages();
	}
}
