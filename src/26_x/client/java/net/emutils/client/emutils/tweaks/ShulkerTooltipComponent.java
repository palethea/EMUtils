package net.emutils.client.emutils.tweaks;

import net.emutils.client.emutils.inventory.ShulkerStylePanelRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;

public final class ShulkerTooltipComponent implements ClientTooltipComponent {
	private final NonNullList<net.minecraft.world.item.ItemStack> contents;

	public ShulkerTooltipComponent(ShulkerTooltipData data) {
		this.contents = NonNullList.withSize(ShulkerStylePanelRenderer.ROWS * ShulkerStylePanelRenderer.COLUMNS, net.minecraft.world.item.ItemStack.EMPTY);
		data.contents().copyInto(this.contents);
	}

	@Override
	public int getWidth(Font textRenderer) {
		return ShulkerStylePanelRenderer.WIDTH;
	}

	@Override
	public int getHeight(Font textRenderer) {
		return ShulkerStylePanelRenderer.HEIGHT;
	}

	@Override
	public void extractImage(Font textRenderer, int x, int y, int width, int height, GuiGraphicsExtractor context) {
		ShulkerStylePanelRenderer.drawPanel(context, x, y, 1.0F);
		ShulkerStylePanelRenderer.drawContents(context, textRenderer, x, y, contents, 1.0F);
	}
}
