package net.emutils.client.emskyblock.features.slayer.slayertracker;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SlayerTrackerData {
	public Map<String, TrackedItem> items = new LinkedHashMap<>();
	public long bossesKilled;
	public long sessionStartMs;

	public void addItem(String itemId, long amount) {
		TrackedItem tracked = items.computeIfAbsent(itemId, ignored -> new TrackedItem());
		tracked.amount += amount;
		if (sessionStartMs <= 0L) {
			sessionStartMs = System.currentTimeMillis();
		}
	}

	public void recordKill() {
		bossesKilled++;
		if (sessionStartMs <= 0L) {
			sessionStartMs = System.currentTimeMillis();
		}
	}

	public void reset() {
		items.clear();
		bossesKilled = 0L;
		sessionStartMs = System.currentTimeMillis();
	}

	public static final class TrackedItem {
		public long amount;
	}
}
