package net.emutils.client.emutils.tweaks.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emutils.client.emhelpers.gui.widget.ConfigToggleButton;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class TweaksSettingsScreen extends EMUtilsScreen {
	public TweaksSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_TWEAKS));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT,
			() -> EMUtilsClient.config().tweakFullbright(),
			EMUtilsClient.config()::setTweakFullbright
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER,
			() -> EMUtilsClient.config().tweakClearWeather(),
			EMUtilsClient.config()::setTweakClearWeather
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_NO_FOG,
			() -> EMUtilsClient.config().tweakNoFog(),
			EMUtilsClient.config()::setTweakNoFog
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_CLEAR_UNDERWATER,
			() -> EMUtilsClient.config().tweakClearUnderwater(),
			EMUtilsClient.config()::setTweakClearUnderwater
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_CLEAR_LAVA,
			() -> EMUtilsClient.config().tweakClearLava(),
			EMUtilsClient.config()::setTweakClearLava
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_NO_ENVIRONMENT_FOG,
			() -> EMUtilsClient.config().tweakNoEnvironmentFog(),
			EMUtilsClient.config()::setTweakNoEnvironmentFog
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_NO_HURT_CAM,
			() -> EMUtilsClient.config().tweakNoHurtCam(),
			EMUtilsClient.config()::setTweakNoHurtCam
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_FREELOOK,
			() -> EMUtilsClient.config().tweakFreelook(),
			EMUtilsClient.config()::setTweakFreelook
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_OWN_NAMETAG,
			() -> EMUtilsClient.config().tweakOwnNametag(),
			EMUtilsClient.config()::setTweakOwnNametag
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_SHULKER_TOOLTIP_PREVIEW,
			() -> EMUtilsClient.config().tweakShulkerTooltipPreview(),
			EMUtilsClient.config()::setTweakShulkerTooltipPreview
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_TWEAK_BUNDLE_TOOLTIP_PREVIEW,
			() -> EMUtilsClient.config().tweakBundleTooltipPreview(),
			EMUtilsClient.config()::setTweakBundleTooltipPreview
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetTweaksDefaults();
			client.setScreen(new TweaksSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
