package net.emutils.client.inventory;

import net.emutils.client.util.EMUtilsTexts;

public enum SlotLockColor {
	RED(EMUtilsTexts.INVENTORY_LOCK_COLOR_RED, 0xAAFF3030),
	YELLOW(EMUtilsTexts.INVENTORY_LOCK_COLOR_YELLOW, 0xAAFFD83D),
	GREEN(EMUtilsTexts.INVENTORY_LOCK_COLOR_GREEN, 0xAA33D17A),
	BLUE(EMUtilsTexts.INVENTORY_LOCK_COLOR_BLUE, 0xAA3584E4);

	private final String labelKey;
	private final int color;

	SlotLockColor(String labelKey, int color) {
		this.labelKey = labelKey;
		this.color = color;
	}

	public String labelKey() {
		return labelKey;
	}

	public int color() {
		return color;
	}

	public SlotLockColor next() {
		SlotLockColor[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static SlotLockColor fromName(String name) {
		if (name != null) {
			for (SlotLockColor color : values()) {
				if (color.name().equals(name)) {
					return color;
				}
			}
		}

		return RED;
	}
}
