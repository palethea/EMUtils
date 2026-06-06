package net.emutils.client.emutils.gui.hub.widget;

import net.emutils.client.emutils.gui.hub.HubPanelTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class HubHeaderButtonWidget extends AbstractWidget {
	private static final int HEIGHT = 20;

	private final Runnable onPress;

	public HubHeaderButtonWidget(int x, int y, int width, Component message, Runnable onPress) {
		super(x, y, width, HEIGHT, message);
		this.onPress = onPress;
	}

	public static int widthFor(Component message) {
		return Minecraft.getInstance().font.width(message) + 16;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		HubPanelTheme.drawHeaderButton(context, getX(), getY(), getWidth(), getHeight(), isHovered(), active);
		int textColor = active ? HubPanelTheme.TEXT_ACCENT : HubPanelTheme.TEXT_MUTED;
		context.centeredText(
			Minecraft.getInstance().font,
			getMessage(),
			getX() + getWidth() / 2,
			getY() + (getHeight() - 8) / 2,
			textColor
		);
	}

	@Override
	public void onClick(MouseButtonEvent click, boolean doubled) {
		onPress.run();
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
		defaultButtonNarrationText(builder);
	}
}
