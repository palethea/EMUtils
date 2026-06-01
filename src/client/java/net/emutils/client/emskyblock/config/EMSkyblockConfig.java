package net.emutils.client.emskyblock.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.HudOverlayAnchor;
import net.emutils.client.emskyblock.config.ConfigVersionDisplay;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

public final class EMSkyblockConfig extends Config {
	@Category(name = "About", desc = "Information about EMUtils and updates.")
	public About about = new About();

	@Category(name = "GUI", desc = "HUD overlays and UI cleanup.")
	public Gui gui = new Gui();

	@Category(name = "Fishing", desc = "Fishing helpers and overlays.")
	public Fishing fishing = new Fishing();

	@Category(name = "Inventory", desc = "Inventory, tooltip, storage, and item value features.")
	public Inventory inventory = new Inventory();

	@Category(name = "Chat", desc = "Chat features.")
	public Chat chat = new Chat();

	@Category(name = "Slayer", desc = "Hypixel SkyBlock slayer profit and drop tracking.")
	public Slayer slayer = new Slayer();

	@Category(name = "Dev", desc = "Developer and reset actions.")
	public Dev dev = new Dev();

	public General general = new General();
	public Tooltips tooltips = inventory.tooltips;
	public EstimatedItemValue eiv = inventory.eiv;
	public AuctionHouse auctionHouse = inventory.auctionHouse;
	public Bazaar bazaar = inventory.bazaar;
	public ExperimentationTable experimentationTable = inventory.experimentationTable;
	public StatsHud statsHud = gui.statsHud;
	public UiCleanup uiCleanup = gui.uiCleanup;
	public Actions actions = dev.actions;

	public static final class General {
		public boolean enabled = false;
	}

		public static final class Chat {
		@Accordion
		@ConfigOption(name = "Rare Drop Messages", desc = "Enhance rare drop messages in chat.")
		public RareDropMessages rareDropMessages = new RareDropMessages();

		@Accordion
		@ConfigOption(name = "Chat Filters", desc = "Filter unwanted chat messages.")
		public ChatFilters chatFilters = new ChatFilters();
	}

	public static final class Slayer {
		@ConfigOption(name = "Profit Tracker", desc = "Show the Slayer Profit Tracker HUD while doing a slayer quest.")
		@ConfigEditorBoolean
		public boolean slayerProfitTrackerEnabled = false;

		@ConfigOption(name = "Show Uptime", desc = "Show tracker uptime and profit per hour.")
		@ConfigEditorBoolean
		public boolean slayerProfitTrackerShowUptime = true;

		@Accordion
		@ConfigOption(name = "Layout", desc = "Position and scale for the Slayer Profit Tracker HUD.")
		public TrackerLayout slayerProfitTrackerLayout = new TrackerLayout();

		@ConfigOption(name = "Open HUD Layout Editor", desc = "Drag SkyBlock HUD elements into custom positions.")
		@ConfigEditorButton(buttonText = "Edit")
		public transient Runnable openHudLayoutEditor = () -> {};

		@ConfigOption(name = "Reset Session", desc = "Clear session slayer profit for the current profile.")
		@ConfigEditorButton(buttonText = "Reset Session")
		public transient Runnable resetSession = () -> {};

		@ConfigOption(name = "Reset All Time", desc = "Clear all-time slayer profit for the current profile.")
		@ConfigEditorButton(buttonText = "Reset All Time")
		public transient Runnable resetAllTime = () -> {};
	}


	public static final class RareDropMessages {
		@ConfigOption(name = "Pet Drop Rarity", desc = "Shows what rarity the pet drop is in the pet drop message.")
		@ConfigEditorBoolean
		public boolean petRarity = true;

		@ConfigOption(name = "Enchanted Book Name", desc = "Shows what enchantment the dropped enchanted book is.")
		@ConfigEditorBoolean
		public boolean enchantedBook = true;

		@ConfigOption(name = "Missing Enchanted Book Message", desc = "Sends a custom Rare Drop message if you get an enchanted book without a message in chat.")
		@ConfigEditorBoolean
		public boolean enchantedBookMissingMessage = false;
	}

	public static final class ChatFilters {
		@ConfigOption(name = "Hypixel Lobbies", desc = "Hide announcements in Hypixel lobbies (player joins, loot boxes, prototype lobby messages, radiating generosity, Hypixel tournaments).")
		@ConfigEditorBoolean
		public boolean hypixelHub = false;

