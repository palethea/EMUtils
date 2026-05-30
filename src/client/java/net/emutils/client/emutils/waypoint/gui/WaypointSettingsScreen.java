package net.emutils.client.emutils.waypoint.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emutils.client.emhelpers.gui.widget.ConfigToggleButton;
import net.emutils.client.emhelpers.gui.widget.IntConfigSlider;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class WaypointSettingsScreen extends EMUtilsScreen {
	public WaypointSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_WAYPOINTS));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_WAYPOINTS,
			() -> EMUtilsClient.config().waypointEnabled(),
			EMUtilsClient.config()::setWaypointEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_WAYPOINT_AUTO_COPY,
			() -> EMUtilsClient.config().waypointAutoCopyCoords(),
			EMUtilsClient.config()::setWaypointAutoCopyCoords
		));
		adder.add(formatButton());
		adder.add(new IntConfigSlider(
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
		adder.add(new IntConfigSlider(
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
		adder.add(fullWidthSettingsButton(
			Text.translatable(EMUtilsTexts.OPTION_CURRENT_WAYPOINTS),
			button -> client.setScreen(new WaypointListScreen(this))
		), SETTINGS_COLUMNS);
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetDeathWaypointDefaults();
			client.setScreen(new WaypointSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private ButtonWidget formatButton() {
		return ButtonWidget.builder(formatMessage(), button -> {
			EMUtilsClient.config().setWaypointCoordinateFormat(EMUtilsClient.config().waypointCoordinateFormat().next());
			button.setMessage(formatMessage());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private static Text formatMessage() {
		return Text.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Text.translatable(EMUtilsTexts.OPTION_WAYPOINT_COORD_FORMAT),
			Text.translatable(EMUtilsClient.config().waypointCoordinateFormat().labelKey())
		);
	}
}
