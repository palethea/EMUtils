package net.emutils.client.emutils.spotify.gui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Identifier;

public final class SpotifyIconButtonWidget extends ButtonWidget {
	private Identifier icon;
	private final int iconSize;

	private SpotifyIconButtonWidget(int x, int y, net.minecraft.text.Text message, Identifier icon, int iconSize, PressAction onPress) {
		super(x, y, 20, 20, message, onPress, DEFAULT_NARRATION_SUPPLIER);
		this.icon = icon;
		this.iconSize = iconSize;
		this.setTooltip(Tooltip.of(message));
	}

	public static SpotifyIconButtonWidget create(int x, int y, net.minecraft.text.Text message, Identifier icon, PressAction onPress) {
		return new SpotifyIconButtonWidget(x, y, message, icon, SpotifyIcons.SIZE, onPress);
	}

	public void setIcon(Identifier icon) {
		this.icon = icon;
	}

	@Override
	protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.drawButton(context);
		int drawX = this.getX() + (this.width - iconSize) / 2;
		int drawY = this.getY() + (this.height - iconSize) / 2;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, drawX, drawY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
	}
}
