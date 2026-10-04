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
import net.emutils.client.emutils.map.MapManager;
import org.jspecify.annotations.Nullable;

public final class WaypointManager {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private static final double NEAR_DISTANCE_BLOCKS = 10.0D;
    private static final double NEAR_DISTANCE_SQUARED =
        NEAR_DISTANCE_BLOCKS * NEAR_DISTANCE_BLOCKS;
    /** How close you have to get to a death waypoint for it to count as reached and be removed. */
    private static final double REACHED_DISTANCE_BLOCKS = 5.0D;
    private static final double REACHED_DISTANCE_SQUARED =
        REACHED_DISTANCE_BLOCKS * REACHED_DISTANCE_BLOCKS;
    /** A death waypoint only counts as reached after you have been this far from it, so respawning next to it doesn't. */
    private static final double ARM_DISTANCE_BLOCKS = 20.0D;
    private static final double ARM_DISTANCE_SQUARED =
        ARM_DISTANCE_BLOCKS * ARM_DISTANCE_BLOCKS;
    private static final long DUPLICATE_CAPTURE_WINDOW_MS = 1_000L;
    private static final int MAX_WAYPOINTS_PER_WORLD = 64;

    private final List<Waypoint> waypoints = new ArrayList<>();
    private long lastCaptureTimestamp;
    /**
     * Waypoints whose nearby-removal prompt was shown in this session. The prompt lives in chat, which
     * a restart clears, so an unanswered one is asked again next time; only an explicit Keep is saved.
     */
    private final Set<String> promptedThisSession = new HashSet<>();
    /** Death waypoints you have been far enough from since the game started, so getting near them again counts as reaching them. */
    private final Set<String> armedDeaths = new HashSet<>();
    /** The old death waypoint file is deleted once the waypoints have been written without it. */
    private boolean legacyDeathFilePending;

    public WaypointManager() {
        load();
    }

    public void captureDeath(Minecraft client) {
        if (!enabled() || client.player == null || client.level == null) {
            return;
        }

        recordDeath(client, client.player.blockPosition());
    }

    /** Adds a death waypoint at {@code blockPos} and drops the oldest ones past the history you keep (#105). */
    public void recordDeath(Minecraft client, BlockPos blockPos) {
        if (!enabled() || client == null || client.level == null) {
            return;
        }

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
        waypoint.setWorld(MapManager.worldIdFor(client, dimension));
        waypoints.add(waypoint);
        trimWaypointsForWorld(worldKey, dimension);
        trimDeathHistory(client, worldKey);
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
        boolean beacon,
        String set
    ) {
        return addCustom(client, null, label, x, y, z, color, beacon, set) != null;
    }

    /**
     * Adds a custom waypoint in the current world, in {@code inDimension} or, when that is null, the one you
     * are in (#105; a shared location can be from another dimension). Returns it, or null when nothing was kept.
     */
    public @Nullable Waypoint addCustom(
        Minecraft client,
        @Nullable String inDimension,
        String label,
        int x,
        int y,
        int z,
        int color,
        boolean beacon,
        String set
    ) {
        if (!enabled() || client == null || client.level == null) {
            return null;
        }

        long timestamp = System.currentTimeMillis();
        String worldKey = worldKey(client);
        String dimension = inDimension == null || inDimension.isBlank() ? dimensionId(client.level) : inDimension;

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
        waypoint.setSet(set);
        waypoint.setWorld(MapManager.worldIdFor(client, dimension));
        // Trimming can drop the oldest waypoint, so keep the whole list to put back if saving fails.
        List<Waypoint> before = new ArrayList<>(waypoints);
        waypoints.add(waypoint);
        trimWaypointsForWorld(worldKey, dimension);
        if (!save()) {
            // Not written, so it wouldn't survive a restart; don't pretend it was added.
            waypoints.clear();
            waypoints.addAll(before);
            return null;
        }
        return waypoint;
    }

