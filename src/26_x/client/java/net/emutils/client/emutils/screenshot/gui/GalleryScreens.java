package net.emutils.client.emutils.screenshot.gui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.screens.Screen;

/** Opens the Screenshot Gallery: the new UI when its dev-only preview is on (#106), the classic one otherwise. */
public final class GalleryScreens {
	private GalleryScreens() {
	}

	public static Screen gallery(Screen parent) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().settingsUiPreview()) {
			return new GalleryScreen(parent);
		}
		return new ScreenshotGalleryScreen(parent);
	}
}
