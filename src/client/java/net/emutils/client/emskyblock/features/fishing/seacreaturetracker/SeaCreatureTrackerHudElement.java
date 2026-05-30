package net.emutils.client.emskyblock.features.fishing.seacreaturetracker;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class SeaCreatureTrackerHudElement extends AbstractHudLayoutElement {
	public SeaCreatureTrackerHudElement() {
		super(HudElementId.SEA_CREATURE_TRACKER);
	}

	@Override
	public void register() {
		SeaCreatureTrackerHudRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(EMUtilsConfig config, MinecraftClient client) {
		return new HudOverlayPlacement.PanelDimensions(
			SeaCreatureTrackerHudRenderer.unscaledPanelWidth(config),
			SeaCreatureTrackerHudRenderer.unscaledPanelHeight(config)
		);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return new HudOverlayPlacement.Position(20, 20);
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
			SeaCreatureTrackerHudRenderer.renderPanel(
				context,
				config,
				SeaCreatureTrackerHudRenderer.previewLines(),
				0,
				0,
				HudLayoutManager.layoutOpacity(id(), config)
			)
		);
	}
}
