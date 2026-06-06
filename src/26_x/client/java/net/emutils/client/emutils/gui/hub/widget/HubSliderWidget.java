package net.emutils.client.emutils.gui.hub.widget;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.emutils.client.emutils.gui.hub.HubPanelTheme;
import net.emutils.client.emutils.gui.hub.HubRoundedGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class HubSliderWidget extends AbstractWidget {
	private static final int TRACK_HEIGHT = 8;
	private static final int THUMB_DIAMETER = 14;

	private final int min;
	private final int max;
	private final IntSupplier getter;
	private final IntConsumer setter;
	private final Component suffix;
	private boolean dragging;

	public HubSliderWidget(
		int x,
		int y,
		int width,
		Component suffix,
		int min,
		int max,
		IntSupplier getter,
		IntConsumer setter
	) {
		super(x, y, width, HubPanelTheme.ROW_HEIGHT, Component.empty());
		this.suffix = suffix;
		this.min = min;
		this.max = max;
		this.getter = getter;
		this.setter = setter;
	}

	public int currentValue() {
		return Mth.clamp(getter.getAsInt(), min, max);
	}

	public Component valueText() {
		return Component.literal(String.valueOf(currentValue())).append(suffix);
	}

	public int valueWidth() {
		return Minecraft.getInstance().font.width(valueText());
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
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

		int thumbCenterX = Mth.clamp(
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
	public void onClick(MouseButtonEvent click, boolean doubled) {
		dragging = true;
		applyMouse(click.x());
	}

	@Override
	protected void onDrag(MouseButtonEvent click, double deltaX, double deltaY) {
		if (dragging) {
			applyMouse(click.x());
		}
	}

	@Override
	public void onRelease(MouseButtonEvent click) {
		dragging = false;
	}

	private void applyMouse(double mouseX) {
		double ratio = Mth.clamp((mouseX - getX()) / getWidth(), 0.0, 1.0);
		int value = min + (int) Math.round(ratio * (max - min));
		setter.accept(value);
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
		defaultButtonNarrationText(builder);
	}
}
