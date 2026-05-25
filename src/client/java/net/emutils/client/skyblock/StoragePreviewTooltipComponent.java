package net.emutils.client.skyblock;

import net.emutils.client.inventory.ShulkerStylePanelRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public final class StoragePreviewTooltipComponent implements TooltipComponent {
	private final int rows;
	private final DefaultedList<ItemStack> contents;

	public StoragePreviewTooltipComponent(StoragePreviewTooltipData data) {
		this.rows = data.rows();
		this.contents = DefaultedList.ofSize(data.rows() * ShulkerStylePanelRenderer.COLUMNS, ItemStack.EMPTY);
		for (int index = 0; index < this.contents.size() && index < data.contents().size(); index++) {
			ItemStack stack = data.contents().get(index);
			this.contents.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
		}
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return ShulkerStylePanelRenderer.WIDTH;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return ShulkerStylePanelRenderer.heightForRows(rows);
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		ShulkerStylePanelRenderer.drawPanel(context, x, y, rows, 1.0F);
		ShulkerStylePanelRenderer.drawContents(context, textRenderer, x, y, rows, contents, 1.0F);
	}
}
