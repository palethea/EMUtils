package net.emutils.client.emutils.food.gui;

import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class FoodHudSettingsScreen extends EMUtilsScreen {
	public FoodHudSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_FOOD_HUD));
	}

	@Override
	protected void initBody() {
		GridLayout.RowHelper adder = initTwoColumnBody();
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD,
			() -> EMUtilsClient.config().foodHud(),
			EMUtilsClient.config()::setFoodHud
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_SATURATION_OVERLAY,
			() -> EMUtilsClient.config().foodHudSaturationOverlay(),
			EMUtilsClient.config()::setFoodHudSaturationOverlay
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_HELD_FOOD_OVERLAY,
			() -> EMUtilsClient.config().foodHudHeldFoodOverlay(),
			EMUtilsClient.config()::setFoodHudHeldFoodOverlay
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_OFFHAND_OVERLAY,
			() -> EMUtilsClient.config().foodHudOffhandOverlay(),
			EMUtilsClient.config()::setFoodHudOffhandOverlay
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_EXHAUSTION_UNDERLAY,
			() -> EMUtilsClient.config().foodHudExhaustionUnderlay(),
			EMUtilsClient.config()::setFoodHudExhaustionUnderlay
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_TOOLTIPS,
			() -> EMUtilsClient.config().foodHudTooltips(),
			EMUtilsClient.config()::setFoodHudTooltips
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_TOOLTIP_ALWAYS,
			() -> EMUtilsClient.config().foodHudTooltipAlways(),
			EMUtilsClient.config()::setFoodHudTooltipAlways
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_FOOD_HUD_VANILLA_ANIMATIONS,
			() -> EMUtilsClient.config().foodHudVanillaAnimations(),
			EMUtilsClient.config()::setFoodHudVanillaAnimations
		));
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetFoodHudDefaults();
			client.setScreenAndShow(new FoodHudSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
