package net.emutils.client.emutils.waypoint;

import java.util.UUID;

public final class Waypoint {
	/** Identifies the waypoint for good; null only in files saved before ids existed, until they are loaded. */
	private String id;
	private int x;
	private int y;
	private int z;
	private String dimension;
	private String serverAddress;
	private long timestamp;
	private boolean nearPromptShown;
	private String label;
	private int color;
	private String type;
	private Boolean beaconEnabled;
	private Boolean hidden;
	/** The set the waypoint is grouped in; null or blank when it is in none. */
	private String set;

	public Waypoint() {
	}

	public Waypoint(int x, int y, int z, String dimension, String serverAddress, long timestamp, String label, int color, WaypointType type) {
		this.id = UUID.randomUUID().toString();
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.serverAddress = serverAddress;
		this.timestamp = timestamp;
		this.label = label;
		this.color = color;
		this.type = type.name();
	}

	public String id() {
		return id;
	}

	/** Gives the waypoint a new id, for old files that have none and for ids that turn up twice. */
	void assignNewId() {
		id = UUID.randomUUID().toString();
	}

	boolean hasId() {
		return id != null && !id.isBlank();
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}

	public String dimension() {
		return dimension;
	}

	public String serverAddress() {
		return serverAddress;
	}

	public long timestamp() {
		return timestamp;
	}

	public boolean nearPromptShown() {
		return nearPromptShown;
	}

	public void setNearPromptShown(boolean nearPromptShown) {
		this.nearPromptShown = nearPromptShown;
	}

	public String label() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int color() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public WaypointType type() {
		return WaypointType.fromName(type);
	}

	public void setType(WaypointType type) {
		this.type = type.name();
	}

	public boolean isDeath() {
		return type() == WaypointType.DEATH;
	}

	public boolean beaconEnabled() {
		return beaconEnabled != null && beaconEnabled;
	}

	public void setBeaconEnabled(boolean beaconEnabled) {
		this.beaconEnabled = beaconEnabled;
	}

	public boolean hidden() {
		return hidden != null && hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	/** The set name, or an empty string when the waypoint is in none. */
	public String set() {
		return set == null ? "" : set;
	}

	public void setSet(String set) {
		this.set = set == null || set.isBlank() ? null : set.trim();
	}

	public boolean matchesDimension(String otherDimension) {
		return dimension != null && dimension.equals(otherDimension);
	}

	public boolean matchesWorldKey(String otherWorldKey) {
		if (serverAddress == null || serverAddress.isBlank()) {
			return otherWorldKey == null || otherWorldKey.isBlank();
		}

		if (serverAddress.equals(otherWorldKey)) {
			return true;
		}

		if (!serverAddress.contains(":")) {
			return ("multiplayer:" + serverAddress).equals(otherWorldKey);
		}

		String normalizedStored = normalizeWorldKey(serverAddress);
		String normalizedOther = normalizeWorldKey(otherWorldKey);
		if (normalizedStored.equals(normalizedOther)) {
			return true;
		}

		return normalizedOther.startsWith("realm:")
			&& normalizedStored.startsWith("multiplayer:")
			&& isLikelyRealmAddress(normalizedStored);
	}

	private static String normalizeWorldKey(String key) {
		return key == null ? "" : key.trim().toLowerCase(java.util.Locale.ROOT);
	}

	private static boolean isLikelyRealmAddress(String key) {
		return key.contains("realms.minecraft.net") || key.contains("mco");
	}

	public boolean sameBlock(int blockX, int blockY, int blockZ) {
		return x == blockX && y == blockY && z == blockZ;
	}
}
