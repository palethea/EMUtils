package net.emutils.client.emutils.chat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;

public final class ChatMentionAlerts {
	private static final long MESSAGE_DEBOUNCE_MILLIS = 7_000L;
	private static final long GLOBAL_DEBOUNCE_MILLIS = 2_000L;
	private static final int TOAST_PREVIEW_LIMIT = 80;
	private static final float ALERT_PITCH = 1.2F;
	private static final String[] SOUND_NAMES = {"Pling", "Chime", "Bell", "Harp", "Xylophone", "Experience Orb", "Amethyst"};
	private static final Map<String, Long> LAST_ALERT_BY_MESSAGE = new HashMap<>();
	private static long lastAlertMillis;

	private ChatMentionAlerts() {
	}

	public static void handle(Minecraft client, Component message) {
		if (
			EMUtilsClient.config() == null
				|| !EMUtilsClient.config().chatMentionAlerts()
				|| client == null
				|| EMUtilsChatMessages.isInternal(message)
		) {
			return;
		}

		String username = client.getUser().getName();
		if (username == null || username.isBlank()) {
			return;
		}

		String content = message.getString().trim();
		if (!ChatMentionDetector.isMention(message, username)) {
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

	private static void notify(Minecraft client, String content) {
		int soundIndex = Math.max(0, Math.min(SOUND_NAMES.length - 1, EMUtilsClient.config().chatMentionAlertSound()));
		float volume = EMUtilsClient.config().chatMentionAlertVolume() / 100.0F;
		if (volume > 0.0F) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(getSoundEvent(soundIndex), volume, ALERT_PITCH));
		}
		String preview = content.length() > TOAST_PREVIEW_LIMIT ? content.substring(0, TOAST_PREVIEW_LIMIT - 3) + "..." : content;
		SystemToast.addOrUpdate(
			client.gui.toastManager(),
			SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
			Component.translatable(EMUtilsTexts.CHAT_MENTION_TOAST_TITLE),
			Component.translatable(EMUtilsTexts.CHAT_MENTION_TOAST_DESCRIPTION, preview)
		);
	}

	private static net.minecraft.sounds.SoundEvent getSoundEvent(int index) {
		return switch (index) {
			case 1 -> SoundEvents.NOTE_BLOCK_CHIME.value();
			case 2 -> SoundEvents.NOTE_BLOCK_BELL.value();
			case 3 -> SoundEvents.NOTE_BLOCK_HARP.value();
			case 4 -> SoundEvents.NOTE_BLOCK_XYLOPHONE.value();
			case 5 -> SoundEvents.EXPERIENCE_ORB_PICKUP;
			case 6 -> SoundEvents.AMETHYST_BLOCK_CHIME;
			default -> SoundEvents.NOTE_BLOCK_PLING.value();
		};
	}

	public static String[] soundNames() {
		return SOUND_NAMES;
	}
}
