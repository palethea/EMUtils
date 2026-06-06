package net.emutils.client.emutils.screenshot.gui;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;

public final class GalleryIconButtonWidget extends Button {
	private final Identifier icon;
	private final int iconSize;

	private GalleryIconButtonWidget(int width, int height, net.minecraft.network.chat.Component message, Identifier icon, int iconSize, Button.OnPress onPress) {
		super(0, 0, width, height, message, onPress, DEFAULT_NARRATION);
		this.icon = icon;
		this.iconSize = iconSize;
		this.setTooltip(Tooltip.create(message));
	}

	public static GalleryIconButtonWidget create(net.minecraft.network.chat.Component message, Identifier icon, int iconSize, Button.OnPress onPress) {
		return new GalleryIconButtonWidget(20, 20, message, icon, iconSize, onPress);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		this.extractDefaultSprite(context);
		int x = this.getX() + (this.width - iconSize) / 2;
		int y = this.getY() + (this.height - iconSize) / 2;
		context.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
	}
}
