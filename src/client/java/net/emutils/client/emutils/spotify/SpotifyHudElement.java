package net.emutils.client.emutils.spotify;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.spotify.gui.SpotifyPlayerOverlay;
import net.emhelpers.client.hud.HudOverlayPlacement;
import net.emhelpers.client.hud.layout.AbstractHudLayoutElement;
import net.emhelpers.client.hud.layout.HudLayoutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class SpotifyHudElement extends AbstractHudLayoutElement {
	public SpotifyHudElement() {
		super(net.emutils.client.EMUtilsHudElements.SPOTIFY);
	}

	@Override
	public void register() {
		SpotifyHudRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, MinecraftClient client) {
		return new HudOverlayPlacement.PanelDimensions(
			SpotifyPlayerOverlay.hudPanelWidth(),
			SpotifyPlayerOverlay.hudPanelHeight()
		);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return HudOverlayPlacement.anchored(((EMUtilsConfig) config).spotifyHudAnchor(), screenWidth, screenHeight, dimensions);
	}

	@Override
	public int defaultOpacityPercent(HudLayoutConfig config) {
		return ((EMUtilsConfig) config).spotifyHudBackgroundOpacity();
	}

	@Override
	public void renderPreview(
		DrawContext context,
		int x,
		int y,
		HudLayoutConfig config,
		MinecraftClient client,
		int scalePercent
	) {
		SpotifyPlayerOverlay.renderHud(
			context,
			x,
			y,
			EMUtilsClient.spotify().state(),
			net.emhelpers.client.hud.layout.HudLayoutManager.layoutOpacity(id(), config),
			scalePercent / 100.0F
		);
	}
}
