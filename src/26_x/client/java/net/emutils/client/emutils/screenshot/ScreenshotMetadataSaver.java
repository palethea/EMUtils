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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ScreenshotMetadataSaver {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ScreenshotMetadataSaver() {
	}

	public static void trySave(File screenshot) {
		if (EMUtilsClient.config() == null || !EMUtilsClient.config().screenshotMetadataSaver()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
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

	private static JsonObject metadata(Minecraft client, File screenshot) {
		BlockPos pos = client.player.blockPosition();
		JsonObject root = new JsonObject();
		root.addProperty("schema", 1);
		root.addProperty("screenshot", screenshot.getName());
		root.addProperty("capturedAt", Instant.now().toString());
		root.addProperty("world", worldKey(client));
		root.addProperty("dimension", dimensionId(client));
		root.addProperty("biome", biomeId(client, pos));
		root.addProperty("facing", client.player.getDirection().name().toLowerCase(Locale.ENGLISH));

		JsonObject position = new JsonObject();
		position.addProperty("x", pos.getX());
		position.addProperty("y", pos.getY());
		position.addProperty("z", pos.getZ());
		root.add("position", position);
		return root;
	}

	private static String dimensionId(Minecraft client) {
		ResourceKey<Level> key = client.level.dimension();
		return key.identifier().toString();
	}

	private static String biomeId(Minecraft client, BlockPos pos) {
		return client.level.getBiome(pos)
			.unwrapKey()
			.map(ResourceKey::identifier)
			.map(Object::toString)
			.orElse("");
	}

	private static String worldKey(Minecraft client) {
		ServerData serverInfo = client.getCurrentServer();
		if (serverInfo != null && serverInfo.ip != null && !serverInfo.ip.isBlank()) {
			return "multiplayer:" + serverInfo.ip;
		}

		if (client.hasSingleplayerServer()) {
			var server = client.getSingleplayerServer();
			if (server != null) {
				return "singleplayer:" + server.getWorldData().getLevelName();
			}
		}

		return client.level.dimension().identifier().toString().toLowerCase(Locale.ENGLISH);
	}
}
