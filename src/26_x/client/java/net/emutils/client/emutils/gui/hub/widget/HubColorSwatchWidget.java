package net.emutils.client.emutils.gui.hub.widget;

import java.util.function.IntSupplier;
import net.emutils.client.emutils.gui.hub.HubLayout;
import net.emutils.client.emutils.gui.hub.HubPanelTheme;
import net.emutils.client.emutils.gui.hub.HubRoundedGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class HubColorSwatchWidget extends AbstractWidget {
	private final IntSupplier getter;
	private final Runnable onOpenPicker;

	public HubColorSwatchWidget(int x, int y, IntSupplier getter, Runnable onOpenPicker) {
		super(x, y, HubLayout.COLOR_SWATCH_SIZE, HubPanelTheme.ROW_HEIGHT, Component.empty());
		this.getter = getter;
		this.onOpenPicker = onOpenPicker;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		int color = getter.getAsInt();
		int swatchX = getX() + getWidth() - HubLayout.COLOR_SWATCH_SIZE;
		int swatchY = getY() + (getHeight() - HubLayout.COLOR_SWATCH_SIZE) / 2;
		if (isHovered()) {
			HubRoundedGraphics.drawRoundedRect(
				context,
				swatchX - 2,
				swatchY - 2,
				swatchX + HubLayout.COLOR_SWATCH_SIZE + 2,
				swatchY + HubLayout.COLOR_SWATCH_SIZE + 2,
				HubPanelTheme.ACCENT_DIM,
				HubRoundedGraphics.RADIUS_SM
			);
		}

		HubRoundedGraphics.drawRoundedRect(
			context,
			swatchX,
			swatchY,
			swatchX + HubLayout.COLOR_SWATCH_SIZE,
			swatchY + HubLayout.COLOR_SWATCH_SIZE,
			color,
			HubRoundedGraphics.RADIUS_SM
		);
	}

	@Override
	public void onClick(MouseButtonEvent click, boolean doubled) {
		onOpenPicker.run();
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
		defaultButtonNarrationText(builder);
	}
}
