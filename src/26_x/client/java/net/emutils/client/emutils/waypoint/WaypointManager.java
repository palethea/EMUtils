package net.emutils.client.emutils.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.AtomicFiles;
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
        Waypoint waypoint = new Waypoint(
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ(),
                dimension,
                worldKey,
                timestamp,
                "Death",
                EMUtilsClient.config().waypointDefaultDeathColor(),
                WaypointType.DEATH
            );
        waypoints.add(waypoint);
        trimWaypointsForWorld(worldKey, dimension);
        save();

        if (EMUtilsClient.config().waypointAutoCopyCoords()) {
            copyCoordinates(client, waypoint.id());
        }
    }

    /**
     * Adds a custom waypoint in the current world and dimension. Returns false, and keeps nothing, when
     * waypoints are off, there's no world, or the waypoints couldn't be written.
     */
    public boolean addCustom(
        Minecraft client,
        String label,
        int x,
        int y,
        int z,
        int color,
        boolean beacon
    ) {
        if (!enabled() || client == null || client.level == null) {
            return false;
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
        // Trimming can drop the oldest waypoint, so keep the whole list to put back if saving fails.
        List<Waypoint> before = new ArrayList<>(waypoints);
        waypoints.add(waypoint);
        trimWaypointsForWorld(worldKey, dimension);
        if (!save()) {
            // Not written, so it wouldn't survive a restart; don't pretend it was added.
            waypoints.clear();
            waypoints.addAll(before);
            return false;
        }
        return true;
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
                net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client),
                nearest.id()
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

    public void keep(Minecraft client, String id) {
        Waypoint waypoint = findById(id);
        if (waypoint == null || client == null || client.gui == null) {
            return;
        }

        WaypointChat.removeNearPrompt(net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client), id);
        net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
            .addClientSystemMessage(EmUtilsChatPrefix.chat(WaypointMessage.kept()));
    }

    public void copyCoordinates(Minecraft client, String id) {
        Waypoint waypoint = findById(id);
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
            net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
                .addClientSystemMessage(
                    EmUtilsChatPrefix.chat(
                        Component.translatable(
                            EMUtilsTexts.WAYPOINT_COORDS_COPIED
                        ).withStyle(ChatFormatting.GREEN)
                    )
                );
        }
    }

    /**
     * Copies where you are, in the waypoint coordinate format: the free camera's
     * position while Free Camera is active, otherwise the player's.
     */
    public void copyCurrentCoordinates(Minecraft client) {
        if (client == null || client.player == null || client.keyboardHandler == null) {
            return;
        }

        BlockPos cameraPos = EMUtilsClient.tweaks() == null
            ? null
            : EMUtilsClient.tweaks().freeCamera().cameraBlockPosition();
        BlockPos pos = cameraPos != null ? cameraPos : client.player.blockPosition();
        String text = WaypointCoordinates.format(
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            EMUtilsClient.config().waypointCoordinateFormat()
        );
        client.keyboardHandler.setClipboard(text);
        if (EMUtilsClient.config().copyCoordinatesFeedback() && client.gui != null) {
            net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
                .addClientSystemMessage(
                    EmUtilsChatPrefix.chat(
                        Component.translatable(
                            cameraPos != null
                                ? EMUtilsTexts.COORDS_COPIED_CAMERA
                                : EMUtilsTexts.COORDS_COPIED,
                            text
                        ).withStyle(ChatFormatting.GREEN)
                    )
                );
        }
    }

    public void clear(Minecraft client, String id) {
        clear(client, id, WaypointMessage::cleared);
    }

    public void clearForCurrentWorld(Minecraft client) {
        if (client == null || client.level == null) {
            return;
        }

        if (!hasWaypointForCurrentWorld(client)) {
            if (client.gui != null) {
                net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
                    .addClientSystemMessage(
                        EmUtilsChatPrefix.chat(WaypointMessage.noneForWorld())
                    );
            }
            return;
        }

        clear(client, WaypointMessage::clearedForWorld);
    }

    public void toggleBeacon(String id) {
        Waypoint waypoint = findById(id);
        if (waypoint != null) {
            waypoint.setBeaconEnabled(!waypoint.beaconEnabled());
            save();
        }
    }

    public void toggleHidden(String id) {
        Waypoint waypoint = findById(id);
        if (waypoint != null) {
            waypoint.setHidden(!waypoint.hidden());
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
        if (net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) instanceof DeathScreen) {
            return false;
        }

        return client.player.isAlive();
    }

    @Nullable
    private Waypoint findNearestUnprompted(Minecraft client) {
        Waypoint nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Waypoint waypoint : waypointsForCurrentWorld(client)) {
            if (waypoint.hidden() || !waypoint.isDeath() || waypoint.nearPromptShown()) {
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
    private Waypoint findById(String id) {
        for (Waypoint waypoint : waypoints) {
            if (waypoint.id().equals(id)) {
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
        String id,
        Supplier<Component> confirmationMessage
    ) {
        Waypoint waypoint = findById(id);
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
            net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
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
            net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
                .addClientSystemMessage(EmUtilsChatPrefix.chat(confirmationMessage.get()));
        }
    }

    private void removeWaypoint(Minecraft client, Waypoint waypoint) {
        if (client != null && client.gui != null) {
            try {
                WaypointChat.removeNearPrompt(
                    net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client),
                    waypoint.id()
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
                if (ensureUniqueIds()) {
                    save();
                }
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
                ensureUniqueIds();
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

    /** Gives an id to every waypoint that has none or shares one; returns whether any changed. */
    private boolean ensureUniqueIds() {
        Set<String> seen = new HashSet<>();
        boolean changed = false;
        for (Waypoint waypoint : waypoints) {
            if (!waypoint.hasId() || !seen.add(waypoint.id())) {
                waypoint.assignNewId();
                seen.add(waypoint.id());
                changed = true;
            }
        }
        return changed;
    }

    /** Writes the waypoints; returns false if that failed. */
    private boolean save() {
        try {
            Files.createDirectories(EMUtilsPaths.configDir());
            if (waypoints.isEmpty()) {
                Files.deleteIfExists(EMUtilsPaths.waypointFile());
                return true;
            }

            WaypointSaveData saveData = new WaypointSaveData();
            saveData.setWaypoints(new ArrayList<>(waypoints));
            AtomicFiles.writeString(EMUtilsPaths.waypointFile(), GSON.toJson(saveData));
            return true;
        } catch (IOException exception) {
            EMUtilsClient.LOGGER.warn("Failed to save waypoints.", exception);
            return false;
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
            if (
                serverInfo.isRealm() &&
                serverInfo.name != null &&
                !serverInfo.name.isBlank()
            ) {
                return "realm:" + normalizeWorldKeyPart(serverInfo.name);
            }
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

    private static String normalizeWorldKeyPart(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
