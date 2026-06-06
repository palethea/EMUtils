package net.emutils.client.emutils.waypoint;

public final class WaypointCoordinates {
	private WaypointCoordinates() {
	}

	public static String plain(Waypoint waypoint) {
		return waypoint.x() + " " + waypoint.y() + " " + waypoint.z();
	}

	public static String format(Waypoint waypoint, WaypointCoordinateFormat format) {
		return switch (format == null ? WaypointCoordinateFormat.PLAIN : format) {
			case COMMA -> waypoint.x() + ", " + waypoint.y() + ", " + waypoint.z();
			case TP_COMMAND -> "/tp @s " + plain(waypoint);
			case PLAIN -> plain(waypoint);
		};
	}
}
