package net.emutils.client.chat;

import net.minecraft.text.Text;

public record ChatMessageMetadata(Text baseMessage, int duplicateCount, long receivedAtMillis) {
	public ChatMessageMetadata {
		if (duplicateCount < 1) {
			duplicateCount = 1;
		}
	}
}
