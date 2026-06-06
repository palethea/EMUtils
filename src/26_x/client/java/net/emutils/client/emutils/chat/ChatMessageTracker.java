package net.emutils.client.emutils.chat;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class ChatMessageTracker {
	private final Map<GuiMessage, ChatMessageMetadata> metadataByLine = new IdentityHashMap<>();

	public void register(GuiMessage line, Component baseMessage, int duplicateCount, long receivedAtMillis, boolean mentionsCurrentPlayer) {
		metadataByLine.put(line, new ChatMessageMetadata(baseMessage.copy(), duplicateCount, receivedAtMillis, mentionsCurrentPlayer));
	}

	public void replaceLine(GuiMessage oldLine, GuiMessage newLine) {
		ChatMessageMetadata metadata = metadataByLine.remove(oldLine);
		if (metadata != null) {
			metadataByLine.put(newLine, metadata);
		}
	}

	public void removeLine(GuiMessage line) {
		metadataByLine.remove(line);
	}

	public ChatMessageMetadata metadataFor(GuiMessage line) {
		ChatMessageMetadata metadata = metadataByLine.get(line);
		if (metadata != null) {
			return metadata;
		}

		return ChatMessageDecorations.metadataFromDisplayed(line.content(), System.currentTimeMillis());
	}

	@Nullable
	public ChatMessageMetadata trackedMetadata(GuiMessage line) {
		return metadataByLine.get(line);
	}

	public void clear() {
		metadataByLine.clear();
	}
}
