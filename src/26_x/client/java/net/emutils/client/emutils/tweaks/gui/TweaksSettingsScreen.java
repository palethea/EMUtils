package net.emutils.client.emutils.tweaks.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emhelpers.client.gui.widget.IntConfigSlider;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class TweaksSettingsScreen extends EMUtilsScreen {
	public TweaksSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_TWEAKS));
	}

	@Override
	protected void initBody() {
		GridLayout.RowHelper adder = initTwoColumnBody();
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT,
			() -> EMUtilsClient.config().tweakFullbright(),
			EMUtilsClient.config()::setTweakFullbright
		));
		adder.addChild(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			Component.translatable(EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT_STRENGTH),
			Component.translatable(EMUtilsTexts.SUFFIX_PERCENT),
			EMUtilsConfig.FULLBRIGHT_STRENGTH_MIN,
			EMUtilsConfig.FULLBRIGHT_STRENGTH_MAX,
			() -> EMUtilsClient.config().tweakFullbrightStrength(),
			EMUtilsClient.config()::setTweakFullbrightStrength
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER,
			() -> EMUtilsClient.config().tweakClearWeather(),
			EMUtilsClient.config()::setTweakClearWeather
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER_HIDE_RAIN,
			() -> EMUtilsClient.config().tweakClearWeatherHideRain(),
			EMUtilsClient.config()::setTweakClearWeatherHideRain
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER_HIDE_SNOW,
			() -> EMUtilsClient.config().tweakClearWeatherHideSnow(),
			EMUtilsClient.config()::setTweakClearWeatherHideSnow
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER_HIDE_RAIN_EFFECTS,
			() -> EMUtilsClient.config().tweakClearWeatherHideRainEffects(),
			EMUtilsClient.config()::setTweakClearWeatherHideRainEffects
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_NO_FOG,
			() -> EMUtilsClient.config().tweakNoFog(),
			EMUtilsClient.config()::setTweakNoFog
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_CLEAR_UNDERWATER,
			() -> EMUtilsClient.config().tweakClearUnderwater(),
			EMUtilsClient.config()::setTweakClearUnderwater
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_CLEAR_LAVA,
			() -> EMUtilsClient.config().tweakClearLava(),
			EMUtilsClient.config()::setTweakClearLava
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_NO_FIRE_OVERLAY,
			() -> EMUtilsClient.config().tweakNoFireOverlay(),
			EMUtilsClient.config()::setTweakNoFireOverlay
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_LOW_FIRE_OVERLAY,
			() -> EMUtilsClient.config().tweakLowFireOverlay(),
			EMUtilsClient.config()::setTweakLowFireOverlay
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_NO_NAUSEA,
			() -> EMUtilsClient.config().tweakNoNausea(),
			EMUtilsClient.config()::setTweakNoNausea
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_NO_SPYGLASS_OVERLAY,
			() -> EMUtilsClient.config().tweakNoSpyglassOverlay(),
			EMUtilsClient.config()::setTweakNoSpyglassOverlay
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_FAST_PLACE,
			() -> EMUtilsClient.config().tweakFastPlace(),
			EMUtilsClient.config()::setTweakFastPlace
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_NO_ENVIRONMENT_FOG,
			() -> EMUtilsClient.config().tweakNoEnvironmentFog(),
			EMUtilsClient.config()::setTweakNoEnvironmentFog
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_NO_HURT_CAM,
			() -> EMUtilsClient.config().tweakNoHurtCam(),
			EMUtilsClient.config()::setTweakNoHurtCam
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_FREELOOK,
			() -> EMUtilsClient.config().tweakFreelook(),
			EMUtilsClient.config()::setTweakFreelook
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_OWN_NAMETAG,
			() -> EMUtilsClient.config().tweakOwnNametag(),
			EMUtilsClient.config()::setTweakOwnNametag
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_SHULKER_TOOLTIP_PREVIEW,
			() -> EMUtilsClient.config().tweakShulkerTooltipPreview(),
			EMUtilsClient.config()::setTweakShulkerTooltipPreview
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_BUNDLE_TOOLTIP_PREVIEW,
			() -> EMUtilsClient.config().tweakBundleTooltipPreview(),
			EMUtilsClient.config()::setTweakBundleTooltipPreview
		));
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetTweaksDefaults();
			client.setScreenAndShow(new TweaksSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
