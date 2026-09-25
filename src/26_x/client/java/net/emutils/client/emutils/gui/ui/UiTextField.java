package net.emutils.client.emutils.gui.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.IntPredicate;
import net.emutils.client.versioned.VersionedInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * A single-line text field drawn by its screen, with a movable caret and selection like a normal text
 * box: arrows (Ctrl for words), Home/End, Shift to select, Ctrl+A/C/X/V, and clicking to place the caret.
 */
public final class UiTextField {
	private static final long BLINK_MILLIS = 530L;

	private final Object owner;
	private final int maxLength;
	private final IntPredicate allowed;
	private String text = "";
	private int cursor;
	private int anchor;
	private boolean focused;
	private int scrollX;
	private long lastEditMillis;
	private int lastX;
	private int lastWidth;

	public UiTextField(Object owner, int maxLength) {
		this(owner, maxLength, codepoint -> true);
	}

	/** A field that only accepts typed or pasted characters matching {@code allowed}. */
	public UiTextField(Object owner, int maxLength, IntPredicate allowed) {
		this.owner = owner;
		this.maxLength = maxLength;
		this.allowed = allowed;
	}

	/** Replaces the text and selects all of it, ready to be typed over. */
	public void setText(String value) {
		text = value.length() > maxLength ? value.substring(0, maxLength) : value;
		anchor = 0;
		cursor = text.length();
		scrollX = 0;
	}

	public String text() {
		return text;
	}

	public boolean focused() {
		return focused;
	}

	public void setFocused(boolean focused) {
		if (this.focused != focused) {
			lastEditMillis = Util.getMillis();
		}
		this.focused = focused;
		if (!focused) {
			anchor = cursor;
		}
		// This field is not an EditBox, so it has to ask for text input itself (needed on SDL, 26.3+).
		VersionedInput.setTextInputFocus(owner, focused);
	}

	/** Re-requests text input after the screen was re-initialized while this field kept focus. */
	public void restoreFocus() {
		if (focused) {
			VersionedInput.setTextInputFocus(owner, true);
		}
	}

	/** Returns true when the key was used; {@code changed} runs when the text changed. */
	public boolean keyPressed(KeyEvent input, Runnable changed) {
		if (!focused) {
			return false;
		}
		boolean shift = input.hasShiftDown();
		boolean word = input.hasControlDownWithQuirk();
		if (input.isEscape()) {
			if (text.isEmpty()) {
				setFocused(false);
			} else {
				replace(0, text.length(), "", changed);
			}
			return true;
		}
		if (input.isSelectAll()) {
			anchor = 0;
			cursor = text.length();
			return true;
		}
		if (input.isCopy()) {
			if (hasSelection()) {
				Minecraft.getInstance().keyboardHandler.setClipboard(selectedText());
			}
			return true;
		}
		if (input.isCut()) {
			if (hasSelection()) {
				Minecraft.getInstance().keyboardHandler.setClipboard(selectedText());
				replaceSelection("", changed);
			}
			return true;
		}
		if (input.isPaste()) {
			replaceSelection(Minecraft.getInstance().keyboardHandler.getClipboard(), changed);
			return true;
		}
		if (input.isLeft()) {
			moveTo(hasSelection() && !shift ? Math.min(cursor, anchor) : word ? previousWord(cursor) : cursor - 1, shift);
			return true;
		}
		if (input.isRight()) {
			moveTo(hasSelection() && !shift ? Math.max(cursor, anchor) : word ? nextWord(cursor) : cursor + 1, shift);
			return true;
		}
		if (input.key() == InputConstants.KEY_HOME) {
			moveTo(0, shift);
			return true;
		}
		if (input.key() == InputConstants.KEY_END) {
			moveTo(text.length(), shift);
			return true;
		}
		if (input.key() == InputConstants.KEY_BACKSPACE) {
			if (hasSelection()) {
				replaceSelection("", changed);
			} else if (cursor > 0) {
				// Ctrl+Backspace (Cmd on macOS, like vanilla text fields) removes the previous word.
				replace(word ? previousWord(cursor) : cursor - 1, cursor, "", changed);
			}
			return true;
		}
		if (input.key() == InputConstants.KEY_DELETE) {
			if (hasSelection()) {
				replaceSelection("", changed);
			} else if (cursor < text.length()) {
				replace(cursor, word ? nextWord(cursor) : cursor + 1, "", changed);
			}
			return true;
		}
		return false;
	}

	public boolean charTyped(CharacterEvent input, Runnable changed) {
		if (!focused || !input.isAllowedChatCharacter() || input.codepoint() == '\t') {
			return false;
		}
		replaceSelection(input.codepointAsString(), changed);
		return true;
	}

