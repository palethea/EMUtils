package net.emutils.client.emutils.waypoint;

public final class WaypointCoordinates {
	private WaypointCoordinates() {
	}

	public static String format(Waypoint waypoint, WaypointCoordinateFormat format) {
		return format(waypoint.x(), waypoint.y(), waypoint.z(), format);
	}

	public static String format(int x, int y, int z, WaypointCoordinateFormat format) {
		return switch (format == null ? WaypointCoordinateFormat.PLAIN : format) {
			case COMMA -> x + ", " + y + ", " + z;
			case TP_COMMAND -> "/tp @s " + x + " " + y + " " + z;
			case PLAIN -> x + " " + y + " " + z;
		};
	}
}
