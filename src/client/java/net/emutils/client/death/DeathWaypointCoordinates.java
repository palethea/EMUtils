package net.emutils.client.death;

public final class DeathWaypointCoordinates {
	private DeathWaypointCoordinates() {
	}

	public static String plain(DeathLocation location) {
		return location.x() + " " + location.y() + " " + location.z();
	}

	public static String format(DeathLocation location, DeathWaypointCoordinateFormat format) {
		return switch (format == null ? DeathWaypointCoordinateFormat.PLAIN : format) {
			case COMMA -> location.x() + ", " + location.y() + ", " + location.z();
			case TP_COMMAND -> "/tp @s " + plain(location);
			case PLAIN -> plain(location);
		};
	}
}
