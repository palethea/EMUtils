package net.emutils.client.hud.layout;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.spotify.SpotifyPlayerOverlay;
import net.emutils.client.hud.HudOverlayPlacement;
import net.emutils.client.hud.HudOverlayRenderer;
import net.emutils.client.inventory.ShulkerStylePanelRenderer;
import net.emutils.client.skyblock.SkyblockActionBarManager;
import net.emutils.client.skyblock.SkyblockActionBarStats;
import net.emutils.client.skyblock.SkyblockStatsHudRenderer;
import net.emutils.client.skyblock.eiv.EstimatedItemValueHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class HudElementMetrics {
	private HudElementMetrics() {
	}

	public static HudOverlayPlacement.PanelDimensions dimensions(HudElementId id, EMUtilsConfig config, MinecraftClient client) {
		return switch (id) {
			case INFO_OVERLAY -> HudOverlayPlacement.hudOverlayDimensions(config);
			case SPOTIFY -> HudOverlayPlacement.spotifyHudDimensions(config);
			case INVENTORY_PREVIEW -> new HudOverlayPlacement.PanelDimensions(
				ShulkerStylePanelRenderer.WIDTH,
				ShulkerStylePanelRenderer.HEIGHT
			);
			case SKYBLOCK_STATS -> {
				SkyblockActionBarManager manager = EMUtilsClient.skyblockActionBar();
				SkyblockActionBarStats stats = manager == null
					? SkyblockStatsHudRenderer.previewStats(null)
					: SkyblockStatsHudRenderer.previewStats(manager);
				float scale = config.skyblockStatsHudScale() / 100.0F;
				yield new HudOverlayPlacement.PanelDimensions(
					Math.round(SkyblockStatsHudRenderer.unscaledPanelWidth(config, stats) * scale),
					Math.round(SkyblockStatsHudRenderer.unscaledPanelHeight(config, stats) * scale)
				);
			}
			case ESTIMATED_ITEM_VALUE -> {
				float scale = config.estimatedItemValueHudScale() / 100.0F;
				yield new HudOverlayPlacement.PanelDimensions(
					Math.round(EstimatedItemValueHudRenderer.unscaledPanelWidth(config) * scale),
					Math.round(EstimatedItemValueHudRenderer.unscaledPanelHeight(config) * scale)
				);
			}
		};
	}

	public static HudOverlayPlacement.Position anchorPosition(
		HudElementId id,
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return switch (id) {
			case INFO_OVERLAY -> HudOverlayPlacement.hudOverlayPosition(
				config,
				screenWidth,
				screenHeight,
				dimensions,
				false
			);
			case SPOTIFY -> HudOverlayPlacement.spotifyHudPosition(config, screenWidth, screenHeight, dimensions);
			case INVENTORY_PREVIEW -> new HudOverlayPlacement.Position(
				(screenWidth - dimensions.width()) / 2,
				screenHeight - 62 - dimensions.height()
			);
			case SKYBLOCK_STATS -> {
				int x = config.skyblockStatsHudAnchor().x(screenWidth, dimensions.width(), HudOverlayPlacement.MARGIN);
				int y = config.skyblockStatsHudAnchor() == net.emutils.client.hud.HudOverlayAnchor.BOTTOM_CENTER
					? screenHeight - 59 - dimensions.height()
					: config.skyblockStatsHudAnchor().y(screenHeight, dimensions.height(), HudOverlayPlacement.MARGIN);
				yield new HudOverlayPlacement.Position(x, y);
			}
			case ESTIMATED_ITEM_VALUE -> {
				int x = config.estimatedItemValueHudAnchor().x(screenWidth, dimensions.width(), HudOverlayPlacement.MARGIN);
				int y = config.estimatedItemValueHudAnchor().y(screenHeight, dimensions.height(), HudOverlayPlacement.MARGIN);
				yield new HudOverlayPlacement.Position(x, y);
			}
		};
	}

	public static void renderPreview(
		HudElementId id,
		DrawContext context,
		int x,
		int y,
		EMUtilsConfig config,
		MinecraftClient client
	) {
		switch (id) {
			case INFO_OVERLAY -> {
				float scale = config.hudScale() / 100.0F;
				int panelWidth = HudOverlayRenderer.unscaledPanelWidth();
				int panelHeight = HudOverlayRenderer.unscaledPanelHeight(config);
				context.getMatrices().pushMatrix();
				try {
					context.getMatrices().translate(x, y);
					context.getMatrices().scale(scale, scale);
					HudOverlayRenderer.renderPanel(context, client, config, 0, 0, panelWidth, panelHeight);
				} finally {
					context.getMatrices().popMatrix();
				}
			}
			case SPOTIFY -> SpotifyPlayerOverlay.renderHud(
				context,
				x,
				y,
				EMUtilsClient.spotify().state(),
				config.spotifyHudBackgroundOpacity(),
				config.spotifyHudScale() / 100.0F
			);
			case INVENTORY_PREVIEW -> {
				float opacity = Math.min(100, Math.max(0, config.inventoryPreviewOpacity())) / 100.0F;
				ShulkerStylePanelRenderer.drawPanel(context, x, y, opacity);
				if (client.player != null) {
					ShulkerStylePanelRenderer.drawMainInventory(
						context,
						client.textRenderer,
						x,
						y,
						client.player.getInventory().getMainStacks(),
						opacity
					);
				}
			}
			case SKYBLOCK_STATS -> {
				SkyblockActionBarManager manager = EMUtilsClient.skyblockActionBar();
				SkyblockActionBarStats stats = manager == null
					? SkyblockStatsHudRenderer.previewStats(null)
					: SkyblockStatsHudRenderer.previewStats(manager);
				float scale = config.skyblockStatsHudScale() / 100.0F;
				int panelWidth = SkyblockStatsHudRenderer.unscaledPanelWidth(config, stats);
				int panelHeight = SkyblockStatsHudRenderer.unscaledPanelHeight(config, stats);
				context.getMatrices().pushMatrix();
				try {
					context.getMatrices().translate(x, y);
					context.getMatrices().scale(scale, scale);
					SkyblockStatsHudRenderer.renderPanel(context, config, stats, 0, 0, panelWidth, panelHeight);
				} finally {
					context.getMatrices().popMatrix();
				}
			}
			case ESTIMATED_ITEM_VALUE -> {
				float scale = config.estimatedItemValueHudScale() / 100.0F;
				context.getMatrices().pushMatrix();
				try {
					context.getMatrices().translate(x, y);
					context.getMatrices().scale(scale, scale);
					EstimatedItemValueHudRenderer.renderPanel(
						context,
						config,
						EstimatedItemValueHudRenderer.previewLines(config),
						0,
						0
					);
				} finally {
					context.getMatrices().popMatrix();
				}
			}
		}
	}
}
