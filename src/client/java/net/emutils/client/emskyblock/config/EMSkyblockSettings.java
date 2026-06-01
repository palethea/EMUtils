package net.emutils.client.emskyblock.config;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.HudOverlayAnchor;
import org.jspecify.annotations.Nullable;

public final class EMSkyblockSettings {
	private EMSkyblockSettings() {
	}

	private static @Nullable EMSkyblockConfig config() {
		try {
			return EMSkyblockConfigManager.config();
		} catch (IllegalStateException ignored) {
			return null;
		}
	}

	public static boolean skyblockEnabled() {
		EMSkyblockConfig config = config();
		return config != null && config.about.enabled;
	}

	public static boolean storagePreviewEnabled() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.storagePreviewEnabled;
	}

	public static boolean priceTooltipShiftForStackTotal() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.display.shiftForStackTotal;
	}

	public static boolean priceTooltipCompactNumbers() {
		EMSkyblockConfig config = config();
		return config != null && config.tooltips.display.compactNumbers;
	}

	public static boolean bazaarTooltipsEnabled() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.bazaar.enabled;
	}

	public static boolean bazaarShowBuyOrder() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.bazaar.showBuyOrder;
	}

	public static boolean bazaarShowSellOrder() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.bazaar.showSellOrder;
	}

	public static boolean bazaarShowInstantBuy() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.bazaar.showInstantBuy;
	}

	public static boolean bazaarShowInstantSell() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.bazaar.showInstantSell;
	}

	public static boolean bazaarShowAverage24h() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.bazaar.showAverage24h;
	}

	public static boolean bazaarHideOnSoulbound() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.bazaar.hideOnSoulbound;
	}

	public static boolean auctionTooltipsEnabled() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.auction.enabled;
	}

	public static boolean auctionShowLowestBin() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.auction.showLowestBin;
	}

	public static boolean auctionShowAverage24h() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.auction.showAverage24h;
	}

	public static boolean auctionHideOnSoulbound() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.auction.hideOnSoulbound;
	}

	public static boolean npcSellPriceTooltipsEnabled() {
		EMSkyblockConfig config = config();
		return config != null && config.tooltips.npc.enabled;
	}

	public static boolean npcHideOnSoulbound() {
		EMSkyblockConfig config = config();
		return config == null || config.tooltips.npc.hideOnSoulbound;
	}

	public static boolean skyblockStatsHudEnabled() {
		EMSkyblockConfig config = config();
		return config != null && config.statsHud.enabled;
	}

	public static boolean skyblockStatsHideActionBar() {
		EMSkyblockConfig config = config();
		return config == null || config.statsHud.hideActionBar;
	}

	public static boolean skyblockStatsShowHealth() {
		EMSkyblockConfig config = config();
		return config == null || config.statsHud.visibleStats.health;
	}

	public static boolean skyblockStatsShowDefense() {
		EMSkyblockConfig config = config();
		return config == null || config.statsHud.visibleStats.defense;
	}

	public static boolean skyblockStatsShowMana() {
		EMSkyblockConfig config = config();
		return config == null || config.statsHud.visibleStats.mana;
	}

	public static boolean skyblockStatsShowSoulflow() {
		EMSkyblockConfig config = config();
		return config == null || config.statsHud.visibleStats.soulflow;
	}

	public static HudOverlayAnchor skyblockStatsHudAnchor() {
		return HudOverlayAnchor.BOTTOM_CENTER;
	}

	public static int skyblockStatsHudBackgroundOpacity() {
		EMSkyblockConfig config = config();
		return clampHudBackgroundOpacity(config == null ? 85 : config.statsHud.layout.backgroundOpacity);
	}

	public static int legacySkyblockStatsHudScale(EMUtilsConfig emConfig) {
		EMSkyblockConfig config = config();
		if (config != null) {
			return config.statsHud.layout.scale;
		}

		return emConfig.legacySkyblockStatsHudScale();
	}

	public static int legacyEstimatedItemValueHudScale(EMUtilsConfig emConfig) {
		EMSkyblockConfig config = config();
		if (config != null) {
			return config.eiv.layout.scale;
		}

		return emConfig.legacyEstimatedItemValueHudScale();
	}

	public static int legacyFishingHookHudScale(EMUtilsConfig emConfig) {
		EMSkyblockConfig config = config();
		if (config != null) {
			return config.fishing.hookDisplay.layout.scale;
		}

		return 100;
	}

	public static boolean estimatedItemValueHudEnabled() {
		EMSkyblockConfig config = config();
		return config != null && config.eiv.hudEnabled;
	}

	public static boolean estimatedItemValueHudBackground() {
		EMSkyblockConfig config = config();
		return config == null || config.eiv.hudBackground;
	}

	public static HudOverlayAnchor estimatedItemValueHudAnchor() {
		return HudOverlayAnchor.TOP_LEFT;
	}

	public static int estimatedItemValueEnchantmentsCap() {
		EMSkyblockConfig config = config();
		return clampEstimatedItemValueEnchantmentsCap(config == null ? 7 : config.eiv.enchantmentsCap);
	}

	public static boolean estimatedItemValueTooltipEnabled() {
		EMSkyblockConfig config = config();
		return config != null && config.eiv.tooltipEnabled;
	}

	public static boolean estimatedItemValueHideOnSoulbound() {
		EMSkyblockConfig config = config();
		return config == null || config.eiv.hideOnSoulbound;
	}

	public static boolean fishingHookDisplayEnabled() {
		EMSkyblockConfig config = config();
		return config != null && config.fishing.hookDisplay.enabled;
	}

	public static String fishingHookCustomAlertText() {
		EMSkyblockConfig config = config();
		String text = config == null ? null : config.fishing.hookDisplay.customAlertText;
		if (text == null || text.isBlank()) {
			return "&c&l!!!";
		}

		return text;
	}

	public static boolean fishingHookShowCountdown() {
		EMSkyblockConfig config = config();
		return config == null || config.fishing.hookDisplay.showCountdown;
	}

	public static boolean fishingHookHideArmorStand() {
		EMSkyblockConfig config = config();
		return config == null || config.fishing.hookDisplay.hideArmorStand;
	}

	public static boolean fishingHookUseCustomCountdownColor() {
		EMSkyblockConfig config = config();
		return config != null && config.fishing.hookDisplay.countdownColors.useCustomColor;
	}

	public static String fishingHookCountdownColorPrefix() {
		EMSkyblockConfig config = config();
		String prefix = config == null ? null : config.fishing.hookDisplay.countdownColors.colorPrefix;
		if (prefix == null || prefix.isBlank()) {
			return "&e&l";
		}

		return prefix;
	}

	public static boolean seaCreatureTrackerEnabled() {
		EMSkyblockConfig config = config();
		return config != null && config.fishing.seaCreatureTracker.enabled;
	}

	public static boolean seaCreatureTrackerShowPercentage() {
		EMSkyblockConfig config = config();
		return config != null && config.fishing.seaCreatureTracker.showPercentage;
	}

	public static boolean seaCreatureTrackerCountDouble() {
		EMSkyblockConfig config = config();
		return config == null || config.fishing.seaCreatureTracker.countDouble;
	}

	public static boolean seaCreatureTrackerOnlyWhileFishing() {
		EMSkyblockConfig config = config();
		return config == null || config.fishing.seaCreatureTracker.onlyWhileFishing;
	}

	public static boolean seaCreatureTrackerShowWithFishingArmor() {
		EMSkyblockConfig config = config();
		return config != null && config.fishing.seaCreatureTracker.showWithFishingArmor;
	}

	public static boolean seaCreatureTrackerShowOnFishingIslands() {
		EMSkyblockConfig config = config();
		return config != null && config.fishing.seaCreatureTracker.showOnFishingIslands;
	}

	public static boolean seaCreatureTrackerShowUptime() {
		EMSkyblockConfig config = config();
		return config == null || config.fishing.seaCreatureTracker.showUptime;
	}

	public static int seaCreatureTrackerMaxLines() {
		EMSkyblockConfig config = config();
		return config == null ? 10 : config.fishing.seaCreatureTracker.maxLines;
	}

	public static boolean fishingProfitTrackerEnabled() {
		EMSkyblockConfig config = config();
		return config != null && config.fishing.fishingProfitTracker.enabled;
	}

	public static boolean fishingProfitTrackerShowWhenPickup() {
		EMSkyblockConfig config = config();
		return config == null || config.fishing.fishingProfitTracker.showWhenPickup;
	}

	public static boolean fishingProfitTrackerShowWithFishingArmor() {
		EMSkyblockConfig config = config();
		return config != null && config.fishing.fishingProfitTracker.showWithFishingArmor;
	}

	public static boolean fishingProfitTrackerShowOnFishingIslands() {
		EMSkyblockConfig config = config();
		return config != null && config.fishing.fishingProfitTracker.showOnFishingIslands;
	}

	public static boolean fishingProfitTrackerShowUptime() {
		EMSkyblockConfig config = config();
		return config == null || config.fishing.fishingProfitTracker.showUptime;
	}

	public static int fishingProfitTrackerMaxLines() {
		EMSkyblockConfig config = config();
		return config == null ? 8 : config.fishing.fishingProfitTracker.maxLines;
	}

	public static boolean slayerProfitTrackerEnabled() {
		EMSkyblockConfig config = config();
		return config != null && config.slayer.slayerProfitTrackerEnabled;
	}

	public static boolean slayerProfitTrackerShowUptime() {
		EMSkyblockConfig config = config();
		return config == null || config.slayer.slayerProfitTrackerShowUptime;
	}

	public static boolean skyblockHideVanillaStatusBars() {
		EMSkyblockConfig config = config();
		return config != null && config.uiCleanup.hideVanillaStatusBars;
	}

	public static boolean skyblockHideActionBarMessages() {
		EMSkyblockConfig config = config();
		return config != null && config.uiCleanup.hideActionBarMessages;
	}

	public static boolean skyblockHideInventoryStatusEffects() {
		EMSkyblockConfig config = config();
		return config != null && config.uiCleanup.hideInventoryStatusEffects;
	}

	public static boolean chatPetRarity() {
		EMSkyblockConfig config = config();
		return config == null || config.chat.rareDropMessages.petRarity;
	}

	public static boolean chatEnchantedBook() {
		EMSkyblockConfig config = config();
		return config == null || config.chat.rareDropMessages.enchantedBook;
	}

	public static boolean chatEnchantedBookMissingMessage() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.rareDropMessages.enchantedBookMissingMessage;
	}

	public static boolean chatFilterHypixelHub() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.hypixelHub;
	}

	public static boolean chatFilterEmpty() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.empty;
	}

	public static boolean chatFilterWarping() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.warping;
	}

	public static boolean chatFilterWelcome() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.welcome;
	}

	public static boolean chatFilterGuildEventExp() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.guildEventExp;
	}

	public static boolean chatFilterKillCombo() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.killCombo;
	}

	public static boolean chatFilterProfileJoin() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.profileJoin;
	}

	public static boolean chatFilterFireSale() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.fireSale;
	}

	public static boolean chatFilterRewardBundles() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.rewardBundles;
	}

	public static boolean chatFilterEventLevelUp() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.eventLevelUp;
	}

	public static boolean chatFilterFactoryUpgrade() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.factoryUpgrade;
	}

	public static boolean chatFilterHoppityBegun() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.hoppityBegun;
	}

	public static boolean chatFilterHoppityEggs() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.hoppityEggs;
	}

	public static boolean chatFilterSacrifice() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.sacrifice;
	}

	public static boolean chatFilterLegacyItems() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.legacyItemsWarning;
	}

	public static boolean chatFilterAlphaAchievements() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.hideAlphaAchievements;
	}

	public static boolean chatFilterParkour() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.parkour;
	}

	public static boolean chatFilterTeleportPads() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.teleportPads;
	}

	public static boolean chatFilterFeastChef() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.feastChef;
	}

	public static boolean chatFilterOthers() {
		EMSkyblockConfig config = config();
		return config != null && config.chat.chatFilters.others;
	}

	private static int clampHudBackgroundOpacity(int opacity) {
		return Math.max(EMUtilsConfig.HUD_BACKGROUND_OPACITY_MIN, Math.min(EMUtilsConfig.HUD_BACKGROUND_OPACITY_MAX, opacity));
	}

	private static int clampEstimatedItemValueEnchantmentsCap(int cap) {
		return Math.max(1, Math.min(20, cap));
	}
}
