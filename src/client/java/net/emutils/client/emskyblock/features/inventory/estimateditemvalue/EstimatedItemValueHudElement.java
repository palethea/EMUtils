package net.emutils.client.emskyblock.features.inventory.estimateditemvalue;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class EstimatedItemValueHudElement extends AbstractHudLayoutElement {
	public EstimatedItemValueHudElement() {
		super(HudElementId.ESTIMATED_ITEM_VALUE);
	}

	@Override
	public void register() {
		EstimatedItemValueHudRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(EMUtilsConfig config, MinecraftClient client) {
		return new HudOverlayPlacement.PanelDimensions(
			EstimatedItemValueHudRenderer.unscaledPanelWidth(config),
			EstimatedItemValueHudRenderer.unscaledPanelHeight(config)
		);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return new HudOverlayPlacement.Position(
			EMSkyblockSettings.estimatedItemValueHudAnchor().x(screenWidth, dimensions.width(), HudOverlayPlacement.MARGIN),
			EMSkyblockSettings.estimatedItemValueHudAnchor().y(screenHeight, dimensions.height(), HudOverlayPlacement.MARGIN)
		);
	}

	@Override
	public int defaultOpacityPercent(EMUtilsConfig config) {
		return EMSkyblockSettings.skyblockStatsHudBackgroundOpacity();
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
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			EstimatedItemValueHudRenderer.renderPanel(
				context,
				config,
				EstimatedItemValueHudRenderer.previewLines(),
				0,
				0,
				net.emutils.client.emhelpers.hud.layout.HudLayoutManager.layoutOpacity(id(), config)
			)
		);
	}
}
