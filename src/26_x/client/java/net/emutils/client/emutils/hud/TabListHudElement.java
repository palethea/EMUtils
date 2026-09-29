package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emutils.hud.layout.HudLayoutConfig;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TabListHudElement extends AbstractHudLayoutElement {
	public TabListHudElement() {
		super(net.emutils.client.EMUtilsHudElements.TAB_LIST);
	}

	@Override
	public void register() {
		TabListRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, Minecraft client) {
		return TabListRenderer.slotDimensions(client, (EMUtilsConfig) config);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return TabListRenderer.defaultPosition((EMUtilsConfig) config, screenWidth, dimensions);
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
		HudOverlayPlacement.PanelDimensions scaled = scaledDimensions(config, client, scalePercent);
		int opacity = HudLayoutManager.layoutOpacity(id(), config);
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			TabListRenderer.renderInSlot(context, client, emUtilsConfig, TabListRenderer.shown(client, emUtilsConfig), 0, 0, x, scaled.width(), opacity)
		);
	}
}
