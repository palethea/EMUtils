package net.emutils.client.emutils.gui.hub.widget;

import net.emutils.client.emutils.gui.hub.HubPanelTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class HubHeaderButtonWidget extends ClickableWidget {
	private static final int HEIGHT = 20;

	private final Runnable onPress;

	public HubHeaderButtonWidget(int x, int y, int width, Text message, Runnable onPress) {
		super(x, y, width, HEIGHT, message);
		this.onPress = onPress;
	}

	public static int widthFor(Text message) {
		return MinecraftClient.getInstance().textRenderer.getWidth(message) + 16;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		HubPanelTheme.drawHeaderButton(context, getX(), getY(), getWidth(), getHeight(), isHovered(), active);
		int textColor = active ? HubPanelTheme.TEXT_ACCENT : HubPanelTheme.TEXT_MUTED;
		context.drawCenteredTextWithShadow(
			MinecraftClient.getInstance().textRenderer,
			getMessage(),
			getX() + getWidth() / 2,
			getY() + (getHeight() - 8) / 2,
			textColor
		);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		onPress.run();
	}

	@Override
	protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
