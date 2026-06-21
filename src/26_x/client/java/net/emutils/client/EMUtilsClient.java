package net.emutils.client;

import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.capes.CustomCapeManager;
import net.emutils.client.emutils.commandshortcuts.CommandShortcutsManager;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.debug.DebugGuiDumpTrigger;
import net.emutils.client.emutils.debug.DebugGuiDumper;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.emutils.client.emutils.waypoint.WaypointRenderer;
import net.emutils.client.emutils.waypoint.gui.AddWaypointScreen;
import net.emutils.client.emutils.waypoint.gui.WaypointListScreen;
import net.emutils.client.emutils.food.FoodHudRenderer;
import net.emutils.client.emutils.food.FoodTooltipComponent;
import net.emutils.client.emutils.food.FoodTooltipData;
import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.minescript.gui.ScriptManagerScreen;
import net.emutils.client.emutils.screenshot.gui.ScreenshotGalleryScreen;
import net.emutils.client.emutils.hud.HudOverlayRenderer;
import net.emhelpers.client.hud.editor.HudLayoutEditorOverlay;
import net.emutils.client.emutils.hud.InfoOverlayHudElement;
import net.emhelpers.client.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.hud.layout.HudLayoutMigration;
import net.emhelpers.client.hud.layout.HudLayoutRegistry;
import net.emutils.client.emutils.inventory.InventoryPreviewHudElement;
import net.emutils.client.emutils.inventory.InventoryToolsManager;
import net.emutils.client.emutils.inventory.MassDropManager;
import net.emutils.client.emutils.minescript.MinescriptKeybindManager;
import net.emutils.client.emutils.reconnect.AutoReconnectManager;
import net.emutils.client.emutils.spotify.SpotifyHudElement;
import net.emutils.client.emutils.spotify.SpotifyPlaybackService;
import net.emutils.client.emutils.tweaks.TweaksManager;
import net.emutils.client.emutils.zoom.ZoomManager;
import net.emhelpers.client.EMHelpers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.emutils.client.emutils.tweaks.ShulkerTooltipComponent;
import net.emutils.client.emutils.tweaks.ShulkerTooltipData;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
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
	private static MassDropManager massDropManager;
	private static CommandShortcutsManager commandShortcutsManager;
	private static MinescriptKeybindManager minescriptKeybindManager;
	private static KeyMapping openGalleryKeyMapping;
	private static KeyMapping openScriptManagerKeyMapping;
	private static KeyMapping openSettingsHubKeyMapping;
	private static KeyMapping openHudLayoutEditorKeyMapping;
	private static KeyMapping openWaypointsKeyMapping;
	private static KeyMapping addWaypointKeyMapping;
	private static KeyMapping massDropKeyMapping;
	private static KeyMapping debugDumpGuiKeyMapping;

	@Override
	public void onInitializeClient() {
		EMHelpers.registerTranslationPrefix(MOD_ID);
		config = EMUtilsConfig.load();
		EMHelpers.configure(MOD_ID, EMUtilsClient::config, () -> zoomManager != null && zoomManager.shouldHideHud());
		HudLayoutMigration.migrateIfNeeded(config);
		autoReconnectManager = new AutoReconnectManager();
		waypointManager = new WaypointManager();
		zoomManager = new ZoomManager();
		tweaksManager = new TweaksManager();
		spotifyPlaybackService = new SpotifyPlaybackService();
		inventoryToolsManager = new InventoryToolsManager();
		massDropManager = new MassDropManager();
		commandShortcutsManager = new CommandShortcutsManager();
		minescriptKeybindManager = new MinescriptKeybindManager();
		registerKeyMappings();
		registerTooltipComponents();

		ClientTickEvents.START_CLIENT_TICK.register(client -> tweaksManager.tickAutoTool(client));
		ClientTickEvents.END_CLIENT_TICK.register(EMUtilsClient::tickClient);
		WaypointRenderer.register();
		registerHudLayoutElements();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			autoReconnectManager.captureCurrentServer(client);
			CustomCapeManager.clear();
			if (client.player != null) {
				CustomCapeManager.onLoadTexture(client.player.getGameProfile());
			}
			inventoryToolsManager.onWorldJoin(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			inventoryToolsManager.onWorldLeave(client);
			tweaksManager.resetSession();
			autoReconnectManager.onDisconnected();
		});

		String version = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");

		LOGGER.info("EMUtils {} loaded.", version);
	}

	private static void tickClient(Minecraft client) {
		MinescriptCompat.tickJobs();
		minescriptKeybindManager.tick(client);
		commandShortcutsManager.tick(client);
		handleKeyMappings(client);
		autoReconnectManager.tick(client);
		waypointManager.tick(client);
		zoomManager.tick(client);
		tweaksManager.tick(client);
		inventoryToolsManager.tick(client);
		FoodHudRenderer.tick(client);
		HudOverlayRenderer.tick(client);
		tickSpotify(client);
	}

	private static void tickSpotify(Minecraft client) {
		EMUtilsConfig config = config();
		if (config == null) {
			return;
		}

		boolean pauseMenu = client.gui.screen() instanceof PauseScreen && config.spotifyPlayerEnabled();
		boolean hud = config.spotifyHudOverlay() && client.player != null && client.level != null;
		if (pauseMenu || hud) {
			spotifyPlaybackService.tick(true);
		}
	}

	private static void registerTooltipComponents() {
		ClientTooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof ShulkerTooltipData shulkerData) {
				return new ShulkerTooltipComponent(shulkerData);
			}
			if (data instanceof FoodTooltipData foodData) {
				return new FoodTooltipComponent(foodData);
			}

			return null;
		});
	}

	private static void registerHudLayoutElements() {
		HudLayoutRegistry.register(MOD_ID, new InfoOverlayHudElement());
		HudLayoutRegistry.register(MOD_ID, new SpotifyHudElement());
		HudLayoutRegistry.register(MOD_ID, new InventoryPreviewHudElement());
	}

	private static void registerKeyMappings() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "general"));
		zoomManager.setKeyMapping(KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.zoom",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_C,
			category
		)));
		KeyMapping freelookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.freelook",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_LALT,
			category
		));
		openSettingsHubKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_settings_hub",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));
		openWaypointsKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_waypoints",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));
		addWaypointKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.add_waypoint",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));
		openGalleryKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_gallery",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));
		openScriptManagerKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_script_manager",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));
		openHudLayoutEditorKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_hud_layout_editor",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping slotLockKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.slot_lock",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping slotBindKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.slot_bind",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping quickStackKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.quick_stack",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));
		massDropKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.mass_drop",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));

		KeyMapping.Category debugCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "debug"));
		debugDumpGuiKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.debug_dump_gui",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			debugCategory
		));

		tweaksManager.setKeyMappings(freelookKey);
		inventoryToolsManager.setKeyMappings(slotLockKey, slotBindKey, quickStackKey);
	}

	private static void handleKeyMappings(Minecraft client) {
		while (openGalleryKeyMapping != null && openGalleryKeyMapping.consumeClick()) {
			if (!(client.gui.screen() instanceof ScreenshotGalleryScreen)) {
				client.setScreenAndShow(new ScreenshotGalleryScreen(client.gui.screen()));
			}
		}
		while (openScriptManagerKeyMapping != null && openScriptManagerKeyMapping.consumeClick()) {
			if (MinescriptCompat.isLoaded() && !(client.gui.screen() instanceof ScriptManagerScreen)) {
				client.setScreenAndShow(new ScriptManagerScreen(client.gui.screen()));
			}
		}
		while (openSettingsHubKeyMapping != null && openSettingsHubKeyMapping.consumeClick()) {
			if (!(client.gui.screen() instanceof CustomHubScreen)) {
				client.setScreenAndShow(new CustomHubScreen(client.gui.screen()));
			}
		}
		while (openWaypointsKeyMapping != null && openWaypointsKeyMapping.consumeClick()) {
			if (!(client.gui.screen() instanceof WaypointListScreen)) {
				client.setScreenAndShow(new WaypointListScreen(client.gui.screen()));
			}
		}
		while (addWaypointKeyMapping != null && addWaypointKeyMapping.consumeClick()) {
			if (!(client.gui.screen() instanceof AddWaypointScreen)) {
				client.setScreenAndShow(new AddWaypointScreen(client.gui.screen()));
			}
		}
		while (openHudLayoutEditorKeyMapping != null && openHudLayoutEditorKeyMapping.consumeClick()) {
			openHudLayoutEditor(client);
		}
		while (massDropKeyMapping != null && massDropKeyMapping.consumeClick()) {
			massDropManager.dropSelected(client);
		}
		DebugGuiDumpTrigger.tryFromBinding(debugDumpGuiKeyMapping);
	}

	public static boolean tryDebugGuiDump(KeyEvent input) {
		return DebugGuiDumpTrigger.tryFromInput(debugDumpGuiKeyMapping, input);
	}

	public static boolean tryOpenHudLayoutEditor(KeyEvent input) {
		if (openHudLayoutEditorKeyMapping == null || !openHudLayoutEditorKeyMapping.matches(input)) {
			return false;
		}

		openHudLayoutEditor(Minecraft.getInstance());
		return true;
	}

	private static void openHudLayoutEditor(Minecraft client) {
		if (client != null && client.gui.screen() instanceof AbstractContainerScreen<?>) {
			HudLayoutEditorOverlay.open(MOD_ID, client);
			return;
		}

		HudLayoutManager.openEditor(MOD_ID, client);
	}

	public static EMUtilsConfig config() {
		return config;
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

	public static MassDropManager massDrop() {
		return massDropManager;
	}

	public static CommandShortcutsManager commandShortcuts() {
		return commandShortcutsManager;
	}

	public static MinescriptKeybindManager minescriptKeybinds() {
		return minescriptKeybindManager;
	}
}
