package net.emutils.client.emutils.map;

import org.jspecify.annotations.Nullable;

/** How close the minimap shows the world (#212): GUI pixels per block, from far out to close up. */
public enum MinimapZoom {
	QUARTER(0.25F, "0.25×"),
	HALF(0.5F, "0.5×"),
	ONE(1.0F, "1×"),
	TWO(2.0F, "2×"),
	FOUR(4.0F, "4×"),
	EIGHT(8.0F, "8×");

	private final float pixelsPerBlock;
	private final String label;

	MinimapZoom(float pixelsPerBlock, String label) {
		this.pixelsPerBlock = pixelsPerBlock;
		this.label = label;
	}

	public float pixelsPerBlock() {
		return pixelsPerBlock;
	}

	public String label() {
		return label;
	}

	public MinimapZoom closer() {
		return values()[Math.min(ordinal() + 1, values().length - 1)];
	}

	public MinimapZoom farther() {
		return values()[Math.max(ordinal() - 1, 0)];
	}

	public static MinimapZoom fromName(@Nullable String name) {
		if (name != null) {
			for (MinimapZoom zoom : values()) {
				if (zoom.name().equals(name)) {
					return zoom;
				}
			}
		}
		return ONE;
	}
}
