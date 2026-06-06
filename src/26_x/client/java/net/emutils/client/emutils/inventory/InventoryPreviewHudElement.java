package net.emutils.client.emutils.inventory;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emhelpers.client.hud.HudOverlayPlacement;
import net.emhelpers.client.hud.layout.AbstractHudLayoutElement;
import net.emhelpers.client.hud.layout.HudLayoutConfig;
import net.emhelpers.client.hud.layout.HudLayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class InventoryPreviewHudElement extends AbstractHudLayoutElement {
	private static final int BOTTOM_MARGIN = 62;

	public InventoryPreviewHudElement() {
		super(net.emutils.client.EMUtilsHudElements.INVENTORY_PREVIEW);
	}

	@Override
	public void register() {
		InventoryPreviewRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, Minecraft client) {
		return new HudOverlayPlacement.PanelDimensions(ShulkerStylePanelRenderer.WIDTH, ShulkerStylePanelRenderer.HEIGHT);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return new HudOverlayPlacement.Position(
			(screenWidth - dimensions.width()) / 2,
			screenHeight - BOTTOM_MARGIN - dimensions.height()
		);
	}

	@Override
	public int defaultOpacityPercent(HudLayoutConfig config) {
		return ((EMUtilsConfig) config).inventoryPreviewOpacity();
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
		float opacity = HudLayoutManager.layoutOpacity(id(), config) / 100.0F;
		renderScaled(context, x, y, scalePercent / 100.0F, () -> {
			ShulkerStylePanelRenderer.drawPanel(context, 0, 0, opacity);
			if (client.player != null) {
				ShulkerStylePanelRenderer.drawMainInventory(
					context,
					client.font,
					0,
					0,
					client.player.getInventory().getNonEquipmentItems(),
					opacity
				);
			}
		});
	}
}
