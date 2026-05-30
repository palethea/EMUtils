package net.emutils.client.emskyblock.features.fishing.trackercommon;

public enum TrackerDisplayMode {
	SESSION("Session"),
	ALL_TIME("All Time");

	private final String displayName;

	TrackerDisplayMode(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	public TrackerDisplayMode next() {
		return this == SESSION ? ALL_TIME : SESSION;
	}
}
