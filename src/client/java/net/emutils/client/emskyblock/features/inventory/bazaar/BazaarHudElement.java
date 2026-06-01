package net.emutils.client.emskyblock.features.inventory.bazaar;

import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class BazaarHudElement extends AbstractHudLayoutElement {
	private final BazaarHudRenderer.ElementType type;

	public BazaarHudElement(BazaarHudRenderer.ElementType type) {
		super(type.id());
		this.type = type;
	}

	@Override
	public void register() {
		BazaarHudRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(EMUtilsConfig config, MinecraftClient client) {
		return BazaarHudRenderer.unscaledDimensions(type, client);
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		EMUtilsConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return new HudOverlayPlacement.Position(
			Math.min(type.defaultX(), Math.max(0, screenWidth - dimensions.width())),
			Math.min(type.defaultY(), Math.max(0, screenHeight - dimensions.height()))
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
			BazaarHudRenderer.renderLegacyPanel(
				context,
				client.textRenderer,
				BazaarHudRenderer.previewLines(type),
				0,
				0,
				HudLayoutManager.layoutOpacity(id(), config)
			)
		);
	}
}
