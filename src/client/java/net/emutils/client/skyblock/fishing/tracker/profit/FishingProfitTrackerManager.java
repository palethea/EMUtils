package net.emutils.client.skyblock.fishing.tracker.profit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.emutils.client.skyblock.SkyblockPrices;
import net.emutils.client.skyblock.config.EMSkyblockSettings;
import net.emutils.client.skyblock.eiv.EivCoinFormat;
import net.emutils.client.skyblock.fishing.FishingActivity;
import net.emutils.client.skyblock.sacks.SkyblockSackChange;
import net.emutils.client.skyblock.sacks.SkyblockSackChangeBatch;
import net.emutils.client.skyblock.sacks.SkyblockSackTracker;
import net.emutils.client.skyblock.tracker.FishingTrackerStorage;
import net.emutils.client.skyblock.tracker.TrackerDisplayMode;
import net.emutils.client.skyblock.tracker.TrackerPanelLine;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class FishingProfitTrackerManager {
	private static final Pattern COINS_CATCH = Pattern.compile(
		".+ CATCH! .+You caught .+(?<coins>[\\d,]+) Coins.+",
		Pattern.CASE_INSENSITIVE
	);
	private static final long CATCH_DEDUPE_MS = 1_500L;

	private static TrackerDisplayMode displayMode = TrackerDisplayMode.SESSION;
	private static long lastCatchRecordMs;

	private FishingProfitTrackerManager() {
	}

	public static TrackerDisplayMode displayMode() {
		return displayMode;
	}

	public static void cycleDisplayMode() {
		displayMode = displayMode.next();
	}

	public static void onPullReady() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!EMSkyblockSettings.skyblockEnabled()
			|| !EMSkyblockSettings.fishingProfitTrackerEnabled()
			|| !SkyblockFeatures.inSkyBlock(client)) {
			return;
		}

		recordCatch();
	}

	public static void onCoinsChat(String message) {
		if (!FishingActivity.isFishing(MinecraftClient.getInstance())) {
			return;
		}

		Matcher matcher = COINS_CATCH.matcher(message);
		if (!matcher.matches()) {
			return;
		}

		long coins = parseInt(matcher.group("coins"));
		if (coins <= 0L) {
			return;
		}

		recordItem(FishingProfitItemRegistry.SKYBLOCK_COIN, coins);
		recordCatch();
	}

	public static void onItemPickup(String itemId, int amount) {
		if (amount <= 0) {
			return;
		}

		Map<String, Long> items = new HashMap<>();
		items.put(itemId, (long) amount);
		onItemPickups(items);
	}

	public static void onItemPickups(Map<String, Long> items) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!canTrackFishingLoot(client)) {
			return;
		}

		boolean anyTracked = false;
		for (Map.Entry<String, Long> entry : items.entrySet()) {
			long amount = entry.getValue();
			if (amount <= 0L || !FishingProfitItemRegistry.isAllowed(entry.getKey())) {
				continue;
			}

			recordItem(entry.getKey(), amount);
			anyTracked = true;
		}

		if (anyTracked) {
			recordCatch();
		}
	}

	public static void onSackChange(SkyblockSackChangeBatch batch) {
		if (SkyblockSackTracker.isManualSackInteractionRecent()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!canTrackFishingLoot(client)) {
			return;
		}

		boolean anyTracked = false;
		for (SkyblockSackChange change : batch.changes()) {
			if (change.delta() <= 0 || !FishingProfitItemRegistry.isAllowed(change.itemId())) {
				continue;
			}

			recordItem(change.itemId(), change.delta());
			anyTracked = true;
		}

		if (anyTracked) {
			recordCatch();
		}
	}

	public static void recordCatch() {
		long now = System.currentTimeMillis();
		if (lastCatchRecordMs > 0L && now - lastCatchRecordMs < CATCH_DEDUPE_MS) {
			FishingActivity.onCatch();
			return;
		}

		lastCatchRecordMs = now;
		FishingTrackerStorage.fishingProfit(TrackerDisplayMode.SESSION).addCatch();
		FishingTrackerStorage.fishingProfit(TrackerDisplayMode.ALL_TIME).addCatch();
		FishingTrackerStorage.saveIfDirty();
		FishingActivity.onCatch();
	}

	private static void recordItem(String itemId, long amount) {
		FishingTrackerStorage.fishingProfit(TrackerDisplayMode.SESSION).addItem(itemId, amount);
		FishingTrackerStorage.fishingProfit(TrackerDisplayMode.ALL_TIME).addItem(itemId, amount);
		FishingTrackerStorage.saveIfDirty();
	}

	private static boolean canTrackFishingLoot(MinecraftClient client) {
		return EMSkyblockSettings.skyblockEnabled()
			&& EMSkyblockSettings.fishingProfitTrackerEnabled()
			&& SkyblockFeatures.inSkyBlock(client)
			&& FishingActivity.isFishing(client);
	}

	public static boolean shouldShow(MinecraftClient client) {
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.fishingProfitTrackerEnabled()) {
			return false;
		}

		if (!SkyblockFeatures.inSkyBlock(client)) {
			return false;
		}

		if (EMSkyblockSettings.fishingProfitTrackerShowWhenPickup()) {
			return FishingActivity.isFishing(client);
		}

		return FishingActivity.isFishingStrict(client);
	}

	public static List<TrackerPanelLine> lines(@Nullable MinecraftClient client, boolean preview) {
		FishingProfitTrackerData data = preview
			? previewData()
			: FishingTrackerStorage.fishingProfit(displayMode);
		List<TrackerPanelLine> lines = new ArrayList<>();
		lines.add(TrackerPanelLine.header(TrackerPanelLine.TrackerHeaderParts.fishingProfit(displayMode)));

		SkyblockPrices prices = EMUtilsClient.skyblockPrices();
		List<ItemLine> itemLines = buildItemLines(data, prices, preview);
		double totalProfit = 0.0D;
		for (ItemLine itemLine : itemLines) {
			totalProfit += itemLine.profit();
			lines.add(TrackerPanelLine.of(itemLine.text()));
		}

		lines.add(TrackerPanelLine.of("§7Times fished: §a" + formatCount(data.totalCatchAmount)));
		lines.add(TrackerPanelLine.of("§7" + (displayMode == TrackerDisplayMode.SESSION ? "Session" : "Total") + " Profit: §6" + EivCoinFormat.compact(totalProfit) + " coins"));

		if (EMSkyblockSettings.fishingProfitTrackerShowUptime() && data.sessionStartMs > 0L) {
			long elapsed = Math.max(0L, System.currentTimeMillis() - data.sessionStartMs);
			if (elapsed > 0L && totalProfit > 0.0D) {
				double perHour = totalProfit * 3_600_000.0D / elapsed;
				lines.add(TrackerPanelLine.of("§7Profit Per Hour: §6" + EivCoinFormat.compact(perHour) + " coins"));
			}
			lines.add(TrackerPanelLine.of("§7Uptime §a" + formatDuration(elapsed)));
		}

		return lines;
	}

	private static List<ItemLine> buildItemLines(
		FishingProfitTrackerData data,
		@Nullable SkyblockPrices prices,
		boolean preview
	) {
		List<ItemLine> lines = new ArrayList<>();
		for (Map.Entry<String, FishingProfitTrackerData.TrackedItem> entry : data.items.entrySet()) {
			long amount = entry.getValue().amount;
			if (amount <= 0L) {
				continue;
			}

			double unitPrice = unitPrice(prices, entry.getKey(), preview);
			double profit = unitPrice * amount;
			String name = displayName(entry.getKey());
			String priceText = profit > 0.0D ? " §7(§6" + EivCoinFormat.compact(profit) + "§7)" : "";
			lines.add(new ItemLine(
				" §7- §7" + formatCount(amount) + "x " + name + priceText,
				profit
			));
		}

		lines.sort(Comparator.comparingDouble(ItemLine::profit).reversed());
		if (lines.size() > EMSkyblockSettings.fishingProfitTrackerMaxLines()) {
			return lines.subList(0, EMSkyblockSettings.fishingProfitTrackerMaxLines());
		}

		return lines;
	}

	private static double unitPrice(@Nullable SkyblockPrices prices, String itemId, boolean preview) {
		if (FishingProfitItemRegistry.SKYBLOCK_COIN.equals(itemId)) {
			return 1.0D;
		}

		if (prices == null) {
			return preview ? 10_000.0D : 0.0D;
		}

		SkyblockPrices.PriceResult result = prices.price(itemId);
		return result.known() ? result.amount() : 0.0D;
	}

	private static String displayName(String itemId) {
		if (FishingProfitItemRegistry.SKYBLOCK_COIN.equals(itemId)) {
			return "§6Fished Coins";
		}

		return "§f" + formatDisplayName(itemId);
	}

	private static String formatDisplayName(String itemId) {
		String[] parts = itemId.toLowerCase(Locale.ROOT).split("_");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}

			if (!builder.isEmpty()) {
				builder.append(' ');
			}

			builder.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				builder.append(part.substring(1));
			}
		}

		return builder.toString();
	}

	private static FishingProfitTrackerData previewData() {
		FishingProfitTrackerData data = new FishingProfitTrackerData();
		data.addItem("FAIRY_LEGGINGS", 3);
		data.totalCatchAmount = 0L;
		data.sessionStartMs = System.currentTimeMillis() - 461_000L;
		return data;
	}

	private static long parseInt(String raw) {
		try {
			return Long.parseLong(raw.replace(",", ""));
		} catch (NumberFormatException ignored) {
			return 0L;
		}
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

	private record ItemLine(String text, double profit) {
	}
}
