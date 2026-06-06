package net.emutils.client.emutils.chat;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;

public final class ChatFeaturesRefresher {
	private ChatFeaturesRefresher() {
	}

	public static void refreshDisplayedMessages() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.inGameHud == null) {
			return;
		}

		ChatHud chatHud = client.inGameHud.getChatHud();
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
