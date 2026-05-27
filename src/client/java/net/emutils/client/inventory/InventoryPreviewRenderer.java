package net.emutils.client.inventory;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.hud.layout.HudElementId;
import net.emutils.client.hud.layout.HudLayoutManager;
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
		if (config == null || client == null || client.player == null || client.world == null) {
			return;
		}
		if ((!config.inventoryToolsEnabled() || !config.inventoryPreviewEnabled()) && !HudLayoutManager.isEditing()) {
			return;
		}
		if (client.currentScreen != null && !HudLayoutManager.isEditing()) {
			return;
		}
		if (!MinecraftClient.isHudEnabled() && !HudLayoutManager.isEditing()) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (HudLayoutManager.isEditing()) {
			return;
		}

		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			HudElementId.INVENTORY_PREVIEW,
			config,
			context.getScaledWindowWidth(),
			context.getScaledWindowHeight(),
			client
		);
		float opacity = layout.opacityPercent() / 100.0F;
		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(layout.position().x(), layout.position().y());
			context.getMatrices().scale(layout.scaleFactor(), layout.scaleFactor());
			ShulkerStylePanelRenderer.drawPanel(context, 0, 0, opacity);
			ShulkerStylePanelRenderer.drawMainInventory(
				context,
				client.textRenderer,
				0,
				0,
				client.player.getInventory().getMainStacks(),
				opacity
			);
		} finally {
			context.getMatrices().popMatrix();
		}
	}
}
