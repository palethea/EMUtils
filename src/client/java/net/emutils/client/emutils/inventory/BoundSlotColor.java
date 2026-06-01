package net.emutils.client.emutils.inventory;

import net.emutils.client.emutils.util.EMUtilsTexts;

public enum BoundSlotColor {
	GRAY(EMUtilsTexts.INVENTORY_BOUND_COLOR_GRAY, 0xAA808080),
	WHITE(EMUtilsTexts.INVENTORY_BOUND_COLOR_WHITE, 0xAAFFFFFF),
	DARK_BLUE(EMUtilsTexts.INVENTORY_BOUND_COLOR_DARK_BLUE, 0xAA1E3A8A);

	private final String labelKey;
	private final int color;

	BoundSlotColor(String labelKey, int color) {
		this.labelKey = labelKey;
		this.color = color;
	}

	public String labelKey() {
		return labelKey;
	}

	public int color() {
		return color;
	}

	public BoundSlotColor next() {
		BoundSlotColor[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static BoundSlotColor fromName(String name) {
		if (name != null) {
			for (BoundSlotColor color : values()) {
				if (color.name().equals(name)) {
					return color;
				}
			}
		}

		return GRAY;
	}
}
