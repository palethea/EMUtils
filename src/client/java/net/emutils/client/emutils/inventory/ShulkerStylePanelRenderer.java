package net.emutils.client.emutils.inventory;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.ColorHelper;

public final class ShulkerStylePanelRenderer {
	public static final Identifier SHULKER_PREVIEW_SPRITE = Identifier.of(EMUtilsClient.MOD_ID, "shulker_preview");
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

	public static void drawPanel(DrawContext context, int x, int y, float opacity) {
		drawPanel(context, x, y, ROWS, opacity);
	}

	public static void drawPanel(DrawContext context, int x, int y, int rows, float opacity) {
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SHULKER_PREVIEW_SPRITE, x, y, WIDTH, heightForRows(rows), opacity);
	}

	public static void drawContents(
		DrawContext context,
		TextRenderer textRenderer,
		int panelX,
		int panelY,
		DefaultedList<ItemStack> contents,
		float opacity
	) {
		drawContents(context, textRenderer, panelX, panelY, ROWS, contents, opacity);
	}

	public static void drawContents(
		DrawContext context,
		TextRenderer textRenderer,
		int panelX,
		int panelY,
		int rows,
		DefaultedList<ItemStack> contents,
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
				context.drawItem(stack, slotX, slotY, index);
				if (opacity >= 1.0F) {
					context.drawStackOverlay(textRenderer, stack, slotX, slotY);
				} else {
					drawStackOverlay(context, textRenderer, stack, slotX, slotY, opacity);
				}
			}
		});
	}

	public static void drawMainInventory(
		DrawContext context,
		TextRenderer textRenderer,
		int panelX,
		int panelY,
		DefaultedList<ItemStack> stacks,
		float opacity
	) {
		DefaultedList<ItemStack> contents = DefaultedList.ofSize(ROWS * COLUMNS, ItemStack.EMPTY);
		for (int index = PlayerInventory.HOTBAR_SIZE; index < PlayerInventory.MAIN_SIZE; index++) {
			contents.set(index - PlayerInventory.HOTBAR_SIZE, stacks.get(index));
		}
		drawContents(context, textRenderer, panelX, panelY, contents, opacity);
	}

	private static void drawStackOverlay(
		DrawContext context,
		TextRenderer textRenderer,
		ItemStack stack,
		int x,
		int y,
		float opacity
	) {
		if (stack.getCount() == 1) {
			return;
		}

		String count = String.valueOf(stack.getCount());
		context.drawText(
			textRenderer,
			count,
			x + 19 - 2 - textRenderer.getWidth(count),
			y + 6 + 3,
			ColorHelper.getWhite(opacity),
			true
		);
	}
}
