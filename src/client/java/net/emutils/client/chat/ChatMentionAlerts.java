package net.emutils.client.chat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public final class ChatMentionAlerts {
	private static final long MESSAGE_DEBOUNCE_MILLIS = 7_000L;
	private static final long GLOBAL_DEBOUNCE_MILLIS = 2_000L;
	private static final int OWN_MESSAGE_PREFIX_LIMIT = 64;
	private static final int TOAST_PREVIEW_LIMIT = 80;
	private static final Map<String, Long> LAST_ALERT_BY_MESSAGE = new HashMap<>();
	private static long lastAlertMillis;

	private ChatMentionAlerts() {
	}

	public static void handle(MinecraftClient client, Text message) {
		if (
			EMUtilsClient.config() == null
				|| !EMUtilsClient.config().chatMentionAlerts()
				|| client == null
				|| EMUtilsChatMessages.isInternal(message)
		) {
			return;
		}

		String username = client.getSession().getUsername();
		if (username == null || username.isBlank()) {
			return;
		}

		String content = message.getString().trim();
		if (content.isEmpty() || isLikelyOwnMessage(content, username) || !mentionsUser(content, username)) {
			return;
		}

		long nowMillis = System.currentTimeMillis();
		String normalized = SmartChatFilter.normalize(ChatMessageDecorations.stripAll(message)).toLowerCase(Locale.ROOT);
		LAST_ALERT_BY_MESSAGE.entrySet().removeIf(entry -> nowMillis - entry.getValue() > MESSAGE_DEBOUNCE_MILLIS);
		Long previousAlertMillis = LAST_ALERT_BY_MESSAGE.get(normalized);
		if (
			previousAlertMillis != null && nowMillis - previousAlertMillis < MESSAGE_DEBOUNCE_MILLIS
				|| nowMillis - lastAlertMillis < GLOBAL_DEBOUNCE_MILLIS
		) {
			return;
		}

		LAST_ALERT_BY_MESSAGE.put(normalized, nowMillis);
		lastAlertMillis = nowMillis;
		client.execute(() -> notify(client, content));
	}

	private static boolean mentionsUser(String content, String username) {
		String quotedUsername = Pattern.quote(username);
		if (Pattern.compile("(?i)(?<![A-Za-z0-9_])@" + quotedUsername + "(?![A-Za-z0-9_])").matcher(content).find()) {
			return true;
		}

		if (!looksLikeChatLine(content)) {
			return false;
		}

		return Pattern.compile("(?i)(?<![A-Za-z0-9_])" + quotedUsername + "(?![A-Za-z0-9_])").matcher(content).find();
	}

	private static boolean isLikelyOwnMessage(String content, String username) {
		String lowerContent = content.toLowerCase(Locale.ROOT);
		String lowerUsername = username.toLowerCase(Locale.ROOT);
		if (lowerContent.startsWith("<" + lowerUsername + ">") || lowerContent.startsWith(lowerUsername + ":")) {
			return true;
		}

		int separator = firstChatSeparator(content);
		if (separator < 0 || separator > OWN_MESSAGE_PREFIX_LIMIT) {
			return false;
		}

		return content.substring(0, separator).toLowerCase(Locale.ROOT).contains(lowerUsername);
	}

	private static boolean looksLikeChatLine(String content) {
		int separator = firstChatSeparator(content);
		return separator >= 0 && separator <= OWN_MESSAGE_PREFIX_LIMIT;
	}

	private static int firstChatSeparator(String content) {
		int separator = -1;
		for (char candidate : new char[] {':', '»', '>'}) {
			int index = content.indexOf(candidate);
			if (index >= 0 && (separator < 0 || index < separator)) {
				separator = index;
			}
		}

		return separator;
	}

	private static void notify(MinecraftClient client, String content) {
		client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.2F));
		String preview = content.length() > TOAST_PREVIEW_LIMIT ? content.substring(0, TOAST_PREVIEW_LIMIT - 3) + "..." : content;
		SystemToast.show(
			client.getToastManager(),
			SystemToast.Type.PERIODIC_NOTIFICATION,
			Text.translatable(EMUtilsTexts.CHAT_MENTION_TOAST_TITLE),
			Text.translatable(EMUtilsTexts.CHAT_MENTION_TOAST_DESCRIPTION, preview)
		);
	}
}
