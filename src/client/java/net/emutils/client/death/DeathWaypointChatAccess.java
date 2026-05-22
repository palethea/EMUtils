package net.emutils.client.death;

import net.minecraft.network.message.MessageSignatureData;

public interface DeathWaypointChatAccess {
	void emutils$removeMessageSilently(MessageSignatureData signature);
}
