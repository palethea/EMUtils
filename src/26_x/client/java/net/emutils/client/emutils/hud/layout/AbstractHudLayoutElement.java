package net.emutils.client.emutils.hud.layout;

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
		try {
			context.pose().translate(x, y);
			context.pose().scale(scale, scale);
			drawUnscaled.run();
		} finally {
			context.pose().popMatrix();
		}
	}
}
