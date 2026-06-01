package net.emutils.client.emskyblock.features.slayer.slayertracker;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class SlayerTrackerHudElement extends AbstractHudLayoutElement {
	public SlayerTrackerHudElement() {
		super(HudElementId.SLAYER_PROFIT_TRACKER);
	}

	@Override
	public void register() {
		SlayerTrackerHudRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(
		EMUtilsConfig config,
		MinecraftClient client
	) {
		return new HudOverlayPlacement.PanelDimensions(
			SlayerTrackerHudRenderer.unscaledPanelWidth(config),
			SlayerTrackerHudRenderer.unscaledPanelHeight(config)
		);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return new HudOverlayPlacement.Position(20, 220);
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
			SlayerTrackerHudRenderer.renderPanel(
				context,
				config,
				SlayerTrackerHudRenderer.previewLines(),
				0,
				0,
				HudLayoutManager.layoutOpacity(id(), config)
			)
		);
	}
}
