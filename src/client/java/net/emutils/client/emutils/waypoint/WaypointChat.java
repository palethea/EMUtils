package net.emutils.client.emutils.waypoint;

import java.nio.charset.StandardCharsets;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;

public final class WaypointChat {
	private WaypointChat() {
	}

	public static void showNearPrompt(ChatHud chatHud, long timestamp) {
		chatHud.addMessage(
			WaypointMessage.nearWaypointPrompt(timestamp),
			createNearPromptSignature(timestamp),
			MessageIndicator.system()
		);
	}

	public static void removeNearPrompt(ChatHud chatHud, long timestamp) {
		((WaypointChatAccess) chatHud).emutils$removeMessageSilently(createNearPromptSignature(timestamp));
	}

	private static MessageSignatureData createNearPromptSignature(long timestamp) {
		byte[] data = new byte[MessageSignatureData.SIZE];
		byte[] seed = ("emutils:waypoint_prompt:" + timestamp).getBytes(StandardCharsets.UTF_8);
		System.arraycopy(seed, 0, data, 0, Math.min(seed.length, data.length));
		return new MessageSignatureData(data);
	}
}
