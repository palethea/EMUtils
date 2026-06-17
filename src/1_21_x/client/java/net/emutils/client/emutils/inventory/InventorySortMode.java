package net.emutils.client.emutils.inventory;

public enum InventorySortMode {
	NAME("emutils.inventory.sort.name"),
	CATEGORY("emutils.inventory.sort.category"),
	QUANTITY("emutils.inventory.sort.quantity");

	private final String labelKey;

	InventorySortMode(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}
}