	/** Places the caret at the clicked position; Shift extends the selection. */
	public void click(Font font, double mouseX, boolean shift) {
		int target = lastWidth <= 0 ? text.length() : indexAt(font, (int) Math.round(mouseX) - lastX + scrollX);
		moveTo(target, shift);
	}

	/**
	 * Draws the text or placeholder vertically centered in the given row, with the selection and a caret
	 * that stays solid while typing and blinks when idle.
	 */
	public void draw(GuiGraphicsExtractor context, Font font, UiTheme theme, int x, int centerY, int width, Component placeholder) {
		lastX = x;
		lastWidth = width;
		int capHeight = UiText.lineHeight(font, UiText.Size.BODY);
		if (text.isEmpty()) {
			UiText.drawCentered(context, font, UiText.ellipsize(font, placeholder, UiText.Size.BODY, width), UiText.Size.BODY, x, centerY, theme.muted());
		}

		int caretOffset = widthOf(font, text.substring(0, cursor));
		if (caretOffset - scrollX > width - 2) {
			scrollX = caretOffset - width + 2;
		} else if (caretOffset - scrollX < 0) {
			scrollX = caretOffset;
		}
		scrollX = Math.max(0, Math.min(scrollX, Math.max(0, widthOf(font, text) - width + 2)));

		context.enableScissor(x - 1, centerY - capHeight - 3, x + width + 1, centerY + capHeight + 3);
		if (hasSelection()) {
			int start = x - scrollX + widthOf(font, text.substring(0, Math.min(cursor, anchor)));
			int end = x - scrollX + widthOf(font, text.substring(0, Math.max(cursor, anchor)));
			context.fill(start, centerY - capHeight / 2 - 2, end, centerY + capHeight / 2 + 3, UiOpacity.apply(UiTheme.fade(theme.accent(), 0.45F)));
		}
		if (!text.isEmpty()) {
			UiText.drawCentered(context, font, Component.literal(text), UiText.Size.BODY, x - scrollX, centerY, theme.text());
		}
		long sinceEdit = Util.getMillis() - lastEditMillis;
		if (focused && (sinceEdit < BLINK_MILLIS || (sinceEdit / BLINK_MILLIS) % 2L == 0L)) {
			int caretX = x - scrollX + caretOffset;
			context.fill(caretX, centerY - capHeight / 2 - 2, caretX + 1, centerY + capHeight / 2 + 3, UiOpacity.apply(theme.text()));
		}
		context.disableScissor();
	}

	private boolean hasSelection() {
		return cursor != anchor;
	}

	private String selectedText() {
		return text.substring(Math.min(cursor, anchor), Math.max(cursor, anchor));
	}

	private void moveTo(int index, boolean extendSelection) {
		cursor = Math.clamp(index, 0, text.length());
		if (!extendSelection) {
			anchor = cursor;
		}
		lastEditMillis = Util.getMillis();
	}

	private void replaceSelection(String value, Runnable changed) {
		replace(Math.min(cursor, anchor), Math.max(cursor, anchor), value, changed);
	}

	private void replace(int start, int end, String value, Runnable changed) {
		StringBuilder filtered = new StringBuilder();
		value.replace('\n', ' ').replace('\r', ' ').codePoints().filter(allowed).forEach(filtered::appendCodePoint);
		String inserted = filtered.toString();
		int room = maxLength - (text.length() - (end - start));
		if (inserted.length() > room) {
			inserted = inserted.substring(0, Math.max(0, room));
		}
		String next = text.substring(0, start) + inserted + text.substring(end);
		cursor = start + inserted.length();
		anchor = cursor;
		lastEditMillis = Util.getMillis();
		if (!next.equals(text)) {
			text = next;
			changed.run();
		}
	}

	private int indexAt(Font font, int offset) {
		for (int i = 0; i < text.length(); i++) {
			int middle = (widthOf(font, text.substring(0, i)) + widthOf(font, text.substring(0, i + 1))) / 2;
			if (offset < middle) {
				return i;
			}
		}
		return text.length();
	}

	private static int widthOf(Font font, String value) {
		return value.isEmpty() ? 0 : UiText.width(font, Component.literal(value), UiText.Size.BODY);
	}

	/** Start of the word before {@code index}, matching vanilla EditBox: spaces before it go with it. */
	private int previousWord(int index) {
		int i = index;
		while (i > 0 && text.charAt(i - 1) == ' ') {
			i--;
		}
		while (i > 0 && text.charAt(i - 1) != ' ') {
			i--;
		}
		return i;
	}

	/** End of the word after {@code index}, including the spaces that follow it. */
	private int nextWord(int index) {
		int i = index;
		while (i < text.length() && text.charAt(i) != ' ') {
			i++;
		}
		while (i < text.length() && text.charAt(i) == ' ') {
			i++;
		}
		return i;
	}
}