		@ConfigOption(name = "Empty", desc = "Hide all empty messages.")
		@ConfigEditorBoolean
		public boolean empty = false;

		@ConfigOption(name = "Warping", desc = "Hide 'Sending request to join...' and 'Warping...' messages.")
		@ConfigEditorBoolean
		public boolean warping = false;

		@ConfigOption(name = "Welcome", desc = "Hide the 'Welcome to SkyBlock' message.")
		@ConfigEditorBoolean
		public boolean welcome = false;

		@ConfigOption(name = "Guild/Event EXP", desc = "Hide Guild and Event EXP messages.")
		@ConfigEditorBoolean
		public boolean guildEventExp = false;

		@ConfigOption(name = "Kill Combo", desc = "Hide messages about your Kill Combo from the Grandma Wolf pet.")
		@ConfigEditorBoolean
		public boolean killCombo = false;

		@ConfigOption(name = "Profile Join", desc = "Hide 'You are playing on profile' and 'Profile ID' chat messages.")
		@ConfigEditorBoolean
		public boolean profileJoin = false;

		@ConfigOption(name = "Fire Sale", desc = "Hide the repeating fire sale reminder chat messages.")
		@ConfigEditorBoolean
		public boolean fireSale = false;

		@ConfigOption(name = "Reward Bundles", desc = "Hide the reminders to claim seasonal reward bundles.")
		@ConfigEditorBoolean
		public boolean rewardBundles = false;

		@ConfigOption(name = "Event Level Up", desc = "Hide event level up messages.")
		@ConfigEditorBoolean
		public boolean eventLevelUp = false;

		@ConfigOption(name = "Factory Upgrade", desc = "Hide Chocolate Factory upgrade and employee promotion messages.")
		@ConfigEditorBoolean
		public boolean factoryUpgrade = false;

		@ConfigOption(name = "Hoppity's Hunt Begin", desc = "Hide \"Hoppity's Hunt has begun\" messages.")
		@ConfigEditorBoolean
		public boolean hoppityBegun = false;

		@ConfigOption(name = "Hoppity's Hunt Eggs", desc = "Hide \"An egg has appeared!\" messages during hoppity's hunt.")
		@ConfigEditorBoolean
		public boolean hoppityEggs = false;

		@ConfigOption(name = "Sacrifice", desc = "Hide other players' sacrifice messages.")
		@ConfigEditorBoolean
		public boolean sacrifice = false;

		@ConfigOption(name = "Legacy Items Warning", desc = "Hide the legacy items in sacks/storage warning.")
		@ConfigEditorBoolean
		public boolean legacyItemsWarning = false;

		@ConfigOption(name = "Block Alpha Achievements", desc = "Hide achievement messages while on the Alpha network.")
		@ConfigEditorBoolean
		public boolean hideAlphaAchievements = false;

		@ConfigOption(name = "Parkour Messages", desc = "Hide parkour messages (starting, stopping, reaching a checkpoint).")
		@ConfigEditorBoolean
		public boolean parkour = false;

		@ConfigOption(name = "Teleport Pad Messages", desc = "Hide annoying messages when using teleport pads.")
		@ConfigEditorBoolean
		public boolean teleportPads = false;

		@ConfigOption(name = "Feast Chef Ted", desc = "Hide annoying messages about Kernels getting added to your purse while farming.")
		@ConfigEditorBoolean
		public boolean feastChef = false;

		@ConfigOption(name = "Others", desc = "Hide other annoying messages (Bazaar/AH minis, slayer, useless drops, party separators, money, winter island, useless warnings, annoying spam, SkyMall, Lottery).")
		@ConfigEditorBoolean
		public boolean others = false;
	}

	public static final class Gui {
		@Category(name = "Stats HUD", desc = "SkyBlock action bar stats overlay.")
		public StatsHud statsHud = new StatsHud();

		@Category(name = "UI Cleanup", desc = "Hide or replace vanilla UI while on SkyBlock.")
		public UiCleanup uiCleanup = new UiCleanup();
	}

	public static final class Inventory {
		@Category(name = "Auction House", desc = "Be smart when buying or selling expensive items in the Auction House.")
		public AuctionHouse auctionHouse = new AuctionHouse();

