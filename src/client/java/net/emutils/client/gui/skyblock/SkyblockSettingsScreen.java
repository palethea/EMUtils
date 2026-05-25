package net.emutils.client.gui.skyblock;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.gui.widget.IntConfigSlider;
import net.emutils.client.hud.HudOverlayAnchor;
import net.emutils.client.util.EMUtilsTexts;
import net.emutils.client.config.EMUtilsConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class SkyblockSettingsScreen extends EMUtilsScreen {
	public SkyblockSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_SKYBLOCK));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK,
			() -> EMUtilsClient.config().skyblockEnabled(),
			EMUtilsClient.config()::setSkyblockEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_STORAGE_PREVIEW,
			() -> EMUtilsClient.config().storagePreviewEnabled(),
			EMUtilsClient.config()::setStoragePreviewEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_BAZAAR_TOOLTIPS,
			() -> EMUtilsClient.config().bazaarTooltipsEnabled(),
			EMUtilsClient.config()::setBazaarTooltipsEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_AUCTION_TOOLTIPS,
			() -> EMUtilsClient.config().auctionTooltipsEnabled(),
			EMUtilsClient.config()::setAuctionTooltipsEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_NPC_SELL_PRICE_TOOLTIPS,
			() -> EMUtilsClient.config().npcSellPriceTooltipsEnabled(),
			EMUtilsClient.config()::setNpcSellPriceTooltipsEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK_STATS_HUD,
			() -> EMUtilsClient.config().skyblockStatsHudEnabled(),
			EMUtilsClient.config()::setSkyblockStatsHudEnabled
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK_STATS_HIDE_ACTION_BAR,
			() -> EMUtilsClient.config().skyblockStatsHideActionBar(),
			EMUtilsClient.config()::setSkyblockStatsHideActionBar
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK_HIDE_VANILLA_STATUS,
			() -> EMUtilsClient.config().skyblockHideVanillaStatusBars(),
			EMUtilsClient.config()::setSkyblockHideVanillaStatusBars
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK_HIDE_ACTION_BAR,
			() -> EMUtilsClient.config().skyblockHideActionBarMessages(),
			EMUtilsClient.config()::setSkyblockHideActionBarMessages
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK_STATS_HEALTH,
			() -> EMUtilsClient.config().skyblockStatsShowHealth(),
			EMUtilsClient.config()::setSkyblockStatsShowHealth
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK_STATS_DEFENSE,
			() -> EMUtilsClient.config().skyblockStatsShowDefense(),
			EMUtilsClient.config()::setSkyblockStatsShowDefense
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK_STATS_MANA,
			() -> EMUtilsClient.config().skyblockStatsShowMana(),
			EMUtilsClient.config()::setSkyblockStatsShowMana
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SKYBLOCK_STATS_SOULFLOW,
			() -> EMUtilsClient.config().skyblockStatsShowSoulflow(),
			EMUtilsClient.config()::setSkyblockStatsShowSoulflow
		));
		adder.add(ButtonWidget.builder(
			Text.translatable(
				EMUtilsTexts.OPTION_SKYBLOCK_STATS_POSITION,
				Text.translatable(EMUtilsClient.config().skyblockStatsHudAnchor().labelKey())
			),
			button -> {
				HudOverlayAnchor next = EMUtilsClient.config().skyblockStatsHudAnchor().next();
				EMUtilsClient.config().setSkyblockStatsHudAnchor(next);
				client.setScreen(new SkyblockSettingsScreen(parent));
			}
		).build());
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_SKYBLOCK_STATS_BACKGROUND_OPACITY,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.HUD_BACKGROUND_OPACITY_MIN,
			EMUtilsConfig.HUD_BACKGROUND_OPACITY_MAX,
			() -> EMUtilsClient.config().skyblockStatsHudBackgroundOpacity(),
			EMUtilsClient.config()::setSkyblockStatsHudBackgroundOpacity
		));
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_SKYBLOCK_STATS_SCALE,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.HUD_SCALE_MIN,
			EMUtilsConfig.HUD_SCALE_MAX,
			() -> EMUtilsClient.config().skyblockStatsHudScale(),
			EMUtilsClient.config()::setSkyblockStatsHudScale
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetSkyblockDefaults();
			client.setScreen(new SkyblockSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
