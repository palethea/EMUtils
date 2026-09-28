package net.emutils.client.emutils.hud;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.hud.layout.AbstractHudLayoutElement;
import net.emutils.client.emutils.hud.layout.HudLayoutConfig;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class KeystrokesHudElement extends AbstractHudLayoutElement {
	public KeystrokesHudElement() {
		super(net.emutils.client.EMUtilsHudElements.KEYSTROKES);
	}

	@Override
	public void register() {
		KeystrokesRenderer.register();
	}

	@Override
	public HudOverlayPlacement.PanelDimensions unscaledDimensions(HudLayoutConfig config, Minecraft client) {
		return new HudOverlayPlacement.PanelDimensions(KeystrokesRenderer.width(), KeystrokesRenderer.height((EMUtilsConfig) config));
	}

	@Override
	public HudOverlayPlacement.Position defaultPosition(
		HudLayoutConfig config,
		int screenWidth,
		int screenHeight,
		HudOverlayPlacement.PanelDimensions dimensions
	) {
		return new HudOverlayPlacement.Position(KeystrokesRenderer.defaultX(), KeystrokesRenderer.defaultY(screenHeight, dimensions.height()));
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
		int opacity = HudLayoutManager.layoutOpacity(id(), config);
		renderScaled(context, x, y, scalePercent / 100.0F, () ->
			KeystrokesRenderer.renderKeys(context, client, (EMUtilsConfig) config, 0, 0, opacity)
		);
	}
}
