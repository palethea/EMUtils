package net.emutils.client.chat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.text.Text;

public final class ChatMessageDecorations {
	private static final Pattern TIMESTAMP_24_PATTERN = Pattern.compile("^\\[(\\d{2}:\\d{2}:\\d{2})] ");
	private static final Pattern TIMESTAMP_12_PATTERN = Pattern.compile("^\\[(\\d{1,2}:\\d{2}:\\d{2}(?:AM|PM))] ", Pattern.CASE_INSENSITIVE);
	private static final Pattern DUPLICATE_SUFFIX_PATTERN = Pattern.compile(" \\(x(\\d+)\\)$");
	private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
	private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h:mm:ssa", Locale.ENGLISH);

	private ChatMessageDecorations() {
	}

	public static Text stripAll(Text message) {
		return Text.literal(stripAllString(message.getString()));
	}

	public static String stripAllString(String content) {
		String withoutTimestamp = stripTimestampString(content);
		return stripDuplicateSuffixString(withoutTimestamp);
	}

	public static String stripTimestampString(String content) {
		Matcher matcher = TIMESTAMP_24_PATTERN.matcher(content);
		if (matcher.find()) {
			return content.substring(matcher.end());
		}

		matcher = TIMESTAMP_12_PATTERN.matcher(content);
		if (matcher.find()) {
			return content.substring(matcher.end());
		}

		return content;
	}

	public static String stripDuplicateSuffixString(String content) {
		return DUPLICATE_SUFFIX_PATTERN.matcher(content).replaceFirst("");
	}

	public static int extractDuplicateCount(Text message) {
		return extractDuplicateCount(message.getString());
	}

	public static int extractDuplicateCount(String content) {
		Matcher matcher = DUPLICATE_SUFFIX_PATTERN.matcher(content);
		if (!matcher.find()) {
			return 1;
		}

		try {
			return Math.max(1, Integer.parseInt(matcher.group(1)));
		} catch (NumberFormatException ignored) {
			return 1;
		}
	}

	public static OptionalLong parseTimestampMillis(Text message) {
		return parseTimestampMillis(message.getString());
	}

	public static OptionalLong parseTimestampMillis(String content) {
		Matcher matcher = TIMESTAMP_24_PATTERN.matcher(content);
		if (matcher.find()) {
			return parseTime(matcher.group(1), TWENTY_FOUR_HOUR_FORMAT);
		}

		matcher = TIMESTAMP_12_PATTERN.matcher(content);
		if (matcher.find()) {
			return parseTime(matcher.group(1), TWELVE_HOUR_FORMAT);
		}

		return OptionalLong.empty();
	}

	public static ChatMessageMetadata metadataFromDisplayed(Text displayed, long fallbackReceivedAtMillis) {
		String displayedString = displayed.getString();
		long receivedAtMillis = parseTimestampMillis(displayedString).orElse(fallbackReceivedAtMillis);
		int duplicateCount = extractDuplicateCount(displayedString);
		Text baseMessage = stripAll(displayed);
		return new ChatMessageMetadata(baseMessage, duplicateCount, receivedAtMillis);
	}

	private static OptionalLong parseTime(String value, DateTimeFormatter formatter) {
		try {
			LocalTime time = LocalTime.parse(value.toUpperCase(Locale.ENGLISH), formatter);
			long millis = time.atDate(LocalDate.now())
				.atZone(ZoneId.systemDefault())
				.toInstant()
				.toEpochMilli();
			return OptionalLong.of(millis);
		} catch (RuntimeException ignored) {
			return OptionalLong.empty();
		}
	}
}
