package net.emutils.client.emutils.chat;

import net.minecraft.text.Text;

public record ChatMessageMetadata(Text baseMessage, int duplicateCount, long receivedAtMillis, boolean mentionsCurrentPlayer) {
	public ChatMessageMetadata {
		if (duplicateCount < 1) {
			duplicateCount = 1;
		}
	}
}
