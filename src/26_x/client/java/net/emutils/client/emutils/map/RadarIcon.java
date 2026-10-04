package net.emutils.client.emutils.map;

/** How a group of entities shows on the radar (#224): by its face, or as a dot in the group's color. */
public enum RadarIcon {
	FACE("emutils.option.radar_icon.face"),
	DOT("emutils.option.radar_icon.dot");

	private final String labelKey;

	RadarIcon(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}
}
