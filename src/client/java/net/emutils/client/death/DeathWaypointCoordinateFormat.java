package net.emutils.client.death;

import net.emutils.client.util.EMUtilsTexts;

public enum DeathWaypointCoordinateFormat {
	PLAIN(EMUtilsTexts.DEATH_COORD_FORMAT_PLAIN),
	COMMA(EMUtilsTexts.DEATH_COORD_FORMAT_COMMA),
	TP_COMMAND(EMUtilsTexts.DEATH_COORD_FORMAT_TP_COMMAND);

	private final String labelKey;

	DeathWaypointCoordinateFormat(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public DeathWaypointCoordinateFormat next() {
		DeathWaypointCoordinateFormat[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static DeathWaypointCoordinateFormat fromName(String name) {
		if (name != null) {
			for (DeathWaypointCoordinateFormat format : values()) {
				if (format.name().equalsIgnoreCase(name)) {
					return format;
				}
			}
		}

		return PLAIN;
	}
}
