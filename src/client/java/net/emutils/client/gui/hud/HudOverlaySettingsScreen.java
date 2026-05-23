package net.emutils.client.gui.hud;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.gui.widget.IntConfigSlider;
import net.emutils.client.hud.HudOverlayAnchor;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class HudOverlaySettingsScreen extends EMUtilsScreen {
	public HudOverlaySettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_HUD_OVERLAY));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_OVERLAY,
			() -> EMUtilsClient.config().hudOverlay(),
			EMUtilsClient.config()::setHudOverlay
		));
		adder.add(positionButton());
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_SHOW_ICONS,
			() -> EMUtilsClient.config().hudShowIcons(),
			EMUtilsClient.config()::setHudShowIcons
		));
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_HUD_BACKGROUND_OPACITY,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.HUD_BACKGROUND_OPACITY_MIN,
			EMUtilsConfig.HUD_BACKGROUND_OPACITY_MAX,
			() -> EMUtilsClient.config().hudBackgroundOpacity(),
			EMUtilsClient.config()::setHudBackgroundOpacity
		));
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_HUD_SCALE,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.HUD_SCALE_MIN,
			EMUtilsConfig.HUD_SCALE_MAX,
			() -> EMUtilsClient.config().hudScale(),
			EMUtilsClient.config()::setHudScale
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_HIDE_WITH_DEBUG,
			() -> EMUtilsClient.config().hudHideWithDebug(),
			EMUtilsClient.config()::setHudHideWithDebug
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_COORDINATES,
			() -> EMUtilsClient.config().hudShowCoordinates(),
			EMUtilsClient.config()::setHudShowCoordinates
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_CHUNK_REGION,
			() -> EMUtilsClient.config().hudShowChunkRegion(),
			EMUtilsClient.config()::setHudShowChunkRegion
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_BIOME,
			() -> EMUtilsClient.config().hudShowBiome(),
			EMUtilsClient.config()::setHudShowBiome
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_PING,
			() -> EMUtilsClient.config().hudShowPing(),
			EMUtilsClient.config()::setHudShowPing
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_FPS,
			() -> EMUtilsClient.config().hudShowFps(),
			EMUtilsClient.config()::setHudShowFps
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_FACING,
			() -> EMUtilsClient.config().hudShowFacing(),
			EMUtilsClient.config()::setHudShowFacing
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_MEMORY,
			() -> EMUtilsClient.config().hudShowMemory(),
			EMUtilsClient.config()::setHudShowMemory
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_SERVER_TIME,
			() -> EMUtilsClient.config().hudShowServerTime(),
			EMUtilsClient.config()::setHudShowServerTime
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_HUD_REAL_TIME,
			() -> EMUtilsClient.config().hudShowRealTime(),
			EMUtilsClient.config()::setHudShowRealTime
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetHudDefaults();
			client.setScreen(new HudOverlaySettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private ButtonWidget positionButton() {
		return ButtonWidget.builder(positionMessage(), button -> {
			HudOverlayAnchor next = EMUtilsClient.config().hudOverlayAnchor().next();
			EMUtilsClient.config().setHudOverlayAnchor(next);
			button.setMessage(positionMessage());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private static Text positionMessage() {
		return Text.translatable(
			EMUtilsTexts.OPTION_VALUE,
			Text.translatable(EMUtilsTexts.OPTION_HUD_POSITION),
			Text.translatable(EMUtilsClient.config().hudOverlayAnchor().labelKey())
		);
	}
}
