package net.emutils.client.skyblock.fishing.tracker.profit;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FishingProfitTrackerData {
	public Map<String, TrackedItem> items = new LinkedHashMap<>();
	public long totalCatchAmount;
	public long sessionStartMs;

	public void addItem(String itemId, long amount) {
		TrackedItem tracked = items.computeIfAbsent(itemId, ignored -> new TrackedItem());
		tracked.amount += amount;
		if (sessionStartMs <= 0L) {
			sessionStartMs = System.currentTimeMillis();
		}
	}

	public void addCatch() {
		totalCatchAmount++;
		if (sessionStartMs <= 0L) {
			sessionStartMs = System.currentTimeMillis();
		}
	}

	public void reset() {
		items.clear();
		totalCatchAmount = 0L;
		sessionStartMs = System.currentTimeMillis();
	}

	public static final class TrackedItem {
		public long amount;
	}
}
