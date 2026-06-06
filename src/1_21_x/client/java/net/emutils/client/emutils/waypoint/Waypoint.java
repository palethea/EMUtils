package net.emutils.client.emutils.waypoint;

public final class Waypoint {
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

	public Waypoint() {
	}

	public Waypoint(int x, int y, int z, String dimension, String serverAddress, long timestamp, String label, int color, WaypointType type) {
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

		return false;
	}

	public boolean sameBlock(int blockX, int blockY, int blockZ) {
		return x == blockX && y == blockY && z == blockZ;
	}
}
