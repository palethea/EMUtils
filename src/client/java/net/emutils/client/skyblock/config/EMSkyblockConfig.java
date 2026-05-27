package net.emutils.client.skyblock.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.hud.HudOverlayAnchor;

public final class EMSkyblockConfig extends Config {
	@Category(name = "General", desc = "Core Hypixel SkyBlock settings")
	public General general = new General();

	@Category(name = "Tooltips", desc = "Storage and price tooltip features")
	public Tooltips tooltips = new Tooltips();

	@Category(name = "Estimated Item Value", desc = "SkyHanni-style item value breakdowns")
	public EstimatedItemValue eiv = new EstimatedItemValue();

	@Category(name = "Stats HUD", desc = "SkyBlock action bar stats overlay")
	public StatsHud statsHud = new StatsHud();

	@Category(name = "Fishing", desc = "Fishing helpers and overlays")
	public Fishing fishing = new Fishing();

	@Category(name = "UI Cleanup", desc = "Hide or replace vanilla UI while on SkyBlock")
	public UiCleanup uiCleanup = new UiCleanup();

	@Category(name = "Actions", desc = "Layout and reset actions")
	public Actions actions = new Actions();

	public static final class General {
		@ConfigOption(name = "SkyBlock Features", desc = "Enable Hypixel SkyBlock QoL features in EMUtils.")
		@ConfigEditorBoolean
		public boolean enabled = false;
	}

	public static final class Tooltips {
		@ConfigOption(name = "Storage Preview", desc = "Show a preview of storage contents when hovering compatible items.")
		@ConfigEditorBoolean
		public boolean storagePreviewEnabled = true;

		@Accordion
		@ConfigOption(name = "Display", desc = "How price lines are formatted and when stack totals apply.")
		public TooltipDisplay display = new TooltipDisplay();

		@Accordion
		@ConfigOption(name = "Bazaar Prices", desc = "Bazaar buy and sell prices in item tooltips.")
		public BazaarPrices bazaar = new BazaarPrices();

		@Accordion
		@ConfigOption(name = "Auction Prices", desc = "Auction House prices in item tooltips.")
		public AuctionPrices auction = new AuctionPrices();

		@Accordion
		@ConfigOption(name = "NPC Sell Prices", desc = "NPC sell prices in item tooltips.")
		public NpcPrices npc = new NpcPrices();

		public static final class TooltipDisplay {
			@ConfigOption(name = "Shift for Stack Total", desc = "Hold Shift while hovering to multiply prices by stack or sack stored count.")
			@ConfigEditorBoolean
			public boolean shiftForStackTotal = true;

			@ConfigOption(name = "Compact Numbers", desc = "Show prices like 5.1M instead of 5,100,000.")
			@ConfigEditorBoolean
			public boolean compactNumbers = false;
		}

		public static final class BazaarPrices {
			@ConfigOption(name = "Enabled", desc = "Show Bazaar prices in item tooltips.")
			@ConfigEditorBoolean
			public boolean enabled = true;

			@ConfigOption(name = "Buy Order", desc = "Price to place a buy order (not instant buy).")
			@ConfigEditorBoolean
			public boolean showBuyOrder = true;

			@ConfigOption(name = "Sell Order", desc = "Price to place a sell order (not instant sell).")
			@ConfigEditorBoolean
			public boolean showSellOrder = true;

			@ConfigOption(name = "Instant Buy", desc = "Instant buy price (fills immediately).")
			@ConfigEditorBoolean
			public boolean showInstantBuy = true;

			@ConfigOption(name = "Instant Sell", desc = "Instant sell price (fills immediately).")
			@ConfigEditorBoolean
			public boolean showInstantSell = true;

			@ConfigOption(name = "24h Average", desc = "Average Bazaar sale price over the last 24 hours.")
			@ConfigEditorBoolean
			public boolean showAverage24h = true;

			@ConfigOption(name = "Hide on Soulbound", desc = "Hide Bazaar prices on Co-op or Solo Soulbound items.")
			@ConfigEditorBoolean
			public boolean hideOnSoulbound = true;
		}

		public static final class AuctionPrices {
			@ConfigOption(name = "Enabled", desc = "Show Auction House prices in item tooltips.")
			@ConfigEditorBoolean
			public boolean enabled = true;

			@ConfigOption(name = "Lowest BIN", desc = "Lowest Buy It Now listing price.")
			@ConfigEditorBoolean
			public boolean showLowestBin = true;

			@ConfigOption(name = "24h Average", desc = "Average sale price over the last 24 hours.")
			@ConfigEditorBoolean
			public boolean showAverage24h = true;

			@ConfigOption(name = "Hide on Soulbound", desc = "Hide Auction House prices on Co-op or Solo Soulbound items.")
			@ConfigEditorBoolean
			public boolean hideOnSoulbound = true;
		}

