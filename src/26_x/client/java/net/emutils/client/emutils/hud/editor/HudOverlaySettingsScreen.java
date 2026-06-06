package net.emutils.client.emutils.hud.editor;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emhelpers.client.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class HudOverlaySettingsScreen extends EMUtilsScreen {
	public HudOverlaySettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_HUD_OVERLAY));
	}

	@Override
	protected void initBody() {
		GridLayout.RowHelper adder = initTwoColumnBody();
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_OVERLAY,
			() -> EMUtilsClient.config().hudOverlay(),
			EMUtilsClient.config()::setHudOverlay
		));
		adder.addChild(Button.builder(Component.translatable(EMUtilsTexts.OPTION_HUD_LAYOUT_EDITOR), button -> {
			if (client != null) {
				HudLayoutManager.openEditor(EMUtilsClient.MOD_ID, client);
			}
		}).width(SETTINGS_BUTTON_WIDTH).build());
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_SHOW_ICONS,
			() -> EMUtilsClient.config().hudShowIcons(),
			EMUtilsClient.config()::setHudShowIcons
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_HIDE_WITH_DEBUG,
			() -> EMUtilsClient.config().hudHideWithDebug(),
			EMUtilsClient.config()::setHudHideWithDebug
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_COORDINATES,
			() -> EMUtilsClient.config().hudShowCoordinates(),
			EMUtilsClient.config()::setHudShowCoordinates
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_CHUNK_REGION,
			() -> EMUtilsClient.config().hudShowChunkRegion(),
			EMUtilsClient.config()::setHudShowChunkRegion
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_BIOME,
			() -> EMUtilsClient.config().hudShowBiome(),
			EMUtilsClient.config()::setHudShowBiome
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_PING,
			() -> EMUtilsClient.config().hudShowPing(),
			EMUtilsClient.config()::setHudShowPing
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_FPS,
			() -> EMUtilsClient.config().hudShowFps(),
			EMUtilsClient.config()::setHudShowFps
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_FACING,
			() -> EMUtilsClient.config().hudShowFacing(),
			EMUtilsClient.config()::setHudShowFacing
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_MEMORY,
			() -> EMUtilsClient.config().hudShowMemory(),
			EMUtilsClient.config()::setHudShowMemory
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_SERVER_TIME,
			() -> EMUtilsClient.config().hudShowServerTime(),
			EMUtilsClient.config()::setHudShowServerTime
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_REAL_TIME,
			() -> EMUtilsClient.config().hudShowRealTime(),
			EMUtilsClient.config()::setHudShowRealTime
		));
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetHudDefaults();
			client.setScreen(new HudOverlaySettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
