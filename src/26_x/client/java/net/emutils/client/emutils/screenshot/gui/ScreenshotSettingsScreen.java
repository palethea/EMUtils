package net.emutils.client.emutils.screenshot.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emhelpers.client.gui.widget.IntConfigSlider;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class ScreenshotSettingsScreen extends EMUtilsScreen {
	public ScreenshotSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_SCREENSHOT_HELPER));
	}

	@Override
	protected void initBody() {
		GridLayout.RowHelper adder = initTwoColumnBody();
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_HELPER,
			() -> EMUtilsClient.config().screenshotHelper(),
			EMUtilsClient.config()::setScreenshotHelper
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_AUTO_COPY,
			() -> EMUtilsClient.config().screenshotAutoCopy(),
			EMUtilsClient.config()::setScreenshotAutoCopy
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_METADATA,
			() -> EMUtilsClient.config().screenshotMetadataSaver(),
			EMUtilsClient.config()::setScreenshotMetadataSaver
		));
		adder.addChild(sortButton());
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SCREENSHOT_DELETE_CONFIRMATION,
			() -> EMUtilsClient.config().screenshotGalleryDeleteConfirmation(),
			EMUtilsClient.config()::setScreenshotGalleryDeleteConfirmation
		));
		adder.addChild(new IntConfigSlider(
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
		adder.addChild(fullWidthSettingsButton(
			Component.translatable(EMUtilsTexts.OPTION_SCREENSHOT_GALLERY),
			button -> client.setScreenAndShow(new ScreenshotGalleryScreen(this))
		), SETTINGS_COLUMNS);
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetScreenshotDefaults();
			client.setScreenAndShow(new ScreenshotSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private Button sortButton() {
		return Button.builder(sortMessage(), button -> {
			EMUtilsClient.config().setScreenshotGallerySort(EMUtilsClient.config().screenshotGallerySort().next());
			button.setMessage(sortMessage());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private static Component sortMessage() {
		return Component.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Component.translatable(EMUtilsTexts.OPTION_SCREENSHOT_SORT),
			Component.translatable(EMUtilsClient.config().screenshotGallerySort().labelKey())
		);
	}
}
