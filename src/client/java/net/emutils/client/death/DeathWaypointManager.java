package net.emutils.client.death;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.util.EMUtilsPaths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.function.Supplier;
import net.minecraft.text.Text;

public final class DeathWaypointManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final double NEAR_DISTANCE_BLOCKS = 10.0D;
	private static final double NEAR_DISTANCE_SQUARED = NEAR_DISTANCE_BLOCKS * NEAR_DISTANCE_BLOCKS;
	private static final long DUPLICATE_CAPTURE_WINDOW_MS = 1_000L;

	private DeathLocation location;
	private long lastCaptureTimestamp;

	public DeathWaypointManager() {
		load();
	}

	public void captureDeath(MinecraftClient client) {
		if (!enabled() || client.player == null || client.world == null) {
			return;
		}

		BlockPos blockPos = client.player.getBlockPos();
		long timestamp = System.currentTimeMillis();
		if (location != null
			&& location.sameBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ())
			&& timestamp - lastCaptureTimestamp < DUPLICATE_CAPTURE_WINDOW_MS) {
			return;
		}

		lastCaptureTimestamp = timestamp;
		location = new DeathLocation(
			blockPos.getX(),
			blockPos.getY(),
			blockPos.getZ(),
			dimensionId(client.world),
			worldKey(client),
			timestamp
		);
		save();
	}

	public void tick(MinecraftClient client) {
		if (!enabled() || location == null || client.player == null || client.world == null) {
			return;
		}

		if (!canInteractWithWaypoint(client)) {
			return;
		}

		if (!matchesCurrentWorld(client)) {
			return;
		}

		if (location.nearPromptShown()) {
			return;
		}

		if (distanceSquaredToPlayer(client) > NEAR_DISTANCE_SQUARED) {
			return;
		}

		try {
			DeathWaypointChat.showNearPrompt(client.inGameHud.getChatHud());
			location.setNearPromptShown(true);
			save();
		} catch (Throwable exception) {
			EMUtilsClient.LOGGER.error("Failed to show death waypoint prompt.", exception);
		}
	}

	public void keep(MinecraftClient client) {
		if (!hasWaypoint() || client == null || client.inGameHud == null) {
			return;
		}

		DeathWaypointChat.removeNearPrompt(client.inGameHud.getChatHud());
		client.inGameHud.getChatHud().addMessage(DeathWaypointMessage.kept());
	}

	public void clear(MinecraftClient client) {
		clear(client, DeathWaypointMessage::cleared);
	}

	public void clearForCurrentWorld(MinecraftClient client) {
		if (client == null || client.world == null) {
			return;
		}

		if (!hasWaypointForCurrentWorld(client)) {
			if (client.inGameHud != null) {
				client.inGameHud.getChatHud().addMessage(DeathWaypointMessage.noneForWorld());
			}
			return;
		}

		clear(client, DeathWaypointMessage::clearedForWorld);
	}

	public boolean hasWaypoint() {
		return location != null;
	}

	public boolean hasWaypointForCurrentWorld(MinecraftClient client) {
		return location != null && client != null && client.world != null && matchesCurrentWorld(client);
	}

	public DeathLocation location() {
		return location;
	}

	public boolean enabled() {
		return EMUtilsClient.config().deathWaypoint();
	}

	public boolean shouldRender(MinecraftClient client) {
		if (!enabled() || location == null || client.player == null || client.world == null) {
			return false;
		}

		return matchesCurrentWorld(client);
	}

	private boolean matchesCurrentWorld(MinecraftClient client) {
		return location.matchesDimension(dimensionId(client.world))
			&& location.matchesWorldKey(worldKey(client));
	}

	public int distanceBlocks(MinecraftClient client) {
		return (int) Math.round(Math.sqrt(distanceSquaredToPlayer(client)));
	}

	public double distanceToCamera(MinecraftClient client) {
		if (client.gameRenderer == null || client.gameRenderer.getCamera() == null) {
			return 1.0D;
		}

		var cameraPos = client.gameRenderer.getCamera().getCameraPos();
		double dx = renderX() - cameraPos.x;
		double dy = renderY() - cameraPos.y;
		double dz = renderZ() - cameraPos.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public float labelScale(MinecraftClient client) {
		double distance = Math.max(1.0D, distanceToCamera(client));
		float scale = (float) (distance * 0.02666667D);
		scale = Math.max(0.35F, Math.min(6.0F, scale));
		return scale * EMUtilsClient.config().deathWaypointSizeMultiplier();
	}

	private static boolean canInteractWithWaypoint(MinecraftClient client) {
		if (client.currentScreen instanceof DeathScreen) {
			return false;
		}

		return client.player.isAlive();
	}

	public double renderX() {
		return location.x() + 0.5D;
	}

	public double renderY() {
		return location.y() + 1.25D;
	}

	public double renderZ() {
		return location.z() + 0.5D;
	}

	private double distanceSquaredToPlayer(MinecraftClient client) {
		double dx = client.player.getX() - renderX();
		double dy = client.player.getY() - renderY();
		double dz = client.player.getZ() - renderZ();
		return dx * dx + dy * dy + dz * dz;
	}

	private void clear(MinecraftClient client, Supplier<Text> confirmationMessage) {
		if (client != null && client.inGameHud != null) {
			try {
				DeathWaypointChat.removeNearPrompt(client.inGameHud.getChatHud());
			} catch (RuntimeException exception) {
				EMUtilsClient.LOGGER.warn("Failed to remove death waypoint prompt from chat.", exception);
			}
		}

		location = null;
		lastCaptureTimestamp = 0L;

		try {
			Files.deleteIfExists(EMUtilsPaths.deathWaypointFile());
		} catch (IOException ignored) {
		}

		if (client != null && client.inGameHud != null) {
			client.inGameHud.getChatHud().addMessage(confirmationMessage.get());
		}
	}

	private void load() {
		if (!Files.exists(EMUtilsPaths.deathWaypointFile())) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(EMUtilsPaths.deathWaypointFile())) {
			location = GSON.fromJson(reader, DeathLocation.class);
		} catch (IOException | JsonParseException | IllegalStateException exception) {
			EMUtilsClient.LOGGER.warn("Failed to load death waypoint.", exception);
			location = null;
		}
	}

	private void save() {
		if (location == null) {
			return;
		}

		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			try (Writer writer = Files.newBufferedWriter(EMUtilsPaths.deathWaypointFile())) {
				GSON.toJson(location, writer);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to save death waypoint.", exception);
		}
	}

	private static String dimensionId(ClientWorld world) {
		RegistryKey<World> key = world.getRegistryKey();
		return key.getValue().toString();
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

		return "";
	}
}
