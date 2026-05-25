package net.emutils.client.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public final class ChatMentionHighlighter {
	private static final int DEFAULT_MENTION_COLOR = 0x7289DA;

	private ChatMentionHighlighter() {
	}

	public static Text highlight(Text message, String username) {
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

		MutableText result = Text.empty();
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

	private static void appendRun(MutableText result, StringBuilder run, Style style, boolean highlight) {
		Style appliedStyle = highlight
			? Style.EMPTY.withColor(mentionColor()).withBold(true)
			: style;
		result.append(Text.literal(run.toString()).setStyle(appliedStyle));
		run.setLength(0);
	}

	private static TextColor mentionColor() {
		EMUtilsConfig config = EMUtilsClient.config();
		int rgb = config == null ? DEFAULT_MENTION_COLOR : config.chatMentionHighlightColor() & 0xFFFFFF;
		return TextColor.fromRgb(rgb);
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
