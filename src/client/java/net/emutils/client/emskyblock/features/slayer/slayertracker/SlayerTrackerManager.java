package net.emutils.client.emskyblock.features.slayer.slayertracker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerDisplayMode;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerPanelLine;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EivCoinFormat;
import net.emutils.client.emskyblock.features.slayer.common.SlayerBossType;
import net.emutils.client.emskyblock.features.slayer.common.SlayerItemRegistry;
import net.emutils.client.emskyblock.pricing.SkyblockPrices;
import net.emutils.client.emskyblock.sacks.SkyblockSackChange;
import net.emutils.client.emskyblock.sacks.SkyblockSackChangeBatch;
import net.emutils.client.emskyblock.sacks.SkyblockSackTracker;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class SlayerTrackerManager {
	private static final long DROP_DEDUPE_MS = 1_500L;
	private static final long SACK_DROP_SUPPRESS_MS = 2_000L;

	private static TrackerDisplayMode displayMode = TrackerDisplayMode.SESSION;
	private static final java.util.Map<String, Long> lastDropMs = new java.util.HashMap<>();
	private static final java.util.Map<String, Long> lastSackMs = new java.util.HashMap<>();
	private static final java.util.Map<String, Long> lastInventoryMs = new java.util.HashMap<>();

	@Nullable
	private static SlayerBossType activeBoss;

	private SlayerTrackerManager() {}

	public static TrackerDisplayMode displayMode() {
		return displayMode;
	}

	public static void cycleDisplayMode() {
		displayMode = displayMode.next();
	}

	public static void resetCurrentMode() {
		if (displayMode == TrackerDisplayMode.SESSION) {
			SlayerTrackerStorage.resetSessionAll();
		} else {
			SlayerTrackerStorage.resetAllTimeAll();
		}
	}

	@Nullable
	public static SlayerBossType activeBoss() {
		return activeBoss;
	}

	public static void onQuestStarted() {
		activeBoss = null;
		lastDropMs.clear();
		lastSackMs.clear();
		lastInventoryMs.clear();
	}

	public static void onQuestType(SlayerBossType boss) {
		activeBoss = boss;
	}

	public static void onBossKilled() {
		SlayerBossType boss = resolveActive();
		if (boss == null) {
			return;
		}
		recordKill(boss);
		activeBoss = null;
	}

	public static void onMiniBossAssist() {
		SlayerBossType boss = resolveActive();
		if (boss == null) {
			return;
		}
	}

	public static void onMaddoxClaim() {
		activeBoss = null;
	}

	public static void onDropDetected(SlayerBossType boss, String itemId, long amount) {
		if (boss == null || itemId == null || amount <= 0L) {
			return;
		}
		if (!SlayerItemRegistry.isAllowedFor(itemId, boss)) {
			return;
		}

		long now = System.currentTimeMillis();
		String key = boss.name() + "\u0000" + itemId;
		Long previous = lastDropMs.get(key);
		if (previous != null && now - previous < DROP_DEDUPE_MS) {
			return;
		}
		lastDropMs.put(key, now);

		recordItem(boss, itemId, amount);
	}

	public static void onSackChange(SkyblockSackChangeBatch batch) {
		if (SkyblockSackTracker.isManualSackInteractionRecent()) {
			return;
		}
		SlayerBossType boss = resolveActive();
		if (boss == null) {
			return;
		}

		long now = System.currentTimeMillis();
		boolean anyTracked = false;
		for (SkyblockSackChange change : batch.changes()) {
			if (change.delta() <= 0) {
				continue;
			}
			if (!SlayerItemRegistry.isAllowedFor(change.itemId(), boss)) {
				continue;
			}

			String key = change.itemId().toLowerCase(Locale.ROOT);
			Long lastSack = lastSackMs.get(key);
			if (lastSack != null && now - lastSack < SACK_DROP_SUPPRESS_MS) {
				continue;
			}

			Long lastInv = lastInventoryMs.get(key);
			if (lastInv != null && now - lastInv < SACK_DROP_SUPPRESS_MS) {
				lastSackMs.put(key, now);
				continue;
			}

			recordItem(boss, change.itemId(), change.delta());
			lastSackMs.put(key, now);
			anyTracked = true;
		}

		if (anyTracked) {
			SlayerTrackerStorage.saveIfDirty();
		}
	}

	@Nullable
	private static SlayerBossType resolveActive() {
		if (activeBoss != null) {
			return activeBoss;
		}
		SlayerBossType fromArea = SlayerBossType.forIsland(
			SkyblockContext.island(),
			SkyblockContext.area()
		);
		return fromArea;
	}

	public static boolean shouldShow(MinecraftClient client) {
		if (!EMSkyblockSettings.skyblockEnabled()) {
			return false;
		}
		if (!EMSkyblockSettings.slayerProfitTrackerEnabled()) {
			return false;
		}
		if (client == null || client.player == null || client.world == null) {
			return false;
		}
		if (!SkyblockContext.inSkyBlock()) {
			return false;
		}
		return resolveActive() != null;
	}

	public static List<TrackerPanelLine> lines(
		@Nullable MinecraftClient client,
		boolean preview
	) {
		List<TrackerPanelLine> lines = new ArrayList<>();
		lines.add(
			TrackerPanelLine.header(
				TrackerPanelLine.TrackerHeaderParts.slayerProfit(displayMode)
			)
		);

		SkyblockPrices prices = preview ? null : EMUtilsClient.skyblockPrices();
		double totalProfit = 0.0D;
		long totalKills = 0L;

		List<SlayerBossType> ordered = orderedBosses();
		for (SlayerBossType boss : ordered) {
			SlayerTrackerData data = preview
				? previewData(boss)
				: SlayerTrackerStorage.bucket(boss, displayMode);

			if (
				data.items.isEmpty() &&
				data.bossesKilled <= 0L &&
				!preview
			) {
				continue;
			}

			lines.add(TrackerPanelLine.of("§e" + boss.displayName() + "§7:"));

			for (Map.Entry<String, SlayerTrackerData.TrackedItem> entry : data.items.entrySet()) {
				long amount = entry.getValue().amount;
				if (amount <= 0L) {
					continue;
				}
				double unit = unitPrice(prices, entry.getKey(), preview);
				double profit = unit * amount;
				totalProfit += profit;
				String priceText = profit > 0.0D
					? " §7(§6" + EivCoinFormat.compact(profit) + "§7)"
					: "";
				lines.add(
					TrackerPanelLine.of(
						"  §7› §7" +
							formatCount(amount) +
							"x §f" +
							SlayerItemRegistry.displayName(entry.getKey()) +
							priceText
					)
				);
			}

			if (data.bossesKilled > 0L) {
				totalKills += data.bossesKilled;
				lines.add(
					TrackerPanelLine.of(
						"  §7› §a" +
							formatCount(data.bossesKilled) +
							" §7bosses killed"
					)
				);
			}
		}

		if (lines.size() == 1) {
			lines.add(TrackerPanelLine.of("§7No slayer drops yet."));
		}

		lines.add(
			TrackerPanelLine.of(
				"§7" +
					(displayMode == TrackerDisplayMode.SESSION
						? "Session"
						: "Total") +
					" Profit: §6" +
					EivCoinFormat.compact(totalProfit) +
					" coins"
			)
		);

		if (totalKills > 0L) {
			lines.add(
				TrackerPanelLine.of(
					"§7Total Bosses: §a" + formatCount(totalKills)
				)
			);
		}

		if (
			EMSkyblockSettings.slayerProfitTrackerShowUptime() &&
			lines.size() > 0
		) {
			long earliest = Long.MAX_VALUE;
			for (SlayerBossType boss : SlayerBossType.values()) {
				SlayerTrackerData d = SlayerTrackerStorage.bucket(
					boss,
					displayMode
				);
				if (d.sessionStartMs > 0L && d.sessionStartMs < earliest) {
					earliest = d.sessionStartMs;
				}
			}
			if (earliest < Long.MAX_VALUE) {
				long elapsed = Math.max(0L, System.currentTimeMillis() - earliest);
				if (elapsed > 0L && totalProfit > 0.0D) {
					double perHour = (totalProfit * 3_600_000.0D) / elapsed;
					lines.add(
						TrackerPanelLine.of(
							"§7Profit Per Hour: §6" +
								EivCoinFormat.compact(perHour) +
								" coins"
						)
					);
				}
				lines.add(
					TrackerPanelLine.of("§7Uptime §a" + formatDuration(elapsed))
				);
			}
		}

		return lines;
	}

	private static List<SlayerBossType> orderedBosses() {
		List<SlayerBossType> bosses = new ArrayList<>();
		bosses.addAll(List.of(SlayerBossType.values()));
		bosses.sort(Comparator.comparingInt(SlayerBossType::ordinal));
		return bosses;
	}

	private static double unitPrice(
		@Nullable SkyblockPrices prices,
		String itemId,
		boolean preview
	) {
		if (prices == null) {
			return preview ? 10_000.0D : 0.0D;
		}
		SkyblockPrices.PriceResult result = prices.price(itemId);
		return result.known() ? result.amount() : 0.0D;
	}

	private static SlayerTrackerData previewData(SlayerBossType boss) {
		SlayerTrackerData data = new SlayerTrackerData();
		switch (boss) {
			case REVENANT -> {
				data.addItem("REVENANT_FLESH", 8);
				data.addItem("REVENANT_VISCERA", 2);
				data.bossesKilled = 3;
			}
			case TARANTULA -> {
				data.addItem("TARANTULA_WEB", 12);
				data.addItem("TOXIC_ARROW_POISON", 4);
				data.bossesKilled = 1;
			}
			case SVEN -> {
				data.addItem("WOLF_TOOTH", 5);
				data.bossesKilled = 2;
			}
			case VOID -> {
				data.addItem("NULL_SPHERE", 6);
				data.bossesKilled = 1;
			}
			case INFERNO -> {
				data.addItem("DERELICT_ASHE", 4);
				data.bossesKilled = 1;
			}
			case VAMPIRE -> {
				data.addItem("COVEN_SEAL", 1);
				data.bossesKilled = 1;
			}
		}
		data.sessionStartMs = System.currentTimeMillis() - 900_000L;
		return data;
	}

	private static void recordItem(SlayerBossType boss, String itemId, long amount) {
		SlayerTrackerStorage.bucket(boss, TrackerDisplayMode.SESSION).addItem(
			itemId,
			amount
		);
		SlayerTrackerStorage.bucket(boss, TrackerDisplayMode.ALL_TIME).addItem(
			itemId,
			amount
		);
		SlayerTrackerStorage.saveIfDirty();
	}

	private static void recordKill(SlayerBossType boss) {
		SlayerTrackerStorage.bucket(boss, TrackerDisplayMode.SESSION).recordKill();
		SlayerTrackerStorage.bucket(boss, TrackerDisplayMode.ALL_TIME).recordKill();
		SlayerTrackerStorage.saveIfDirty();
	}

	private static String formatCount(long value) {
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
