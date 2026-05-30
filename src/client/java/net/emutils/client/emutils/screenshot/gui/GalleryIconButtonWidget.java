package net.emutils.client.emutils.screenshot.gui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Identifier;

public final class GalleryIconButtonWidget extends ButtonWidget {
	private final Identifier icon;
	private final int iconSize;

	private GalleryIconButtonWidget(int width, int height, net.minecraft.text.Text message, Identifier icon, int iconSize, PressAction onPress) {
		super(0, 0, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
		this.icon = icon;
		this.iconSize = iconSize;
		this.setTooltip(Tooltip.of(message));
	}

	public static GalleryIconButtonWidget create(net.minecraft.text.Text message, Identifier icon, int iconSize, PressAction onPress) {
		return new GalleryIconButtonWidget(20, 20, message, icon, iconSize, onPress);
	}

	@Override
	protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.drawButton(context);
		int x = this.getX() + (this.width - iconSize) / 2;
		int y = this.getY() + (this.height - iconSize) / 2;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
	}
}
