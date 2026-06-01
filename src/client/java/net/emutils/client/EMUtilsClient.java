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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
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
	private static CommandShortcutsManager commandShortcutsManager;
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
		EMHelpers.configure(MOD_ID, EMUtilsClient::config, () -> zoomManager != null && zoomManager.shouldHideHud());
		HudLayoutMigration.migrateIfNeeded(config);
		autoReconnectManager = new AutoReconnectManager();
		waypointManager = new WaypointManager();
		zoomManager = new ZoomManager();
		tweaksManager = new TweaksManager();
		spotifyPlaybackService = new SpotifyPlaybackService();
		inventoryToolsManager = new InventoryToolsManager();
		commandShortcutsManager = new CommandShortcutsManager();
		minescriptKeybindManager = new MinescriptKeybindManager();
		registerKeyBindings();
		registerTooltipComponents();

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
		commandShortcutsManager.tick(client);
		handleKeyBindings(client);
		autoReconnectManager.tick(client);
		waypointManager.tick(client);
		zoomManager.tick(client);
		tweaksManager.tick(client);
		inventoryToolsManager.tick(client);
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

			return null;
		});
	}

	private static void registerHudLayoutElements() {
		HudLayoutRegistry.register(MOD_ID, new InfoOverlayHudElement());
		HudLayoutRegistry.register(MOD_ID, new SpotifyHudElement());
		HudLayoutRegistry.register(MOD_ID, new InventoryPreviewHudElement());
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
			openHudLayoutEditor(client);
		}
		DebugGuiDumpTrigger.tryFromBinding(debugDumpGuiKeyBinding);
	}

	public static boolean tryDebugGuiDump(KeyInput input) {
		return DebugGuiDumpTrigger.tryFromInput(debugDumpGuiKeyBinding, input);
	}

	public static boolean tryOpenHudLayoutEditor(KeyInput input) {
		if (openHudLayoutEditorKeyBinding == null || !openHudLayoutEditorKeyBinding.matchesKey(input)) {
			return false;
		}

		openHudLayoutEditor(MinecraftClient.getInstance());
		return true;
	}

	private static void openHudLayoutEditor(MinecraftClient client) {
		if (client != null && client.currentScreen instanceof HandledScreen<?>) {
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

	public static CommandShortcutsManager commandShortcuts() {
		return commandShortcutsManager;
	}

	public static MinescriptKeybindManager minescriptKeybinds() {
		return minescriptKeybindManager;
	}
}
