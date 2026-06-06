package net.emutils.client.emutils.waypoint;

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
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class WaypointManager {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private static final double NEAR_DISTANCE_BLOCKS = 10.0D;
    private static final double NEAR_DISTANCE_SQUARED =
        NEAR_DISTANCE_BLOCKS * NEAR_DISTANCE_BLOCKS;
    private static final long DUPLICATE_CAPTURE_WINDOW_MS = 1_000L;
    private static final int MAX_WAYPOINTS_PER_WORLD = 64;

    private final List<Waypoint> waypoints = new ArrayList<>();
    private long lastCaptureTimestamp;

    public WaypointManager() {
        load();
    }

    public void captureDeath(Minecraft client) {
        if (!enabled() || client.player == null || client.level == null) {
            return;
        }

        BlockPos blockPos = client.player.blockPosition();
        long timestamp = System.currentTimeMillis();
        String worldKey = worldKey(client);
        String dimension = dimensionId(client.level);

        for (Waypoint existing : waypoints) {
            if (
                existing.isDeath() &&
                matchesWorld(existing, worldKey, dimension) &&
                existing.sameBlock(
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ()
                ) &&
                timestamp - existing.timestamp() < DUPLICATE_CAPTURE_WINDOW_MS
            ) {
                return;
            }
        }

        lastCaptureTimestamp = timestamp;
        waypoints.add(
            new Waypoint(
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ(),
                dimension,
                worldKey,
                timestamp,
                "Death",
                EMUtilsClient.config().waypointDefaultDeathColor(),
                WaypointType.DEATH
            )
        );
        trimWaypointsForWorld(worldKey, dimension);
        save();

        if (EMUtilsClient.config().waypointAutoCopyCoords()) {
            copyCoordinates(client, timestamp);
        }
    }

    public void addCustom(
        Minecraft client,
        String label,
        int x,
        int y,
        int z,
        int color,
        boolean beacon
    ) {
        if (!enabled() || client == null || client.level == null) {
            return;
        }

        long timestamp = System.currentTimeMillis();
        String worldKey = worldKey(client);
        String dimension = dimensionId(client.level);

        Waypoint waypoint = new Waypoint(
            x,
            y,
            z,
            dimension,
            worldKey,
            timestamp,
            label,
            color,
            WaypointType.CUSTOM
        );
        waypoint.setBeaconEnabled(beacon);
        waypoints.add(waypoint);
        trimWaypointsForWorld(worldKey, dimension);
        save();
    }

    public void tick(Minecraft client) {
        if (!enabled() || client.player == null || client.level == null) {
            return;
        }

        if (!canInteractWithWaypoint(client)) {
            return;
        }

        Waypoint nearest = findNearestUnprompted(client);
        if (nearest == null) {
            return;
        }

        if (distanceSquaredToPlayer(client, nearest) > NEAR_DISTANCE_SQUARED) {
            return;
        }

        try {
            WaypointChat.showNearPrompt(
                client.gui.getChat(),
                nearest.timestamp()
            );
            nearest.setNearPromptShown(true);
            save();
        } catch (Throwable exception) {
            EMUtilsClient.LOGGER.error(
                "Failed to show waypoint prompt.",
                exception
            );
        }
    }

    public void keep(Minecraft client, long timestamp) {
        Waypoint waypoint = findByTimestamp(timestamp);
        if (waypoint == null || client == null || client.gui == null) {
            return;
        }

        WaypointChat.removeNearPrompt(client.gui.getChat(), timestamp);
        client.gui
            .getChat()
            .addClientSystemMessage(EmUtilsChatPrefix.chat(WaypointMessage.kept()));
    }

    public void copyCoordinates(Minecraft client, long timestamp) {
        Waypoint waypoint = findByTimestamp(timestamp);
        if (waypoint == null || client == null || client.keyboardHandler == null) {
            return;
        }

        client.keyboardHandler.setClipboard(
            WaypointCoordinates.format(
                waypoint,
                EMUtilsClient.config().waypointCoordinateFormat()
            )
        );
        if (client.gui != null) {
            client.gui
                .getChat()
                .addClientSystemMessage(
                    EmUtilsChatPrefix.chat(
                        Component.translatable(
                            EMUtilsTexts.WAYPOINT_COORDS_COPIED
                        ).withStyle(ChatFormatting.GREEN)
                    )
                );
        }
    }

    public void clear(Minecraft client, long timestamp) {
        clear(client, timestamp, WaypointMessage::cleared);
    }

    public void clearForCurrentWorld(Minecraft client) {
        if (client == null || client.level == null) {
            return;
        }

        if (!hasWaypointForCurrentWorld(client)) {
            if (client.gui != null) {
                client.gui
                    .getChat()
                    .addClientSystemMessage(
                        EmUtilsChatPrefix.chat(WaypointMessage.noneForWorld())
                    );
            }
            return;
        }

        clear(client, WaypointMessage::clearedForWorld);
    }

    public void toggleBeacon(long timestamp) {
        Waypoint waypoint = findByTimestamp(timestamp);
        if (waypoint != null) {
            waypoint.setBeaconEnabled(!waypoint.beaconEnabled());
            save();
        }
    }

    public boolean hasWaypoint() {
        return !waypoints.isEmpty();
    }

    public boolean hasWaypointForCurrentWorld(Minecraft client) {
        return !waypointsForCurrentWorld(client).isEmpty();
    }

    public List<Waypoint> waypointsForCurrentWorld(Minecraft client) {
        if (client == null || client.level == null) {
            return List.of();
        }

        String worldKey = worldKey(client);
        String dimension = dimensionId(client.level);
        return waypoints
            .stream()
            .filter(wp -> matchesWorld(wp, worldKey, dimension))
            .sorted(Comparator.comparingLong(Waypoint::timestamp).reversed())
            .toList();
    }

    public boolean enabled() {
        return EMUtilsClient.config().waypointEnabled();
    }

    public boolean shouldRender(Minecraft client) {
        return (
            enabled() &&
            client.player != null &&
            client.level != null &&
            !waypointsForCurrentWorld(client).isEmpty()
        );
    }

    public int distanceBlocks(Minecraft client, Waypoint waypoint) {
        return (int) Math.round(
            Math.sqrt(distanceSquaredToPlayer(client, waypoint))
        );
    }

    public double distanceToCamera(Minecraft client, Waypoint waypoint) {
        if (
            client.gameRenderer == null ||
            client.gameRenderer.getMainCamera() == null
        ) {
            return 1.0D;
        }

        var cameraPos = client.gameRenderer.getMainCamera().position();
        double dx = renderX(waypoint) - cameraPos.x;
        double dy = renderY(waypoint) - cameraPos.y;
        double dz = renderZ(waypoint) - cameraPos.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public float labelScale(Minecraft client, Waypoint waypoint) {
        double distance = Math.max(1.0D, distanceToCamera(client, waypoint));
        float scale = (float) (distance * 0.02666667D);
        scale = Math.max(0.35F, Math.min(6.0F, scale));
        return scale * EMUtilsClient.config().waypointSizeMultiplier();
    }

    public static double renderX(Waypoint waypoint) {
        return waypoint.x() + 0.5D;
    }

    public static double renderY(Waypoint waypoint) {
        return waypoint.y() + 1.25D;
    }

    public static double renderZ(Waypoint waypoint) {
        return waypoint.z() + 0.5D;
    }

    private static boolean canInteractWithWaypoint(Minecraft client) {
        if (client.screen instanceof DeathScreen) {
            return false;
        }

        return client.player.isAlive();
    }

    @Nullable
    private Waypoint findNearestUnprompted(Minecraft client) {
        Waypoint nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Waypoint waypoint : waypointsForCurrentWorld(client)) {
            if (!waypoint.isDeath() || waypoint.nearPromptShown()) {
                continue;
            }

            double distance = distanceSquaredToPlayer(client, waypoint);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = waypoint;
            }
        }

        return nearest;
    }

    @Nullable
    private Waypoint findByTimestamp(long timestamp) {
        for (Waypoint waypoint : waypoints) {
            if (waypoint.timestamp() == timestamp) {
                return waypoint;
            }
        }

        return null;
    }

    private double distanceSquaredToPlayer(
        Minecraft client,
        Waypoint waypoint
    ) {
        double dx = client.player.getX() - renderX(waypoint);
        double dy = client.player.getY() - renderY(waypoint);
        double dz = client.player.getZ() - renderZ(waypoint);
        return dx * dx + dy * dy + dz * dz;
    }

    private void clear(
        Minecraft client,
        long timestamp,
        Supplier<Component> confirmationMessage
    ) {
        Waypoint waypoint = findByTimestamp(timestamp);
        if (waypoint == null) {
            return;
        }

        clear(client, confirmationMessage, waypoint);
    }

    private void clear(
        Minecraft client,
        Supplier<Component> confirmationMessage
    ) {
        if (client == null || client.level == null) {
            return;
        }

        String worldKey = worldKey(client);
        String dimension = dimensionId(client.level);
        List<Waypoint> removed = waypointsForCurrentWorld(client);
        if (removed.isEmpty()) {
            return;
        }

        waypoints.removeIf(wp -> matchesWorld(wp, worldKey, dimension));
        for (Waypoint waypoint : removed) {
            removeWaypoint(client, waypoint);
        }

        save();

        if (client.gui != null) {
            client.gui
                .getChat()
                .addClientSystemMessage(EmUtilsChatPrefix.chat(confirmationMessage.get()));
        }
    }

    private void clear(
        Minecraft client,
        Supplier<Component> confirmationMessage,
        Waypoint waypoint
    ) {
        if (!waypoints.remove(waypoint)) {
            return;
        }

        removeWaypoint(client, waypoint);
        save();

        if (client != null && client.gui != null) {
            client.gui
                .getChat()
                .addClientSystemMessage(EmUtilsChatPrefix.chat(confirmationMessage.get()));
        }
    }

    private void removeWaypoint(Minecraft client, Waypoint waypoint) {
        if (client != null && client.gui != null) {
            try {
                WaypointChat.removeNearPrompt(
                    client.gui.getChat(),
                    waypoint.timestamp()
                );
            } catch (RuntimeException exception) {
                EMUtilsClient.LOGGER.warn(
                    "Failed to remove waypoint prompt from chat.",
                    exception
                );
            }
        }
    }

    private void trimWaypointsForWorld(String worldKey, String dimension) {
        List<Waypoint> worldWaypoints = waypoints
            .stream()
            .filter(wp -> matchesWorld(wp, worldKey, dimension))
            .sorted(Comparator.comparingLong(Waypoint::timestamp))
            .toList();

        int excess = worldWaypoints.size() - MAX_WAYPOINTS_PER_WORLD;
        for (int index = 0; index < excess; index++) {
            waypoints.remove(worldWaypoints.get(index));
        }
    }

    private static boolean matchesWorld(
        Waypoint waypoint,
        String worldKey,
        String dimension
    ) {
        return (
            waypoint.matchesDimension(dimension) &&
            waypoint.matchesWorldKey(worldKey)
        );
    }

    private void load() {
        if (!Files.exists(EMUtilsPaths.waypointFile())) {
            migrateFromDeathFile();
            return;
        }

        try {
            String json = Files.readString(EMUtilsPaths.waypointFile());
            WaypointSaveData saveData = GSON.fromJson(
                json,
                WaypointSaveData.class
            );
            if (
                saveData != null &&
                saveData.waypoints() != null &&
                !saveData.waypoints().isEmpty()
            ) {
                waypoints.clear();
                waypoints.addAll(saveData.waypoints());
                return;
            }
        } catch (
            IOException
            | JsonParseException
            | IllegalStateException exception
        ) {
            EMUtilsClient.LOGGER.warn("Failed to load waypoints.", exception);
            waypoints.clear();
        }
    }

    private void migrateFromDeathFile() {
        if (!Files.exists(EMUtilsPaths.deathWaypointFile())) {
            return;
        }

        EMUtilsClient.LOGGER.info(
            "Migrating death waypoints to unified waypoint format."
        );

        try {
            String json = Files.readString(EMUtilsPaths.deathWaypointFile());
            com.google.gson.JsonElement root = GSON.fromJson(
                json,
                com.google.gson.JsonElement.class
            );

            if (root != null && root.isJsonObject()) {
                com.google.gson.JsonObject obj = root.getAsJsonObject();
                if (obj.has("deaths")) {
                    com.google.gson.JsonArray deathsArray = obj.getAsJsonArray(
                        "deaths"
                    );
                    for (com.google.gson.JsonElement element : deathsArray) {
                        Waypoint death = GSON.fromJson(element, Waypoint.class);
                        if (death.dimension() != null) {
                            death.setType(WaypointType.DEATH);
                            if (
                                death.label() == null || death.label().isBlank()
                            ) {
                                death.setLabel("Death");
                            }
                            if (death.color() == 0) {
                                death.setColor(
                                    EMUtilsClient.config().waypointDefaultDeathColor()
                                );
                            }
                            waypoints.add(death);
                        }
                    }
                } else {
                    Waypoint legacy = GSON.fromJson(json, Waypoint.class);
                    if (legacy != null && legacy.dimension() != null) {
                        legacy.setType(WaypointType.DEATH);
                        if (
                            legacy.label() == null || legacy.label().isBlank()
                        ) {
                            legacy.setLabel("Death");
                        }
                        if (legacy.color() == 0) {
                            legacy.setColor(
                                EMUtilsClient.config().waypointDefaultDeathColor()
                            );
                        }
                        waypoints.add(legacy);
                    }
                }
            }

            if (!waypoints.isEmpty()) {
                save();
                Files.deleteIfExists(EMUtilsPaths.deathWaypointFile());
                EMUtilsClient.LOGGER.info(
                    "Migrated {} death waypoints to unified format.",
                    waypoints.size()
                );
            }
        } catch (
            IOException
            | JsonParseException
            | IllegalStateException exception
        ) {
            EMUtilsClient.LOGGER.warn(
                "Failed to migrate death waypoints.",
                exception
            );
        }
    }

    private void save() {
        try {
            Files.createDirectories(EMUtilsPaths.configDir());
            if (waypoints.isEmpty()) {
                Files.deleteIfExists(EMUtilsPaths.waypointFile());
                return;
            }

            WaypointSaveData saveData = new WaypointSaveData();
            saveData.setWaypoints(new ArrayList<>(waypoints));
            try (
                Writer writer = Files.newBufferedWriter(
                    EMUtilsPaths.waypointFile()
                )
            ) {
                GSON.toJson(saveData, writer);
            }
        } catch (IOException exception) {
            EMUtilsClient.LOGGER.warn("Failed to save waypoints.", exception);
        }
    }

    private static String dimensionId(ClientLevel world) {
        ResourceKey<Level> key = world.dimension();
        return key.identifier().toString();
    }

    private static String worldKey(Minecraft client) {
        ServerData serverInfo = client.getCurrentServer();
        if (
            serverInfo != null &&
            serverInfo.ip != null &&
            !serverInfo.ip.isBlank()
        ) {
            return "multiplayer:" + serverInfo.ip;
        }

        if (client.hasSingleplayerServer()) {
            var server = client.getSingleplayerServer();
            if (server != null) {
                return (
                    "singleplayer:" + server.getWorldData().getLevelName()
                );
            }
        }

        return "";
    }
}
