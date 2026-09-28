package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emutils.hud.layout.HudLayoutConfig;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class LookAtInfoHudElement extends AbstractHudLayoutElement {
	public LookAtInfoHudElement() {
		super(net.emutils.client.EMUtilsHudElements.LOOK_AT_INFO);
	}

	@Override
	public void register() {
		LookAtInfoRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, Minecraft client) {
		EMUtilsConfig emUtilsConfig = (EMUtilsConfig) config;
		LookAtInfoData data = LookAtInfoRenderer.shown(emUtilsConfig);
		return new HudOverlayPlacement.PanelDimensions(
			LookAtInfoRenderer.slotWidth(emUtilsConfig, data),
			LookAtInfoRenderer.cardHeight(emUtilsConfig, data)
		);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return new HudOverlayPlacement.Position(LookAtInfoRenderer.defaultX(screenWidth, dimensions.width()), LookAtInfoRenderer.defaultY());
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
		LookAtInfoData data = LookAtInfoRenderer.shown(emUtilsConfig);
		int slotWidth = Math.round(LookAtInfoRenderer.slotWidth(emUtilsConfig, data) * scalePercent / 100.0F);
		int opacity = HudLayoutManager.layoutOpacity(id(), config);
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			LookAtInfoRenderer.renderInSlot(context, client, emUtilsConfig, data, 0, 0, x, slotWidth, context.guiWidth(), opacity)
		);
	}
}