		@Category(name = "Bazaar", desc = "Be smart when buying or selling many items in the Bazaar.")
		public Bazaar bazaar = new Bazaar();

		@Category(name = "Experimentation Table", desc = "QoL features for the Experimentation Table.")
		public ExperimentationTable experimentationTable = new ExperimentationTable();

		@Category(name = "Tooltips", desc = "Storage and price tooltip features.")
		public Tooltips tooltips = new Tooltips();

		@Category(name = "Estimated Item Value", desc = "SkyHanni-style item value breakdowns.")
		public EstimatedItemValue eiv = new EstimatedItemValue();
	}

	public static final class AuctionHouse {
		@Accordion
		@ConfigOption(name = "Auctions Price Comparison", desc = "Highlight auctions based on listed price versus estimated item value.")
		public AuctionHousePriceComparison priceComparison = new AuctionHousePriceComparison();

		@ConfigOption(name = "Highlight Auctions", desc = "Highlight your sold and expired auctions in Manage Auctions.")
		@ConfigEditorBoolean
		public boolean highlightAuctions = true;

		@ConfigOption(name = "Sold Color", desc = "Color of sold items.")
		@ConfigEditorColour
		public ChromaColour soldColor = ChromaColour.fromStaticRGB(85, 255, 85, 150);

		@ConfigOption(name = "Expired Color", desc = "Color of expired items.")
		@ConfigEditorColour
		public ChromaColour expiredColor = ChromaColour.fromStaticRGB(255, 85, 85, 150);

		@ConfigOption(name = "Highlight Underbid Auctions", desc = "Highlight underbid own lowest BIN auctions that are outbid.")
		@ConfigEditorBoolean
		public boolean highlightAuctionsUnderbid = false;

		@ConfigOption(name = "Underbid Color", desc = "Color of underbid BIN items.")
		@ConfigEditorColour
		public ChromaColour underbidColor = ChromaColour.fromStaticRGB(255, 170, 0, 150);

		@ConfigOption(
			name = "Auto Copy Underbid",
			desc = "Automatically copy the estimated item price minus 1 coin in Create BIN Auction."
		)
		@ConfigEditorBoolean
		public boolean autoCopyUnderbidPrice = false;

		@ConfigOption(
			name = "Copy Underbid Keybind",
			desc = "Copy the hovered Auction House price minus 1 coin to the clipboard."
		)
		@ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
		public int copyUnderbidKeybind = GLFW.GLFW_KEY_UNKNOWN;

		@ConfigOption(
			name = "Price Website",
			desc = "Add a button to Auction House searches that opens the item page on sky.coflnet.com."
		)
		@ConfigEditorBoolean
		public boolean openPriceWebsite = false;

		@ConfigOption(name = "Outbid Alert", desc = "Send a warning when you are outbid on an auction.")
		@ConfigEditorBoolean
		public boolean auctionOutbid = false;
	}

	public static final class AuctionHousePriceComparison {
		@ConfigOption(
			name = "Show Price Comparison",
			desc = "Highlight auctions by the difference between their estimated value and listed price. This is only an estimate."
		)
		@ConfigEditorBoolean
		public boolean enabled = false;

		@ConfigOption(name = "Good Color", desc = "Color for good value items.")
		@ConfigEditorColour
		public ChromaColour good = ChromaColour.fromStaticRGB(85, 255, 85, 150);

		@ConfigOption(name = "Very Good Color", desc = "Color for very good value items.")
		@ConfigEditorColour
		public ChromaColour veryGood = ChromaColour.fromStaticRGB(0, 139, 0, 170);

		@ConfigOption(name = "Bad Color", desc = "Color for bad value items.")
		@ConfigEditorColour
		public ChromaColour bad = ChromaColour.fromStaticRGB(255, 255, 85, 150);

		@ConfigOption(name = "Very Bad Color", desc = "Color for very bad value items.")
		@ConfigEditorColour
		public ChromaColour veryBad = ChromaColour.fromStaticRGB(225, 43, 30, 170);
	}

	public static final class Bazaar {
		@ConfigOption(name = "Purchase Helper", desc = "Highlight the Bazaar result opened by /bz, cancelled-order reorders, and shopping-list shortcuts.")
		@ConfigEditorBoolean
		public boolean purchaseHelper = true;