    /**
     * Changes a waypoint's name, position, color, beacon and set in place. Returns false, and keeps nothing,
     * when the waypoint is gone or the waypoints couldn't be written.
     */
    public boolean update(
        String id,
        String label,
        int x,
        int y,
        int z,
        int color,
        boolean beacon,
        String set
    ) {
        Waypoint waypoint = findById(id);
        if (waypoint == null) {
            return false;
        }

        String oldLabel = waypoint.label();
        String oldSet = waypoint.set();
        int oldX = waypoint.x();
        int oldY = waypoint.y();
        int oldZ = waypoint.z();
        int oldColor = waypoint.color();
        boolean oldBeacon = waypoint.beaconEnabled();
        boolean oldPrompt = waypoint.nearPromptShown();
        boolean wasPrompted = promptedThisSession.contains(id);

        boolean moved = !waypoint.sameBlock(x, y, z);
        waypoint.setLabel(label);
        waypoint.setPosition(x, y, z);
        waypoint.setColor(color);
        waypoint.setBeaconEnabled(beacon);
        waypoint.setSet(set);
        if (moved) {
            // A death waypoint moved somewhere new can ask to be removed again once you get near it.
            waypoint.setNearPromptShown(false);
        }
        if (!save()) {
            waypoint.setLabel(oldLabel);
            waypoint.setPosition(oldX, oldY, oldZ);
            waypoint.setColor(oldColor);
            waypoint.setBeaconEnabled(oldBeacon);
            waypoint.setSet(oldSet);
            waypoint.setNearPromptShown(oldPrompt);
            return false;
        }
        if (moved) {
            promptedThisSession.remove(id);
            if (wasPrompted) {
                // The prompt in chat was for the old spot; a new one comes when you get near the new one.
                removeWaypoint(Minecraft.getInstance(), waypoint);
            }
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

        checkReached(client, client.player.getX(), client.player.getY(), client.player.getZ());
    }

    /**
     * Death waypoints in this dimension that you are at (#105): removed, or asked about in chat, as the When
     * Reached setting says. One you haven't been far from since the game started, such as the one you respawned
     * next to, doesn't count until you have; one you chose to keep or hid never does.
     */
    public void checkReached(Minecraft client, double x, double y, double z) {
        WaypointReachAction action = EMUtilsClient.config().waypointReachAction();
        for (Waypoint waypoint : waypointsForCurrentWorld(client)) {
            if (!waypoint.isDeath() || waypoint.hidden() || waypoint.nearPromptShown() || promptedThisSession.contains(waypoint.id())) {
                continue;
            }

            double distance = distanceSquared(x, y, z, waypoint);
            if (distance > ARM_DISTANCE_SQUARED) {
                armedDeaths.add(waypoint.id());
                continue;
            }
            if (action == WaypointReachAction.KEEP || !armedDeaths.contains(waypoint.id())) {
                continue;
            }

            if (action == WaypointReachAction.REMOVE) {
                if (distance <= REACHED_DISTANCE_SQUARED) {
                    clear(client, waypoint.id(), WaypointMessage::reachedRemoved);
                }
                continue;
            }

            if (distance > NEAR_DISTANCE_SQUARED) {
                continue;
            }
            try {
                WaypointChat.showNearPrompt(
                    net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client),
                    waypoint.id()
                );
                promptedThisSession.add(waypoint.id());
            } catch (Throwable exception) {
                EMUtilsClient.LOGGER.error(
                    "Failed to show waypoint prompt.",
                    exception
                );
            }
            return;
        }
    }

    /**
     * Keeps this world's newest death waypoints, as many as the Deaths to Keep setting says, and drops the
     * older ones. A death waypoint you chose to keep is yours to delete: it stays and isn't counted.
     */
    private void trimDeathHistory(Minecraft client, String worldKey) {
        List<Waypoint> deaths = waypoints
            .stream()
            .filter(wp -> wp.isDeath() && !wp.nearPromptShown() && wp.matchesWorldKey(worldKey))
            .sorted(Comparator.comparingLong(Waypoint::timestamp))
            .toList();

        int excess = deaths.size() - EMUtilsClient.config().deathWaypointKeep();
        for (int index = 0; index < excess; index++) {
            Waypoint old = deaths.get(index);
            waypoints.remove(old);
            promptedThisSession.remove(old.id());
            armedDeaths.remove(old.id());
            removeWaypoint(client, old);
        }
    }

