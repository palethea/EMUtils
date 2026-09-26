package net.emutils.client.emutils.profile;

/**
 * The colors of a profile's icon circle (#89), saved by name. Each is dark enough for the white icon
 * on it in both themes.
 */
public enum ProfileColor {
	GRAY(0xFF6B766F),
	GREEN(0xFF16A058),
	TEAL(0xFF12877F),
	BLUE(0xFF2F6FD6),
	PURPLE(0xFF7B4FD0),
	PINK(0xFFC23D7A),
	RED(0xFFD0453A),
	ORANGE(0xFFC77700);

	private final int argb;

	ProfileColor(int argb) {
		this.argb = argb;
	}

	public int argb() {
		return argb;
	}

	public static ProfileColor byName(String name) {
		for (ProfileColor color : values()) {
			if (color.name().equalsIgnoreCase(name)) {
				return color;
			}
		}
		return GRAY;
	}
}
