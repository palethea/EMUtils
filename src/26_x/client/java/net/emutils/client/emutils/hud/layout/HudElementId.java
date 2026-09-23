package net.emutils.client.emutils.hud.layout;

import java.util.Objects;

public final class HudElementId {
	private final String configKey;
	private final String labelKey;

	private HudElementId(String configKey, String labelKey) {
		this.configKey = Objects.requireNonNull(configKey, "configKey");
		this.labelKey = Objects.requireNonNull(labelKey, "labelKey");
	}

	public static HudElementId of(String configKey, String labelKey) {
		return new HudElementId(configKey, labelKey);
	}

	public String labelKey() {
		return labelKey;
	}

	public String configKey() {
		return configKey;
	}

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof HudElementId id && configKey.equals(id.configKey);
	}

	@Override
	public int hashCode() {
		return configKey.hashCode();
	}

	@Override
	public String toString() {
		return configKey;
	}
}
