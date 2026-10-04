package net.emutils.client.emutils.map;

/**
 * The groups of entities the entity radar (#224) tells apart, each shown or hidden, drawn as a face or a dot,
 * and framed in a color of its own. Listed as the settings show them; {@link #drawOrder} puts the ones that
 * matter most on top.
 */
public enum RadarGroup {
	PLAYERS("players", true, RadarIcon.FACE, 0xFF1E1E1E, 7),
	NPCS("npcs", true, RadarIcon.FACE, 0xFF9AA0A6, 4),
	HOSTILE("hostile", true, RadarIcon.FACE, 0xFFFF5555, 6),
	ANIMALS("animals", true, RadarIcon.FACE, 0xFF6BE36B, 1),
	WATER("water", true, RadarIcon.FACE, 0xFF4FD8E8, 2),
	VILLAGERS("villagers", true, RadarIcon.FACE, 0xFFF0A040, 3),
	PETS("pets", true, RadarIcon.FACE, 0xFF6FC8FF, 5),
	ITEMS("items", false, RadarIcon.DOT, 0xFFFFD84A, 0);

	private final String key;
	private final boolean shownByDefault;
	private final RadarIcon defaultIcon;
	private final int defaultColor;
	private final int drawOrder;

	RadarGroup(String key, boolean shownByDefault, RadarIcon defaultIcon, int defaultColor, int drawOrder) {
		this.key = key;
		this.shownByDefault = shownByDefault;
		this.defaultIcon = defaultIcon;
		this.defaultColor = defaultColor;
		this.drawOrder = drawOrder;
	}

	/** The group's name in the config file. */
	public String key() {
		return key;
	}

	public boolean shownByDefault() {
		return shownByDefault;
	}

	public RadarIcon defaultIcon() {
		return defaultIcon;
	}

	public int defaultColor() {
		return defaultColor;
	}

	/** Later groups are drawn over earlier ones. */
	public int drawOrder() {
		return drawOrder;
	}

	/** Items have no face, so they're only ever a dot. */
	public boolean hasFaces() {
		return this != ITEMS;
	}

	/** The settings' label for showing the group, picking its icon, and its color. */
	public String showKey() {
		return "emutils.option.radar_show." + key;
	}

	public String iconKey() {
		return "emutils.option.radar_icon." + key;
	}

	public String colorKey() {
		return "emutils.option.radar_color." + key;
	}
}
