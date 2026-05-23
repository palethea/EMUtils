package net.emutils.client.gui.death;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.gui.widget.IntConfigSlider;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;

public final class DeathWaypointSettingsScreen extends EMUtilsScreen {
	public DeathWaypointSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_DEATH_WAYPOINTS));
	}

	@Override
	protected void initBody() {
		DirectionalLayoutWidget body = createVerticalBody();
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_DEATH_WAYPOINT,
			() -> EMUtilsClient.config().deathWaypoint(),
			EMUtilsClient.config()::setDeathWaypoint
		));
		body.add(new IntConfigSlider(
			0,
			0,
			200,
			20,
			EMUtilsTexts.OPTION_WAYPOINT_OPACITY,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.DEATH_WAYPOINT_OPACITY_MIN,
			EMUtilsConfig.DEATH_WAYPOINT_OPACITY_MAX,
			() -> EMUtilsClient.config().deathWaypointOpacity(),
			EMUtilsClient.config()::setDeathWaypointOpacity
		));
		body.add(new IntConfigSlider(
			0,
			0,
			200,
			20,
			EMUtilsTexts.OPTION_WAYPOINT_SIZE,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.DEATH_WAYPOINT_SIZE_MIN,
			EMUtilsConfig.DEATH_WAYPOINT_SIZE_MAX,
			() -> EMUtilsClient.config().deathWaypointSize(),
			EMUtilsClient.config()::setDeathWaypointSize
		));
		body.add(ButtonWidget.builder(
			Text.translatable(EMUtilsTexts.OPTION_CURRENT_WAYPOINTS),
			button -> client.setScreen(new DeathWaypointListScreen(this))
		).width(200).build());
		layout.addBody(body);
	}
}
