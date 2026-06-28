package net.emutils.client.emutils.inventory;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emhelpers.client.hud.layout.HudElementId;
import net.emhelpers.client.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class InventoryPreviewRenderer {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "inventory_preview");
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

	private static void render(GuiGraphicsExtractor context) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || client == null || client.player == null || client.level == null) {
			return;
		}
		if ((!config.inventoryToolsEnabled() || !config.inventoryPreviewEnabled()) && !HudLayoutManager.isEditing()) {
			return;
		}
		if (net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) != null && !HudLayoutManager.isEditing()) {
			return;
		}
		if (net.emutils.client.emutils.compat.MinecraftClientCompat.isHudHidden(client) && !HudLayoutManager.isEditing()) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (HudLayoutManager.isEditing()) {
			return;
		}

		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			net.emutils.client.EMUtilsHudElements.INVENTORY_PREVIEW,
			config,
			context.guiWidth(),
			context.guiHeight(),
			client
		);
		float opacity = layout.opacityPercent() / 100.0F;
		context.pose().pushMatrix();
		try {
			context.pose().translate(layout.position().x(), layout.position().y());
			context.pose().scale(layout.scaleFactor(), layout.scaleFactor());
			ShulkerStylePanelRenderer.drawPanel(context, 0, 0, opacity);
			ShulkerStylePanelRenderer.drawMainInventory(
				context,
				client.font,
				0,
				0,
				client.player.getInventory().getNonEquipmentItems(),
				opacity
			);
		} finally {
			context.pose().popMatrix();
		}
	}
}
