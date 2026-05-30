package net.emutils.client.emutils.gui.hub.widget;

import net.emutils.client.emutils.gui.hub.HubPanelTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class HubActionButtonWidget extends ClickableWidget {
	private final PressAction onPress;

	public HubActionButtonWidget(int x, int y, int width, Text message, PressAction onPress) {
		super(x, y, width, HubPanelTheme.ROW_HEIGHT, message);
		this.onPress = onPress;
	}

	@FunctionalInterface
	public interface PressAction {
		void onPress(HubActionButtonWidget button);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		HubPanelTheme.drawActionButton(context, getX(), getY(), getWidth(), getHeight(), isHovered(), active);
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
		onPress.onPress(this);
	}

	@Override
	protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
