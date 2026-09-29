package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** How the custom tab list (#170) orders the players. */
public enum TabListSort {
	/** Vanilla's order: the server's tab list order, spectators last, then team, then name. */
	VANILLA(EMUtilsTexts.OPTION_TAB_LIST_SORT_VANILLA),
	NAME(EMUtilsTexts.OPTION_TAB_LIST_SORT_NAME),
	/** Lowest ping first; players whose ping isn't known yet go last. */
	PING(EMUtilsTexts.OPTION_TAB_LIST_SORT_PING);

	private final String labelKey;

	TabListSort(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static TabListSort fromName(@Nullable String name) {
		if (name != null) {
			for (TabListSort sort : values()) {
				if (sort.name().equals(name)) {
					return sort;
				}
			}
		}
		return VANILLA;
	}
}
