package net.emutils.client.emutils.gui.ui;

import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A small dialog that asks for one line of text, such as a new script's name. Enter or the confirm
 * button submits; if {@code onConfirm} returns a message, it shows under the field and the dialog
 * stays open. Esc, Cancel and clicking outside back out.
 */
public final class UiPromptDialog {
	private static final int WIDTH = 300;
	private static final int PADDING = 16;
	private static final int BUTTON_HEIGHT = 20;
	private static final int FIELD_HEIGHT = 22;
	private static final int LINE_HEIGHT = 11;

	private final Font font;
	private final Component title;
	private final Component message;
	private final Component placeholder;
	private final Component confirmLabel;
	private final Function<String, @Nullable Component> onConfirm;
	private final UiSheetFrame frame;
	private final UiTextField field;
	private @Nullable Component error;
	private int x;
	private int y;
	private int height;
	private int fieldY;
	private int confirmX;
	private int confirmWidth;
	private int cancelX;
	private int cancelWidth;
	private int buttonsY;

	public UiPromptDialog(
		Font font,
		UiAnim anim,
		Component title,
		Component message,
		String initialText,
		Component placeholder,
		Component confirmLabel,
		Function<String, @Nullable Component> onConfirm
	) {
		this.font = font;
		this.title = title;
		this.message = message;
		this.placeholder = placeholder;
		this.confirmLabel = confirmLabel;
		this.onConfirm = onConfirm;
		this.frame = new UiSheetFrame(anim, "prompt:" + System.identityHashCode(this), 14);
		this.field = new UiTextField(this, 128);
		field.setText(initialText);
		field.setFocused(true);
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
		int errorHeight = error == null ? 0 : 8 + LINE_HEIGHT;
		height = PADDING + titleHeight + 10 + lines.size() * LINE_HEIGHT + 10 + FIELD_HEIGHT + errorHeight + 16 + BUTTON_HEIGHT + PADDING;
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
		fieldY = textTop + lines.size() * LINE_HEIGHT + 10;
		int fieldWidth = WIDTH - PADDING * 2;
		UiShapes.borderedRect(context, x + PADDING, fieldY, fieldWidth, FIELD_HEIGHT, 8, theme.surface(), error != null ? theme.warning() : field.focused() ? theme.accent() : theme.line());
		field.draw(context, font, theme, x + PADDING + 8, fieldY + FIELD_HEIGHT / 2, fieldWidth - 16, placeholder);
		if (error != null) {
			UiText.draw(context, font, UiText.ellipsize(font, error, UiText.Size.BODY, fieldWidth), UiText.Size.BODY, x + PADDING, fieldY + FIELD_HEIGHT + 8, theme.warning());
		}
		buttonsY = y + height - PADDING - BUTTON_HEIGHT;
		confirmWidth = UiWidgets.buttonWidth(font, confirmLabel) + 8;
		confirmX = x + WIDTH - PADDING - confirmWidth;
		cancelWidth = UiWidgets.buttonWidth(font, CommonComponents.GUI_CANCEL) + 4;
		cancelX = confirmX - 6 - cancelWidth;
		UiWidgets.button(context, font, theme, cancelX, buttonsY, cancelWidth, BUTTON_HEIGHT, CommonComponents.GUI_CANCEL, UiWidgets.ButtonStyle.GHOST, hover(mouseX, mouseY, cancelX, cancelWidth));
		UiWidgets.button(context, font, theme, confirmX, buttonsY, confirmWidth, BUTTON_HEIGHT, confirmLabel, UiWidgets.ButtonStyle.PRIMARY, hover(mouseX, mouseY, confirmX, confirmWidth));
		frame.endBody(context);
		frame.end();
	}

	private float hover(int mouseX, int mouseY, int buttonX, int buttonWidth) {
		return !frame.closing() && contains(mouseX, mouseY, buttonX, buttonsY, buttonWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F;
	}

	/** Handles a left click; the dialog takes every click while it is open. */
	public void mouseClicked(double mouseX, double mouseY, boolean shift) {
		if (frame.closing()) {
			return;
		}
		if (contains(mouseX, mouseY, x + PADDING, fieldY, WIDTH - PADDING * 2, FIELD_HEIGHT)) {
			field.setFocused(true);
			field.click(font, mouseX, shift);
		} else if (contains(mouseX, mouseY, confirmX, buttonsY, confirmWidth, BUTTON_HEIGHT)) {
			confirm();
		} else if (contains(mouseX, mouseY, cancelX, buttonsY, cancelWidth, BUTTON_HEIGHT) || !contains(mouseX, mouseY, x, y, WIDTH, height)) {
			close();
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
			close();
		} else {
			field.keyPressed(input, () -> error = null);
		}
	}

	public void charTyped(CharacterEvent input) {
		if (!frame.closing()) {
			field.charTyped(input, () -> error = null);
		}
	}

	private void confirm() {
		error = onConfirm.apply(field.text());
		if (error == null) {
			close();
		}
	}

	private void close() {
		field.setFocused(false);
		frame.close();
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