		public static final class NpcPrices {
			@ConfigOption(name = "Enabled", desc = "Show NPC sell prices in item tooltips.")
			@ConfigEditorBoolean
			public boolean enabled = false;

			@ConfigOption(name = "Hide on Soulbound", desc = "Hide NPC sell prices on Co-op or Solo Soulbound items.")
			@ConfigEditorBoolean
			public boolean hideOnSoulbound = true;
		}
	}

	public static final class EstimatedItemValue {
		@ConfigOption(name = "EIV HUD", desc = "Show an Estimated Item Value breakdown while hovering SkyBlock items.")
		@ConfigEditorBoolean
		public boolean hudEnabled = false;

		/** Legacy compatibility. HUD opacity now lives in the HUD Layout Editor. */
		public boolean hudBackground = true;

		@Accordion
		@ConfigOption(name = "EIV Layout", desc = "Position and scale for the Estimated Item Value HUD. Use the HUD Layout Editor.")
		public EivLayout layout = new EivLayout();

		@ConfigOption(name = "Enchantment Cap", desc = "Maximum number of enchantments to include in EIV calculations.")
		@ConfigEditorSlider(minValue = 1, maxValue = 20, minStep = 1)
		public int enchantmentsCap = 7;

		@ConfigOption(name = "Estimated Value Tooltip", desc = "Show estimated value in item tooltips when it exceeds the base item value.")
		@ConfigEditorBoolean
		public boolean tooltipEnabled = true;

		@ConfigOption(name = "Hide on Soulbound", desc = "Hide the EIV total line in item tooltips on Co-op or Solo Soulbound items.")
		@ConfigEditorBoolean
		public boolean hideOnSoulbound = true;

		@ConfigOption(name = "Open HUD Layout Editor", desc = "Drag SkyBlock HUD elements into custom positions.")
		@ConfigEditorButton(buttonText = "Edit")
		public transient Runnable openHudLayoutEditor = () -> {};
	}

	public static final class EivLayout {
		/** Legacy scale; migrated to {@link net.emutils.client.hud.layout.HudCustomLayoutEntry}. */
		public int scale = 100;
	}

	public static final class Fishing {
		@Accordion
		@ConfigOption(
			name = "Fishing Hook Display",
			desc = "Show Hypixel's fishing hook timer on your HUD. Replaces the !!! pull alert and optional countdown."
		)
		public FishingHookDisplay hookDisplay = new FishingHookDisplay();

		@Accordion
		@ConfigOption(name = "Sea Creature Tracker", desc = "Track sea creatures caught while fishing.")
		public SeaCreatureTracker seaCreatureTracker = new SeaCreatureTracker();

		@Accordion
		@ConfigOption(name = "Fishing Profit Tracker", desc = "Track items and coins gained while fishing.")
		public FishingProfitTracker fishingProfitTracker = new FishingProfitTracker();
	}

	public static final class SeaCreatureTracker {
		@ConfigOption(name = "Enabled", desc = "Show the Sea Creature Tracker HUD while fishing.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@ConfigOption(name = "Show Percentage", desc = "Show what percentage each sea creature represents.")
		@ConfigEditorBoolean
		public boolean showPercentage = false;

		@ConfigOption(name = "Count Double Hook", desc = "Count double-hook catches as two sea creatures.")
		@ConfigEditorBoolean
		public boolean countDouble = true;

		@ConfigOption(name = "Only While Fishing", desc = "Hide the tracker when you are not actively fishing.")
		@ConfigEditorBoolean
		public boolean onlyWhileFishing = true;

		@ConfigOption(name = "Show Uptime", desc = "Show how long the current tracker session has been running.")
		@ConfigEditorBoolean
		public boolean showUptime = true;

		@ConfigOption(name = "Max Lines", desc = "Maximum number of sea creatures listed in the HUD.")
		@ConfigEditorSlider(minValue = 3, maxValue = 20, minStep = 1)
		public int maxLines = 10;

		@Accordion
		@ConfigOption(name = "Layout", desc = "Position and scale for the Sea Creature Tracker HUD.")
		public TrackerLayout layout = new TrackerLayout();

		@ConfigOption(name = "Open HUD Layout Editor", desc = "Drag SkyBlock HUD elements into custom positions.")
		@ConfigEditorButton(buttonText = "Edit")
		public transient Runnable openHudLayoutEditor = () -> {};

		@ConfigOption(name = "Reset Session", desc = "Clear session sea creature counts for the current profile.")
		@ConfigEditorButton(buttonText = "Reset Session")
		public transient Runnable resetSession = () -> {};

		@ConfigOption(name = "Reset All Time", desc = "Clear all-time sea creature counts for the current profile.")
		@ConfigEditorButton(buttonText = "Reset All Time")
		public transient Runnable resetAllTime = () -> {};
	}

