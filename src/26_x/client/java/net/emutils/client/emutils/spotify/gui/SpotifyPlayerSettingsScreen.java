package net.emutils.client.emutils.spotify.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class SpotifyPlayerSettingsScreen extends EMUtilsScreen {
	public SpotifyPlayerSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_SPOTIFY_PLAYER));
	}

	@Override
	protected void initBody() {
		GridLayout grid = new GridLayout();
		grid.spacing(4);
		grid.defaultCellSetting().alignHorizontallyCenter();
		GridLayout.RowHelper adder = grid.createRowHelper(SETTINGS_COLUMNS);
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SPOTIFY_PLAYER,
			() -> EMUtilsClient.config().spotifyPlayerEnabled(),
			EMUtilsClient.config()::setSpotifyPlayerEnabled
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SPOTIFY_HUD_OVERLAY,
			() -> EMUtilsClient.config().spotifyHudOverlay(),
			EMUtilsClient.config()::setSpotifyHudOverlay
		));
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetSpotifyPlayerDefaults();
			client.setScreenAndShow(new SpotifyPlayerSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
		layout.addToContents(grid);
	}
}

