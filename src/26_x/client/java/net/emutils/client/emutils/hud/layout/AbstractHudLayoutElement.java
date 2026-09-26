package net.emutils.client.emutils.hud.layout;

import net.emutils.client.emutils.gui.ui.UiRasterScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class AbstractHudLayoutElement implements HudLayoutElement {
	private final HudElementId id;

	protected AbstractHudLayoutElement(HudElementId id) {
		this.id = id;
	}

	@Override
	public HudElementId id() {
		return id;
	}

	protected static void renderScaled(GuiGraphicsExtractor context, int x, int y, float scale, Runnable drawUnscaled) {
		context.pose().pushMatrix();
		// Text and icons of the EMUtils UI are rasterized for the scale, so they stay sharp.
		UiRasterScale.set(scale);
		try {
			context.pose().translate(x, y);
			context.pose().scale(scale, scale);
			drawUnscaled.run();
		} finally {
			UiRasterScale.reset();
			context.pose().popMatrix();
		}
	}
}
