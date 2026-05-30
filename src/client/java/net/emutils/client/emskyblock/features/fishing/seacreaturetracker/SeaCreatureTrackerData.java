package net.emutils.client.emskyblock.features.fishing.seacreaturetracker;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SeaCreatureTrackerData {
	public Map<String, Integer> counts = new LinkedHashMap<>();
	public long sessionStartMs;

	public void add(String creatureId, int amount) {
		counts.merge(creatureId, amount, Integer::sum);
		if (sessionStartMs <= 0L) {
			sessionStartMs = System.currentTimeMillis();
		}
	}

	public void resetCounts() {
		counts.clear();
		sessionStartMs = System.currentTimeMillis();
	}

	public int total() {
		int total = 0;
		for (int value : counts.values()) {
			total += value;
		}

		return total;
	}
}
