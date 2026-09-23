package net.emutils.client.emutils.hud;

public final class HudOverlayPlacement {
	public static final int MARGIN = 8;
	public static final int STACK_GAP = 4;

	private HudOverlayPlacement() {
	}

	public record PanelDimensions(int width, int height) {
	}

	public record Position(int x, int y) {
	}

	public static PanelDimensions scaled(PanelDimensions unscaled, int scalePercent) {
		float scale = scalePercent / 100.0F;
		return new PanelDimensions(
			Math.round(unscaled.width() * scale),
			Math.round(unscaled.height() * scale)
		);
	}

	public static Position anchored(
		HudOverlayAnchor anchor,
		int screenWidth,
		int screenHeight,
		PanelDimensions dimensions
	) {
		return new Position(
			anchor.x(screenWidth, dimensions.width(), MARGIN),
			anchor.y(screenHeight, dimensions.height(), MARGIN)
		);
	}
}
