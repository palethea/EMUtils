package net.emutils.client.gui.zoom;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.gui.widget.IntConfigSlider;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class ZoomSettingsScreen extends EMUtilsScreen {
	public ZoomSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_ZOOM));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM,
			() -> EMUtilsClient.config().zoomEnabled(),
			EMUtilsClient.config()::setZoomEnabled
		));
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			Text.translatable(EMUtilsTexts.OPTION_ZOOM_AMOUNT),
			Text.literal("x"),
			EMUtilsConfig.ZOOM_AMOUNT_MIN,
			EMUtilsConfig.ZOOM_AMOUNT_MAX,
			() -> EMUtilsClient.config().zoomAmount(),
			EMUtilsClient.config()::setZoomAmount
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM_SMOOTH_TRANSITION,
			() -> EMUtilsClient.config().zoomSmoothTransition(),
			EMUtilsClient.config()::setZoomSmoothTransition
		));
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			Text.translatable(EMUtilsTexts.OPTION_ZOOM_TRANSITION_SPEED),
			Text.literal("x/s"),
			EMUtilsConfig.ZOOM_TRANSITION_SPEED_MIN,
			EMUtilsConfig.ZOOM_TRANSITION_SPEED_MAX,
			() -> EMUtilsClient.config().zoomTransitionSpeed(),
			EMUtilsClient.config()::setZoomTransitionSpeed
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM_CINEMATIC_CAMERA,
			() -> EMUtilsClient.config().zoomCinematicCamera(),
			EMUtilsClient.config()::setZoomCinematicCamera
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM_HIDE_HAND,
			() -> EMUtilsClient.config().zoomHideHand(),
			EMUtilsClient.config()::setZoomHideHand
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_ZOOM_HIDE_HUD,
			() -> EMUtilsClient.config().zoomHideHud(),
			EMUtilsClient.config()::setZoomHideHud
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetZoomDefaults();
			client.setScreen(new ZoomSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
