package net.emutils.client.death;

import java.nio.charset.StandardCharsets;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;

public final class DeathWaypointChat {
	private DeathWaypointChat() {
	}

	public static void showNearPrompt(ChatHud chatHud, long deathTimestamp) {
		chatHud.addMessage(
			DeathWaypointMessage.nearWaypointPrompt(deathTimestamp),
			createNearPromptSignature(deathTimestamp),
			MessageIndicator.system()
		);
	}

	public static void removeNearPrompt(ChatHud chatHud, long deathTimestamp) {
		((DeathWaypointChatAccess) chatHud).emutils$removeMessageSilently(createNearPromptSignature(deathTimestamp));
	}

	private static MessageSignatureData createNearPromptSignature(long deathTimestamp) {
		byte[] data = new byte[MessageSignatureData.SIZE];
		byte[] seed = ("emutils:death_waypoint_prompt:" + deathTimestamp).getBytes(StandardCharsets.UTF_8);
		System.arraycopy(seed, 0, data, 0, Math.min(seed.length, data.length));
		return new MessageSignatureData(data);
	}
}
