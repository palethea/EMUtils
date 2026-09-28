package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emutils.hud.layout.HudLayoutConfig;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ArmorStatusHudElement extends AbstractHudLayoutElement {
	public ArmorStatusHudElement() {
		super(net.emutils.client.EMUtilsHudElements.ARMOR_STATUS);
	}

	@Override
	public void register() {
		ArmorStatusRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, Minecraft client) {
		EMUtilsConfig emUtilsConfig = (EMUtilsConfig) config;
		return new HudOverlayPlacement.PanelDimensions(ArmorStatusRenderer.width(emUtilsConfig), ArmorStatusRenderer.slotHeight(emUtilsConfig));
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return new HudOverlayPlacement.Position(
			ArmorStatusRenderer.defaultX(screenWidth, dimensions.width()),
			ArmorStatusRenderer.defaultY(screenHeight, dimensions.height())
		);
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
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			ArmorStatusRenderer.renderInSlot(context, client, (EMUtilsConfig) config, 0, 0, false, true, opacity)
		);
	}
}
