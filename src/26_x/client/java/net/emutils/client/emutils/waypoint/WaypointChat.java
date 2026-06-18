package net.emutils.client.emutils.waypoint;

import java.nio.charset.StandardCharsets;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;

public final class WaypointChat {
	private WaypointChat() {
	}

	public static void showNearPrompt(ChatComponent chatHud, long timestamp) {
		chatHud.addPlayerMessage(
			WaypointMessage.nearWaypointPrompt(timestamp),
			createNearPromptSignature(timestamp),
			GuiMessageTag.system()
		);
	}

	public static void removeNearPrompt(ChatComponent chatHud, long timestamp) {
		((WaypointChatAccess) chatHud).emutils$removeMessageSilently(createNearPromptSignature(timestamp));
	}

	private static MessageSignature createNearPromptSignature(long timestamp) {
		byte[] data = new byte[MessageSignature.BYTES];
		byte[] seed = ("emutils:waypoint_prompt:" + timestamp).getBytes(StandardCharsets.UTF_8);
		System.arraycopy(seed, 0, data, 0, Math.min(seed.length, data.length));
		return new MessageSignature(data);
	}
}
