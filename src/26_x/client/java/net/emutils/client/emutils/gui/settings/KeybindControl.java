package net.emutils.client.emutils.gui.settings;

import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * A keybind's keycap with the × that unbinds it (#155), shared by the settings sheets' Keybinds sections
 * and the Keybinds page. The × shows while the row is hovered and the key is bound; room for it is always
 * kept, so the key name doesn't shift when it appears.
 */
final class KeybindControl {
	static final int CLEAR = UiWidgets.KEYCAP_HEIGHT;
	private static final int CLEAR_GAP = 3;

	private KeybindControl() {
	}

	/**
	 * Draws the keycap ending at {@code right}, centered on {@code centerY}, and the × left of it.
	 * Returns where they are, for clicks.
	 */
	static Layout draw(GuiGraphicsExtractor context, Font font, UiTheme theme, UiAnim anim, KeybindCapture capture, KeyMapping key, int right, int centerY, boolean rowHovered, int mouseX, int mouseY) {
		Component cap = capture.label(key);
		int capWidth = UiWidgets.keycapWidth(font, cap);
		int capX = right - capWidth;
		int capY = centerY - UiWidgets.KEYCAP_HEIGHT / 2;
		boolean listening = capture.isListening(key);
		UiWidgets.keycap(context, font, theme, capX, capY, cap, listening, KeybindCapture.clashes(key), rowHovered ? 1.0F : 0.0F);

		int clearX = capX - CLEAR_GAP - CLEAR;
		boolean clearable = clearable(capture, key);
		float shown = anim.towards("key-clear:" + key.getName(), clearable && rowHovered, 16.0F);
		if (shown > 0.01F) {
			boolean clearHovered = clearable && contains(mouseX, mouseY, clearX, capY, CLEAR, CLEAR);
			int color = UiTheme.fade(clearHovered ? theme.warning() : theme.textSecondary(), shown);
			UiWidgets.ghostIconButton(context, theme, clearX, capY, CLEAR, HubIcons.X, color, clearHovered ? shown : 0.0F);
		}
		return new Layout(capX, capY, clearX);
	}

	/**
	 * The key name, or while the key waits for input, how to cancel, clear or reset it; the two fade
	 * into each other.
	 */
	static void drawName(GuiGraphicsExtractor context, Font font, UiTheme theme, UiAnim anim, KeybindCapture capture, KeyMapping key, Component name, int x, int centerY, int maxWidth) {
		float listening = anim.transition("key-hint:" + key.getName(), capture.isListening(key), 0.15F);
		if (listening < 0.99F) {
			UiText.drawCentered(context, font, UiText.ellipsize(font, name, UiText.Size.BOLD, maxWidth), UiText.Size.BOLD, x, centerY, UiTheme.fade(theme.text(), 1.0F - listening));
		}
		if (listening > 0.01F) {
			Component hint = Component.translatable(EMUtilsTexts.UI_KEYBIND_HINT);
			UiText.drawCentered(context, font, UiText.ellipsize(font, hint, UiText.Size.SMALL, maxWidth), UiText.Size.SMALL, x, centerY, UiTheme.fade(theme.textSecondary(), listening));
		}
	}

	/** The widest {@link #draw} can get for this key, to keep the name clear of it. */
	static int width(Font font, KeybindCapture capture, KeyMapping key) {
		return UiWidgets.keycapWidth(font, capture.label(key)) + CLEAR_GAP + CLEAR;
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	/** Whether the × is there for this key: it is bound and not waiting for a new key. */
	static boolean clearable(KeybindCapture capture, KeyMapping key) {
		return !key.isUnbound() && !capture.isListening(key);
	}

	record Layout(int capX, int capY, int clearX) {
		/**
		 * Whether a click here hits the × of {@code key}, when it has one; it counts 2 pixels around it, so
		 * it's easy to hit.
		 */
		boolean hitsClear(KeybindCapture capture, KeyMapping key, double mouseX, double mouseY) {
			return clearable(capture, key) && contains(mouseX, mouseY, clearX - 2, capY - 2, CLEAR + 4, CLEAR + 4);
		}
	}
}
