package net.emutils.client.emutils.chat;

import net.minecraft.network.chat.Component;

public record ChatMessageMetadata(Component baseMessage, int duplicateCount, long receivedAtMillis, boolean mentionsCurrentPlayer) {
	public ChatMessageMetadata {
		if (duplicateCount < 1) {
			duplicateCount = 1;
		}
	}
}
