package net.emutils.client.emutils.gui.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * A vertically scrolling region. Space for the scrollbar is always reserved so content never shifts
 * when it appears, and content fades out at the top and bottom edges instead of being cut off. The
 * scrollbar thumb can be dragged, and clicking its track jumps there.
 */
public final class UiScrollArea {
	public static final int GUTTER = 6;
	private static final int SCROLLBAR_WIDTH = 3;
	private static final int SCROLLBAR_HOVER_WIDTH = 5;
	private static final double SCROLL_STEP = 24.0;

	private int x;
	private int y;
	private int width;
	private int height;
	private int contentHeight;
	private double offset;
	private double target;
	private @Nullable UiAnim anim;
	private String key = "";
	private float hover;
	private boolean dragging;
	private double dragStartMouse;
	private double dragStartOffset;

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

	/** Current scroll offset with its fraction, for smooth drawing. */
	public float exactOffset() {
		return (float) offset;
	}

	public int maxScroll() {
		return Math.max(0, contentHeight - height);
	}

	public void reset() {
		offset = 0.0;
		target = 0.0;
	}

	/** Where the scroll is heading, which the smooth offset follows. */
	public double target() {
		return target;
	}

	/** Scrolls smoothly to {@code target}, for example to keep a caret in view. */
	public void scrollTo(double target) {
		this.target = Mth.clamp(target, 0.0, maxScroll());
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

	/** Advances the smooth scroll and the scrollbar's hover; call once per frame before drawing. */
	public void animate(UiAnim anim, String key, int mouseX, int mouseY) {
		this.anim = anim;
		this.key = key;
		offset = anim.towards(key, (float) target, 18.0F);
		hover = anim.towards(key + ":bar", dragging || onScrollbar(mouseX, mouseY), 16.0F);
	}

	/** Whether the point is over the scrollbar's track, which is a little wider than the gutter to be easy to hit. */
	private boolean onScrollbar(double mouseX, double mouseY) {
		return maxScroll() > 0 && mouseX >= x + width - GUTTER - 2 && mouseX < x + width + 2 && mouseY >= y && mouseY < y + height;
	}

	/** Starts dragging the thumb; a click on the track first moves the thumb under the mouse. */
	public boolean mouseClicked(double mouseX, double mouseY) {
		if (!onScrollbar(mouseX, mouseY)) {
			return false;
		}
		int thumbY = thumbY();
		int thumbHeight = thumbHeight();
		if (mouseY < thumbY || mouseY >= thumbY + thumbHeight) {
			target = Mth.clamp((mouseY - thumbHeight / 2.0 - (y + 4)) / thumbTravel() * maxScroll(), 0.0, maxScroll());
		}
		dragging = true;
		dragStartMouse = mouseY;
		dragStartOffset = target;
		return true;
	}

	/** Moves the content with the dragged thumb, without easing, so it follows the mouse exactly. */
	public boolean mouseDragged(double mouseY) {
		if (!dragging) {
			return false;
		}
		target = Mth.clamp(dragStartOffset + (mouseY - dragStartMouse) / thumbTravel() * maxScroll(), 0.0, maxScroll());
		offset = target;
		if (anim != null) {
			anim.set(key, (float) target);
		}
		return true;
	}

	public boolean mouseReleased() {
		boolean wasDragging = dragging;
		dragging = false;
		return wasDragging;
	}

	public boolean dragging() {
		return dragging;
	}

	private int trackHeight() {
		return height - 8;
	}

	private int thumbHeight() {
		return Math.max(18, trackHeight() * height / Math.max(1, contentHeight));
	}

	private double thumbTravel() {
		return Math.max(1, trackHeight() - thumbHeight());
	}

	private int thumbY() {
		return y + 4 + (int) Math.round(thumbTravel() * (offset / Math.max(1, maxScroll())));
	}

	public void begin(GuiGraphicsExtractor context) {
		context.enableScissor(x, y, x + width, y + height);
	}

	/**
	 * Ends the clipped region and draws the edge fades in {@code background}, the color behind the
	 * content, plus the scrollbar when the content is taller than the area. The thumb blends from
	 * {@code thumbColor} to {@code thumbHoverColor} and widens while hovered or dragged.
	 */
	public void end(GuiGraphicsExtractor context, int background, int fadeHeight, int thumbColor, int thumbHoverColor) {
		context.disableScissor();
		int fadeWidth = x + width - GUTTER;
		if (offset > 0.5) {
			context.fillGradient(x, y, fadeWidth, y + fadeHeight, UiOpacity.apply(background), background & 0x00FFFFFF);
		}
		if (offset < maxScroll() - 0.5) {
			context.fillGradient(x, y + height - fadeHeight, fadeWidth, y + height, background & 0x00FFFFFF, UiOpacity.apply(background));
		}
		if (maxScroll() > 0) {
			// Both widths are odd, so the thumb stays centered in the gutter when it widens.
			int thumbWidth = hover > 0.5F ? SCROLLBAR_HOVER_WIDTH : SCROLLBAR_WIDTH;
			int thumbX = x + width - GUTTER + (GUTTER - thumbWidth + 1) / 2;
			UiShapes.pill(context, thumbX, thumbY(), thumbWidth, thumbHeight(), UiTheme.mix(thumbColor, thumbHoverColor, hover));
		}
	}

	private void clamp() {
		target = Mth.clamp(target, 0.0, maxScroll());
		offset = Mth.clamp(offset, 0.0, maxScroll());
	}
}