		@ConfigOption(name = "Order Helper", desc = "Show visual hints in Bazaar order views for ready or outbid orders.")
		@ConfigEditorBoolean
		public boolean orderHelper = false;

		@ConfigOption(name = "Best Sell Method", desc = "Show the price difference between instant sell and sell offer.")
		@ConfigEditorBoolean
		public boolean bestSellMethod = false;

		@ConfigOption(
			name = "Daily Limit Tracker",
			desc = "Show your progress towards the daily 15 billion coin Bazaar trade limit."
		)
		@ConfigEditorBoolean
		public boolean dailyLimitTracker = false;

		@ConfigOption(
			name = "Cancelled Buy Order Clipboard",
			desc = "Send missing items from cancelled buy orders in chat and copy the amount to the clipboard."
		)
		@ConfigEditorBoolean
		public boolean cancelledBuyOrderClipboard = false;

		@ConfigOption(
			name = "Price Website",
			desc = "Add a button to Bazaar product inventories that opens the item page on skyblock.bz."
		)
		@ConfigEditorBoolean
		public boolean openPriceWebsite = false;

		@ConfigOption(
			name = "Max Items With Purse",
			desc = "Calculate how many of the opened Bazaar item you can buy with your purse."
		)
		@ConfigEditorBoolean
		public boolean maxPurseItems = false;

		@ConfigOption(
			name = "Craft Materials Bazaar",
			desc = "In the crafting view, show a shopping list of required materials with Bazaar or Auction prices."
		)
		@ConfigEditorBoolean
		public boolean craftMaterialsFromBazaar = true;
	}

	public static final class ExperimentationTable {
		@Accordion
		@ConfigOption(name = "Profit Tracker", desc = "Tracker for drops and XP you get from experiments.")
		public ExperimentsProfitTracker experimentsProfitTracker = new ExperimentsProfitTracker();

		@Accordion
		@ConfigOption(name = "Dry-Streak Display", desc = "Display attempts and XP since your last ULTRA-RARE.")
		public ExperimentsDryStreak dryStreak = new ExperimentsDryStreak();

		@Accordion
		@ConfigOption(name = "Experiment Addons", desc = "Helpers for Chronomatron and Ultrasequencer.")
		public ExperimentsAddons addons = new ExperimentsAddons();

		@Accordion
		@ConfigOption(name = "Superpairs", desc = "Helpers and overlays for Superpairs.")
		public ExperimentsSuperpairs superpairs = new ExperimentsSuperpairs();

		@ConfigOption(
			name = "Guardian Reminder",
			desc = "Warn when opening the Experimentation Table without a Guardian pet equipped."
		)
		@ConfigEditorBoolean
		public boolean guardianReminder = false;
	}

	public static final class ExperimentsProfitTracker {
		@ConfigOption(name = "Enabled", desc = "Track drops, XP, bottles, and time spent in experiments.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@ConfigOption(name = "Track Time Spent", desc = "Track time spent doing add-ons and experiments.")
		@ConfigEditorBoolean
		public boolean trackTimeSpent = false;

		@ConfigOption(name = "Track Used Bottles", desc = "Track thrown XP bottles while near the Experimentation Table.")
		@ConfigEditorBoolean
		public boolean trackUsedBottles = true;

		@ConfigOption(name = "Bottle Warnings", desc = "Display warnings once per session about bottles being auto-tracked.")
		@ConfigEditorBoolean
		public boolean bottleWarnings = true;
	}

	public static final class ExperimentsDryStreak {
		@ConfigOption(name = "Enabled", desc = "Display attempts and XP since your last ULTRA-RARE.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@ConfigOption(name = "Attempts", desc = "Display attempts since the last ULTRA-RARE.")
		@ConfigEditorBoolean
		public boolean attemptsSince = true;

		@ConfigOption(name = "XP", desc = "Display XP since the last ULTRA-RARE.")
		@ConfigEditorBoolean
		public boolean xpSince = true;
	}

	public static final class ExperimentsAddons {
		@ConfigOption(name = "Enabled", desc = "Enable helpers for Chronomatron and Ultrasequencer.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@ConfigOption(
			name = "Next Click Helper",
			desc = "Highlight the next slot to click in Chronomatron and show the Ultrasequencer sequence."
		)
		@ConfigEditorBoolean
		public boolean highlightNextClick = true;

