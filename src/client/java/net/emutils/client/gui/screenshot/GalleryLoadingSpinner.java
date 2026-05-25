package net.emutils.client.gui.screenshot;

import net.minecraft.client.gui.DrawContext;

public final class GalleryLoadingSpinner {
	private static final int DOT_COUNT = 8;
	private static final int DOT_RADIUS = 1;
	private static final int ORBIT_RADIUS = 9;

	private GalleryLoadingSpinner() {
	}

	public static void render(DrawContext context, int centerX, int centerY) {
		render(context, centerX, centerY, ORBIT_RADIUS);
	}

	public static void render(DrawContext context, int centerX, int centerY, int orbitRadius) {
		long millis = System.currentTimeMillis();
		for (int index = 0; index < DOT_COUNT; index++) {
			double angle = Math.toRadians((millis / 16.0) + index * (360.0 / DOT_COUNT));
			int dotX = centerX + (int) Math.round(Math.cos(angle) * orbitRadius);
			int dotY = centerY + (int) Math.round(Math.sin(angle) * orbitRadius);
			int alpha = 55 + (index * 200 / DOT_COUNT);
			int color = 0xFF000000 | (alpha << 16) | (alpha << 8) | alpha;
			context.fill(dotX - DOT_RADIUS, dotY - DOT_RADIUS, dotX + DOT_RADIUS + 1, dotY + DOT_RADIUS + 1, color);
		}
	}
}
