package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** How the custom tab list (#170) shows a player's ping. */
public enum TabListPing {
	/** Vanilla's connection bars. */
	BARS(EMUtilsTexts.OPTION_TAB_LIST_PING_BARS),
	NUMBER(EMUtilsTexts.OPTION_TAB_LIST_PING_NUMBER),
	BOTH(EMUtilsTexts.OPTION_TAB_LIST_PING_BOTH);

	private final String labelKey;

	TabListPing(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	boolean bars() {
		return this != NUMBER;
	}

	boolean number() {
		return this != BARS;
	}

	public static TabListPing fromName(@Nullable String name) {
		if (name != null) {
			for (TabListPing ping : values()) {
				if (ping.name().equals(name)) {
					return ping;
				}
			}
		}
		return BARS;
	}
}
