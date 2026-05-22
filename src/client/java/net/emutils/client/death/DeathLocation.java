package net.emutils.client.death;

public final class DeathLocation {
	private int x;
	private int y;
	private int z;
	private String dimension;
	private String serverAddress;
	private long deathTimestamp;
	private boolean nearPromptShown;

	public DeathLocation() {
	}

	public DeathLocation(int x, int y, int z, String dimension, String serverAddress, long deathTimestamp) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.serverAddress = serverAddress;
		this.deathTimestamp = deathTimestamp;
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

	public long deathTimestamp() {
		return deathTimestamp;
	}

	public boolean nearPromptShown() {
		return nearPromptShown;
	}

	public void setNearPromptShown(boolean nearPromptShown) {
		this.nearPromptShown = nearPromptShown;
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

		// Older saves stored the multiplayer address without a prefix.
		if (!serverAddress.contains(":")) {
			return ("multiplayer:" + serverAddress).equals(otherWorldKey);
		}

		return false;
	}

	public boolean sameBlock(int blockX, int blockY, int blockZ) {
		return x == blockX && y == blockY && z == blockZ;
	}
}
