package net.emutils.client.emutils.food.gui;

import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class FoodHudSettingsScreen extends EMUtilsScreen {
	public FoodHudSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_FOOD_HUD));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD,
			() -> EMUtilsClient.config().foodHud(),
			EMUtilsClient.config()::setFoodHud
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_SATURATION_OVERLAY,
			() -> EMUtilsClient.config().foodHudSaturationOverlay(),
			EMUtilsClient.config()::setFoodHudSaturationOverlay
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_HELD_FOOD_OVERLAY,
			() -> EMUtilsClient.config().foodHudHeldFoodOverlay(),
			EMUtilsClient.config()::setFoodHudHeldFoodOverlay
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_OFFHAND_OVERLAY,
			() -> EMUtilsClient.config().foodHudOffhandOverlay(),
			EMUtilsClient.config()::setFoodHudOffhandOverlay
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_EXHAUSTION_UNDERLAY,
			() -> EMUtilsClient.config().foodHudExhaustionUnderlay(),
			EMUtilsClient.config()::setFoodHudExhaustionUnderlay
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_TOOLTIPS,
			() -> EMUtilsClient.config().foodHudTooltips(),
			EMUtilsClient.config()::setFoodHudTooltips
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_TOOLTIP_ALWAYS,
			() -> EMUtilsClient.config().foodHudTooltipAlways(),
			EMUtilsClient.config()::setFoodHudTooltipAlways
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_VANILLA_ANIMATIONS,
			() -> EMUtilsClient.config().foodHudVanillaAnimations(),
			EMUtilsClient.config()::setFoodHudVanillaAnimations
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetFoodHudDefaults();
			client.setScreen(new FoodHudSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
