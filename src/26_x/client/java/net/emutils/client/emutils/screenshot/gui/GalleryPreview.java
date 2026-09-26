package net.emutils.client.emutils.screenshot.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiSheetFrame;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.screenshot.ScreenshotRepository.ScreenshotEntry;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * A screenshot shown large in a sheet over the gallery (#106), with its name and date, the same actions
 * as the grid, and previous/next buttons. The arrow keys and the mouse wheel step through the
 * screenshots, Delete deletes, and Esc or clicking outside closes it.
 */
final class GalleryPreview {
	private static final int PADDING = 16;
	private static final int HEADER = 28;
	private static final int BUTTON = 22;
	private static final int BUTTON_GAP = 4;
	private static final int MAX_WIDTH = 960;

	private final Font font;
	private final GalleryScreen gallery;
	private final UiSheetFrame frame;
	private int index;
	private int x;
	private int y;
	private int width;
	private int height;
	private int imageWidth;
	private int imageHeight;
	private int footerY;
	private int closeX;
	private int actionsX;

	GalleryPreview(Font font, UiAnim anim, GalleryScreen gallery, int index) {
		this.font = font;
		this.gallery = gallery;
		this.index = index;
		this.frame = new UiSheetFrame(anim, "gallery-preview:" + System.identityHashCode(this), 16);
	}

	boolean isClosed() {
		return frame.isClosed();
	}

	void close() {
		frame.close();
	}

	/** Keeps the shown screenshot valid after the list changed, for example after a delete. */
	void screenshotsChanged() {
		List<ScreenshotEntry> screenshots = gallery.screenshots();
		if (screenshots.isEmpty()) {
			frame.close();
			return;
		}
		index = Math.min(index, screenshots.size() - 1);
	}

	/** The screenshot shown now. */
	Path shown() {
		List<ScreenshotEntry> screenshots = gallery.screenshots();
		return screenshots.get(Math.min(index, screenshots.size() - 1)).path();
	}

	/** Keeps showing {@code path} after screenshots were added before it; falls back like a delete if it's gone. */
	void keepShowing(Path path) {
		List<ScreenshotEntry> screenshots = gallery.screenshots();
		for (int i = 0; i < screenshots.size(); i++) {
			if (screenshots.get(i).path().equals(path)) {
				index = i;
				return;
			}
		}
		screenshotsChanged();
	}

	private void step(int delta) {
		int count = gallery.screenshots().size();
		if (count > 0) {
			index = Math.clamp(index + delta, 0, count - 1);
		}
	}

	private void layout(int screenWidth, int screenHeight) {
		int chrome = PADDING + HEADER + 10 + 12 + BUTTON + PADDING;
		width = Math.min(MAX_WIDTH, screenWidth - 60);
		imageWidth = width - PADDING * 2;
		imageHeight = imageWidth * 9 / 16;
		if (chrome + imageHeight > screenHeight - 40) {
			imageHeight = Math.max(60, screenHeight - 40 - chrome);
			imageWidth = imageHeight * 16 / 9;
			width = imageWidth + PADDING * 2;
		}
		height = chrome + imageHeight;
		x = (screenWidth - width) / 2;
		y = (screenHeight - height) / 2;
		footerY = y + height - PADDING - BUTTON;
	}

	void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		List<ScreenshotEntry> screenshots = gallery.screenshots();
		if (screenshots.isEmpty()) {
			frame.close();
		}
		layout(screenWidth, screenHeight);
		frame.firstFrame();
		if (!frame.begin(context, theme, screenWidth, screenHeight, x, y, width, height)) {
			return;
		}
		if (frame.closing()) {
			mouseX = Integer.MIN_VALUE / 2;
		}
		Component tooltip = null;
		if (!screenshots.isEmpty()) {
			ScreenshotEntry screenshot = screenshots.get(Math.min(index, screenshots.size() - 1));
			int left = x + PADDING;
			int right = x + width - PADDING;

			closeX = right - BUTTON;
			boolean closeHovered = contains(mouseX, mouseY, closeX, y + PADDING, BUTTON, BUTTON);
			UiWidgets.ghostIconButton(context, theme, closeX, y + PADDING, BUTTON, HubIcons.X, theme.textSecondary(), closeHovered ? 1.0F : 0.0F);
			if (closeHovered) {
				tooltip = Component.translatable(EMUtilsTexts.UI_CLOSE);
			}
			Component name = UiText.ellipsize(font, Component.literal(screenshot.filename()), UiText.Size.BOLD, closeX - 10 - left);
			UiText.draw(context, font, name, UiText.Size.BOLD, left, y + PADDING + 2, theme.text());
			Component date = Component.literal(GalleryScreen.DATE_FORMAT.format(Instant.ofEpochMilli(screenshot.modifiedMillis()).atZone(ZoneId.systemDefault())));
			UiText.draw(context, font, date, UiText.Size.BODY, left, y + PADDING + 2 + UiText.lineHeight(font, UiText.Size.BOLD) + 6, theme.muted());

			gallery.drawScreenshot(context, theme, screenshot, left, y + PADDING + HEADER + 10, imageWidth, imageHeight);

			// Footer: previous, the position, next on the left; the actions on the right.
			boolean prevHovered = index > 0 && contains(mouseX, mouseY, left, footerY, BUTTON, BUTTON);
			boolean nextHovered = index < screenshots.size() - 1 && contains(mouseX, mouseY, left + BUTTON + BUTTON_GAP, footerY, BUTTON, BUTTON);
			UiWidgets.ghostIconButton(context, theme, left, footerY, BUTTON, HubIcons.CHEVRON_LEFT, index > 0 ? theme.textSecondary() : UiTheme.fade(theme.muted(), 0.4F), prevHovered ? 1.0F : 0.0F);
			UiWidgets.ghostIconButton(context, theme, left + BUTTON + BUTTON_GAP, footerY, BUTTON, HubIcons.CHEVRON_RIGHT, index < screenshots.size() - 1 ? theme.textSecondary() : UiTheme.fade(theme.muted(), 0.4F), nextHovered ? 1.0F : 0.0F);
			if (prevHovered) {
				tooltip = Component.translatable(EMUtilsTexts.UI_PREVIOUS);
			} else if (nextHovered) {
				tooltip = Component.translatable(EMUtilsTexts.UI_NEXT);
			}
			Component position = Component.translatable(EMUtilsTexts.UI_POSITION_OF, index + 1, screenshots.size());
			UiText.drawCentered(context, font, position, UiText.Size.LABEL, left + BUTTON * 2 + BUTTON_GAP + 10, footerY + BUTTON / 2, theme.muted());

			int count = GalleryScreen.ACTION_ICONS.length;
			actionsX = right - count * BUTTON - (count - 1) * BUTTON_GAP;
			for (int i = 0; i < count; i++) {
				int actionX = actionsX + i * (BUTTON + BUTTON_GAP);
				boolean hovered = contains(mouseX, mouseY, actionX, footerY, BUTTON, BUTTON);
				int color = i == 3 && hovered ? theme.warning() : theme.textSecondary();
				UiWidgets.ghostIconButton(context, theme, actionX, footerY, BUTTON, GalleryScreen.ACTION_ICONS[i], color, hovered ? 1.0F : 0.0F);
				if (hovered) {
					tooltip = Component.translatable(GalleryScreen.ACTION_TIPS[i]);
				}
			}
		}
		frame.endBody(context);
		if (tooltip != null) {
			UiWidgets.tooltip(context, font, theme, tooltip, mouseX, mouseY, screenWidth, screenHeight);
		}
		frame.end();
	}

	// ---- input ----------------------------------------------------------------------------------

	void mouseClicked(double mouseX, double mouseY) {
		if (frame.closing()) {
			return;
		}
		int left = x + PADDING;
		if (!contains(mouseX, mouseY, x, y, width, height) || contains(mouseX, mouseY, closeX, y + PADDING, BUTTON, BUTTON)) {
			frame.close();
		} else if (contains(mouseX, mouseY, left, footerY, BUTTON, BUTTON)) {
			step(-1);
		} else if (contains(mouseX, mouseY, left + BUTTON + BUTTON_GAP, footerY, BUTTON, BUTTON)) {
			step(1);
		} else {
			for (int i = 0; i < GalleryScreen.ACTION_ICONS.length; i++) {
				if (contains(mouseX, mouseY, actionsX + i * (BUTTON + BUTTON_GAP), footerY, BUTTON, BUTTON)) {
					gallery.runAction(index, i);
				}
			}
		}
	}

	void scrolled(double amount) {
		if (!frame.closing() && amount != 0.0) {
			step(amount > 0.0 ? -1 : 1);
		}
	}

	void keyPressed(KeyEvent input) {
		if (frame.closing()) {
			return;
		}
		if (input.isEscape()) {
			frame.close();
		} else if (input.key() == InputConstants.KEY_LEFT) {
			step(-1);
		} else if (input.key() == InputConstants.KEY_RIGHT) {
			step(1);
		} else if (input.key() == InputConstants.KEY_DELETE) {
			gallery.runAction(index, 3);
		}
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
