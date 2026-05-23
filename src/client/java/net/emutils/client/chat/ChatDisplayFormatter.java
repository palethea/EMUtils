package net.emutils.client.chat;

import net.emutils.client.config.EMUtilsConfig;
import net.minecraft.text.Text;

public final class ChatDisplayFormatter {
	private ChatDisplayFormatter() {
	}

	public static Text format(ChatMessageMetadata metadata, EMUtilsConfig config) {
		Text message = metadata.duplicateCount() > 1
			? SmartChatFilter.withDuplicateCount(metadata.baseMessage(), metadata.duplicateCount())
			: metadata.baseMessage().copy();

		if (config.chatTimestamps()) {
			return ChatTimestampFormatter.prependTimestamp(
				message,
				config.chatTimestamp24Hour(),
				metadata.receivedAtMillis()
			);
		}

		return message;
	}
}
