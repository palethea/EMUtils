package net.emutils.client.spotify;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.spotify.SpotifyPlayerOverlay;
import net.emutils.client.hud.HudOverlayPlacement;
import net.emutils.client.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.hud.layout.HudElementId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class SpotifyHudElement extends AbstractHudLayoutElement {
	public SpotifyHudElement() {
		super(HudElementId.SPOTIFY);
	}

	@Override
	public void register() {
		SpotifyHudRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(EMUtilsConfig config, MinecraftClient client) {
		return new HudOverlayPlacement.PanelDimensions(
			SpotifyPlayerOverlay.hudPanelWidth(),
			SpotifyPlayerOverlay.hudPanelHeight()
		);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return HudOverlayPlacement.spotifyHudPosition(config, screenWidth, screenHeight, dimensions);
	}

	@Override
	public int defaultOpacityPercent(EMUtilsConfig config) {
		return config.spotifyHudBackgroundOpacity();
	}

	@Override
	public void renderPreview(
		DrawContext context,
		int x,
		int y,
		EMUtilsConfig config,
		MinecraftClient client,
		int scalePercent
	) {
		SpotifyPlayerOverlay.renderHud(
			context,
			x,
			y,
			EMUtilsClient.spotify().state(),
			net.emutils.client.hud.layout.HudLayoutManager.layoutOpacity(id(), config),
			scalePercent / 100.0F
		);
	}
}
