package net.emutils.client.emutils.inventory;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.core.NonNullList;
import net.minecraft.util.ARGB;

public final class ShulkerStylePanelRenderer {
	public static final Identifier SHULKER_PREVIEW_SPRITE = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "shulker_preview");
	public static final int PANEL_BORDER = 7;
	public static final int ROWS = 3;
	public static final int COLUMNS = 9;
	public static final int SLOT_SIZE = 18;
	public static final int SLOT_ORIGIN = 8;
	public static final int WIDTH = PANEL_BORDER * 2 + COLUMNS * SLOT_SIZE;
	public static final int HEIGHT = PANEL_BORDER * 2 + ROWS * SLOT_SIZE;

	private ShulkerStylePanelRenderer() {
	}

	public static int heightForRows(int rows) {
		return PANEL_BORDER * 2 + rows * SLOT_SIZE;
	}

	public static void drawPanel(GuiGraphicsExtractor context, int x, int y, float opacity) {
		drawPanel(context, x, y, ROWS, opacity);
	}

	public static void drawPanel(GuiGraphicsExtractor context, int x, int y, int rows, float opacity) {
		context.blitSprite(RenderPipelines.GUI_TEXTURED, SHULKER_PREVIEW_SPRITE, x, y, WIDTH, heightForRows(rows), opacity);
	}

	public static void drawContents(
		GuiGraphicsExtractor context,
		Font textRenderer,
		int panelX,
		int panelY,
		NonNullList<ItemStack> contents,
		float opacity
	) {
		drawContents(context, textRenderer, panelX, panelY, ROWS, contents, opacity);
	}

	public static void drawContents(
		GuiGraphicsExtractor context,
		Font textRenderer,
		int panelX,
		int panelY,
		int rows,
		NonNullList<ItemStack> contents,
		float opacity
	) {
		int originX = panelX + SLOT_ORIGIN;
		int originY = panelY + SLOT_ORIGIN;
		InventoryPreviewRenderer.withItemOpacity(opacity, () -> {
			for (int index = 0; index < contents.size(); index++) {
				ItemStack stack = contents.get(index);
				if (stack.isEmpty()) {
					continue;
				}

				int column = index % COLUMNS;
				int row = index / COLUMNS;
				int slotX = originX + column * SLOT_SIZE;
				int slotY = originY + row * SLOT_SIZE;
				context.item(stack, slotX, slotY, index);
				if (opacity >= 1.0F) {
					context.itemDecorations(textRenderer, stack, slotX, slotY);
				} else {
					drawStackOverlay(context, textRenderer, stack, slotX, slotY, opacity);
				}
			}
		});
	}

	public static void drawMainInventory(
		GuiGraphicsExtractor context,
		Font textRenderer,
		int panelX,
		int panelY,
		NonNullList<ItemStack> stacks,
		float opacity
	) {
		NonNullList<ItemStack> contents = NonNullList.withSize(ROWS * COLUMNS, ItemStack.EMPTY);
		for (int index = Inventory.SELECTION_SIZE; index < Inventory.INVENTORY_SIZE; index++) {
			contents.set(index - Inventory.SELECTION_SIZE, stacks.get(index));
		}
		drawContents(context, textRenderer, panelX, panelY, contents, opacity);
	}

	private static void drawStackOverlay(
		GuiGraphicsExtractor context,
		Font textRenderer,
		ItemStack stack,
		int x,
		int y,
		float opacity
	) {
		if (stack.getCount() == 1) {
			return;
		}

		String count = String.valueOf(stack.getCount());
		context.text(
			textRenderer,
			count,
			x + 19 - 2 - textRenderer.width(count),
			y + 6 + 3,
			ARGB.white(opacity),
			true
		);
	}
}
