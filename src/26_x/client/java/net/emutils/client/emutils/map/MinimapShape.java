package net.emutils.client.emutils.map;

import net.emutils.client.emutils.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

/** The minimap's outline (#212). */
public enum MinimapShape {
	SQUARE(EMUtilsTexts.OPTION_MINIMAP_SHAPE_SQUARE),
	ROUND(EMUtilsTexts.OPTION_MINIMAP_SHAPE_ROUND);

	private final String labelKey;

	MinimapShape(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public static MinimapShape fromName(@Nullable String name) {
		if (name != null) {
			for (MinimapShape shape : values()) {
				if (shape.name().equals(name)) {
					return shape;
				}
			}
		}
		return SQUARE;
	}
}