		@ConfigOption(name = "Color", desc = "Color for the next slot.")
		@ConfigEditorColour
		public ChromaColour nextColor = ChromaColour.fromStaticRGB(85, 255, 85, 170);

		@ConfigOption(name = "Second Color", desc = "Color for later slots.")
		@ConfigEditorColour
		public ChromaColour secondColor = ChromaColour.fromStaticRGB(255, 255, 85, 110);

		@ConfigOption(name = "Prevent Misclicks", desc = "Prevent clicking wrong Chronomatron colors or Ultrasequencer slots.")
		@ConfigEditorBoolean
		public boolean preventMisclicks = true;

		@ConfigOption(
			name = "Max Clicks Alert",
			desc = "Alert when you reach the maximum clicks from Chronomatron or Ultrasequencer."
		)
		@ConfigEditorBoolean
		public boolean maxSequenceAlert = true;
	}

	public static final class ExperimentsSuperpairs {
		@Accordion
		@ConfigOption(name = "Keep Items Visible", desc = "Keep clicked items visible to help create matches.")
		public SuperpairsVisibility clickedItemsVisible = new SuperpairsVisibility();

		@ConfigOption(name = "Superpair Data", desc = "Display useful data while doing the Superpairs experiment.")
		@ConfigEditorBoolean
		public boolean display = false;

		@ConfigOption(name = "Superpairs XP Overlay", desc = "Show how much XP every pair is worth in Superpairs.")
		@ConfigEditorBoolean
		public boolean xpOverlay = true;

		@ConfigOption(name = "ULTRA-RARE Book Alert", desc = "Send a chat message, title, and sound when you find an ULTRA-RARE book.")
		@ConfigEditorBoolean
		public boolean ultraRareBookAlert = false;
	}

	public static final class SuperpairsVisibility {
		@ConfigOption(name = "Enabled", desc = "Keep clicked items visible to help create matches.")
		@ConfigEditorBoolean
		public boolean enabled = true;
	}

	public static final class Dev {
		@Category(name = "Actions", desc = "Layout and reset actions.")
		public Actions actions = new Actions();
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
		/** Legacy scale; migrated to {@link net.emutils.client.emhelpers.hud.layout.HudCustomLayoutEntry}. */
		public int scale = 100;
	}

	public static final class Fishing {
		@Category(
			name = "Fishing Hook Display",
			desc = "Show Hypixel's fishing hook timer on your HUD. Replaces the !!! pull alert and optional countdown."
		)
		public FishingHookDisplay hookDisplay = new FishingHookDisplay();

		@Category(name = "Sea Creature Tracker", desc = "Track sea creatures caught while fishing.")
		public SeaCreatureTracker seaCreatureTracker = new SeaCreatureTracker();

		@Category(name = "Fishing Profit Tracker", desc = "Track items and coins gained while fishing.")
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

		@ConfigOption(name = "Show With Fishing Armor", desc = "Keep the tracker visible whenever you are wearing any fishing armor.")
		@ConfigEditorBoolean
		public boolean showWithFishingArmor = false;

		@ConfigOption(name = "Show On Fishing Islands", desc = "Keep the tracker visible on fishing islands even when not actively fishing.")
		@ConfigEditorBoolean
		public boolean showOnFishingIslands = false;

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

		@ConfigOption(name = "Show With Fishing Armor", desc = "Keep the tracker visible whenever you are wearing any fishing armor.")
		@ConfigEditorBoolean
		public boolean showWithFishingArmor = false;

		@ConfigOption(name = "Show On Fishing Islands", desc = "Keep the tracker visible on fishing islands even when not actively fishing.")
		@ConfigEditorBoolean
		public boolean showOnFishingIslands = false;

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
		/** Legacy scale; migrated to {@link net.emutils.client.emhelpers.hud.layout.HudCustomLayoutEntry}. */
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
		/** Legacy scale; migrated to {@link net.emutils.client.emhelpers.hud.layout.HudCustomLayoutEntry}. */
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

		/** Legacy scale; migrated to {@link net.emutils.client.emhelpers.hud.layout.HudCustomLayoutEntry}. */
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

	public static final class About {
		@ConfigOption(name = "", desc = "")
		@ConfigVersionDisplay
		public transient Runnable currentVersion = () -> {};

