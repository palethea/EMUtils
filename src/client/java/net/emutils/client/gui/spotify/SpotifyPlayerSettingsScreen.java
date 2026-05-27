package net.emutils.client.gui.spotify;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.gui.widget.IntConfigSlider;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class SpotifyPlayerSettingsScreen extends EMUtilsScreen {
	public SpotifyPlayerSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_SPOTIFY_PLAYER));
	}

	@Override
	protected void initBody() {
		GridWidget grid = new GridWidget();
		grid.setSpacing(4);
		grid.getMainPositioner().alignHorizontalCenter();
		GridWidget.Adder adder = grid.createAdder(SETTINGS_COLUMNS);
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SPOTIFY_PLAYER,
			() -> EMUtilsClient.config().spotifyPlayerEnabled(),
			EMUtilsClient.config()::setSpotifyPlayerEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SPOTIFY_HUD_OVERLAY,
			() -> EMUtilsClient.config().spotifyHudOverlay(),
			EMUtilsClient.config()::setSpotifyHudOverlay
		));
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_SPOTIFY_HUD_BACKGROUND_OPACITY,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.HUD_BACKGROUND_OPACITY_MIN,
			EMUtilsConfig.HUD_BACKGROUND_OPACITY_MAX,
			() -> EMUtilsClient.config().spotifyHudBackgroundOpacity(),
			EMUtilsClient.config()::setSpotifyHudBackgroundOpacity
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetSpotifyPlayerDefaults();
			client.setScreen(new SpotifyPlayerSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
		layout.addBody(grid);
	}
}

