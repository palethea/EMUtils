package net.emutils.client.emutils.gui.ui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * A right-click menu: a short list of actions at the mouse, kept on screen. Clicking an item runs it,
 * clicking anywhere else or pressing Esc closes the menu. It fades in quickly and closes at once, so
 * the click that closes it can do its own thing right away.
 */
public final class UiContextMenu {
	private static final int ITEM_HEIGHT = 20;
	private static final int PADDING = 4;
	private static final int MIN_WIDTH = 120;

	/** One action; a {@code danger} item (such as delete) is drawn in the warning color. */
	public record Item(Component label, boolean enabled, boolean danger, Runnable action) {
		public static Item of(Component label, Runnable action) {
			return new Item(label, true, false, action);
		}
	}

	private final Font font;
	private final UiAnim anim;
	private final List<Item> items;
	private final String key = "menu:" + System.identityHashCode(this);
	private final int anchorX;
	private final int anchorY;
	private int x;
	private int y;
	private int width;
	private int height;
	private boolean closed;

	public UiContextMenu(Font font, UiAnim anim, int mouseX, int mouseY, List<Item> items) {
		this.font = font;
		this.anim = anim;
		this.items = List.copyOf(items);
		this.anchorX = mouseX;
		this.anchorY = mouseY;
		anim.set(key, 0.0F);
	}

	public boolean isClosed() {
		return closed;
	}

	public void close() {
		closed = true;
	}

	public void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		if (closed) {
			return;
		}
		width = MIN_WIDTH;
		for (Item item : items) {
			width = Math.max(width, UiText.width(font, item.label(), UiText.Size.BODY) + 24);
		}
		height = items.size() * ITEM_HEIGHT + PADDING * 2;
		// Opens down and right from the mouse, flipping when it would leave the screen.
		x = anchorX + width + 4 > screenWidth ? Math.max(4, anchorX - width) : anchorX;
		y = anchorY + height + 4 > screenHeight ? Math.max(4, anchorY - height) : anchorY;

		float shown = anim.towards(key, 1.0F, 22.0F);
		UiOpacity.set(shown);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, (1.0F - shown) * -3.0F);
		UiShapes.shadow(context, x, y, width, height, 8, 10, theme.shadow());
		UiShapes.borderedRect(context, x, y, width, height, 8, theme.surface(), theme.line());
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			int top = y + PADDING + i * ITEM_HEIGHT;
			boolean hovered = item.enabled() && contains(mouseX, mouseY, x, top, width, ITEM_HEIGHT);
			if (hovered) {
				UiShapes.roundedRect(context, x + PADDING, top, width - PADDING * 2, ITEM_HEIGHT, 5, item.danger() ? UiTheme.fade(theme.warning(), 0.16F) : theme.hover());
			}
			int color = !item.enabled() ? UiTheme.fade(theme.muted(), 0.7F) : item.danger() ? theme.warning() : theme.text();
			UiText.drawCentered(context, font, item.label(), UiText.Size.BODY, x + 12, top + ITEM_HEIGHT / 2, color);
		}
		context.pose().popMatrix();
		UiOpacity.reset();
	}

	/** Handles a click anywhere while the menu is open: runs the clicked item, and always closes. */
	public void mouseClicked(double mouseX, double mouseY) {
		if (closed) {
			return;
		}
		closed = true;
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			if (item.enabled() && contains(mouseX, mouseY, x, y + PADDING + i * ITEM_HEIGHT, width, ITEM_HEIGHT)) {
				item.action().run();
				return;
			}
		}
	}

	/** Whether the click lands on the menu itself, so the screen doesn't also act on it. */
	public boolean contains(double mouseX, double mouseY) {
		return contains(mouseX, mouseY, x, y, width, height);
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
