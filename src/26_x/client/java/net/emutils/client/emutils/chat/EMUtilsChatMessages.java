package net.emutils.client.emutils.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class EMUtilsChatMessages {
	private static final String INTERNAL_KEY_PREFIX = "emutils.";

	private EMUtilsChatMessages() {
	}

	public static boolean isInternal(Component message) {
		return hasInternalTranslation(message);
	}

	private static boolean hasInternalTranslation(Component text) {
		if (text.getContents() instanceof TranslatableContents content) {
			if (content.getKey().startsWith(INTERNAL_KEY_PREFIX)) {
				return true;
			}

			for (Object arg : content.getArgs()) {
				if (arg instanceof Component argText && hasInternalTranslation(argText)) {
					return true;
				}
			}
		}

		for (Component sibling : text.getSiblings()) {
			if (hasInternalTranslation(sibling)) {
				return true;
			}
		}

		return false;
	}
}
