package net.emutils.client.emutils.gui.hub.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.emutils.client.emutils.gui.hub.HubPanelTheme;
import net.emutils.client.emutils.gui.hub.HubRoundedGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class HubCycleWidget<T> extends AbstractWidget {
	private final Consumer<T> setter;
	private final Supplier<T> next;
	private final Supplier<Component> label;

	public HubCycleWidget(
		int x,
		int y,
		int width,
		Supplier<T> getter,
		Consumer<T> setter,
		Supplier<T> next,
		Supplier<Component> label
	) {
		super(x, y, width, HubPanelTheme.ROW_HEIGHT, Component.empty());
		this.setter = setter;
		this.next = next;
		this.label = label;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		Component value = label.get();
		int textWidth = Minecraft.getInstance().font.width(value);
		int pillLeft = getX() + getWidth() - textWidth - 16;
		int pillTop = getY() + (getHeight() - 18) / 2;
		int pillBottom = pillTop + 18;
		HubRoundedGraphics.drawPill(context, pillLeft, pillTop, getX() + getWidth(), pillBottom, HubPanelTheme.TRACK);
		context.text(
			Minecraft.getInstance().font,
			value,
			pillLeft + 8,
			pillTop + 5,
			HubPanelTheme.TEXT_ACCENT
		);
	}

	@Override
	public void onClick(MouseButtonEvent click, boolean doubled) {
		setter.accept(next.get());
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
		defaultButtonNarrationText(builder);
	}
}
