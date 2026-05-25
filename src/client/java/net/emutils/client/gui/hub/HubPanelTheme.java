package net.emutils.client.gui.hub;

import net.minecraft.client.gui.DrawContext;

public final class HubPanelTheme {
	public static final int BACKGROUND = 0xCC1A2233;
	public static final int SHADOW = 0x55000000;
	public static final int BORDER = 0x88101828;
	public static final int SURFACE = 0xAA243044;
	public static final int SURFACE_HOVER = 0xCC2E3C56;
	public static final int SURFACE_SELECTED = 0xD6384868;
	public static final int ACCENT = 0xFF2EE66F;
	public static final int ACCENT_DIM = 0x662EE66F;
	public static final int TRACK = 0xCC121A28;
	public static final int TEXT_PRIMARY = 0xFFF2F5FA;
	public static final int TEXT_MUTED = 0xFF9AA8C2;
	public static final int TEXT_ACCENT = 0xFF2EE66F;
	public static final int DIVIDER = 0x44101828;
	public static final int DIM_OVERLAY = 0x66080C14;

	public static final int ROW_HEIGHT = 26;
	public static final int SECTION_GAP = 8;

	private HubPanelTheme() {
	}

	public static void drawPanel(DrawContext context, int x, int y, int width, int height) {
		HubRoundedGraphics.drawRoundedRect(context, x + 2, y + 3, x + width + 2, y + height + 3, SHADOW, HubRoundedGraphics.RADIUS_LG);
		HubRoundedGraphics.drawRoundedRect(context, x, y, x + width, y + height, BACKGROUND, HubRoundedGraphics.RADIUS_LG);
	}

	public static void drawSidebarSurface(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, 0x5520283C);
	}

	public static void drawContentSurface(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, 0x3320283C);
	}

	public static void drawRowBackground(DrawContext context, int x, int y, int width, int height, boolean hovered) {
		HubRoundedGraphics.drawRoundedRect(
			context,
			x,
			y,
			x + width,
			y + height,
			hovered ? SURFACE_HOVER : SURFACE,
			HubRoundedGraphics.RADIUS_MD
		);
	}

	public static void drawSelectedCategory(DrawContext context, int x, int y, int width, int height) {
		HubRoundedGraphics.drawRoundedRect(context, x, y, x + width, y + height, SURFACE_SELECTED, HubRoundedGraphics.RADIUS_MD);
		context.fill(x, y + 4, x + 2, y + height - 4, ACCENT);
	}

	public static void drawHeaderButton(DrawContext context, int x, int y, int width, int height, boolean hovered, boolean active) {
		int color = !active ? TRACK : hovered ? SURFACE_HOVER : SURFACE;
		HubRoundedGraphics.drawRoundedRect(context, x, y, x + width, y + height, color, HubRoundedGraphics.RADIUS_MD);
	}

	public static void drawActionButton(DrawContext context, int x, int y, int width, int height, boolean hovered, boolean active) {
		int color = !active ? TRACK : hovered ? SURFACE_HOVER : SURFACE;
		HubRoundedGraphics.drawRoundedRect(context, x, y, x + width, y + height, color, HubRoundedGraphics.RADIUS_MD);
	}
}
