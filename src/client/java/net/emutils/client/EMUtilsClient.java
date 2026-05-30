package net.emutils.client;

import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.capes.CustomCapeManager;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.debug.DebugGuiDumpTrigger;
import net.emutils.client.emutils.debug.DebugGuiDumper;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.emutils.client.emutils.waypoint.WaypointRenderer;
import net.emutils.client.emutils.waypoint.gui.AddWaypointScreen;
import net.emutils.client.emutils.waypoint.gui.WaypointListScreen;
import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.minescript.gui.ScriptManagerScreen;
import net.emutils.client.emutils.screenshot.gui.ScreenshotGalleryScreen;
import net.emutils.client.emhelpers.hud.HudOverlayRenderer;
import net.emutils.client.emhelpers.hud.InfoOverlayHudElement;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emhelpers.hud.layout.HudLayoutMigration;
import net.emutils.client.emhelpers.hud.layout.HudLayoutRegistry;
import net.emutils.client.emutils.inventory.InventoryPreviewHudElement;
import net.emutils.client.emutils.inventory.InventoryToolsManager;
import net.emutils.client.emutils.minescript.MinescriptKeybindManager;
import net.emutils.client.emutils.reconnect.AutoReconnectManager;
import net.emutils.client.emskyblock.features.gui.statshud.SkyblockActionBarManager;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.context.SkyblockEvent;
import net.emutils.client.emskyblock.context.SkyblockManager;
import net.emutils.client.emskyblock.pricing.SkyblockPrices;
import net.emutils.client.emskyblock.features.gui.statshud.SkyblockStatsHudElement;
import net.emutils.client.emskyblock.features.inventory.storagepreview.StoragePreviewManager;
import net.emutils.client.emskyblock.features.inventory.storagepreview.StoragePreviewTooltipComponent;
import net.emutils.client.emskyblock.features.inventory.storagepreview.StoragePreviewTooltipData;
import net.emutils.client.emskyblock.pricing.auction.AuctionPriceFetcher;
import net.emutils.client.emskyblock.pricing.bazaar.BazaarPriceFetcher;
import net.emutils.client.emskyblock.api.SkyblockApiContext;
import net.emutils.client.emskyblock.api.SkyblockApiManager;
import net.emutils.client.emskyblock.config.EMSkyblockConfig;
import net.emutils.client.emskyblock.config.EMSkyblockConfigManager;
import net.emutils.client.emskyblock.pricing.npc.NpcPriceFetcher;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EivEssenceCosts;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EstimatedItemValueData;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EstimatedItemValueHudElement;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EstimatedItemValueManager;
import net.emutils.client.emskyblock.features.fishing.hookdisplay.FishingHookDisplayManager;
import net.emutils.client.emskyblock.features.fishing.hookdisplay.FishingHookHudElement;
import net.emutils.client.emskyblock.features.fishing.common.FishingInventoryPickupTracker;
import net.emutils.client.emskyblock.features.fishing.profittracker.FishingProfitItemRegistry;
import net.emutils.client.emskyblock.features.fishing.profittracker.FishingProfitTrackerManager;
import net.emutils.client.emskyblock.features.fishing.profittracker.FishingProfitTrackerHudElement;
import net.emutils.client.emskyblock.features.fishing.seacreaturetracker.SeaCreatureRegistry;
import net.emutils.client.emskyblock.features.fishing.seacreaturetracker.SeaCreatureTrackerHudElement;
import net.emutils.client.emskyblock.sacks.SkyblockSackTracker;
import net.emutils.client.emskyblock.features.fishing.trackercommon.FishingTrackerStorage;
import net.emutils.client.emutils.spotify.SpotifyHudElement;
import net.emutils.client.emutils.spotify.SpotifyPlaybackService;
import net.emutils.client.emutils.tweaks.TweaksManager;
import net.emutils.client.emutils.zoom.ZoomManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.emutils.client.emutils.tweaks.ShulkerTooltipComponent;
import net.emutils.client.emutils.tweaks.ShulkerTooltipData;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMUtilsClient implements ClientModInitializer {
	public static final String MOD_ID = "emutils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static EMUtilsConfig config;
	private static AutoReconnectManager autoReconnectManager;
	private static WaypointManager waypointManager;
	private static ZoomManager zoomManager;
	private static TweaksManager tweaksManager;
	private static SpotifyPlaybackService spotifyPlaybackService;
	private static InventoryToolsManager inventoryToolsManager;
	private static StoragePreviewManager storagePreviewManager;
	private static SkyblockManager skyblockManager;
	private static SkyblockActionBarManager skyblockActionBarManager;
	private static SkyblockApiManager skyblockApiManager;
	private static SkyblockPrices skyblockPrices;
	private static MinescriptKeybindManager minescriptKeybindManager;
	private static KeyBinding openGalleryKeyBinding;
	private static KeyBinding openScriptManagerKeyBinding;
	private static KeyBinding openSettingsHubKeyBinding;
	private static KeyBinding openHudLayoutEditorKeyBinding;
	private static KeyBinding openWaypointsKeyBinding;
	private static KeyBinding addWaypointKeyBinding;
	private static KeyBinding debugDumpGuiKeyBinding;

	@Override
	public void onInitializeClient() {
		config = EMUtilsConfig.load();
		EMSkyblockConfigManager.init(config);
		HudLayoutMigration.migrateIfNeeded(config);
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EMSkyblockConfigManager.saveIfDirty());
		autoReconnectManager = new AutoReconnectManager();
		waypointManager = new WaypointManager();
		zoomManager = new ZoomManager();
		tweaksManager = new TweaksManager();
		spotifyPlaybackService = new SpotifyPlaybackService();
		inventoryToolsManager = new InventoryToolsManager();
		skyblockManager = new SkyblockManager();
		SkyblockContext.bind(skyblockManager);
		storagePreviewManager = new StoragePreviewManager();
		skyblockActionBarManager = new SkyblockActionBarManager();
		skyblockApiManager = new SkyblockApiManager();
		SkyblockApiContext.bind(skyblockApiManager);
		skyblockPrices = new SkyblockPrices(skyblockApiManager);
		skyblockManager.events().addListener(event -> {
			if (event instanceof SkyblockEvent.ProfileJoin || event instanceof SkyblockEvent.IslandJoin) {
				skyblockPrices.fetchNow(MinecraftClient.getInstance());
			}
		});
		minescriptKeybindManager = new MinescriptKeybindManager();
		registerKeyBindings();
		registerTooltipComponents();

		ClientTickEvents.END_CLIENT_TICK.register(EMUtilsClient::tickClient);
		WaypointRenderer.register();
		EstimatedItemValueData.load();
		EivEssenceCosts.load();
		SeaCreatureRegistry.load();
		FishingProfitItemRegistry.load();
		SkyblockSackTracker.addListener(FishingProfitTrackerManager::onSackChange);
		registerHudLayoutElements();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			autoReconnectManager.captureCurrentServer(client);
			CustomCapeManager.clear();
			if (client.player != null) {
				CustomCapeManager.onLoadTexture(client.player.getGameProfile());
			}
			inventoryToolsManager.onWorldJoin(client);
			skyblockManager.onWorldJoin(client);
			storagePreviewManager.onWorldJoin(client);
			skyblockApiManager.fetchNow(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			inventoryToolsManager.onWorldLeave(client);
			skyblockManager.onWorldLeave(client);
			skyblockActionBarManager.clear();
			storagePreviewManager.onWorldLeave(client);
			EstimatedItemValueManager.get().clear();
			FishingHookDisplayManager.clear();
			net.emutils.client.emskyblock.features.fishing.common.FishingActivity.clear();
			skyblockApiManager.clear();
			autoReconnectManager.onDisconnected();
		});

		String version = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");

		LOGGER.info("EMUtils {} loaded.", version);
	}

	private static void tickClient(MinecraftClient client) {
		MinescriptCompat.tickJobs();
		minescriptKeybindManager.tick(client);
		handleKeyBindings(client);
		autoReconnectManager.tick(client);
		waypointManager.tick(client);
		zoomManager.tick(client);
		tweaksManager.tick(client);
		inventoryToolsManager.tick(client);
		skyblockManager.tick(client);
		storagePreviewManager.tick(client);
		skyblockApiManager.tick(client);
		EstimatedItemValueManager.get().tick();
		FishingHookDisplayManager.tick(client);
		FishingTrackerStorage.onProfileScopeChanged(client);
		FishingInventoryPickupTracker.tick(client);
		HudOverlayRenderer.tick(client);
		tickSpotify(client);
	}

	private static void tickSpotify(MinecraftClient client) {
		EMUtilsConfig config = config();
		if (config == null) {
			return;
		}

		boolean pauseMenu = client.currentScreen instanceof GameMenuScreen && config.spotifyPlayerEnabled();
		boolean hud = config.spotifyHudOverlay() && client.player != null && client.world != null;
		if (pauseMenu || hud) {
			spotifyPlaybackService.tick(true);
		}
	}

	private static void registerTooltipComponents() {
		TooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof ShulkerTooltipData shulkerData) {
				return new ShulkerTooltipComponent(shulkerData);
			}

			if (data instanceof StoragePreviewTooltipData storagePreviewData) {
				return new StoragePreviewTooltipComponent(storagePreviewData);
			}

			return null;
		});
	}

	private static void registerHudLayoutElements() {
		HudLayoutRegistry.register(new InfoOverlayHudElement());
		HudLayoutRegistry.register(new SpotifyHudElement());
		HudLayoutRegistry.register(new InventoryPreviewHudElement());
		HudLayoutRegistry.register(new SkyblockStatsHudElement());
		HudLayoutRegistry.register(new EstimatedItemValueHudElement());
		HudLayoutRegistry.register(new FishingHookHudElement());
		HudLayoutRegistry.register(new SeaCreatureTrackerHudElement());
		HudLayoutRegistry.register(new FishingProfitTrackerHudElement());
	}

	private static void registerKeyBindings() {
		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "general"));
		zoomManager.setKeyBinding(KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.zoom",
			InputUtil.Type.KEYSYM,
			InputUtil.GLFW_KEY_C,
			category
		)));
		KeyBinding freelookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.freelook",
			InputUtil.Type.KEYSYM,
			InputUtil.GLFW_KEY_LEFT_ALT,
			category
		));
		openSettingsHubKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.open_settings_hub",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			category
		));
		openWaypointsKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.open_waypoints",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			category
		));
		addWaypointKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.add_waypoint",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			category
		));
		openGalleryKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.open_gallery",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			category
		));
		openScriptManagerKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.open_script_manager",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			category
		));
		openHudLayoutEditorKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.open_hud_layout_editor",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			category
		));
		KeyBinding slotLockKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.slot_lock",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			category
		));
		KeyBinding slotBindKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.slot_bind",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			category
		));

		KeyBinding.Category debugCategory = KeyBinding.Category.create(Identifier.of(MOD_ID, "debug"));
		debugDumpGuiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.debug_dump_gui",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			debugCategory
		));

		tweaksManager.setKeyBindings(freelookKey);
		inventoryToolsManager.setKeyBindings(slotLockKey, slotBindKey);
	}

	private static void handleKeyBindings(MinecraftClient client) {
		while (openGalleryKeyBinding != null && openGalleryKeyBinding.wasPressed()) {
			if (!(client.currentScreen instanceof ScreenshotGalleryScreen)) {
				client.setScreen(new ScreenshotGalleryScreen(client.currentScreen));
			}
		}
		while (openScriptManagerKeyBinding != null && openScriptManagerKeyBinding.wasPressed()) {
			if (MinescriptCompat.isLoaded() && !(client.currentScreen instanceof ScriptManagerScreen)) {
				client.setScreen(new ScriptManagerScreen(client.currentScreen));
			}
		}
		while (openSettingsHubKeyBinding != null && openSettingsHubKeyBinding.wasPressed()) {
			if (!(client.currentScreen instanceof CustomHubScreen)) {
				client.setScreen(new CustomHubScreen(client.currentScreen));
			}
		}
		while (openWaypointsKeyBinding != null && openWaypointsKeyBinding.wasPressed()) {
			if (!(client.currentScreen instanceof WaypointListScreen)) {
				client.setScreen(new WaypointListScreen(client.currentScreen));
			}
		}
		while (addWaypointKeyBinding != null && addWaypointKeyBinding.wasPressed()) {
			if (!(client.currentScreen instanceof AddWaypointScreen)) {
				client.setScreen(new AddWaypointScreen(client.currentScreen));
			}
		}
		while (openHudLayoutEditorKeyBinding != null && openHudLayoutEditorKeyBinding.wasPressed()) {
			HudLayoutManager.openEditor(client);
		}
		DebugGuiDumpTrigger.tryFromBinding(debugDumpGuiKeyBinding);
	}

	public static boolean tryDebugGuiDump(KeyInput input) {
		return DebugGuiDumpTrigger.tryFromInput(debugDumpGuiKeyBinding, input);
	}

	public static EMUtilsConfig config() {
		return config;
	}

	public static EMSkyblockConfig emSkyblockConfig() {
		return EMSkyblockConfigManager.config();
	}

	public static AutoReconnectManager autoReconnect() {
		return autoReconnectManager;
	}

	public static WaypointManager waypoint() {
		return waypointManager;
	}

	public static ZoomManager zoom() {
		return zoomManager;
	}

	public static TweaksManager tweaks() {
		return tweaksManager;
	}

	public static SpotifyPlaybackService spotify() {
		return spotifyPlaybackService;
	}

	public static InventoryToolsManager inventoryTools() {
		return inventoryToolsManager;
	}

	public static StoragePreviewManager storagePreview() {
		return storagePreviewManager;
	}

	public static SkyblockManager skyblock() {
		return skyblockManager;
	}

	public static SkyblockActionBarManager skyblockActionBar() {
		return skyblockActionBarManager;
	}

	public static SkyblockApiManager skyblockApis() {
		return skyblockApiManager;
	}

	public static SkyblockPrices skyblockPrices() {
		return skyblockPrices;
	}

	public static BazaarPriceFetcher bazaarPrices() {
		return skyblockPrices.bazaar();
	}

	public static AuctionPriceFetcher auctionPrices() {
		return skyblockPrices.auction();
	}

	public static NpcPriceFetcher npcPrices() {
		return skyblockPrices.npc();
	}

	public static MinescriptKeybindManager minescriptKeybinds() {
		return minescriptKeybindManager;
	}
}
