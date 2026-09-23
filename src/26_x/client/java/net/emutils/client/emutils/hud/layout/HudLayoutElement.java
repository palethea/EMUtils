package net.emutils.client.emutils.hud.layout;

import net.emutils.client.emutils.hud.HudOverlayPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * One HUD overlay in the layout editor: metrics, preview, and Fabric registration live together.
 * To add a new element: extend {@link AbstractHudLayoutElement}, add a {@link HudElementId} + lang key,
 * then {@code HudLayoutRegistry.register(new YourHudElement());} from client init.
 */
public interface HudLayoutElement {
	HudElementId id();

	HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, Minecraft client);

	HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	);

	void renderPreview(
		GuiGraphicsExtractor context,
		int x,
		int y,
		HudLayoutConfig config,
		Minecraft client,
		int scalePercent
	);

	void register();

	default HudOverlayPlacement.PanelDimensions scaledDimensions(
		HudLayoutConfig config,
		Minecraft client,
		int scalePercent
	) {
		return HudOverlayPlacement.scaled(unscaledDimensions(config, client), scalePercent);
	}

	default int defaultOpacityPercent(HudLayoutConfig config) {
		return 100;
	}

	default int scaleFromResize(
		HudLayoutConfig config,
		Minecraft client,
		int anchorX,
		int anchorY,
		int mouseX,
		int mouseY
	) {
		HudOverlayPlacement.PanelDimensions unscaled = unscaledDimensions(config, client);
		if (unscaled.width() <= 0 || unscaled.height() <= 0) {
			return 100;
		}

		float widthRatio = (mouseX - anchorX) / (float) unscaled.width();
		float heightRatio = (mouseY - anchorY) / (float) unscaled.height();
		return HudLayoutManager.clampLayoutScale(Math.round(Math.max(widthRatio, heightRatio) * 100.0F));
	}
}