    public void keep(Minecraft client, String id) {
        Waypoint waypoint = findById(id);
        if (waypoint == null || client == null || client.gui == null) {
            return;
        }

        // Keep is the player's answer, so it is remembered and the waypoint isn't asked about again.
        waypoint.setNearPromptShown(true);
        save();
        WaypointChat.removeNearPrompt(net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client), id);
        net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
            .addClientSystemMessage(EmUtilsChatPrefix.chat(WaypointMessage.kept()));
    }

    public void copyCoordinates(Minecraft client, String id) {
        Waypoint waypoint = findById(id);
        if (waypoint != null) {
            copyCoordinates(client, waypoint.x(), waypoint.y(), waypoint.z());
        }
    }

    /** Copies these coordinates in the waypoint format, for a waypoint shown at converted coordinates. */
    public void copyCoordinates(Minecraft client, int x, int y, int z) {
        if (client == null || client.keyboardHandler == null) {
            return;
        }

        client.keyboardHandler.setClipboard(
            WaypointCoordinates.format(
                x,
                y,
                z,
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

    /** Turns the beacon on or off; returns false, with nothing changed, if the waypoints couldn't be written. */
    public boolean toggleBeacon(String id) {
        Waypoint waypoint = findById(id);
        if (waypoint == null) {
            return false;
        }
        boolean before = waypoint.beaconEnabled();
        waypoint.setBeaconEnabled(!before);
        if (!save()) {
            waypoint.setBeaconEnabled(before);
            reportSaveFailed(Minecraft.getInstance());
            return false;
        }
        return true;
    }

    /** Hides or shows the waypoint; returns false, with nothing changed, if the waypoints couldn't be written. */
    public boolean toggleHidden(String id) {
        Waypoint waypoint = findById(id);
        if (waypoint == null) {
            return false;
        }
        boolean before = waypoint.hidden();
        waypoint.setHidden(!before);
        if (!save()) {
            waypoint.setHidden(before);
            reportSaveFailed(Minecraft.getInstance());
            return false;
        }
        return true;
    }

    /** Tells the player a change wasn't kept because the waypoints file couldn't be written. */
    private void reportSaveFailed(@Nullable Minecraft client) {
        if (client != null && client.gui != null) {
            net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
                .addClientSystemMessage(EmUtilsChatPrefix.chat(WaypointMessage.saveFailed()));
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
            .filter(wp -> matchesWorld(wp, worldKey, dimension) && wp.matchesWorld(MapManager.worldIdFor(client, dimension)))
            .sorted(Comparator.comparingLong(Waypoint::timestamp).reversed())
            .toList();
    }

    /**
     * The waypoints of this world as they are in the dimension you are in (#105): the ones made here at their
     * own coordinates and, with {@code includeOtherDimensions}, the ones made in other dimensions after them,
     * converted between the Overworld and the Nether and listed with their own coordinates otherwise. In no
     * particular order.
     */
    public List<WaypointEntry> entriesForCurrentWorld(Minecraft client, boolean includeOtherDimensions) {
        if (client == null || client.level == null) {
            return List.of();
        }
        return entriesIn(client, dimensionId(client.level), includeOtherDimensions);
    }

    /** Like {@link #entriesForCurrentWorld}, but as they are in {@code dimension}, which the world map may be showing. */
    public List<WaypointEntry> entriesIn(Minecraft client, String dimension, boolean includeOtherDimensions) {
        if (client == null || client.level == null) {
            return List.of();
        }
        return entriesIn(client, dimension, MapManager.worldIdFor(client, dimension), includeOtherDimensions);
    }

    /**
     * Like the above, in one of the server's worlds of that dimension (#219), which the world map may be
     * showing: waypoints made in another of its worlds are left out.
     */
    public List<WaypointEntry> entriesIn(Minecraft client, String dimension, @Nullable String worldId, boolean includeOtherDimensions) {
        if (client == null || client.level == null) {
            return List.of();
        }

        String worldKey = worldKey(client);
        List<WaypointEntry> entries = new ArrayList<>();
        for (Waypoint waypoint : waypoints) {
            if (!waypoint.matchesWorldKey(worldKey)) {
                continue;
            }
            if (!waypoint.matchesWorld(waypoint.matchesDimension(dimension) ? worldId : MapManager.worldIdFor(client, waypoint.dimension()))) {
                continue;
            }
            if (waypoint.matchesDimension(dimension)) {
                entries.add(new WaypointEntry(waypoint, waypoint.x(), waypoint.y(), waypoint.z(), true, true));
            } else if (includeOtherDimensions && waypoint.dimension() != null) {
                int[] converted = WaypointDimensions.convert(waypoint.dimension(), dimension, waypoint.x(), waypoint.z());
                entries.add(converted == null
                    ? new WaypointEntry(waypoint, waypoint.x(), waypoint.y(), waypoint.z(), false, false)
                    : new WaypointEntry(waypoint, converted[0], waypoint.y(), converted[1], false, true));
            }
        }
        return entries;
    }

    /** The waypoints that have a place in the world you are in: this dimension's, plus converted ones if that's on. */
    public List<WaypointEntry> renderEntries(Minecraft client) {
        return entriesForCurrentWorld(client, EMUtilsClient.config().waypointShowOtherDimensions())
            .stream()
            .filter(WaypointEntry::placeable)
            .toList();
    }

    /** The waypoints that have a place in {@code dimension}: its own, plus converted ones if that's on. */
    public List<WaypointEntry> renderEntries(Minecraft client, String dimension) {
        return entriesIn(client, dimension, EMUtilsClient.config().waypointShowOtherDimensions())
            .stream()
            .filter(WaypointEntry::placeable)
            .toList();
    }

    /** The waypoints that have a place in one of the server's worlds of {@code dimension} (#219). */
    public List<WaypointEntry> renderEntries(Minecraft client, String dimension, @Nullable String worldId) {
        return entriesIn(client, dimension, worldId, EMUtilsClient.config().waypointShowOtherDimensions())
            .stream()
            .filter(WaypointEntry::placeable)
            .toList();
    }

    /** Removes the waypoints of this world that were made in other dimensions; for UI snapshots. */
    public void clearOtherDimensionsForSnapshot(Minecraft client) {
        if (client == null || client.level == null) {
            return;
        }
        String worldKey = worldKey(client);
        String dimension = dimensionId(client.level);
        if (waypoints.removeIf(waypoint -> waypoint.matchesWorldKey(worldKey) && !waypoint.matchesDimension(dimension))) {
            save();
        }
    }

    /**
     * Whether this world already has a waypoint at that block in {@code inDimension} (the one you are in when
     * null). A null {@code y} matches any height.
     */
    public boolean hasWaypointAt(Minecraft client, @Nullable String inDimension, int x, @Nullable Integer y, int z) {
        if (client == null || client.level == null) {
            return false;
        }
        String worldKey = worldKey(client);
        String dimension = inDimension == null || inDimension.isBlank() ? dimensionId(client.level) : inDimension;
        for (Waypoint waypoint : waypoints) {
            if (matchesWorld(waypoint, worldKey, dimension) && waypoint.x() == x && waypoint.z() == z && (y == null || waypoint.y() == y)) {
                return true;
            }
        }
        return false;
    }

    /** Says the waypoint in chat, as the server's players will see it: only when the player asks for it (#105). */
    public void shareInChat(Minecraft client, String id) {
        Waypoint waypoint = findById(id);
        if (waypoint == null || client == null || client.level == null || client.getConnection() == null) {
            return;
        }

        String text = WaypointShare.format(waypoint, EMUtilsClient.config().waypointShareFormat(), dimensionId(client.level));
        client.getConnection().sendChat(text);
        if (client.gui != null) {
            net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
                .addClientSystemMessage(
                    EmUtilsChatPrefix.chat(
                        Component.translatable(
                            EMUtilsTexts.WAYPOINT_SHARED,
                            waypoint.label()
                        ).withStyle(ChatFormatting.GREEN)
                    )
                );
        }
    }

    /** Says a spot in chat in the share format, as a waypoint named {@code label}; the world map's Share Location (#215). */
    public void shareLocation(Minecraft client, String label, int x, int y, int z, String dimension) {
        if (client == null || client.level == null || client.getConnection() == null) {
            return;
        }
        Waypoint spot = new Waypoint(x, y, z, dimension, worldKey(client), System.currentTimeMillis(), label, EMUtilsClient.config().waypointDefaultCustomColor(), WaypointType.CUSTOM);
        client.getConnection().sendChat(WaypointShare.format(spot, EMUtilsClient.config().waypointShareFormat(), dimensionId(client.level)));
        if (client.gui != null) {
            net.emutils.client.emutils.compat.MinecraftClientCompat.chat(client)
                .addClientSystemMessage(
                    EmUtilsChatPrefix.chat(
                        Component.translatable(EMUtilsTexts.WAYPOINT_SHARED, label).withStyle(ChatFormatting.GREEN)
                    )
                );
        }
    }

    /** The names of the sets in use in this world, alphabetically. */
    public List<String> setsForCurrentWorld(Minecraft client) {
        return entriesForCurrentWorld(client, true)
            .stream()
            .map(entry -> entry.waypoint().set())
            .filter(set -> !set.isEmpty())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
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
            !renderEntries(client).isEmpty()
        );
    }

    /** How far the player is from the entry in its dimension, in blocks; only meaningful for a placeable entry. */
    public double distance(Minecraft client, WaypointEntry entry) {
        double dx = client.player.getX() - entry.renderX();
        double dy = client.player.getY() - entry.renderY();
        double dz = client.player.getZ() - entry.renderZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public int distanceBlocks(Minecraft client, WaypointEntry entry) {
        return (int) Math.round(distance(client, entry));
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
    private Waypoint findById(String id) {
        for (Waypoint waypoint : waypoints) {
            if (waypoint.id().equals(id)) {
                return waypoint;
            }
        }

        return null;
    }

    private static double distanceSquared(double x, double y, double z, Waypoint waypoint) {
        double dx = x - renderX(waypoint);
        double dy = y - renderY(waypoint);
        double dz = z - renderZ(waypoint);
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

        List<Waypoint> before = new ArrayList<>(waypoints);
        waypoints.removeIf(wp -> matchesWorld(wp, worldKey, dimension));
        if (!save()) {
            // Not written, so they would all be back after a restart; keep them and say so.
            waypoints.clear();
            waypoints.addAll(before);
            reportSaveFailed(client);
            return;
        }
        for (Waypoint waypoint : removed) {
            promptedThisSession.remove(waypoint.id());
            removeWaypoint(client, waypoint);
        }

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
        int index = waypoints.indexOf(waypoint);
        if (index < 0) {
            return;
        }

        waypoints.remove(index);
        if (!save()) {
            waypoints.add(index, waypoint);
            reportSaveFailed(client);
            return;
        }
        promptedThisSession.remove(waypoint.id());
        removeWaypoint(client, waypoint);

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
                // The old file is only deleted once the new one is written, so a failed write can be tried again.
                legacyDeathFilePending = true;
                if (save() && !legacyDeathFilePending) {
                    EMUtilsClient.LOGGER.info(
                        "Migrated {} death waypoints to unified format.",
                        waypoints.size()
                    );
                } else {
                    EMUtilsClient.LOGGER.warn(
                        "Could not save the migrated death waypoints; keeping {} to try again.",
                        EMUtilsPaths.deathWaypointFile().getFileName()
                    );
                }
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

    /** Deletes the old death waypoint file after its waypoints were written to the new one; if that fails, it is tried again with the next save. */
    private void deleteLegacyDeathFileIfPending() {
        if (!legacyDeathFilePending) {
            return;
        }
        try {
            Files.deleteIfExists(EMUtilsPaths.deathWaypointFile());
            legacyDeathFilePending = false;
        } catch (IOException exception) {
            EMUtilsClient.LOGGER.warn("Failed to delete the old death waypoint file.", exception);
        }
    }

    /** Writes the waypoints; returns false if that failed. */
    private boolean save() {
        try {
            Files.createDirectories(EMUtilsPaths.configDir());
            if (waypoints.isEmpty()) {
                Files.deleteIfExists(EMUtilsPaths.waypointFile());
                deleteLegacyDeathFileIfPending();
                return true;
            }

            WaypointSaveData saveData = new WaypointSaveData();
            saveData.setWaypoints(new ArrayList<>(waypoints));
            AtomicFiles.writeString(EMUtilsPaths.waypointFile(), GSON.toJson(saveData));
            deleteLegacyDeathFileIfPending();
            return true;
        } catch (IOException exception) {
            EMUtilsClient.LOGGER.warn("Failed to save waypoints.", exception);
            return false;
        }
    }

    /** The dimension's id, such as {@code minecraft:overworld}; the map keys its files by it too (#215). */
    public static String dimensionId(ClientLevel world) {
        ResourceKey<Level> key = world.dimension();
        return key.identifier().toString();
    }

    /** Which world or server you are in, as waypoints and the map (#215) store it; empty when in none. */
    public static String worldKey(Minecraft client) {
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
