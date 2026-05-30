package net.emutils.client.emutils.inventory;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class InventoryPreviewHudElement extends AbstractHudLayoutElement {
	private static final int BOTTOM_MARGIN = 62;

	public InventoryPreviewHudElement() {
		super(HudElementId.INVENTORY_PREVIEW);
	}

	@Override
	public void register() {
		InventoryPreviewRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(EMUtilsConfig config, MinecraftClient client) {
		return new HudOverlayPlacement.PanelDimensions(ShulkerStylePanelRenderer.WIDTH, ShulkerStylePanelRenderer.HEIGHT);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		EMUtilsConfig config,
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
	public int defaultOpacityPercent(EMUtilsConfig config) {
		return config.inventoryPreviewOpacity();
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
		float opacity = HudLayoutManager.layoutOpacity(id(), config) / 100.0F;
		renderScaled(context, x, y, scalePercent / 100.0F, () -> {
			ShulkerStylePanelRenderer.drawPanel(context, 0, 0, opacity);
			if (client.player != null) {
				ShulkerStylePanelRenderer.drawMainInventory(
					context,
					client.textRenderer,
					0,
					0,
					client.player.getInventory().getMainStacks(),
					opacity
				);
			}
		});
	}
}
