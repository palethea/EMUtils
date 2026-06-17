package net.emutils.client.emutils.waypoint.gui;

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

public final class WaypointSettingsScreen extends EMUtilsScreen {
	public WaypointSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_WAYPOINTS));
	}

	@Override
	protected void initBody() {
		GridLayout.RowHelper adder = initTwoColumnBody();
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_WAYPOINTS,
			() -> EMUtilsClient.config().waypointEnabled(),
			EMUtilsClient.config()::setWaypointEnabled
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_WAYPOINT_AUTO_COPY,
			() -> EMUtilsClient.config().waypointAutoCopyCoords(),
			EMUtilsClient.config()::setWaypointAutoCopyCoords
		));
		adder.addChild(formatButton());
		adder.addChild(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_WAYPOINT_OPACITY,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.DEATH_WAYPOINT_OPACITY_MIN,
			EMUtilsConfig.DEATH_WAYPOINT_OPACITY_MAX,
			() -> EMUtilsClient.config().waypointOpacity(),
			EMUtilsClient.config()::setWaypointOpacity
		));
		adder.addChild(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_WAYPOINT_SIZE,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.DEATH_WAYPOINT_SIZE_MIN,
			EMUtilsConfig.DEATH_WAYPOINT_SIZE_MAX,
			() -> EMUtilsClient.config().waypointSize(),
			EMUtilsClient.config()::setWaypointSize
		));
		adder.addChild(fullWidthSettingsButton(
			Component.translatable(EMUtilsTexts.OPTION_CURRENT_WAYPOINTS),
			button -> minecraft.setScreenAndShow(new WaypointListScreen(this))
		), SETTINGS_COLUMNS);
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetDeathWaypointDefaults();
			minecraft.setScreenAndShow(new WaypointSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private Button formatButton() {
		return Button.builder(formatMessage(), button -> {
			EMUtilsClient.config().setWaypointCoordinateFormat(EMUtilsClient.config().waypointCoordinateFormat().next());
			button.setMessage(formatMessage());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private static Component formatMessage() {
		return Component.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Component.translatable(EMUtilsTexts.OPTION_WAYPOINT_COORD_FORMAT),
			Component.translatable(EMUtilsClient.config().waypointCoordinateFormat().labelKey())
		);
	}
}
