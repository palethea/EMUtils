package net.emutils.client;

import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.capes.CustomCapeManager;
import net.emutils.client.emutils.commandshortcuts.CommandShortcutsManager;
import net.emutils.client.emutils.command.EMUtilsCommand;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.profile.Profile;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.network.chat.Component;
import net.emutils.client.emutils.profile.ProfileManager;
import net.emutils.client.emutils.debug.DebugGuiDumpTrigger;
import net.emutils.client.emutils.debug.DebugGuiDumper;
import net.emutils.client.emutils.debug.BackgroundLaunch;
import net.emutils.client.emutils.debug.SmokeLaunchVerifier;
import net.emutils.client.emutils.debug.UiSnapshotter;
import net.emutils.client.emutils.waypoint.WaypointManager;
import net.emutils.client.emutils.waypoint.WaypointRenderer;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.emutils.client.emutils.food.FoodHudRenderer;
import net.emutils.client.emutils.food.FoodTooltipComponent;
import net.emutils.client.emutils.food.FoodTooltipData;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.gui.settings.SettingsWarmup;
import net.emutils.client.emutils.gui.ui.UiClosingScreens;
import net.emutils.client.emutils.gui.ui.UiFontRenderer;
import net.emutils.client.emutils.minescript.gui.ScriptsScreen;
import net.emutils.client.emutils.screenshot.gui.GalleryScreen;
import net.emutils.client.emutils.screenshot.gui.GalleryThumbnails;
import net.emutils.client.emutils.hud.HudOverlayRenderer;
import net.emutils.client.emutils.hud.editor.HudLayoutEditorOverlay;
import net.emutils.client.emutils.hud.InfoOverlayHudElement;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.hud.layout.HudLayoutMigration;
import net.emutils.client.emutils.hud.layout.HudLayoutRegistry;
import net.emutils.client.emutils.inventory.InventoryPreviewHudElement;
import net.emutils.client.emutils.inventory.InventoryToolsManager;
import net.emutils.client.emutils.inventory.MassDropManager;
import net.emutils.client.emutils.minescript.MinescriptKeybindManager;
import net.emutils.client.emutils.reconnect.AutoReconnectManager;
import net.emutils.client.emutils.render.BeaconRadiusRenderer;
import net.emutils.client.emutils.render.LightLevelOverlayRenderer;
import net.emutils.client.emutils.spotify.SpotifyHudElement;
import net.emutils.client.emutils.spotify.SpotifyPlaybackService;
import net.emutils.client.emutils.tweaks.TweaksManager;
import net.emutils.client.emutils.zoom.ZoomManager;
import net.emutils.client.versioned.VersionedInput;
import net.emutils.client.emutils.EMHelpers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.emutils.client.emutils.tweaks.ShulkerTooltipComponent;
import net.emutils.client.emutils.tweaks.ShulkerTooltipData;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
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
	private static ProfileManager profileManager;
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
	private static KeyMapping nextProfileKeyMapping;

	@Override
	public void onInitializeClient() {
		EMHelpers.registerTranslationPrefix(MOD_ID);
		profileManager = ProfileManager.load();
		config = profileManager.loadActive();
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
		EMUtilsCommand.register();
		SmokeLaunchVerifier.registerIfEnabled();

		ClientTickEvents.START_CLIENT_TICK.register(client -> tweaksManager.tickAutoTool(client));
		ClientTickEvents.END_CLIENT_TICK.register(EMUtilsClient::tickClient);
		WaypointRenderer.register();
		BeaconRadiusRenderer.register();
		LightLevelOverlayRenderer.register();
		registerHudLayoutElements();
		UiClosingScreens.register();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			autoReconnectManager.captureCurrentServer(client);
			CustomCapeManager.clear();
			if (client.player != null) {
				CustomCapeManager.onLoadTexture(client.player.getGameProfile());
			}
			inventoryToolsManager.onWorldJoin(client);
			profileManager.onJoin(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			inventoryToolsManager.onWorldLeave(client);
			tweaksManager.resetSession();
			autoReconnectManager.onDisconnected();
			GalleryThumbnails.freeShared();
			// Disconnects arrive on the network thread; write on the render thread, which changes the config.
			client.execute(EMUtilsClient::flushConfig);
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> flushConfig());

		String version = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");

		LOGGER.info("EMUtils {} loaded.", version);
	}

	private static void tickClient(Minecraft client) {
		SettingsWarmup.tick(client);
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
		BackgroundLaunch.tick(client);
		UiFontRenderer.freeReleased();
		if (config != null) {
			config.flushIfDue();
		}
		SmokeLaunchVerifier.tick(client);
		UiSnapshotter.tick(client);
	}

	private static void tickSpotify(Minecraft client) {
		EMUtilsConfig config = config();
		if (config == null) {
			return;
		}

		if (!config.spotifyEnabled()) {
			return;
		}
		boolean pauseMenu = net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) instanceof PauseScreen && config.spotifyPlayerEnabled();
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
			VersionedInput.keyboardType(),
			InputConstants.KEY_C,
			category
		)));
		KeyMapping freelookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.freelook",
			VersionedInput.keyboardType(),
			InputConstants.KEY_LALT,
			category
		));
		openSettingsHubKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_settings_hub",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		openWaypointsKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_waypoints",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		addWaypointKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.add_waypoint",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		openGalleryKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_gallery",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		openScriptManagerKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_script_manager",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		openHudLayoutEditorKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.open_hud_layout_editor",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping slotLockKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.slot_lock",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping slotBindKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.slot_bind",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping quickStackKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.quick_stack",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping placeBelowKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.place_below",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping lockedYPlacementKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.locked_y_placement",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping freeCameraKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.free_camera",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping beaconRadiusKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.beacon_radius_outline",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		KeyMapping lightLevelOverlayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.light_level_overlay",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));
		massDropKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.mass_drop",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));

		nextProfileKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.next_profile",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			category
		));

		KeyMapping.Category debugCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "debug"));
		debugDumpGuiKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.emutils.debug_dump_gui",
			VersionedInput.keyboardType(),
			InputConstants.UNKNOWN.getValue(),
			debugCategory
		));

		tweaksManager.setKeyMappings(freelookKey, placeBelowKey, lockedYPlacementKey, freeCameraKey);
		BeaconRadiusRenderer.setKeyMapping(beaconRadiusKey);
		LightLevelOverlayRenderer.setKeyMapping(lightLevelOverlayKey);
		inventoryToolsManager.setKeyMappings(slotLockKey, slotBindKey, quickStackKey);
	}

	private static void handleKeyMappings(Minecraft client) {
		while (nextProfileKeyMapping != null && nextProfileKeyMapping.consumeClick()) {
			if (profileManager.profiles().size() < 2) {
				continue;
			}
			Profile next = profileManager.pickNext();
			if (next == null) {
				ProfileManager.reportSwitchFailed(client);
			} else if (client.player != null) {
				client.player.sendSystemMessage(EmUtilsChatPrefix.chat(Component.translatable(EMUtilsTexts.PROFILE_SWITCHED, next.name())));
			}
		}
		while (openGalleryKeyMapping != null && openGalleryKeyMapping.consumeClick()) {
			Screen current = net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client);
			if (!(current instanceof GalleryScreen)) {
				client.gui.setScreen(new GalleryScreen(current));
			}
		}
		while (openScriptManagerKeyMapping != null && openScriptManagerKeyMapping.consumeClick()) {
			Screen current = net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client);
			if (MinescriptCompat.isLoaded() && !(current instanceof ScriptsScreen)) {
				client.gui.setScreen(new ScriptsScreen(current));
			}
		}
		while (openSettingsHubKeyMapping != null && openSettingsHubKeyMapping.consumeClick()) {
			if (!(net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) instanceof SettingsScreen)) {
				client.gui.setScreen(new SettingsScreen(net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client)));
			}
		}
		while (openWaypointsKeyMapping != null && openWaypointsKeyMapping.consumeClick()) {
			Screen current = net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client);
			if (!(current instanceof WaypointsScreen)) {
				client.gui.setScreen(new WaypointsScreen(current));
			}
		}
		while (addWaypointKeyMapping != null && addWaypointKeyMapping.consumeClick()) {
			Screen current = net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client);
			if (!(current instanceof WaypointsScreen)) {
				client.gui.setScreen(WaypointsScreen.addWaypoint(current));
			}
		}
		while (openHudLayoutEditorKeyMapping != null && openHudLayoutEditorKeyMapping.consumeClick()) {
			openHudLayoutEditor(client);
		}
		while (massDropKeyMapping != null && massDropKeyMapping.consumeClick()) {
			massDropManager.dropSelected(client);
		}
		BeaconRadiusRenderer.tick();
		LightLevelOverlayRenderer.tick();
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
		if (client != null && net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) instanceof AbstractContainerScreen<?>) {
			HudLayoutEditorOverlay.open(MOD_ID, client);
			return;
		}

		HudLayoutManager.openEditor(MOD_ID, client);
	}

	/** Writes pending config changes now instead of waiting for the save delay. */
	private static void flushConfig() {
		if (config != null) {
			config.flush();
		}
	}

	public static void replaceConfig(EMUtilsConfig next) {
		config = next;
		next.applyRuntimeState();
	}

	public static EMUtilsConfig config() {
		return config;
	}

	public static ProfileManager profiles() {
		return profileManager;
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
