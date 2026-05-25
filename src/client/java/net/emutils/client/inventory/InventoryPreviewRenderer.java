package net.emutils.client.inventory;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class InventoryPreviewRenderer {
	private static final Identifier ID = Identifier.of(EMUtilsClient.MOD_ID, "inventory_preview");
	private static final int BOTTOM_MARGIN = 62;
	private static float itemRenderOpacity = 1.0F;

	private InventoryPreviewRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	public static float itemRenderOpacity() {
		return itemRenderOpacity;
	}

	public static void withItemOpacity(float opacity, Runnable draw) {
		float previous = itemRenderOpacity;
		itemRenderOpacity = opacity;
		try {
			draw.run();
		} finally {
			itemRenderOpacity = previous;
		}
	}

	private static void render(DrawContext context) {
		EMUtilsConfig config = EMUtilsClient.config();
		MinecraftClient client = MinecraftClient.getInstance();
		if (config == null
			|| !config.inventoryToolsEnabled()
			|| !config.inventoryPreviewEnabled()
			|| client == null
			|| client.player == null
			|| client.world == null
			|| client.currentScreen != null
			|| !MinecraftClient.isHudEnabled()) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}

		int x = (context.getScaledWindowWidth() - ShulkerStylePanelRenderer.WIDTH) / 2;
		int y = context.getScaledWindowHeight() - BOTTOM_MARGIN - ShulkerStylePanelRenderer.HEIGHT;
		float opacity = Math.min(100, Math.max(0, config.inventoryPreviewOpacity())) / 100.0F;
		ShulkerStylePanelRenderer.drawPanel(context, x, y, opacity);
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
