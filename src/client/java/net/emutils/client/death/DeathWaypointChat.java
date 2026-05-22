package net.emutils.client.death;

import java.nio.charset.StandardCharsets;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;

public final class DeathWaypointChat {
	private static final MessageSignatureData NEAR_PROMPT_SIGNATURE = createNearPromptSignature();

	private DeathWaypointChat() {
	}

	public static void showNearPrompt(ChatHud chatHud) {
		chatHud.addMessage(DeathWaypointMessage.nearWaypointPrompt(), NEAR_PROMPT_SIGNATURE, MessageIndicator.system());
	}

	public static void removeNearPrompt(ChatHud chatHud) {
		((DeathWaypointChatAccess) chatHud).emutils$removeMessageSilently(NEAR_PROMPT_SIGNATURE);
	}

	private static MessageSignatureData createNearPromptSignature() {
		byte[] data = new byte[MessageSignatureData.SIZE];
		byte[] seed = "emutils:death_waypoint_prompt".getBytes(StandardCharsets.UTF_8);
		System.arraycopy(seed, 0, data, 0, Math.min(seed.length, data.length));
		return new MessageSignatureData(data);
	}
}
