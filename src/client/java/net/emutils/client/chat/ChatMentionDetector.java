package net.emutils.client.chat;

import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.text.Text;

public final class ChatMentionDetector {
	private static final int OWN_MESSAGE_PREFIX_LIMIT = 64;

	private ChatMentionDetector() {
	}

	public static boolean isMention(Text message, String username) {
		if (message == null || username == null || username.isBlank()) {
			return false;
		}

		String content = message.getString().trim();
		return !content.isEmpty()
			&& !isLikelyOwnMessage(content, username)
			&& mentionsUser(content, username);
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

	static boolean looksLikeChatLine(String content) {
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
}
