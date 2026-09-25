package net.emutils.client.emutils.spotify.gui;

import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** A round playback button of the pause menu player; the primary one (play/pause) is filled with the accent. */
final class SpotifyControlButton extends Button {
	private Identifier icon;
	private final boolean primary;

	SpotifyControlButton(int x, int y, int size, Component message, Identifier icon, boolean primary, Runnable action) {
		super(x, y, size, size, message, ignored -> action.run(), DEFAULT_NARRATION);
		this.icon = icon;
		this.primary = primary;
		setTooltip(Tooltip.create(message));
	}

	void setIcon(Identifier icon) {
		this.icon = icon;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		UiTheme theme = UiTheme.current();
		boolean hot = isHoveredOrFocused();
		int fill = primary
			? (hot ? theme.accentHover() : theme.accent())
			: (hot ? theme.surfaceHover() : theme.surfaceAlt());
		UiShapes.circle(context, getX(), getY(), width, fill);
		int iconSize = Math.round(width * 0.45F);
		int offset = (width - iconSize) / 2;
		UiIcons.draw(context, icon, getX() + offset, getY() + offset, iconSize, primary ? 0xFFFFFFFF : theme.text());
	}
}
