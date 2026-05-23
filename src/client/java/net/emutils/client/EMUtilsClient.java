package net.emutils.client;

import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.death.DeathWaypointManager;
import net.emutils.client.death.DeathWaypointRenderer;
import net.emutils.client.gui.screenshot.ScreenshotGalleryScreen;
import net.emutils.client.hud.HudOverlayRenderer;
import net.emutils.client.reconnect.AutoReconnectManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
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
	private static KeyBinding openGalleryKeyBinding;

	@Override
	public void onInitializeClient() {
		config = EMUtilsConfig.load();
		autoReconnectManager = new AutoReconnectManager();
		deathWaypointManager = new DeathWaypointManager();
		registerKeyBindings();

		ClientTickEvents.END_CLIENT_TICK.register(EMUtilsClient::tickClient);
		DeathWaypointRenderer.register();
		HudOverlayRenderer.register();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> autoReconnectManager.captureCurrentServer(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> autoReconnectManager.onDisconnected());

		String version = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");

		LOGGER.info("EMUtils {} loaded.", version);
	}

	private static void tickClient(MinecraftClient client) {
		handleKeyBindings(client);
		autoReconnectManager.tick(client);
		deathWaypointManager.tick(client);
		HudOverlayRenderer.tick(client);
	}

	private static void registerKeyBindings() {
		openGalleryKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emutils.open_gallery",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(),
			KeyBinding.Category.create(Identifier.of(MOD_ID, "general"))
		));
	}

	private static void handleKeyBindings(MinecraftClient client) {
		while (openGalleryKeyBinding != null && openGalleryKeyBinding.wasPressed()) {
			if (!(client.currentScreen instanceof ScreenshotGalleryScreen)) {
				client.setScreen(new ScreenshotGalleryScreen(client.currentScreen));
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
}
