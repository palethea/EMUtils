package net.emutils.client.tweaks;

import net.emutils.client.inventory.ShulkerStylePanelRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.util.collection.DefaultedList;

public final class ShulkerTooltipComponent implements TooltipComponent {
	private final DefaultedList<net.minecraft.item.ItemStack> contents;

	public ShulkerTooltipComponent(ShulkerTooltipData data) {
		this.contents = DefaultedList.ofSize(ShulkerStylePanelRenderer.ROWS * ShulkerStylePanelRenderer.COLUMNS, net.minecraft.item.ItemStack.EMPTY);
		data.contents().copyTo(this.contents);
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return ShulkerStylePanelRenderer.WIDTH;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return ShulkerStylePanelRenderer.HEIGHT;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		ShulkerStylePanelRenderer.drawPanel(context, x, y, 1.0F);
		ShulkerStylePanelRenderer.drawContents(context, textRenderer, x, y, contents, 1.0F);
	}
}
