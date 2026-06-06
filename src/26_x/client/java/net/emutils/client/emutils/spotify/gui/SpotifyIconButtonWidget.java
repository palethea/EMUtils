package net.emutils.client.emutils.spotify.gui;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;

public final class SpotifyIconButtonWidget extends Button {
	private Identifier icon;
	private final int iconSize;

	private SpotifyIconButtonWidget(int x, int y, net.minecraft.network.chat.Component message, Identifier icon, int iconSize, Button.OnPress onPress) {
		super(x, y, 20, 20, message, onPress, DEFAULT_NARRATION);
		this.icon = icon;
		this.iconSize = iconSize;
		this.setTooltip(Tooltip.create(message));
	}

	public static SpotifyIconButtonWidget create(int x, int y, net.minecraft.network.chat.Component message, Identifier icon, Button.OnPress onPress) {
		return new SpotifyIconButtonWidget(x, y, message, icon, SpotifyIcons.SIZE, onPress);
	}

	public void setIcon(Identifier icon) {
		this.icon = icon;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		this.extractDefaultSprite(context);
		int drawX = this.getX() + (this.width - iconSize) / 2;
		int drawY = this.getY() + (this.height - iconSize) / 2;
		context.blit(RenderPipelines.GUI_TEXTURED, icon, drawX, drawY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
	}
}
