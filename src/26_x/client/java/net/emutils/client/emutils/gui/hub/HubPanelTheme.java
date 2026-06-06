package net.emutils.client.emutils.gui.hub;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HubPanelTheme {
	public static final int BACKGROUND = 0xFF1A2233;
	public static final int CONTENT = 0xFF222D42;
	public static final int BORDER = 0xFF101828;
	public static final int SURFACE = 0xFF243044;
	public static final int SURFACE_HOVER = 0xFF2E3C56;
	public static final int SURFACE_SELECTED = 0xFF384868;
	public static final int ACCENT = 0xFF2EE66F;
	public static final int ACCENT_DIM = 0xFF1E5B36;
	public static final int TRACK = 0xFF121A28;
	public static final int TEXT_PRIMARY = 0xFFF2F5FA;
	public static final int TEXT_MUTED = 0xFF9AA8C2;
	public static final int TEXT_DIM = 0xFF6F7D96;
	public static final int TEXT_ACCENT = 0xFF2EE66F;
	public static final int DIVIDER = 0xFF101828;
	public static final int DIM_OVERLAY = 0x88060A12;

	public static final int ROW_HEIGHT = 26;
	public static final int SECTION_GAP = 8;

	private HubPanelTheme() {
	}

	public static void drawPanel(GuiGraphicsExtractor context, int x, int y, int width, int height) {
		HubRoundedGraphics.drawRoundedRect(context, x, y, x + width, y + height, BACKGROUND, HubRoundedGraphics.RADIUS_LG);
	}

	public static void drawSidebarSurface(GuiGraphicsExtractor context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, BACKGROUND);
	}

	public static void drawContentSurface(GuiGraphicsExtractor context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, CONTENT);
	}

	public static void drawRowBackground(GuiGraphicsExtractor context, int x, int y, int width, int height, boolean hovered) {
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

	public static void drawSelectedCategory(GuiGraphicsExtractor context, int x, int y, int width, int height) {
		HubRoundedGraphics.drawRoundedRect(context, x, y, x + width, y + height, SURFACE_SELECTED, HubRoundedGraphics.RADIUS_MD);
		context.fill(x, y + 4, x + 2, y + height - 4, ACCENT);
	}

	public static void drawHeaderButton(GuiGraphicsExtractor context, int x, int y, int width, int height, boolean hovered, boolean active) {
		int color = !active ? TRACK : hovered ? SURFACE_HOVER : SURFACE;
		HubRoundedGraphics.drawRoundedRect(context, x, y, x + width, y + height, color, HubRoundedGraphics.RADIUS_MD);
	}

	public static void drawActionButton(GuiGraphicsExtractor context, int x, int y, int width, int height, boolean hovered, boolean active) {
		int color = !active ? TRACK : hovered ? SURFACE_HOVER : SURFACE;
		HubRoundedGraphics.drawRoundedRect(context, x, y, x + width, y + height, color, HubRoundedGraphics.RADIUS_MD);
	}
}
