package net.emutils.client.gui.screenshot;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;

public final class ScreenshotSettingsScreen extends EMUtilsScreen {
	public ScreenshotSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_SCREENSHOT_HELPER));
	}

	@Override
	protected void initBody() {
		DirectionalLayoutWidget body = createVerticalBody();
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_HELPER,
			() -> EMUtilsClient.config().screenshotHelper(),
			EMUtilsClient.config()::setScreenshotHelper
		));
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_AUTO_COPY,
			() -> EMUtilsClient.config().screenshotAutoCopy(),
			EMUtilsClient.config()::setScreenshotAutoCopy
		));
		body.add(ButtonWidget.builder(
			Text.translatable(EMUtilsTexts.OPTION_SCREENSHOT_GALLERY),
			button -> client.setScreen(new ScreenshotGalleryScreen(this))
		).width(200).build());
		layout.addBody(body);
	}
}
