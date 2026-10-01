package net.emutils.client.emutils.waypoint;

import net.emutils.client.emutils.util.EMUtilsTexts;

/** How Share in chat writes a waypoint (#105). The Xaero's Minimap and JourneyMap forms are read by those mods too. */
public enum WaypointShareFormat {
	PLAIN(EMUtilsTexts.WAYPOINT_SHARE_FORMAT_PLAIN),
	XAERO(EMUtilsTexts.WAYPOINT_SHARE_FORMAT_XAERO),
	JOURNEYMAP(EMUtilsTexts.WAYPOINT_SHARE_FORMAT_JOURNEYMAP);

	private final String labelKey;

	WaypointShareFormat(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static WaypointShareFormat fromName(String name) {
		if (name != null) {
			for (WaypointShareFormat format : values()) {
				if (format.name().equalsIgnoreCase(name)) {
					return format;
				}
			}
		}

		return PLAIN;
	}
}
