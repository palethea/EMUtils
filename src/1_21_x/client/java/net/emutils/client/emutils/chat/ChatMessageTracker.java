package net.emutils.client.emutils.chat;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class ChatMessageTracker {
	private final Map<ChatHudLine, ChatMessageMetadata> metadataByLine = new IdentityHashMap<>();

	public void register(ChatHudLine line, Text baseMessage, int duplicateCount, long receivedAtMillis, boolean mentionsCurrentPlayer) {
		metadataByLine.put(line, new ChatMessageMetadata(baseMessage.copy(), duplicateCount, receivedAtMillis, mentionsCurrentPlayer));
	}

	public void replaceLine(ChatHudLine oldLine, ChatHudLine newLine) {
		ChatMessageMetadata metadata = metadataByLine.remove(oldLine);
		if (metadata != null) {
			metadataByLine.put(newLine, metadata);
		}
	}

	public void removeLine(ChatHudLine line) {
		metadataByLine.remove(line);
	}

	public ChatMessageMetadata metadataFor(ChatHudLine line) {
		ChatMessageMetadata metadata = metadataByLine.get(line);
		if (metadata != null) {
			return metadata;
		}

		return ChatMessageDecorations.metadataFromDisplayed(line.content(), System.currentTimeMillis());
	}

	@Nullable
	public ChatMessageMetadata trackedMetadata(ChatHudLine line) {
		return metadataByLine.get(line);
	}

	public void clear() {
		metadataByLine.clear();
	}
}
