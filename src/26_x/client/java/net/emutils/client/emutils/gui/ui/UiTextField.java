package net.emutils.client.emutils.gui.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.emutils.client.versioned.VersionedInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** A single-line text field drawn by its screen; the caret is always at the end. */
public final class UiTextField {
	private final Object owner;
	private final int maxLength;
	private String text = "";
	private boolean focused;

	public UiTextField(Object owner, int maxLength) {
		this.owner = owner;
		this.maxLength = maxLength;
	}

	public String text() {
		return text;
	}

	public boolean focused() {
		return focused;
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
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
		if (input.isEscape()) {
			if (text.isEmpty()) {
				setFocused(false);
			} else {
				set("", changed);
			}
			return true;
		}
		if (input.isPaste()) {
			set(text + Minecraft.getInstance().keyboardHandler.getClipboard(), changed);
			return true;
		}
		if (input.key() == InputConstants.KEY_BACKSPACE) {
			if (!text.isEmpty()) {
				// Ctrl+Backspace (Cmd on macOS, like vanilla text fields) removes the previous word.
				set(text.substring(0, input.hasControlDownWithQuirk() ? previousWordStart(text) : text.length() - 1), changed);
			}
			return true;
		}
		if (input.key() == InputConstants.KEY_DELETE) {
			set("", changed);
			return true;
		}
		return false;
	}

	public boolean charTyped(CharacterEvent input, Runnable changed) {
		if (!focused || !input.isAllowedChatCharacter() || input.codepoint() == '\t') {
			return false;
		}
		set(text + input.codepointAsString(), changed);
		return true;
	}

	private void set(String value, Runnable changed) {
		String clamped = value.replace('\n', ' ');
		if (clamped.length() > maxLength) {
			clamped = clamped.substring(0, maxLength);
		}
		if (!clamped.equals(text)) {
			text = clamped;
			changed.run();
		}
	}

	/** Draws the text or placeholder, vertically centered in the given row, with a blinking caret. */
	public void draw(GuiGraphicsExtractor context, Font font, UiTheme theme, int x, int centerY, int maxWidth, Component placeholder) {
		if (text.isEmpty()) {
			UiText.drawCentered(context, font, UiText.ellipsize(font, placeholder, UiText.Size.BODY, maxWidth), UiText.Size.BODY, x, centerY, theme.placeholder());
		} else {
			Component value = Component.literal(visibleTail(font, maxWidth - 4));
			UiText.drawCentered(context, font, value, UiText.Size.BODY, x, centerY, theme.text());
		}
		if (focused && (Util.getMillis() / 500L) % 2L == 0L) {
			int caretX = x + (text.isEmpty() ? 0 : UiText.width(font, Component.literal(visibleTail(font, maxWidth - 4)), UiText.Size.BODY) + 1);
			int lineHeight = UiText.lineHeight(font, UiText.Size.BODY);
			context.fill(caretX, centerY - lineHeight / 2 - 1, caretX + 1, centerY + lineHeight / 2 + 1, theme.text());
		}
	}

	/** The end of the text that fits in {@code maxWidth}, so the caret stays visible while typing. */
	private String visibleTail(Font font, int maxWidth) {
		int start = 0;
		while (start < text.length() && UiText.width(font, Component.literal(text.substring(start)), UiText.Size.BODY) > maxWidth) {
			start++;
		}
		return text.substring(start);
	}

	/** Start of the word before the end of {@code value}, matching vanilla EditBox: trailing spaces go with the word. */
	private static int previousWordStart(String value) {
		int index = value.length();
		while (index > 0 && value.charAt(index - 1) == ' ') {
			index--;
		}
		while (index > 0 && value.charAt(index - 1) != ' ') {
			index--;
		}
		return index;
	}
}
