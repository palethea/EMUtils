package net.emutils.client.hud;

import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.spotify.SpotifyPlayerOverlay;
import net.emutils.client.hud.layout.HudElementId;
import net.emutils.client.hud.layout.HudLayoutManager;

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

	public static PanelDimensions spotifyHudDimensions(int scalePercent) {
		return scaled(
			new PanelDimensions(SpotifyPlayerOverlay.hudPanelWidth(), SpotifyPlayerOverlay.hudPanelHeight()),
			scalePercent
		);
	}

	public static Position spotifyHudPosition(
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		PanelDimensions dimensions
	) {
		HudOverlayAnchor anchor = config.spotifyHudAnchor();
		return anchored(anchor, screenWidth, screenHeight, dimensions);
	}

	public static PanelDimensions hudOverlayDimensions(EMUtilsConfig config) {
		return scaled(
			new PanelDimensions(
				HudOverlayRenderer.unscaledPanelWidth(),
				HudOverlayRenderer.unscaledPanelHeight(config)
			),
			HudLayoutManager.layoutScale(HudElementId.INFO_OVERLAY, config)
		);
	}

	public static Position hudOverlayPosition(
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		PanelDimensions dimensions,
		boolean spotifyHudVisible
	) {
		HudOverlayAnchor anchor = config.hudOverlayAnchor();
		Position position = anchored(anchor, screenWidth, screenHeight, dimensions);
		if (!spotifyHudVisible || anchor != config.spotifyHudAnchor()) {
			return position;
		}

		int spotifyScale = HudLayoutManager.layoutScale(HudElementId.SPOTIFY, config);
		PanelDimensions spotifyDimensions = spotifyHudDimensions(spotifyScale);
		Position spotifyPosition = spotifyHudPosition(config, screenWidth, screenHeight, spotifyDimensions);
		int stackedY = anchor.isTop()
			? spotifyPosition.y() + spotifyDimensions.height() + STACK_GAP
			: spotifyPosition.y() - dimensions.height() - STACK_GAP;
		return new Position(position.x(), stackedY);
	}

	private static Position anchored(
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
