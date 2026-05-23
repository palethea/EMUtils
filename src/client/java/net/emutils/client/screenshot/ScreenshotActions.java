package net.emutils.client.screenshot;

import java.io.File;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public final class ScreenshotActions {
	private ScreenshotActions() {
	}

	public static boolean copyWithFeedback(MinecraftClient client, File screenshot) {
		boolean copied = ScreenshotClipboard.copyImage(screenshot);
		postCopyFeedback(client, copied);
		return copied;
	}

	public static void openImage(File screenshot) {
		Util.getOperatingSystem().open(screenshot);
	}

	public static void openFolder(File screenshot) {
		File parent = screenshot.getParentFile();
		if (parent != null) {
			Util.getOperatingSystem().open(parent);
		}
	}

	private static void postCopyFeedback(MinecraftClient client, boolean copied) {
		if (client == null || client.inGameHud == null) {
			return;
		}

		String key = copied ? EMUtilsTexts.CHAT_SCREENSHOT_COPY_SUCCESS : EMUtilsTexts.CHAT_SCREENSHOT_COPY_FAILURE;
		Formatting formatting = copied ? Formatting.GREEN : Formatting.RED;
		client.inGameHud.getChatHud().addMessage(Text.translatable(key).formatted(formatting), null, null);
	}
}
