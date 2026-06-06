package net.emutils.client.emutils.chat;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class ChatDisplayFormatter {
	private ChatDisplayFormatter() {
	}

	public static Component format(ChatMessageMetadata metadata, EMUtilsConfig config, @Nullable String username) {
		Component message = metadata.duplicateCount() > 1
			? SmartChatFilter.withDuplicateCount(metadata.baseMessage(), metadata.duplicateCount())
			: metadata.baseMessage().copy();

		if (config.chatTimestamps()) {
			message = ChatTimestampFormatter.prependTimestamp(
				message,
				config.chatTimestamp24Hour(),
				metadata.receivedAtMillis()
			);
		}

		if (config.chatMentionHighlight() && metadata.mentionsCurrentPlayer() && username != null) {
			message = ChatMentionHighlighter.highlight(message, username);
		}

		return message;
	}
}
