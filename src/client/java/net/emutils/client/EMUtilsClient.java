package net.emutils.client;

import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.reconnect.AutoReconnectManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMUtilsClient implements ClientModInitializer {
	public static final String MOD_ID = "emutils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static EMUtilsConfig config;
	private static AutoReconnectManager autoReconnectManager;

	@Override
	public void onInitializeClient() {
		config = EMUtilsConfig.load();
		autoReconnectManager = new AutoReconnectManager();

		ClientTickEvents.END_CLIENT_TICK.register(EMUtilsClient::tickClient);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> autoReconnectManager.captureCurrentServer(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> autoReconnectManager.onDisconnected());

		String version = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");

		LOGGER.info("EMUtils {} loaded.", version);
	}

	private static void tickClient(MinecraftClient client) {
		autoReconnectManager.tick(client);
	}

	public static EMUtilsConfig config() {
		return config;
	}

	public static AutoReconnectManager autoReconnect() {
		return autoReconnectManager;
	}
}
