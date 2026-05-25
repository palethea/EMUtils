package net.emutils.client.hud;

import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.spotify.SpotifyPlayerOverlay;

public final class HudOverlayPlacement {
	public static final int MARGIN = 8;
	public static final int STACK_GAP = 4;

	private HudOverlayPlacement() {
	}

	public record PanelDimensions(int width, int height) {
	}

	public record Position(int x, int y) {
	}

	public static PanelDimensions spotifyHudDimensions(EMUtilsConfig config) {
		float scale = config.spotifyHudScale() / 100.0F;
		return scaled(SpotifyPlayerOverlay.hudPanelWidth(), SpotifyPlayerOverlay.hudPanelHeight(), scale);
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
		float scale = config.hudScale() / 100.0F;
		int panelWidth = HudOverlayRenderer.unscaledPanelWidth();
		int panelHeight = HudOverlayRenderer.unscaledPanelHeight(config);
		return scaled(panelWidth, panelHeight, scale);
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

		PanelDimensions spotifyDimensions = spotifyHudDimensions(config);
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

	private static PanelDimensions scaled(int width, int height, float scale) {
		return new PanelDimensions(Math.round(width * scale), Math.round(height * scale));
	}
}
