package net.emutils.client.emutils.gui.hub.widget;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.emutils.client.emutils.gui.hub.HubPanelTheme;
import net.emutils.client.emutils.gui.hub.HubRoundedGraphics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class HubSliderWidget extends ClickableWidget {
	private static final int TRACK_HEIGHT = 8;
	private static final int THUMB_DIAMETER = 14;

	private final int min;
	private final int max;
	private final IntSupplier getter;
	private final IntConsumer setter;
	private final Text suffix;
	private boolean dragging;

	public HubSliderWidget(
		int x,
		int y,
		int width,
		Text suffix,
		int min,
		int max,
		IntSupplier getter,
		IntConsumer setter
	) {
		super(x, y, width, HubPanelTheme.ROW_HEIGHT, Text.empty());
		this.suffix = suffix;
		this.min = min;
		this.max = max;
		this.getter = getter;
		this.setter = setter;
	}

	public int currentValue() {
		return MathHelper.clamp(getter.getAsInt(), min, max);
	}

	public Text valueText() {
		return Text.literal(String.valueOf(currentValue())).append(suffix);
	}

	public int valueWidth() {
		return MinecraftClient.getInstance().textRenderer.getWidth(valueText());
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		int trackLeft = getX();
		int trackRight = getX() + getWidth();
		int trackTop = getY() + (getHeight() - TRACK_HEIGHT) / 2;
		int trackBottom = trackTop + TRACK_HEIGHT;
		double ratio = max == min ? 0.0 : (currentValue() - min) / (double) (max - min);
		int fillRight = trackLeft + (int) Math.round((trackRight - trackLeft) * ratio);

		HubRoundedGraphics.drawPill(context, trackLeft, trackTop, trackRight, trackBottom, HubPanelTheme.TRACK);

		int accentRight = Math.max(trackLeft + TRACK_HEIGHT, fillRight);
		if (accentRight > trackLeft) {
			HubRoundedGraphics.drawPill(context, trackLeft, trackTop, accentRight, trackBottom, HubPanelTheme.ACCENT);
		}

		int thumbCenterX = MathHelper.clamp(
			fillRight,
			trackLeft + THUMB_DIAMETER / 2,
			trackRight - THUMB_DIAMETER / 2
		);
		int thumbCenterY = trackTop + TRACK_HEIGHT / 2;
		if (isHovered() || dragging) {
			HubRoundedGraphics.drawCircle(context, thumbCenterX, thumbCenterY, THUMB_DIAMETER + 2, HubPanelTheme.ACCENT_DIM);
		}

		HubRoundedGraphics.drawCircle(context, thumbCenterX, thumbCenterY, THUMB_DIAMETER, HubPanelTheme.TEXT_PRIMARY);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		dragging = true;
		applyMouse(click.x());
	}

	@Override
	protected void onDrag(Click click, double deltaX, double deltaY) {
		if (dragging) {
			applyMouse(click.x());
		}
	}

	@Override
	public void onRelease(Click click) {
		dragging = false;
	}

	private void applyMouse(double mouseX) {
		double ratio = MathHelper.clamp((mouseX - getX()) / getWidth(), 0.0, 1.0);
		int value = min + (int) Math.round(ratio * (max - min));
		setter.accept(value);
	}

	@Override
	protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
