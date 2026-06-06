package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emhelpers.client.hud.HudOverlayPlacement;
import net.emhelpers.client.hud.layout.AbstractHudLayoutElement;
import net.emhelpers.client.hud.layout.HudLayoutConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class InfoOverlayHudElement extends AbstractHudLayoutElement {
	public InfoOverlayHudElement() {
		super(net.emutils.client.EMUtilsHudElements.INFO_OVERLAY);
	}

	@Override
	public void register() {
		HudOverlayRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, Minecraft client) {
		EMUtilsConfig emUtilsConfig = (EMUtilsConfig) config;
		return new HudOverlayPlacement.PanelDimensions(
			HudOverlayRenderer.unscaledPanelWidth(),
			HudOverlayRenderer.unscaledPanelHeight(emUtilsConfig)
		);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		EMUtilsConfig emUtilsConfig = (EMUtilsConfig) config;
		return HudOverlayPlacement.anchored(
			emUtilsConfig.hudOverlayAnchor(),
			screenWidth,
			screenHeight,
			dimensions
		);
	}

	@Override
	public int defaultOpacityPercent(HudLayoutConfig config) {
		return ((EMUtilsConfig) config).hudBackgroundOpacity();
	}

	@Override
	public void renderPreview(
		GuiGraphicsExtractor context,
		int x,
		int y,
		HudLayoutConfig config,
		Minecraft client,
		int scalePercent
	) {
		EMUtilsConfig emUtilsConfig = (EMUtilsConfig) config;
		int panelWidth = HudOverlayRenderer.unscaledPanelWidth();
		int panelHeight = HudOverlayRenderer.unscaledPanelHeight(emUtilsConfig);
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			HudOverlayRenderer.renderPanel(
				context,
				client,
				emUtilsConfig,
				0,
				0,
				panelWidth,
				panelHeight,
				net.emhelpers.client.hud.layout.HudLayoutManager.layoutOpacity(id(), config)
			)
		);
	}
}
