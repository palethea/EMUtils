package net.emutils.client.emskyblock.features.gui.statshud;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.HudOverlayAnchor;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class SkyblockStatsHudElement extends AbstractHudLayoutElement {
	public SkyblockStatsHudElement() {
		super(HudElementId.SKYBLOCK_STATS);
	}

	@Override
	public void register() {
		SkyblockStatsHudRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(EMUtilsConfig config, MinecraftClient client) {
		SkyblockActionBarManager manager = EMUtilsClient.skyblockActionBar();
		SkyblockActionBarStats stats = manager == null
			? SkyblockStatsHudRenderer.previewStats(null)
			: SkyblockStatsHudRenderer.previewStats(manager);
		return new HudOverlayPlacement.PanelDimensions(
			SkyblockStatsHudRenderer.unscaledPanelWidth(config, stats),
			SkyblockStatsHudRenderer.unscaledPanelHeight(config, stats)
		);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		HudOverlayAnchor anchor = EMSkyblockSettings.skyblockStatsHudAnchor();
		int x = anchor.x(screenWidth, dimensions.width(), HudOverlayPlacement.MARGIN);
		int y = anchor == HudOverlayAnchor.BOTTOM_CENTER
			? screenHeight - 59 - dimensions.height()
			: anchor.y(screenHeight, dimensions.height(), HudOverlayPlacement.MARGIN);
		return new HudOverlayPlacement.Position(x, y);
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
		SkyblockActionBarManager manager = EMUtilsClient.skyblockActionBar();
		SkyblockActionBarStats stats = manager == null
			? SkyblockStatsHudRenderer.previewStats(null)
			: SkyblockStatsHudRenderer.previewStats(manager);
		int panelWidth = SkyblockStatsHudRenderer.unscaledPanelWidth(config, stats);
		int panelHeight = SkyblockStatsHudRenderer.unscaledPanelHeight(config, stats);
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			SkyblockStatsHudRenderer.renderPanel(
				context,
				config,
				stats,
				0,
				0,
				panelWidth,
				panelHeight,
				net.emutils.client.emhelpers.hud.layout.HudLayoutManager.layoutOpacity(id(), config)
			)
		);
	}
}
