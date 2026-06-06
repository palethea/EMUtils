package net.emutils.client.emutils.chat;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class ChatTimestampFormatter {
	private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
	private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h:mm:ssa", Locale.ENGLISH);

	private ChatTimestampFormatter() {
	}

	public static Component prependTimestamp(Component message, boolean twentyFourHour) {
		return prependTimestamp(message, twentyFourHour, System.currentTimeMillis());
	}

	public static Component prependTimestamp(Component message, boolean twentyFourHour, long receivedAtMillis) {
		DateTimeFormatter formatter = twentyFourHour ? TWENTY_FOUR_HOUR_FORMAT : TWELVE_HOUR_FORMAT;
		LocalTime time = Instant.ofEpochMilli(receivedAtMillis).atZone(ZoneId.systemDefault()).toLocalTime();
		return Component.empty()
			.append(Component.literal("[" + time.format(formatter) + "] ").withStyle(ChatFormatting.GRAY))
			.append(message.copy());
	}
}
