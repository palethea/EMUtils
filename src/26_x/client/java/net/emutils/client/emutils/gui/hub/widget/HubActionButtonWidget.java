package net.emutils.client.emutils.gui.hub.widget;

import net.emutils.client.emutils.gui.hub.HubPanelTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class HubActionButtonWidget extends AbstractWidget {
	private final PressAction onPress;

	public HubActionButtonWidget(int x, int y, int width, Component message, PressAction onPress) {
		super(x, y, width, HubPanelTheme.ROW_HEIGHT, message);
		this.onPress = onPress;
	}

	@FunctionalInterface
	public interface PressAction {
		void onPress(HubActionButtonWidget button);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		HubPanelTheme.drawActionButton(context, getX(), getY(), getWidth(), getHeight(), isHovered(), active);
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
		onPress.onPress(this);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
		defaultButtonNarrationText(builder);
	}
}
