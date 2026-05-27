package net.emutils.client.skyblock.fishing.tracker.seacreature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.emutils.client.skyblock.config.EMSkyblockSettings;
import net.emutils.client.skyblock.fishing.FishingActivity;
import net.emutils.client.skyblock.tracker.FishingTrackerStorage;
import net.emutils.client.skyblock.tracker.TrackerDisplayMode;
import net.emutils.client.skyblock.tracker.TrackerPanelLine;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class SeaCreatureTrackerManager {
	private static final long DOUBLE_HOOK_TIMEOUT_MS = 8_000L;

	private static TrackerDisplayMode displayMode = TrackerDisplayMode.SESSION;
	private static boolean pendingDoubleHook;
	private static long pendingDoubleHookMs;

	private SeaCreatureTrackerManager() {
	}

	public static TrackerDisplayMode displayMode() {
		return displayMode;
	}

	public static void cycleDisplayMode() {
		displayMode = displayMode.next();
	}

	public static void onDoubleHookChat() {
		pendingDoubleHook = true;
		pendingDoubleHookMs = System.currentTimeMillis();
	}

	public static boolean onSeaCreatureChat(String cleanMessage) {
		return onSeaCreatureChat(cleanMessage, true);
	}

	public static boolean onSeaCreatureChat(String cleanMessage, boolean recordTracker) {
		SeaCreatureDefinition creature = SeaCreatureRegistry.fromChat(cleanMessage);
		if (creature == null) {
			if (pendingDoubleHook && System.currentTimeMillis() - pendingDoubleHookMs > DOUBLE_HOOK_TIMEOUT_MS) {
				pendingDoubleHook = false;
				pendingDoubleHookMs = 0L;
			}
			return false;
		}

		int amount = pendingDoubleHook && EMSkyblockSettings.seaCreatureTrackerCountDouble() ? 2 : 1;
		pendingDoubleHook = false;
		pendingDoubleHookMs = 0L;
		FishingActivity.onSeaCreatureHook(MinecraftClient.getInstance());
		if (recordTracker) {
			recordCatch(creature.id(), amount);
		}
		return true;
	}

	private static void recordCatch(String creatureId, int amount) {
		FishingTrackerStorage.seaCreature(TrackerDisplayMode.SESSION).add(creatureId, amount);
		FishingTrackerStorage.seaCreature(TrackerDisplayMode.ALL_TIME).add(creatureId, amount);
		FishingTrackerStorage.saveIfDirty();
	}

	public static boolean shouldShow(MinecraftClient client) {
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.seaCreatureTrackerEnabled()) {
			return false;
		}

		if (!SkyblockFeatures.inSkyBlock(client)) {
			return false;
		}

		if (EMSkyblockSettings.seaCreatureTrackerOnlyWhileFishing()) {
			return FishingActivity.isFishing(client);
		}

		return client.player != null;
	}

	public static List<TrackerPanelLine> lines(@Nullable MinecraftClient client, boolean preview) {
		SeaCreatureTrackerData data = preview
			? previewData()
			: FishingTrackerStorage.seaCreature(displayMode);
		List<TrackerPanelLine> lines = new ArrayList<>();
		lines.add(TrackerPanelLine.header(TrackerPanelLine.TrackerHeaderParts.seaCreature(displayMode)));

		if (data.counts.isEmpty()) {
			lines.add(TrackerPanelLine.of("§7No sea creatures tracked yet."));
			return lines;
		}

		int total = data.total();
		data.counts.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
			.limit(EMSkyblockSettings.seaCreatureTrackerMaxLines())
			.forEach(entry -> {
				SeaCreatureDefinition definition = SeaCreatureRegistry.byId(entry.getKey());
				String name = definition == null ? entry.getKey() : definition.displayName();
				String color = definition == null ? "§e" : definition.chatColor();
				String suffix = "";
				if (EMSkyblockSettings.seaCreatureTrackerShowPercentage() && total > 0) {
					double pct = entry.getValue() * 100.0D / total;
					suffix = String.format(Locale.US, " §7%.1f%%", pct);
				}

				String rare = (definition != null && definition.rare()) ? "§l" : "";
				lines.add(TrackerPanelLine.of(
					" §7- §e" + formatCount(entry.getValue()) + " " + color + rare + name + suffix
				));
			});

		lines.add(TrackerPanelLine.of(" §7- §e" + formatCount(total) + " §7Total Sea Creatures"));
		if (EMSkyblockSettings.seaCreatureTrackerShowUptime() && data.sessionStartMs > 0L) {
			long elapsed = Math.max(0L, System.currentTimeMillis() - data.sessionStartMs);
			lines.add(TrackerPanelLine.of("§7Uptime §a" + formatDuration(elapsed)));
		}

		return lines;
	}

	private static SeaCreatureTrackerData previewData() {
		SeaCreatureTrackerData data = new SeaCreatureTrackerData();
		data.add("Catfish", 1);
		data.add("Rider of the Deep", 1);
		data.sessionStartMs = System.currentTimeMillis() - 271_000L;
		return data;
	}

	private static String formatCount(int value) {
		return String.format(Locale.US, "%,d", value);
	}

	private static String formatDuration(long millis) {
		long seconds = millis / 1000L;
		long minutes = seconds / 60L;
		long hours = minutes / 60L;
		if (hours > 0L) {
			return String.format(Locale.US, "%dh %dm", hours, minutes % 60L);
		}

		if (minutes > 0L) {
			return String.format(Locale.US, "%dm %ds", minutes, seconds % 60L);
		}

		return seconds + "s";
	}
}
