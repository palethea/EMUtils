package net.emutils.client.emutils.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public final class ChatMentionHighlighter {
	private static final int DEFAULT_MENTION_COLOR = 0x7289DA;

	private ChatMentionHighlighter() {
	}

	public static Component highlight(Component message, String username) {
		if (message == null || username == null || username.isBlank()) {
			return message;
		}

		String plain = message.getString();
		List<Range> ranges = mentionRanges(plain, username);
		if (ranges.isEmpty()) {
			return message;
		}

		boolean[] highlighted = new boolean[plain.length()];
		for (Range range : ranges) {
			for (int index = range.start(); index < range.end(); index++) {
				highlighted[index] = true;
			}
		}

		MutableComponent result = Component.empty();
		StringBuilder run = new StringBuilder();
		Style[] pendingStyle = new Style[] {Style.EMPTY};
		boolean[] pendingHighlight = new boolean[] {false};
		int[] charIndex = {0};

		message.visit((style, string) -> {
			for (int index = 0; index < string.length(); index++) {
				boolean highlight = highlighted[charIndex[0]++];
				if (run.length() > 0 && (!Objects.equals(style, pendingStyle[0]) || highlight != pendingHighlight[0])) {
					appendRun(result, run, pendingStyle[0], pendingHighlight[0]);
				}

				pendingStyle[0] = style;
				pendingHighlight[0] = highlight;
				run.append(string.charAt(index));
			}

			return Optional.empty();
		}, Style.EMPTY);

		if (run.length() > 0) {
			appendRun(result, run, pendingStyle[0], pendingHighlight[0]);
		}

		return result;
	}

	private static void appendRun(MutableComponent result, StringBuilder run, Style style, boolean highlight) {
		Style appliedStyle = highlight ? highlightStyle() : style;
		result.append(Component.literal(run.toString()).setStyle(appliedStyle));
		run.setLength(0);
	}

	private static Style highlightStyle() {
		EMUtilsConfig config = EMUtilsClient.config();
		int rgb = config == null ? DEFAULT_MENTION_COLOR : config.chatMentionHighlightColor() & 0xFFFFFF;
		TextColor color = TextColor.fromRgb(rgb);
		int styleOrdinal = config == null ? 0 : config.chatMentionHighlightStyle();
		return switch (styleOrdinal) {
			case 1 -> Style.EMPTY.withColor(color).withItalic(true);
			case 2 -> Style.EMPTY.withColor(color).withUnderlined(true);
			case 3 -> Style.EMPTY.withColor(color);
			default -> Style.EMPTY.withColor(color).withBold(true);
		};
	}

	static List<Range> mentionRanges(String content, String username) {
		if (content.isBlank()) {
			return List.of();
		}

		String quotedUsername = Pattern.quote(username);
		Pattern atMention = Pattern.compile("(?i)(?<![A-Za-z0-9_])@" + quotedUsername + "(?![A-Za-z0-9_])");
		Pattern plainMention = Pattern.compile("(?i)(?<![A-Za-z0-9_])" + quotedUsername + "(?![A-Za-z0-9_])");

		List<Range> ranges = new ArrayList<>();
		collectMatches(atMention, content, ranges);
		if (ChatMentionDetector.looksLikeChatLine(content)) {
			collectMatches(plainMention, content, ranges);
		}

		return mergeRanges(ranges);
	}

	private static void collectMatches(Pattern pattern, String content, List<Range> ranges) {
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			ranges.add(new Range(matcher.start(), matcher.end()));
		}
	}

	private static List<Range> mergeRanges(List<Range> ranges) {
		if (ranges.isEmpty()) {
			return List.of();
		}

		ranges.sort(Comparator.comparingInt(Range::start));
		List<Range> merged = new ArrayList<>();
		Range current = ranges.getFirst();
		for (int index = 1; index < ranges.size(); index++) {
			Range next = ranges.get(index);
			if (next.start() <= current.end()) {
				current = new Range(current.start(), Math.max(current.end(), next.end()));
			} else {
				merged.add(current);
				current = next;
			}
		}

		merged.add(current);
		return merged;
	}

	record Range(int start, int end) {
	}
}
