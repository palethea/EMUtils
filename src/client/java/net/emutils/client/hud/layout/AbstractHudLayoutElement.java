package net.emutils.client.hud.layout;

import net.minecraft.client.gui.DrawContext;

public abstract class AbstractHudLayoutElement implements HudLayoutElement {
	private final HudElementId id;

	protected AbstractHudLayoutElement(HudElementId id) {
		this.id = id;
	}

	@Override
	public HudElementId id() {
		return id;
	}

	protected static void renderScaled(DrawContext context, int x, int y, float scale, Runnable drawUnscaled) {
		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(x, y);
			context.getMatrices().scale(scale, scale);
			drawUnscaled.run();
		} finally {
			context.getMatrices().popMatrix();
		}
	}
}
