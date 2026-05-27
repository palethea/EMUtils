package net.emutils.client.hud.layout;

import java.util.List;
import net.minecraft.client.gui.DrawContext;

public final class HudLayoutEditorChrome {
	public static final int OUTLINE_COLOR = 0xFF40C060;
	public static final int HANDLE_COLOR = 0xFF80FF90;
	public static final int OPACITY_BUTTON_COLOR = 0xFF60A8FF;
	public static final int GUIDE_COLOR = 0x66FFFFFF;
	public static final int HANDLE_SIZE = 10;

	private HudLayoutEditorChrome() {
	}

	public static void drawOutline(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + 1, OUTLINE_COLOR);
		context.fill(x, y + height - 1, x + width, y + height, OUTLINE_COLOR);
		context.fill(x, y, x + 1, y + height, OUTLINE_COLOR);
		context.fill(x + width - 1, y, x + width, y + height, OUTLINE_COLOR);
	}

	public static void drawResizeHandle(DrawContext context, int x, int y, int width) {
		int handleX = x + width - HANDLE_SIZE;
		int handleY = y;
		context.fill(handleX, handleY, handleX + HANDLE_SIZE, handleY + HANDLE_SIZE, HANDLE_COLOR);
	}

	public static void drawOpacityButton(DrawContext context, int x, int y) {
		context.fill(x, y, x + HANDLE_SIZE, y + HANDLE_SIZE, OPACITY_BUTTON_COLOR);
		context.fill(x + 3, y + 3, x + HANDLE_SIZE - 3, y + HANDLE_SIZE - 3, 0xAA101725);
	}

	public static void drawSnapGuides(DrawContext context, List<HudLayoutSnapping.GuideLine> guides, int screenWidth, int screenHeight) {
		for (HudLayoutSnapping.GuideLine guide : guides) {
			if (guide.axis() == HudLayoutSnapping.GuideAxis.VERTICAL) {
				context.fill(guide.coordinate(), 0, guide.coordinate() + 1, screenHeight, GUIDE_COLOR);
			} else {
				context.fill(0, guide.coordinate(), screenWidth, guide.coordinate() + 1, GUIDE_COLOR);
			}
		}
	}

	public static boolean isResizeHandleHit(int mouseX, int mouseY, int x, int y, int width) {
		int handleX = x + width - HANDLE_SIZE;
		int handleY = y;
		return mouseX >= handleX
			&& mouseX < handleX + HANDLE_SIZE
			&& mouseY >= handleY
			&& mouseY < handleY + HANDLE_SIZE;
	}

	public static boolean isOpacityButtonHit(int mouseX, int mouseY, int x, int y) {
		return mouseX >= x
			&& mouseX < x + HANDLE_SIZE
			&& mouseY >= y
			&& mouseY < y + HANDLE_SIZE;
	}
}
