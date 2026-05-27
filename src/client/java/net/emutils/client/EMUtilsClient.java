package net.emutils.client;

import net.emutils.client.compat.MinescriptCompat;
import net.emutils.client.capes.CustomCapeManager;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.debug.DebugGuiDumpTrigger;
import net.emutils.client.debug.DebugGuiDumper;
import net.emutils.client.death.DeathWaypointManager;
import net.emutils.client.death.DeathWaypointRenderer;
import net.emutils.client.gui.hub.CustomHubScreen;
import net.emutils.client.gui.minescript.ScriptManagerScreen;
import net.emutils.client.gui.screenshot.ScreenshotGalleryScreen;
import net.emutils.client.hud.HudOverlayRenderer;
import net.emutils.client.hud.InfoOverlayHudElement;
import net.emutils.client.hud.layout.HudLayoutManager;
import net.emutils.client.hud.layout.HudLayoutMigration;
import net.emutils.client.hud.layout.HudLayoutRegistry;
import net.emutils.client.inventory.InventoryPreviewHudElement;
import net.emutils.client.inventory.InventoryToolsManager;
import net.emutils.client.minescript.MinescriptKeybindManager;
import net.emutils.client.reconnect.AutoReconnectManager;
import net.emutils.client.skyblock.SkyblockActionBarManager;
import net.emutils.client.skyblock.SkyblockContext;
import net.emutils.client.skyblock.SkyblockEvent;
import net.emutils.client.skyblock.SkyblockManager;
import net.emutils.client.skyblock.SkyblockPrices;
import net.emutils.client.skyblock.SkyblockStatsHudElement;
import net.emutils.client.skyblock.StoragePreviewManager;
import net.emutils.client.skyblock.StoragePreviewTooltipComponent;
import net.emutils.client.skyblock.StoragePreviewTooltipData;
import net.emutils.client.skyblock.auction.AuctionPriceFetcher;
import net.emutils.client.skyblock.bazaar.BazaarPriceFetcher;
import net.emutils.client.skyblock.config.EMSkyblockConfig;
import net.emutils.client.skyblock.config.EMSkyblockConfigManager;
import net.emutils.client.skyblock.npc.NpcPriceFetcher;
import net.emutils.client.skyblock.eiv.EivEssenceCosts;
import net.emutils.client.skyblock.eiv.EstimatedItemValueData;
import net.emutils.client.skyblock.eiv.EstimatedItemValueHudElement;
import net.emutils.client.skyblock.eiv.EstimatedItemValueManager;
import net.emutils.client.skyblock.fishing.FishingHookDisplayManager;
import net.emutils.client.skyblock.fishing.FishingHookHudElement;
import net.emutils.client.skyblock.fishing.FishingInventoryPickupTracker;
import net.emutils.client.skyblock.fishing.tracker.profit.FishingProfitItemRegistry;
import net.emutils.client.skyblock.fishing.tracker.profit.FishingProfitTrackerManager;
import net.emutils.client.skyblock.fishing.tracker.profit.FishingProfitTrackerHudElement;
import net.emutils.client.skyblock.fishing.tracker.seacreature.SeaCreatureRegistry;
import net.emutils.client.skyblock.fishing.tracker.seacreature.SeaCreatureTrackerHudElement;
import net.emutils.client.skyblock.sacks.SkyblockSackTracker;
import net.emutils.client.skyblock.tracker.FishingTrackerStorage;
import net.emutils.client.spotify.SpotifyHudElement;
import net.emutils.client.spotify.SpotifyPlaybackService;
import net.emutils.client.tweaks.TweaksManager;
import net.emutils.client.zoom.ZoomManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.emutils.client.tweaks.ShulkerTooltipComponent;
import net.emutils.client.tweaks.ShulkerTooltipData;
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
	private static DeathWaypointManager deathWaypointManager;
	private static ZoomManager zoomManager;
	private static TweaksManager tweaksManager;
	private static SpotifyPlaybackService spotifyPlaybackService;
	private static InventoryToolsManager inventoryToolsManager;
	private static StoragePreviewManager storagePreviewManager;
	private static SkyblockManager skyblockManager;
	private static SkyblockActionBarManager skyblockActionBarManager;
	private static SkyblockPrices skyblockPrices;
	private static MinescriptKeybindManager minescriptKeybindManager;
	private static KeyBinding openGalleryKeyBinding;
	private static KeyBinding openScriptManagerKeyBinding;
	private static KeyBinding openSettingsHubKeyBinding;
	private static KeyBinding openHudLayoutEditorKeyBinding;
	private static KeyBinding debugDumpGuiKeyBinding;

	@Override
	public void onInitializeClient() {
		config = EMUtilsConfig.load();
		EMSkyblockConfigManager.init(config);
		HudLayoutMigration.migrateIfNeeded(config);
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EMSkyblockConfigManager.saveIfDirty());
		autoReconnectManager = new AutoReconnectManager();
		deathWaypointManager = new DeathWaypointManager();
		zoomManager = new ZoomManager();
		tweaksManager = new TweaksManager();
		spotifyPlaybackService = new SpotifyPlaybackService();
		inventoryToolsManager = new InventoryToolsManager();
		skyblockManager = new SkyblockManager();
		SkyblockContext.bind(skyblockManager);
		storagePreviewManager = new StoragePreviewManager();
		skyblockActionBarManager = new SkyblockActionBarManager();
		skyblockPrices = new SkyblockPrices();
		skyblockManager.events().addListener(event -> {
			if (event instanceof SkyblockEvent.ProfileJoin || event instanceof SkyblockEvent.IslandJoin) {
				skyblockPrices.fetchNow(MinecraftClient.getInstance());
			}
		});
		minescriptKeybindManager = new MinescriptKeybindManager();
		registerKeyBindings();
		registerTooltipComponents();

		ClientTickEvents.END_CLIENT_TICK.register(EMUtilsClient::tickClient);
		DeathWaypointRenderer.register();
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
			skyblockPrices.fetchNow(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			inventoryToolsManager.onWorldLeave(client);
			skyblockManager.onWorldLeave(client);
			skyblockActionBarManager.clear();
			storagePreviewManager.onWorldLeave(client);
			EstimatedItemValueManager.get().clear();
			FishingHookDisplayManager.clear();
			net.emutils.client.skyblock.fishing.FishingActivity.clear();
			skyblockPrices.clear();
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
		deathWaypointManager.tick(client);
		zoomManager.tick(client);
		tweaksManager.tick(client);
		inventoryToolsManager.tick(client);
		skyblockManager.tick(client);
		storagePreviewManager.tick(client);
		skyblockPrices.tick(client);
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
		openSettingsHubKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.open_settings_hub",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			category
		));
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
		tweaksManager.setKeyBindings(freelookKey);
		inventoryToolsManager.setKeyBindings(slotLockKey, slotBindKey);
		debugDumpGuiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.debug_dump_gui",
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

	public static DeathWaypointManager deathWaypoint() {
		return deathWaypointManager;
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
