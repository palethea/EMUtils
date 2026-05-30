package net.emutils.client.emutils.waypoint;

import net.emutils.client.emhelpers.util.EMUtilsTexts;

public enum WaypointCoordinateFormat {
	PLAIN(EMUtilsTexts.WAYPOINT_COORD_FORMAT_PLAIN),
	COMMA(EMUtilsTexts.WAYPOINT_COORD_FORMAT_COMMA),
	TP_COMMAND(EMUtilsTexts.WAYPOINT_COORD_FORMAT_TP_COMMAND);

	private final String labelKey;

	WaypointCoordinateFormat(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public WaypointCoordinateFormat next() {
		WaypointCoordinateFormat[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static WaypointCoordinateFormat fromName(String name) {
		if (name != null) {
			for (WaypointCoordinateFormat format : values()) {
				if (format.name().equalsIgnoreCase(name)) {
					return format;
				}
			}
		}

		return PLAIN;
	}
}
