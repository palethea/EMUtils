package net.emutils.client.emutils.zoom.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emhelpers.client.gui.widget.IntConfigSlider;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class ZoomSettingsScreen extends EMUtilsScreen {
	public ZoomSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_ZOOM));
	}

	@Override
	protected void initBody() {
		GridLayout.RowHelper adder = initTwoColumnBody();
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM,
			() -> EMUtilsClient.config().zoomEnabled(),
			EMUtilsClient.config()::setZoomEnabled
		));
		adder.addChild(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			Component.translatable(EMUtilsTexts.OPTION_ZOOM_AMOUNT),
			Component.literal("x"),
			EMUtilsConfig.ZOOM_AMOUNT_MIN,
			EMUtilsConfig.ZOOM_AMOUNT_MAX,
			() -> EMUtilsClient.config().zoomAmount(),
			EMUtilsClient.config()::setZoomAmount
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM_SMOOTH_TRANSITION,
			() -> EMUtilsClient.config().zoomSmoothTransition(),
			EMUtilsClient.config()::setZoomSmoothTransition
		));
		adder.addChild(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			Component.translatable(EMUtilsTexts.OPTION_ZOOM_TRANSITION_SPEED),
			Component.literal("x/s"),
			EMUtilsConfig.ZOOM_TRANSITION_SPEED_MIN,
			EMUtilsConfig.ZOOM_TRANSITION_SPEED_MAX,
			() -> EMUtilsClient.config().zoomTransitionSpeed(),
			EMUtilsClient.config()::setZoomTransitionSpeed
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM_CINEMATIC_CAMERA,
			() -> EMUtilsClient.config().zoomCinematicCamera(),
			EMUtilsClient.config()::setZoomCinematicCamera
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM_HIDE_HAND,
			() -> EMUtilsClient.config().zoomHideHand(),
			EMUtilsClient.config()::setZoomHideHand
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM_HIDE_HUD,
			() -> EMUtilsClient.config().zoomHideHud(),
			EMUtilsClient.config()::setZoomHideHud
		));
		adder.addChild(new IntConfigSlider(
			0, 0, SETTINGS_BUTTON_WIDTH, 20,
			Component.translatable(EMUtilsTexts.OPTION_ZOOM_OUT_SPEED),
			Component.translatable("emutils.suffix.zoom_out_speed"),
			EMUtilsConfig.ZOOM_OUT_SPEED_MIN,
			EMUtilsConfig.ZOOM_OUT_SPEED_MAX,
			() -> EMUtilsClient.config().zoomOutSpeedMultiplierRaw(),
			EMUtilsClient.config()::setZoomOutSpeedMultiplier
		));
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetZoomDefaults();
			client.setScreenAndShow(new ZoomSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
