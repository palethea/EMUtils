package net.emutils.client.emutils.inventory;

public enum InventorySortSpeed {
	NORMAL("emutils.inventory.sort_speed.normal"),
	ANTI_CHEAT("emutils.inventory.sort_speed.anti_cheat");

	private final String labelKey;

	InventorySortSpeed(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public InventorySortSpeed next() {
		return this == NORMAL ? ANTI_CHEAT : NORMAL;
	}

	public static InventorySortSpeed fromName(String name) {
		for (InventorySortSpeed speed : values()) {
			if (speed.name().equalsIgnoreCase(name)) {
				return speed;
			}
		}
		return NORMAL;
	}
}
