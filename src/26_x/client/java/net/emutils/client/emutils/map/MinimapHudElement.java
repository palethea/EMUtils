package net.emutils.client.emutils.map;

import net.emutils.client.EMUtilsHudElements;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.hud.HudOverlayPlacement;
import net.emutils.client.emutils.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emutils.hud.layout.HudLayoutConfig;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The minimap in the HUD Layout Editor (#212): placed in the top-right corner by default. */
public final class MinimapHudElement extends AbstractHudLayoutElement {
	public MinimapHudElement() {
		super(EMUtilsHudElements.MINIMAP);
	}

	@Override
	public void register() {
		MinimapRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, Minecraft client) {
		return new HudOverlayPlacement.PanelDimensions(MinimapRenderer.width(), MinimapRenderer.height((EMUtilsConfig) config, client.font));
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return new HudOverlayPlacement.Position(MinimapRenderer.defaultX(screenWidth), MinimapRenderer.defaultY());
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
		int opacity = HudLayoutManager.layoutOpacity(id(), config);
		float scale = scalePercent / 100.0F;
		renderScaled(context, x, y, scale, () ->
			MinimapRenderer.drawMap(context, client, (EMUtilsConfig) config, scale, opacity, 1.0F)
		);
	}
}
