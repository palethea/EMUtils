package net.emutils.client.emutils.screenshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ScreenshotMetadataSaver {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ScreenshotMetadataSaver() {
	}

	public static void trySave(File screenshot) {
		if (EMUtilsClient.config() == null || !EMUtilsClient.config().screenshotMetadataSaver()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null || client.world == null) {
			return;
		}

		try {
			Path metadataDir = screenshot.toPath().resolveSibling("metadata");
			Files.createDirectories(metadataDir);
			Path metadataPath = metadataDir.resolve(screenshot.getName() + ".json");
			Files.writeString(metadataPath, GSON.toJson(metadata(client, screenshot)));
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to save screenshot metadata.", exception);
		}
	}

	private static JsonObject metadata(MinecraftClient client, File screenshot) {
		BlockPos pos = client.player.getBlockPos();
		JsonObject root = new JsonObject();
		root.addProperty("schema", 1);
		root.addProperty("screenshot", screenshot.getName());
		root.addProperty("capturedAt", Instant.now().toString());
		root.addProperty("world", worldKey(client));
		root.addProperty("dimension", dimensionId(client));
		root.addProperty("biome", biomeId(client, pos));
		root.addProperty("facing", client.player.getHorizontalFacing().asString());

		JsonObject position = new JsonObject();
		position.addProperty("x", pos.getX());
		position.addProperty("y", pos.getY());
		position.addProperty("z", pos.getZ());
		root.add("position", position);
		return root;
	}

	private static String dimensionId(MinecraftClient client) {
		RegistryKey<World> key = client.world.getRegistryKey();
		return key.getValue().toString();
	}

	private static String biomeId(MinecraftClient client, BlockPos pos) {
		return client.world.getBiome(pos)
			.getKey()
			.map(RegistryKey::getValue)
			.map(Object::toString)
			.orElse("");
	}

	private static String worldKey(MinecraftClient client) {
		ServerInfo serverInfo = client.getCurrentServerEntry();
		if (serverInfo != null && serverInfo.address != null && !serverInfo.address.isBlank()) {
			return "multiplayer:" + serverInfo.address;
		}

		if (client.isIntegratedServerRunning()) {
			var server = client.getServer();
			if (server != null) {
				return "singleplayer:" + server.getSaveProperties().getLevelName();
			}
		}

		return client.world.getRegistryKey().getValue().toString().toLowerCase(Locale.ENGLISH);
	}
}
