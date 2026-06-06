package net.emutils.client.emutils.waypoint;

import net.emutils.client.emutils.util.EMUtilsTexts;

public enum WaypointType {
	DEATH(EMUtilsTexts.WAYPOINT_TYPE_DEATH),
	CUSTOM(EMUtilsTexts.WAYPOINT_TYPE_CUSTOM);

	private final String labelKey;

	WaypointType(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static WaypointType fromName(String name) {
		if (name != null) {
			for (WaypointType type : values()) {
				if (type.name().equalsIgnoreCase(name)) {
					return type;
				}
			}
		}

		return CUSTOM;
	}
}
