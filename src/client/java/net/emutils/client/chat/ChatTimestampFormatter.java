package net.emutils.client.chat;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatTimestampFormatter {
	private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
	private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h:mm:ssa", Locale.ENGLISH);

	private ChatTimestampFormatter() {
	}

	public static Text prependTimestamp(Text message, boolean twentyFourHour) {
		return prependTimestamp(message, twentyFourHour, System.currentTimeMillis());
	}

	public static Text prependTimestamp(Text message, boolean twentyFourHour, long receivedAtMillis) {
		DateTimeFormatter formatter = twentyFourHour ? TWENTY_FOUR_HOUR_FORMAT : TWELVE_HOUR_FORMAT;
		LocalTime time = Instant.ofEpochMilli(receivedAtMillis).atZone(ZoneId.systemDefault()).toLocalTime();
		return Text.empty()
			.append(Text.literal("[" + time.format(formatter) + "] ").formatted(Formatting.GRAY))
			.append(message.copy());
	}
}
