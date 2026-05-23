package net.emutils.client.chat;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public final class EMUtilsChatMessages {
	private static final String INTERNAL_KEY_PREFIX = "emutils.";

	private EMUtilsChatMessages() {
	}

	public static boolean isInternal(Text message) {
		return hasInternalTranslation(message);
	}

	private static boolean hasInternalTranslation(Text text) {
		if (text.getContent() instanceof TranslatableTextContent content) {
			if (content.getKey().startsWith(INTERNAL_KEY_PREFIX)) {
				return true;
			}

			for (Object arg : content.getArgs()) {
				if (arg instanceof Text argText && hasInternalTranslation(argText)) {
					return true;
				}
			}
		}

		for (Text sibling : text.getSiblings()) {
			if (hasInternalTranslation(sibling)) {
				return true;
			}
		}

		return false;
	}
}