	public static final class FishingProfitTracker {
		@ConfigOption(name = "Enabled", desc = "Show the Fishing Profit Tracker HUD while fishing.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@ConfigOption(
			name = "Show When Pickup",
			desc = "Keep the tracker visible briefly after catching something, even while moving."
		)
		@ConfigEditorBoolean
		public boolean showWhenPickup = true;

		@ConfigOption(name = "Show Uptime", desc = "Show tracker uptime and profit per hour.")
		@ConfigEditorBoolean
		public boolean showUptime = true;

		@ConfigOption(name = "Max Item Lines", desc = "Maximum number of tracked items shown in the HUD.")
		@ConfigEditorSlider(minValue = 3, maxValue = 20, minStep = 1)
		public int maxLines = 8;

		@Accordion
		@ConfigOption(name = "Layout", desc = "Position and scale for the Fishing Profit Tracker HUD.")
		public TrackerLayout layout = new TrackerLayout();

		@ConfigOption(name = "Open HUD Layout Editor", desc = "Drag SkyBlock HUD elements into custom positions.")
		@ConfigEditorButton(buttonText = "Edit")
		public transient Runnable openHudLayoutEditor = () -> {};

		@ConfigOption(name = "Reset Session", desc = "Clear session fishing profit for the current profile.")
		@ConfigEditorButton(buttonText = "Reset Session")
		public transient Runnable resetSession = () -> {};

		@ConfigOption(name = "Reset All Time", desc = "Clear all-time fishing profit for the current profile.")
		@ConfigEditorButton(buttonText = "Reset All Time")
		public transient Runnable resetAllTime = () -> {};
	}

	public static final class TrackerLayout {
		/** Legacy scale; migrated to {@link net.emutils.client.hud.layout.HudCustomLayoutEntry}. */
		public int scale = 100;
	}

	public static final class FishingHookDisplay {
		@ConfigOption(
			name = "Enabled",
			desc = "Show a large on-screen alert while holding a fishing rod and your bobber has a hook timer."
		)
		@ConfigEditorBoolean
		public boolean enabled = true;

		@ConfigOption(
			name = "Custom Alert",
			desc = "Text shown when the hook is ready to pull (replaces Hypixel's !!!). Use & color codes, e.g. &c&l!!!"
		)
		@ConfigEditorText
		public String customAlertText = "&c&l!!!";

		@ConfigOption(
			name = "Show Countdown",
			desc = "Show the numeric countdown before the pull alert (e.g. 3.2)."
		)
		@ConfigEditorBoolean
		public boolean showCountdown = true;

		@Accordion
		@ConfigOption(name = "Countdown Colors", desc = "Optional custom color prefix for the countdown numbers.")
		public CountdownColors countdownColors = new CountdownColors();

		@ConfigOption(
			name = "Hide Armor Stand",
			desc = "Hide Hypixel's small armor stand label above the bobber while this overlay is active."
		)
		@ConfigEditorBoolean
		public boolean hideArmorStand = true;

		@Accordion
		@ConfigOption(name = "Layout", desc = "Position and scale for the fishing hook overlay. Use the HUD Layout Editor.")
		public FishingHookLayout layout = new FishingHookLayout();

		@ConfigOption(name = "Open HUD Layout Editor", desc = "Drag SkyBlock HUD elements into custom positions.")
		@ConfigEditorButton(buttonText = "Edit")
		public transient Runnable openHudLayoutEditor = () -> {};
	}

	public static final class CountdownColors {
		@ConfigOption(
			name = "Custom Countdown Color",
			desc = "Apply a custom color prefix to countdown numbers instead of Hypixel's yellow."
		)
		@ConfigEditorBoolean
		public boolean useCustomColor = false;

		@ConfigOption(
			name = "Countdown Color Prefix",
			desc = "Color codes prepended to the countdown value, e.g. &e&l for yellow bold."
		)
		@ConfigEditorText
		public String colorPrefix = "&e&l";
	}

	public static final class FishingHookLayout {
		/** Legacy scale; migrated to {@link net.emutils.client.hud.layout.HudCustomLayoutEntry}. */
		public int scale = 100;
	}

