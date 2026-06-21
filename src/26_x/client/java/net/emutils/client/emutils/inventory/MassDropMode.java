package net.emutils.client.emutils.inventory;

public enum MassDropMode {
	LEGIT("emutils.mass_drop.mode.legit"),
	UNFAIR("emutils.mass_drop.mode.unfair");

	private final String labelKey;

	MassDropMode(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public MassDropMode next() {
		return this == LEGIT ? UNFAIR : LEGIT;
	}
}
