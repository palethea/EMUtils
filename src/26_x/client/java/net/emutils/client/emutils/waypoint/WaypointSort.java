package net.emutils.client.emutils.waypoint;

import java.util.Comparator;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/** How the waypoints list is ordered (#105). */
public enum WaypointSort {
	DISTANCE("emutils.ui.waypoint.sort.distance"),
	NAME("emutils.ui.waypoint.sort.name"),
	NEWEST("emutils.ui.waypoint.sort.newest");

	private final String labelKey;

	WaypointSort(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public WaypointSort next() {
		WaypointSort[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static WaypointSort fromName(String name) {
		for (WaypointSort sort : values()) {
			if (sort.name().equalsIgnoreCase(name)) {
				return sort;
			}
		}
		return DISTANCE;
	}

	/**
	 * The order for this sort. {@code distance} is how far an entry is from the player; it is only asked for
	 * entries that have a position here, and the ones that don't come last when sorting by distance, by name.
	 */
	public Comparator<WaypointEntry> comparator(ToDoubleFunction<WaypointEntry> distance) {
		Comparator<WaypointEntry> byName = Comparator
			.comparing((WaypointEntry entry) -> label(entry).toLowerCase(Locale.ROOT))
			.thenComparing(entry -> label(entry));
		return switch (this) {
			case NAME -> byName;
			case NEWEST -> Comparator.comparingLong((WaypointEntry entry) -> entry.waypoint().timestamp()).reversed().thenComparing(byName);
			case DISTANCE -> Comparator
				.comparing((WaypointEntry entry) -> !entry.placeable())
				.thenComparingDouble(entry -> entry.placeable() ? distance.applyAsDouble(entry) : 0.0D)
				.thenComparing(byName);
		};
	}

	private static String label(WaypointEntry entry) {
		String label = entry.waypoint().label();
		return label == null ? "" : label;
	}
}
