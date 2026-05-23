package net.emutils.client.death;

public final class DeathWaypointCoordinates {
	private DeathWaypointCoordinates() {
	}

	public static String plain(DeathLocation location) {
		return location.x() + " " + location.y() + " " + location.z();
	}
}
