package net.emutils.client.hud;

import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.hud.layout.HudElementId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class InfoOverlayHudElement extends AbstractHudLayoutElement {
	public InfoOverlayHudElement() {
		super(HudElementId.INFO_OVERLAY);
	}

	@Override
	public void register() {
		HudOverlayRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(EMUtilsConfig config, MinecraftClient client) {
		return new HudOverlayPlacement.PanelDimensions(
			HudOverlayRenderer.unscaledPanelWidth(),
			HudOverlayRenderer.unscaledPanelHeight(config)
		);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return HudOverlayPlacement.hudOverlayPosition(config, screenWidth, screenHeight, dimensions, false);
	}

	@Override
	public int defaultOpacityPercent(EMUtilsConfig config) {
		return config.hudBackgroundOpacity();
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
		int panelWidth = HudOverlayRenderer.unscaledPanelWidth();
		int panelHeight = HudOverlayRenderer.unscaledPanelHeight(config);
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			HudOverlayRenderer.renderPanel(
				context,
				client,
				config,
				0,
				0,
				panelWidth,
				panelHeight,
				net.emutils.client.hud.layout.HudLayoutManager.layoutOpacity(id(), config)
			)
		);
	}
}
