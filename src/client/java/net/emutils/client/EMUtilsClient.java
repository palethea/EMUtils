package net.emutils.client;

import net.emutils.client.compat.MinescriptCompat;
import net.emutils.client.capes.CustomCapeManager;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.death.DeathWaypointManager;
import net.emutils.client.death.DeathWaypointRenderer;
import net.emutils.client.gui.minescript.ScriptManagerScreen;
import net.emutils.client.gui.screenshot.ScreenshotGalleryScreen;
import net.emutils.client.hud.HudOverlayRenderer;
import net.emutils.client.inventory.InventoryPreviewRenderer;
import net.emutils.client.inventory.InventoryToolsManager;
import net.emutils.client.skyblock.StoragePreviewManager;
import net.emutils.client.skyblock.StoragePreviewTooltipComponent;
import net.emutils.client.skyblock.StoragePreviewTooltipData;
import net.emutils.client.minescript.MinescriptKeybindManager;
import net.emutils.client.reconnect.AutoReconnectManager;
import net.emutils.client.spotify.SpotifyHudRenderer;
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
	private static MinescriptKeybindManager minescriptKeybindManager;
	private static KeyBinding openGalleryKeyBinding;
	private static KeyBinding openScriptManagerKeyBinding;

	@Override
	public void onInitializeClient() {
		config = EMUtilsConfig.load();
		autoReconnectManager = new AutoReconnectManager();
		deathWaypointManager = new DeathWaypointManager();
		zoomManager = new ZoomManager();
		tweaksManager = new TweaksManager();
		spotifyPlaybackService = new SpotifyPlaybackService();
		inventoryToolsManager = new InventoryToolsManager();
		storagePreviewManager = new StoragePreviewManager();
		minescriptKeybindManager = new MinescriptKeybindManager();
		registerKeyBindings();
		registerTooltipComponents();

		ClientTickEvents.END_CLIENT_TICK.register(EMUtilsClient::tickClient);
		DeathWaypointRenderer.register();
		HudOverlayRenderer.register();
		SpotifyHudRenderer.register();
		InventoryPreviewRenderer.register();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			autoReconnectManager.captureCurrentServer(client);
			CustomCapeManager.clear();
			if (client.player != null) {
				CustomCapeManager.onLoadTexture(client.player.getGameProfile());
			}
			inventoryToolsManager.onWorldJoin(client);
			storagePreviewManager.onWorldJoin(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			inventoryToolsManager.onWorldLeave(client);
			storagePreviewManager.onWorldLeave(client);
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
		storagePreviewManager.tick(client);
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
	}

	public static EMUtilsConfig config() {
		return config;
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

	public static MinescriptKeybindManager minescriptKeybinds() {
		return minescriptKeybindManager;
	}
}