	public static final class StatsHud {
		@ConfigOption(name = "Stats HUD", desc = "Show parsed SkyBlock stats from the action bar.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@ConfigOption(name = "Hide Parsed Action Bar", desc = "Hide action bar text that is already shown in the Stats HUD.")
		@ConfigEditorBoolean
		public boolean hideActionBar = true;

		@Accordion
		@ConfigOption(name = "Stats Layout", desc = "Opacity for the Stats HUD. Position and scale are in the HUD Layout Editor.")
		public StatsLayout layout = new StatsLayout();

		@Accordion
		@ConfigOption(name = "Visible Stats", desc = "Choose which parsed stats are shown.")
		public VisibleStats visibleStats = new VisibleStats();

		@ConfigOption(name = "Open HUD Layout Editor", desc = "Drag SkyBlock HUD elements into custom positions.")
		@ConfigEditorButton(buttonText = "Edit")
		public transient Runnable openHudLayoutEditor = () -> {};
	}

	public static final class StatsLayout {
		/** Legacy compatibility. HUD opacity now lives in the HUD Layout Editor. */
		public int backgroundOpacity = 85;

		/** Legacy scale; migrated to {@link net.emutils.client.hud.layout.HudCustomLayoutEntry}. */
		public int scale = 100;
	}

	public static final class VisibleStats {
		@ConfigOption(name = "Health", desc = "Show health in the Stats HUD.")
		@ConfigEditorBoolean
		public boolean health = true;

		@ConfigOption(name = "Defense", desc = "Show defense in the Stats HUD.")
		@ConfigEditorBoolean
		public boolean defense = true;

		@ConfigOption(name = "Mana", desc = "Show mana in the Stats HUD.")
		@ConfigEditorBoolean
		public boolean mana = true;

		@ConfigOption(name = "Soulflow", desc = "Show soulflow in the Stats HUD.")
		@ConfigEditorBoolean
		public boolean soulflow = true;
	}

	public static final class UiCleanup {
		@ConfigOption(name = "Hide Vanilla Status Bars", desc = "Hide vanilla health, hunger, and armor bars while on SkyBlock.")
		@ConfigEditorBoolean
		public boolean hideVanillaStatusBars = false;

		@ConfigOption(name = "Hide Action Bar Messages", desc = "Hide SkyBlock action bar messages replaced by EMUtils overlays.")
		@ConfigEditorBoolean
		public boolean hideActionBarMessages = false;

		@ConfigOption(name = "Hide Inventory Status Effects", desc = "Hide the vanilla status effect panel in inventory screens on SkyBlock.")
		@ConfigEditorBoolean
		public boolean hideInventoryStatusEffects = false;
	}

	public static final class Actions {
		@ConfigOption(name = "Reset SkyBlock Settings", desc = "Restore all EMSkyblock settings to their defaults.")
		@ConfigEditorButton(buttonText = "Reset")
		public transient Runnable resetDefaults = () -> {};
	}

	@Override
	public StructuredText getTitle() {
		return StructuredText.of("EMSkyblock");
	}

	public void applyDefaults() {
		general = new General();
		tooltips = new Tooltips();
		eiv = new EstimatedItemValue();
		statsHud = new StatsHud();
		fishing = new Fishing();
		uiCleanup = new UiCleanup();
		actions = new Actions();
	}

	public void applyLegacy(EMUtilsConfig legacy) {
		if (legacy == null) {
			return;
		}

		general.enabled = legacy.skyblockEnabled();
		tooltips.storagePreviewEnabled = legacy.storagePreviewEnabled();
		tooltips.bazaar.enabled = legacy.bazaarTooltipsEnabled();
		tooltips.auction.enabled = legacy.auctionTooltipsEnabled();
		tooltips.npc.enabled = legacy.npcSellPriceTooltipsEnabled();
		statsHud.enabled = legacy.skyblockStatsHudEnabled();
		statsHud.hideActionBar = legacy.skyblockStatsHideActionBar();
		statsHud.visibleStats.health = legacy.skyblockStatsShowHealth();
		statsHud.visibleStats.defense = legacy.skyblockStatsShowDefense();
		statsHud.visibleStats.mana = legacy.skyblockStatsShowMana();
		statsHud.visibleStats.soulflow = legacy.skyblockStatsShowSoulflow();
		statsHud.layout.backgroundOpacity = legacy.skyblockStatsHudBackgroundOpacity();
		statsHud.layout.scale = legacy.legacySkyblockStatsHudScale();
		eiv.hudEnabled = legacy.estimatedItemValueHudEnabled();
		eiv.layout.scale = legacy.legacyEstimatedItemValueHudScale();
		eiv.enchantmentsCap = legacy.estimatedItemValueEnchantmentsCap();
		eiv.tooltipEnabled = legacy.estimatedItemValueShowExactTotal();
		uiCleanup.hideVanillaStatusBars = legacy.skyblockHideVanillaStatusBars();
		uiCleanup.hideActionBarMessages = legacy.skyblockHideActionBarMessages();
		uiCleanup.hideInventoryStatusEffects = legacy.skyblockHideInventoryStatusEffects();
	}
}
