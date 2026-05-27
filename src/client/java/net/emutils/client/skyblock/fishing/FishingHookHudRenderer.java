package net.emutils.client.skyblock.fishing;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.hud.layout.HudElementId;
import net.emutils.client.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class FishingHookHudRenderer {
	private static final Identifier ID = Identifier.of(EMUtilsClient.MOD_ID, "fishing_hook_display");

	private FishingHookHudRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	public static int unscaledPanelWidth(TextRenderer textRenderer, Text text) {
		return textRenderer.getWidth(text);
	}

	public static int unscaledPanelHeight(TextRenderer textRenderer) {
		return textRenderer.fontHeight;
	}

	public static void renderText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y) {
		context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFFFF);
	}

	public static void renderTextCenteredInAnchor(
		DrawContext context,
		TextRenderer textRenderer,
		Text text,
		int anchorWidth,
		int y
	) {
		int drawX = Math.max(0, (anchorWidth - textRenderer.getWidth(text)) / 2);
		renderText(context, textRenderer, text, drawX, y);
	}

	private static void render(DrawContext context) {
		EMUtilsConfig config = EMUtilsClient.config();
		MinecraftClient client = MinecraftClient.getInstance();
		if (config == null || client.player == null || client.world == null) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}

		if (HudLayoutManager.isEditing()) {
			return;
		}

		Text text = resolveText();
		if (text == null) {
			return;
		}

		TextRenderer textRenderer = client.textRenderer;
		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			HudElementId.FISHING_HOOK,
			config,
			context.getScaledWindowWidth(),
			context.getScaledWindowHeight(),
			client
		);

		int anchorWidth = FishingHookDisplayManager.layoutAnchorWidth(textRenderer);
		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(layout.position().x(), layout.position().y());
			context.getMatrices().scale(layout.scaleFactor(), layout.scaleFactor());
			renderTextCenteredInAnchor(context, textRenderer, text, anchorWidth, 0);
		} finally {
			context.getMatrices().popMatrix();
		}
	}

	private static Text resolveText() {
		return FishingHookDisplayManager.displayText();
	}
}
