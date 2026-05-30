package net.emutils.client.emutils.screenshot;

import java.io.File;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;

public final class ScreenshotAutoCopy {
	private ScreenshotAutoCopy() {
	}

	public static void tryCopy(File screenshot) {
		if (!EMUtilsClient.config().screenshotAutoCopy()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}

		client.execute(() -> {
			ScreenshotActions.copyWithFeedback(client, screenshot);
		});
	}
}
