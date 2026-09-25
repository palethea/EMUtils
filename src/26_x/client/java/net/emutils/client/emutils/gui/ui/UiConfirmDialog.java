package net.emutils.client.emutils.gui.ui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * A small dialog that asks before doing something that can't be undone, such as deleting. Enter
 * confirms, Esc and Cancel back out, and clicking outside it does too.
 */
public final class UiConfirmDialog {
	private static final int WIDTH = 260;
	private static final int PADDING = 16;
	private static final int BUTTON_HEIGHT = 20;
	private static final int LINE_HEIGHT = 11;

	private final Font font;
	private final Component title;
	private final Component message;
	private final Component confirmLabel;
	private final Runnable onConfirm;
	private final UiSheetFrame frame;
	private int x;
	private int y;
	private int height;
	private int confirmX;
	private int confirmWidth;
	private int cancelX;
	private int cancelWidth;
	private int buttonsY;

	public UiConfirmDialog(Font font, UiAnim anim, Component title, Component message, Component confirmLabel, Runnable onConfirm) {
		this.font = font;
		this.title = title;
		this.message = message;
		this.confirmLabel = confirmLabel;
		this.onConfirm = onConfirm;
		this.frame = new UiSheetFrame(anim, "confirm:" + System.identityHashCode(this), 14);
	}

	public boolean isClosed() {
		return frame.isClosed();
	}

	/** Whether the dialog is animating out; it no longer takes keys then. */
	public boolean closing() {
		return frame.closing();
	}

	public void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		List<Component> lines = UiText.wrap(font, message, UiText.Size.BODY, WIDTH - PADDING * 2);
		int titleHeight = UiText.lineHeight(font, UiText.Size.BOLD);
		height = PADDING + titleHeight + 10 + lines.size() * LINE_HEIGHT + 14 + BUTTON_HEIGHT + PADDING;
		x = (screenWidth - WIDTH) / 2;
		y = (screenHeight - height) / 2;
		if (frame.firstFrame()) {
			UiText.prepare(title, UiText.Size.BOLD);
			for (Component line : lines) {
				UiText.prepare(line, UiText.Size.BODY);
			}
		}
		if (!frame.begin(context, theme, screenWidth, screenHeight, x, y, WIDTH, height)) {
			return;
		}
		UiText.draw(context, font, title, UiText.Size.BOLD, x + PADDING, y + PADDING, theme.text());
		int textTop = y + PADDING + titleHeight + 10;
		for (int i = 0; i < lines.size(); i++) {
			UiText.draw(context, font, lines.get(i), UiText.Size.BODY, x + PADDING, textTop + i * LINE_HEIGHT, theme.textSecondary());
		}
		buttonsY = y + height - PADDING - BUTTON_HEIGHT;
		confirmWidth = UiWidgets.buttonWidth(font, confirmLabel) + 8;
		confirmX = x + WIDTH - PADDING - confirmWidth;
		cancelWidth = UiWidgets.buttonWidth(font, CommonComponents.GUI_CANCEL) + 4;
		cancelX = confirmX - 6 - cancelWidth;
		UiWidgets.button(context, font, theme, cancelX, buttonsY, cancelWidth, BUTTON_HEIGHT, CommonComponents.GUI_CANCEL, UiWidgets.ButtonStyle.GHOST, hover(mouseX, mouseY, cancelX, cancelWidth));
		UiWidgets.button(context, font, theme, confirmX, buttonsY, confirmWidth, BUTTON_HEIGHT, confirmLabel, UiWidgets.ButtonStyle.DANGER, hover(mouseX, mouseY, confirmX, confirmWidth));
		frame.endBody(context);
		frame.end();
	}

	private float hover(int mouseX, int mouseY, int buttonX, int buttonWidth) {
		return !frame.closing() && contains(mouseX, mouseY, buttonX, buttonsY, buttonWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F;
	}

	/** Handles a left click; the dialog takes every click while it is open. */
	public void mouseClicked(double mouseX, double mouseY) {
		if (frame.closing()) {
			return;
		}
		if (contains(mouseX, mouseY, confirmX, buttonsY, confirmWidth, BUTTON_HEIGHT)) {
			confirm();
		} else if (contains(mouseX, mouseY, cancelX, buttonsY, cancelWidth, BUTTON_HEIGHT) || !contains(mouseX, mouseY, x, y, WIDTH, height)) {
			frame.close();
		}
	}

	/** Enter confirms and Esc cancels; the dialog takes every key while it is open. */
	public void keyPressed(KeyEvent input) {
		if (frame.closing()) {
			return;
		}
		if (input.isConfirmation()) {
			confirm();
		} else if (input.isEscape()) {
			frame.close();
		}
	}

	private void confirm() {
		onConfirm.run();
		frame.close();
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