		@ConfigOption(name = "SkyBlock Features", desc = "Enable Hypixel SkyBlock QoL features in EMUtils.")
		@ConfigEditorBoolean
		public boolean enabled = false;

		@ConfigOption(name = "Update Stream", desc = "How frequently you want updates for EMUtils")
		@ConfigEditorDropdown
		public EmUtilsVersion updateStream = EmUtilsVersion.FULL;

		@Accordion
		@ConfigOption(name = "Used Software", desc = "Information about used software and licenses")
		public Licenses licenses = new Licenses();

		public enum EmUtilsVersion {
			FULL("Full"),
			BETA("Beta");

			private final String displayName;

			EmUtilsVersion(String displayName) {
				this.displayName = displayName;
			}

			@Override
			public String toString() {
				return displayName;
			}
		}

		public static final class Licenses {
			@ConfigOption(name = "MoulConfig", desc = "MoulConfig is available under the LGPL 3.0 License or later version")
			@ConfigEditorButton(buttonText = "Source")
			public transient Runnable moulConfig = () -> {};

			@ConfigOption(name = "Fabric Loader", desc = "Fabric Loader is available under the Apache-2.0 license")
			@ConfigEditorButton(buttonText = "Source")
			public transient Runnable fabricLoader = () -> {};

			@ConfigOption(name = "Fabric API", desc = "Fabric API is available under the Apache-2.0 license")
			@ConfigEditorButton(buttonText = "Source")
			public transient Runnable fabricApi = () -> {};

			@ConfigOption(name = "Mixin", desc = "Mixin is available under the MIT License")
			@ConfigEditorButton(buttonText = "Source")
			public transient Runnable mixin = () -> {};

			@ConfigOption(name = "MixinExtras", desc = "MixinExtras is available under the MIT License")
			@ConfigEditorButton(buttonText = "Source")
			public transient Runnable mixinExtras = () -> {};

			@ConfigOption(name = "SkyHanni-REPO", desc = "SkyHanni's data repository is available under the MIT License")
			@ConfigEditorButton(buttonText = "Source")
			public transient Runnable skyHanniRepo = () -> {};
		}
	}

	@Override
	public StructuredText getTitle() {
		String version = FabricLoader.getInstance()
			.getModContainer(EMUtilsClient.MOD_ID)
			.map(c -> c.getMetadata().getVersion().getFriendlyString())
			.orElse("?");
		return StructuredText.of("§7EMUtils v" + version + " by §aPalethea§7, config by §eMoulberry §7and §enea89");
	}

	public void applyDefaults() {
		about = new About();
		gui = new Gui();
		fishing = new Fishing();
		inventory = new Inventory();
		chat = new Chat();
		slayer = new Slayer();
		dev = new Dev();
		general = new General();
		syncLegacyAliases();
	}

	public void migrateCategoryLayout(boolean preferLegacyFields) {
		if (gui == null) gui = new Gui();
		if (inventory == null) inventory = new Inventory();
		if (dev == null) dev = new Dev();
		if (fishing == null) fishing = new Fishing();
		if (chat == null) chat = new Chat();
		if (slayer == null) slayer = new Slayer();

		if (preferLegacyFields) {
			if (tooltips != null) inventory.tooltips = tooltips;
			if (eiv != null) inventory.eiv = eiv;
			if (auctionHouse != null) inventory.auctionHouse = auctionHouse;
			if (bazaar != null) inventory.bazaar = bazaar;
			if (experimentationTable != null) inventory.experimentationTable = experimentationTable;
			if (statsHud != null) gui.statsHud = statsHud;
			if (uiCleanup != null) gui.uiCleanup = uiCleanup;
			if (actions != null) dev.actions = actions;
		}

		syncLegacyAliases();
	}

	private void syncLegacyAliases() {
		tooltips = inventory.tooltips;
		eiv = inventory.eiv;
		auctionHouse = inventory.auctionHouse;
		bazaar = inventory.bazaar;
		experimentationTable = inventory.experimentationTable;
		statsHud = gui.statsHud;
		uiCleanup = gui.uiCleanup;
		actions = dev.actions;
	}

	public void applyLegacy(EMUtilsConfig legacy) {
		if (legacy == null) {
			return;
		}

		about.enabled = legacy.skyblockEnabled();
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
