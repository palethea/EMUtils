package net.emutils.client.emutils.waypoint;

import java.nio.charset.StandardCharsets;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;

public final class WaypointChat {
	private WaypointChat() {
	}

	public static void showNearPrompt(ChatComponent chatHud, String id) {
		chatHud.addPlayerMessage(
			WaypointMessage.nearWaypointPrompt(id),
			createNearPromptSignature(id),
			GuiMessageTag.system()
		);
	}

	public static void removeNearPrompt(ChatComponent chatHud, String id) {
		((WaypointChatAccess) chatHud).emutils$removeMessageSilently(createNearPromptSignature(id));
	}

	private static MessageSignature createNearPromptSignature(String id) {
		byte[] data = new byte[MessageSignature.BYTES];
		byte[] seed = ("emutils:waypoint_prompt:" + id).getBytes(StandardCharsets.UTF_8);
		System.arraycopy(seed, 0, data, 0, Math.min(seed.length, data.length));
		return new MessageSignature(data);
	}
}
