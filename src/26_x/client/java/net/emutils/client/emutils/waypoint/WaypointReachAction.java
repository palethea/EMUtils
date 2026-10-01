package net.emutils.client.emutils.waypoint;

import net.emutils.client.emutils.util.EMUtilsTexts;

/** What happens to a death waypoint when you reach it (#105). */
public enum WaypointReachAction {
	REMOVE(EMUtilsTexts.WAYPOINT_REACH_REMOVE),
	ASK(EMUtilsTexts.WAYPOINT_REACH_ASK),
	KEEP(EMUtilsTexts.WAYPOINT_REACH_KEEP);

	private final String labelKey;

	WaypointReachAction(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static WaypointReachAction fromName(String name) {
		if (name != null) {
			for (WaypointReachAction action : values()) {
				if (action.name().equalsIgnoreCase(name)) {
					return action;
				}
			}
		}

		return REMOVE;
	}
}
