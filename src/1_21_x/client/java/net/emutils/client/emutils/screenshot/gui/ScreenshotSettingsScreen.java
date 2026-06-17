package net.emutils.client.emutils.screenshot.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emhelpers.client.gui.widget.IntConfigSlider;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class ScreenshotSettingsScreen extends EMUtilsScreen {
	public ScreenshotSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_SCREENSHOT_HELPER));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_HELPER,
			() -> EMUtilsClient.config().screenshotHelper(),
			EMUtilsClient.config()::setScreenshotHelper
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_AUTO_COPY,
			() -> EMUtilsClient.config().screenshotAutoCopy(),
			EMUtilsClient.config()::setScreenshotAutoCopy
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_METADATA,
			() -> EMUtilsClient.config().screenshotMetadataSaver(),
			EMUtilsClient.config()::setScreenshotMetadataSaver
		));
		adder.add(sortButton());
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_DELETE_CONFIRMATION,
			() -> EMUtilsClient.config().screenshotGalleryDeleteConfirmation(),
			EMUtilsClient.config()::setScreenshotGalleryDeleteConfirmation
		));
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_SCREENSHOT_MAX_COUNT,
			"",
			EMUtilsConfig.SCREENSHOT_MAX_COUNT_MIN,
			EMUtilsConfig.SCREENSHOT_MAX_COUNT_MAX,
			() -> EMUtilsClient.config().screenshotGalleryMaxCount(),
			EMUtilsClient.config()::setScreenshotGalleryMaxCount
		));
		adder.add(fullWidthSettingsButton(
			Text.translatable(EMUtilsTexts.OPTION_SCREENSHOT_GALLERY),
			button -> client.setScreen(new ScreenshotGalleryScreen(this))
		), SETTINGS_COLUMNS);
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetScreenshotDefaults();
			client.setScreen(new ScreenshotSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private ButtonWidget sortButton() {
		return ButtonWidget.builder(sortMessage(), button -> {
			EMUtilsClient.config().setScreenshotGallerySort(EMUtilsClient.config().screenshotGallerySort().next());
			button.setMessage(sortMessage());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private static Text sortMessage() {
		return Text.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Text.translatable(EMUtilsTexts.OPTION_SCREENSHOT_SORT),
			Text.translatable(EMUtilsClient.config().screenshotGallerySort().labelKey())
		);
	}
}
