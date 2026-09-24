package net.emutils.client.emutils.gui.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * A vertically scrolling region. Space for the scrollbar is always reserved so content never shifts
 * when it appears, and content fades out at the top and bottom edges instead of being cut off.
 */
public final class UiScrollArea {
	public static final int GUTTER = 6;
	private static final int SCROLLBAR_WIDTH = 3;
	private static final double SCROLL_STEP = 24.0;

	private int x;
	private int y;
	private int width;
	private int height;
	private int contentHeight;
	private double offset;
	private double target;

	public void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		clamp();
	}

	public void setContentHeight(int contentHeight) {
		this.contentHeight = contentHeight;
		clamp();
	}

	/** Width available to content, which excludes the scrollbar gutter. */
	public int contentWidth() {
		return width - GUTTER;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int height() {
		return height;
	}

	/** Current scroll offset, rounded to whole pixels. */
	public int offset() {
		return (int) Math.round(offset);
	}

	public int maxScroll() {
		return Math.max(0, contentHeight - height);
	}

	public void reset() {
		offset = 0.0;
		target = 0.0;
	}

	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	public boolean scroll(double mouseX, double mouseY, double amount) {
		if (!contains(mouseX, mouseY) || maxScroll() <= 0) {
			return false;
		}
		target = Mth.clamp(target - amount * SCROLL_STEP, 0.0, maxScroll());
		return true;
	}

	/** Advances the smooth scroll; call once per frame before drawing. */
	public void animate(UiAnim anim, String key) {
		offset = anim.towards(key, (float) target, 18.0F);
	}

	public void begin(GuiGraphicsExtractor context) {
		context.enableScissor(x, y, x + width, y + height);
	}

	/**
	 * Ends the clipped region and draws the edge fades in {@code background}, the color behind the
	 * content, plus the scrollbar when the content is taller than the area.
	 */
	public void end(GuiGraphicsExtractor context, int background, int fadeHeight, int thumbColor) {
		context.disableScissor();
		int fadeWidth = x + width - GUTTER;
		if (offset > 0.5) {
			context.fillGradient(x, y, fadeWidth, y + fadeHeight, background, background & 0x00FFFFFF);
		}
		if (offset < maxScroll() - 0.5) {
			context.fillGradient(x, y + height - fadeHeight, fadeWidth, y + height, background & 0x00FFFFFF, background);
		}
		if (maxScroll() > 0) {
			int trackHeight = height - 8;
			int thumbHeight = Math.max(18, trackHeight * height / Math.max(1, contentHeight));
			int thumbY = y + 4 + (int) Math.round((trackHeight - thumbHeight) * (offset / maxScroll()));
			int thumbX = x + width - GUTTER + (GUTTER - SCROLLBAR_WIDTH) / 2;
			UiShapes.pill(context, thumbX, thumbY, SCROLLBAR_WIDTH, thumbHeight, thumbColor);
		}
	}

	private void clamp() {
		target = Mth.clamp(target, 0.0, maxScroll());
		offset = Mth.clamp(offset, 0.0, maxScroll());
	}
}
