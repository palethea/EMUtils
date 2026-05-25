package net.emutils.client.gui.hub.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.emutils.client.gui.hub.HubPanelTheme;
import net.emutils.client.gui.hub.HubRoundedGraphics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class HubCycleWidget<T> extends ClickableWidget {
	private final Consumer<T> setter;
	private final Supplier<T> next;
	private final Supplier<Text> label;

	public HubCycleWidget(
		int x,
		int y,
		int width,
		Supplier<T> getter,
		Consumer<T> setter,
		Supplier<T> next,
		Supplier<Text> label
	) {
		super(x, y, width, HubPanelTheme.ROW_HEIGHT, Text.empty());
		this.setter = setter;
		this.next = next;
		this.label = label;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		Text value = label.get();
		int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(value);
		int pillLeft = getX() + getWidth() - textWidth - 16;
		int pillTop = getY() + (getHeight() - 18) / 2;
		int pillBottom = pillTop + 18;
		HubRoundedGraphics.drawPill(context, pillLeft, pillTop, getX() + getWidth(), pillBottom, HubPanelTheme.TRACK);
		context.drawTextWithShadow(
			MinecraftClient.getInstance().textRenderer,
			value,
			pillLeft + 8,
			pillTop + 5,
			HubPanelTheme.TEXT_ACCENT
		);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		setter.accept(next.get());
	}

	@Override
	protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
