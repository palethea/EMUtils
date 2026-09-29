package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emutils.hud.layout.HudLayoutConfig;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ScoreboardHudElement extends AbstractHudLayoutElement {
	public ScoreboardHudElement() {
		super(net.emutils.client.EMUtilsHudElements.SCOREBOARD);
	}

	@Override
	public void register() {
		ScoreboardRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, Minecraft client) {
		return ScoreboardRenderer.slotDimensions(client, (EMUtilsConfig) config);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return ScoreboardRenderer.defaultPosition(screenWidth, screenHeight, dimensions);
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
		HudOverlayPlacement.PanelDimensions scaled = scaledDimensions(config, client, scalePercent);
		int opacity = HudLayoutManager.layoutOpacity(id(), config);
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			ScoreboardRenderer.renderInSlot(
				context,
				client,
				emUtilsConfig,
				ScoreboardRenderer.shown(client),
				0,
				0,
				x,
				y,
				scaled.width(),
				scaled.height(),
				opacity
			)
		);
	}
}
