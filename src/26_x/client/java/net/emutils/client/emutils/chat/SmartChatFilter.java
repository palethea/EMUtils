package net.emutils.client.emutils.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

public final class SmartChatFilter {
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private final List<Entry> entries = new ArrayList<>();

	public PendingMessage prepare(
		Component message,
		long nowMillis,
		int windowSeconds,
		boolean useTimeWindow,
		@Nullable GuiMessage previousLine,
		@Nullable ChatMessageMetadata previousMetadata
	) {
		if (useTimeWindow) {
			return prepareTimeWindow(message, nowMillis, windowSeconds);
		}

		return prepareAdjacent(message, previousLine, previousMetadata);
	}

	public void track(PendingMessage pending, GuiMessage displayedLine) {
		if (pending.key.isEmpty()) {
			return;
		}

		entries.add(new Entry(pending.key, pending.firstMillis, pending.latestMillis, pending.duplicateCount, displayedLine));
	}

	public void clear() {
		entries.clear();
	}

	public static Component withDuplicateCount(Component message, int duplicateCount) {
		return Component.empty()
			.append(message.copy())
			.append(Component.literal(" (x" + duplicateCount + ")").withStyle(ChatFormatting.GRAY));
	}

	public static String normalize(Component message) {
		return WHITESPACE.matcher(message.getString().trim()).replaceAll(" ");
	}

	private PendingMessage prepareAdjacent(
		Component message,
		@Nullable GuiMessage previousLine,
		@Nullable ChatMessageMetadata previousMetadata
	) {
		String key = normalize(message);
		if (key.isEmpty() || previousLine == null) {
			return PendingMessage.empty();
		}

		String previousKey = previousMetadata != null
			? normalize(previousMetadata.baseMessage())
			: normalize(ChatMessageDecorations.stripAll(previousLine.content()));
		if (!key.equals(previousKey)) {
			return PendingMessage.empty();
		}

		int previousCount = previousMetadata != null
			? previousMetadata.duplicateCount()
			: ChatMessageDecorations.extractDuplicateCount(previousLine.content());

		long nowMillis = System.currentTimeMillis();
		return new PendingMessage(key, nowMillis, nowMillis, previousCount + 1, List.of(previousLine));
	}

	private PendingMessage prepareTimeWindow(Component message, long nowMillis, int windowSeconds) {
		long windowMillis = windowSeconds * 1000L;
		prune(nowMillis, windowMillis);

		String key = normalize(message);
		if (key.isEmpty()) {
			return PendingMessage.empty();
		}

		List<Entry> matches = new ArrayList<>();
		for (Entry entry : entries) {
			if (entry.key.equals(key) && nowMillis - entry.latestMillis <= windowMillis) {
				matches.add(entry);
			}
		}

		if (matches.isEmpty()) {
			return new PendingMessage(key, nowMillis, nowMillis, 1, List.of());
		}

		entries.removeAll(matches);

		int count = 1;
		long firstMillis = nowMillis;
		List<GuiMessage> previousLines = new ArrayList<>();
		for (Entry match : matches) {
			count += match.duplicateCount;
			firstMillis = Math.min(firstMillis, match.firstMillis);
			previousLines.add(match.displayedLine);
		}

		return new PendingMessage(key, firstMillis, nowMillis, count, List.copyOf(previousLines));
	}

	private void prune(long nowMillis, long windowMillis) {
		entries.removeIf(entry -> nowMillis - entry.latestMillis > windowMillis);
	}

	private record Entry(String key, long firstMillis, long latestMillis, int duplicateCount, GuiMessage displayedLine) {
	}

	public record PendingMessage(String key, long firstMillis, long latestMillis, int duplicateCount, List<GuiMessage> previousLines) {
		private static PendingMessage empty() {
			return new PendingMessage("", 0L, 0L, 1, List.of());
		}

		public boolean hasDuplicates() {
			return !previousLines.isEmpty();
		}
	}
}
