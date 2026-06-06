package net.emutils.client.emutils.gui.hub.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.emutils.client.emutils.gui.hub.HubPanelTheme;
import net.emutils.client.emutils.gui.hub.HubRoundedGraphics;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class HubToggleWidget extends ClickableWidget {
	private static final int TRACK_WIDTH = 36;
	private static final int TRACK_HEIGHT = 16;
	private static final int THUMB_DIAMETER = 12;
	private static final int TRACK_PADDING = 2;

	private final BooleanSupplier getter;
	private final Consumer<Boolean> setter;

	public HubToggleWidget(int x, int y, int width, BooleanSupplier getter, Consumer<Boolean> setter) {
		super(x, y, width, HubPanelTheme.ROW_HEIGHT, Text.empty());
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		boolean enabled = getter.getAsBoolean();
		int trackX = getX() + getWidth() - TRACK_WIDTH;
		int trackY = getY() + (getHeight() - TRACK_HEIGHT) / 2;
		int trackColor = enabled ? HubPanelTheme.ACCENT : HubPanelTheme.TRACK;
		HubRoundedGraphics.drawPill(context, trackX, trackY, trackX + TRACK_WIDTH, trackY + TRACK_HEIGHT, trackColor);

		int travel = TRACK_WIDTH - THUMB_DIAMETER - TRACK_PADDING * 2;
		int thumbCenterX = trackX + TRACK_PADDING + THUMB_DIAMETER / 2 + (enabled ? travel : 0);
		int thumbCenterY = trackY + TRACK_HEIGHT / 2;
		HubRoundedGraphics.drawCircle(context, thumbCenterX, thumbCenterY, THUMB_DIAMETER, HubPanelTheme.TEXT_PRIMARY);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		setter.accept(!getter.getAsBoolean());
	}

	@Override
	protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
