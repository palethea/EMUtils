package net.emutils.client.emskyblock.features.fishing.hookdisplay;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class FishingHookHudElement extends AbstractHudLayoutElement {
	public FishingHookHudElement() {
		super(HudElementId.FISHING_HOOK);
	}

	@Override
	public void register() {
		FishingHookHudRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(EMUtilsConfig config, MinecraftClient client) {
		TextRenderer textRenderer = client.textRenderer;
		return new HudOverlayPlacement.PanelDimensions(
			FishingHookDisplayManager.layoutAnchorWidth(textRenderer),
			FishingHookHudRenderer.unscaledPanelHeight(textRenderer)
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
			(screenWidth - dimensions.width()) / 2,
			screenHeight / 2 - dimensions.height() / 2
		);
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
		TextRenderer textRenderer = client.textRenderer;
		Text preview = FishingHookDisplayManager.layoutAnchorText();
		int anchorWidth = FishingHookDisplayManager.layoutAnchorWidth(textRenderer);
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			FishingHookHudRenderer.renderTextCenteredInAnchor(context, textRenderer, preview, anchorWidth, 0)
		);
	}
}
