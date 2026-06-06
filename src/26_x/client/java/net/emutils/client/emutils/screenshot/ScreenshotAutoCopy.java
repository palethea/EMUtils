package net.emutils.client.emutils.screenshot;

import java.io.File;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;

public final class ScreenshotAutoCopy {
	private ScreenshotAutoCopy() {
	}

	public static void tryCopy(File screenshot) {
		if (!EMUtilsClient.config().screenshotAutoCopy()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}

		client.execute(() -> {
			ScreenshotActions.copyWithFeedback(client, screenshot);
		});
	}
}
