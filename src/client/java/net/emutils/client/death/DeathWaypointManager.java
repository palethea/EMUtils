package net.emutils.client.death;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.util.EMUtilsPaths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public final class DeathWaypointManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final double NEAR_DISTANCE_BLOCKS = 10.0D;
	private static final double NEAR_DISTANCE_SQUARED = NEAR_DISTANCE_BLOCKS * NEAR_DISTANCE_BLOCKS;
	private static final long DUPLICATE_CAPTURE_WINDOW_MS = 1_000L;
	private static final int MAX_DEATHS_PER_WORLD = 32;

	private final List<DeathLocation> deaths = new ArrayList<>();
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
		String worldKey = worldKey(client);
		String dimension = dimensionId(client.world);

		for (DeathLocation existing : deaths) {
			if (matchesWorld(existing, worldKey, dimension)
				&& existing.sameBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ())
				&& timestamp - existing.deathTimestamp() < DUPLICATE_CAPTURE_WINDOW_MS) {
				return;
			}
		}

		lastCaptureTimestamp = timestamp;
		deaths.add(new DeathLocation(
			blockPos.getX(),
			blockPos.getY(),
			blockPos.getZ(),
			dimension,
			worldKey,
			timestamp
		));
		trimDeathsForWorld(worldKey, dimension);
		save();
	}

	public void tick(MinecraftClient client) {
		if (!enabled() || client.player == null || client.world == null) {
			return;
		}

		if (!canInteractWithWaypoint(client)) {
			return;
		}

		DeathLocation nearest = findNearestUnprompted(client);
		if (nearest == null) {
			return;
		}

		if (distanceSquaredToPlayer(client, nearest) > NEAR_DISTANCE_SQUARED) {
			return;
		}

		try {
			DeathWaypointChat.showNearPrompt(client.inGameHud.getChatHud(), nearest.deathTimestamp());
			nearest.setNearPromptShown(true);
			save();
		} catch (Throwable exception) {
			EMUtilsClient.LOGGER.error("Failed to show death waypoint prompt.", exception);
		}
	}

	public void keep(MinecraftClient client, long deathTimestamp) {
		DeathLocation location = findByTimestamp(deathTimestamp);
		if (location == null || client == null || client.inGameHud == null) {
			return;
		}

		DeathWaypointChat.removeNearPrompt(client.inGameHud.getChatHud(), deathTimestamp);
		client.inGameHud.getChatHud().addMessage(DeathWaypointMessage.kept());
	}

	public void clear(MinecraftClient client, long deathTimestamp) {
		clear(client, deathTimestamp, DeathWaypointMessage::cleared);
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
		return !deaths.isEmpty();
	}

	public boolean hasWaypointForCurrentWorld(MinecraftClient client) {
		return !deathsForCurrentWorld(client).isEmpty();
	}

	public List<DeathLocation> deathsForCurrentWorld(MinecraftClient client) {
		if (client == null || client.world == null) {
			return List.of();
		}

		String worldKey = worldKey(client);
		String dimension = dimensionId(client.world);
		return deaths.stream()
			.filter(death -> matchesWorld(death, worldKey, dimension))
			.sorted(Comparator.comparingLong(DeathLocation::deathTimestamp).reversed())
			.toList();
	}

	public boolean enabled() {
		return EMUtilsClient.config().deathWaypoint();
	}

	public boolean shouldRender(MinecraftClient client) {
		return enabled() && client.player != null && client.world != null && !deathsForCurrentWorld(client).isEmpty();
	}

	public int distanceBlocks(MinecraftClient client, DeathLocation location) {
		return (int) Math.round(Math.sqrt(distanceSquaredToPlayer(client, location)));
	}

	public double distanceToCamera(MinecraftClient client, DeathLocation location) {
		if (client.gameRenderer == null || client.gameRenderer.getCamera() == null) {
			return 1.0D;
		}

		var cameraPos = client.gameRenderer.getCamera().getCameraPos();
		double dx = renderX(location) - cameraPos.x;
		double dy = renderY(location) - cameraPos.y;
		double dz = renderZ(location) - cameraPos.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public float labelScale(MinecraftClient client, DeathLocation location) {
		double distance = Math.max(1.0D, distanceToCamera(client, location));
		float scale = (float) (distance * 0.02666667D);
		scale = Math.max(0.35F, Math.min(6.0F, scale));
		return scale * EMUtilsClient.config().deathWaypointSizeMultiplier();
	}

	public int labelIndex(MinecraftClient client, DeathLocation location) {
		List<DeathLocation> worldDeaths = deathsForCurrentWorld(client);
		for (int index = 0; index < worldDeaths.size(); index++) {
			if (worldDeaths.get(index).deathTimestamp() == location.deathTimestamp()) {
				return index;
			}
		}

		return 0;
	}

	private static boolean canInteractWithWaypoint(MinecraftClient client) {
		if (client.currentScreen instanceof DeathScreen) {
			return false;
		}

		return client.player.isAlive();
	}

	public static double renderX(DeathLocation location) {
		return location.x() + 0.5D;
	}

	public static double renderY(DeathLocation location) {
		return location.y() + 1.25D;
	}

	public static double renderZ(DeathLocation location) {
		return location.z() + 0.5D;
	}

	@Nullable
	private DeathLocation findNearestUnprompted(MinecraftClient client) {
		DeathLocation nearest = null;
		double nearestDistance = Double.MAX_VALUE;

		for (DeathLocation location : deathsForCurrentWorld(client)) {
			if (location.nearPromptShown()) {
				continue;
			}

			double distance = distanceSquaredToPlayer(client, location);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = location;
			}
		}

		return nearest;
	}

	@Nullable
	private DeathLocation findByTimestamp(long deathTimestamp) {
		for (DeathLocation location : deaths) {
			if (location.deathTimestamp() == deathTimestamp) {
				return location;
			}
		}

		return null;
	}

	private double distanceSquaredToPlayer(MinecraftClient client, DeathLocation location) {
		double dx = client.player.getX() - renderX(location);
		double dy = client.player.getY() - renderY(location);
		double dz = client.player.getZ() - renderZ(location);
		return dx * dx + dy * dy + dz * dz;
	}

	private void clear(MinecraftClient client, long deathTimestamp, Supplier<Text> confirmationMessage) {
		DeathLocation location = findByTimestamp(deathTimestamp);
		if (location == null) {
			return;
		}

		clear(client, confirmationMessage, location);
	}

	private void clear(MinecraftClient client, Supplier<Text> confirmationMessage) {
		if (client == null || client.world == null) {
			return;
		}

		String worldKey = worldKey(client);
		String dimension = dimensionId(client.world);
		List<DeathLocation> removed = deathsForCurrentWorld(client);
		if (removed.isEmpty()) {
			return;
		}

		deaths.removeIf(death -> matchesWorld(death, worldKey, dimension));
		for (DeathLocation location : removed) {
			removeDeath(client, location);
		}

		save();

		if (client.inGameHud != null) {
			client.inGameHud.getChatHud().addMessage(confirmationMessage.get());
		}
	}

	private void clear(MinecraftClient client, Supplier<Text> confirmationMessage, DeathLocation location) {
		if (!deaths.remove(location)) {
			return;
		}

		removeDeath(client, location);
		save();

		if (client != null && client.inGameHud != null) {
			client.inGameHud.getChatHud().addMessage(confirmationMessage.get());
		}
	}

	private void removeDeath(MinecraftClient client, DeathLocation location) {
		if (client != null && client.inGameHud != null) {
			try {
				DeathWaypointChat.removeNearPrompt(client.inGameHud.getChatHud(), location.deathTimestamp());
			} catch (RuntimeException exception) {
				EMUtilsClient.LOGGER.warn("Failed to remove death waypoint prompt from chat.", exception);
			}
		}
	}

	private void trimDeathsForWorld(String worldKey, String dimension) {
		List<DeathLocation> worldDeaths = deaths.stream()
			.filter(death -> matchesWorld(death, worldKey, dimension))
			.sorted(Comparator.comparingLong(DeathLocation::deathTimestamp))
			.toList();

		int excess = worldDeaths.size() - MAX_DEATHS_PER_WORLD;
		for (int index = 0; index < excess; index++) {
			deaths.remove(worldDeaths.get(index));
		}
	}

	private static boolean matchesWorld(DeathLocation location, String worldKey, String dimension) {
		return location.matchesDimension(dimension) && location.matchesWorldKey(worldKey);
	}

	private void load() {
		if (!Files.exists(EMUtilsPaths.deathWaypointFile())) {
			return;
		}

		try {
			String json = Files.readString(EMUtilsPaths.deathWaypointFile());
			DeathWaypointSaveData saveData = GSON.fromJson(json, DeathWaypointSaveData.class);
			if (saveData != null && saveData.deaths() != null && !saveData.deaths().isEmpty()) {
				deaths.clear();
				deaths.addAll(saveData.deaths());
				return;
			}

			DeathLocation legacy = GSON.fromJson(json, DeathLocation.class);
			if (legacy != null && legacy.dimension() != null) {
				deaths.clear();
				deaths.add(legacy);
			}
		} catch (IOException | JsonParseException | IllegalStateException exception) {
			EMUtilsClient.LOGGER.warn("Failed to load death waypoints.", exception);
			deaths.clear();
		}
	}

	private void save() {
		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			if (deaths.isEmpty()) {
				Files.deleteIfExists(EMUtilsPaths.deathWaypointFile());
				return;
			}

			DeathWaypointSaveData saveData = new DeathWaypointSaveData();
			saveData.setDeaths(new ArrayList<>(deaths));
			try (Writer writer = Files.newBufferedWriter(EMUtilsPaths.deathWaypointFile())) {
				GSON.toJson(saveData, writer);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to save death waypoints.", exception);
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
