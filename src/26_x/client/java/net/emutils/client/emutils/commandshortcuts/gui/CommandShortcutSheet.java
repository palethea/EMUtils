package net.emutils.client.emutils.commandshortcuts.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.commandshortcuts.CommandShortcut;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiSheetFrame;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.input.StoredKeyCombo;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Adds or edits a command shortcut in a sheet over the list (#131): an optional name, the command or
 * chat message, and the keys that send it. The key field captures a combination while it's focused;
 * one another shortcut already uses is named instead of saved. Enter saves, Esc cancels (or stops
 * capturing), Tab moves between the fields.
 */
final class CommandShortcutSheet {
	private static final int WIDTH = 340;
	private static final int PADDING = 16;
	private static final int FIELD_HEIGHT = 20;
	private static final int KEY_HEIGHT = 26;
	private static final int BUTTON_HEIGHT = 20;
	private static final int LINE_HEIGHT = 11;

	private final Font font;
	private final @Nullable CommandShortcut existing;
	private final Runnable onSaved;
	private final UiSheetFrame frame;
	private final UiTextField name = new UiTextField(this, 48);
	private final UiTextField text = new UiTextField(this, 256);
	private @Nullable StoredKeyCombo keyCombo;
	/** Whether the key field is focused and turns the next key combination into the shortcut's keys. */
	private boolean capturing;
	private int x;
	private int y;
	private int height;
	private int nameY;
	private int textY;
	private int keyY;
	private int footerY;
	private int saveX;
	private int saveWidth;
	private int cancelX;
	private int cancelWidth;

	CommandShortcutSheet(Font font, UiAnim anim, @Nullable CommandShortcut existing, Runnable onSaved) {
		this.font = font;
		this.existing = existing;
		this.onSaved = onSaved;
		this.frame = new UiSheetFrame(anim, "command-shortcut:" + System.identityHashCode(this), 16);
		if (existing != null) {
			name.setText(existing.name());
			text.setText(existing.text());
			keyCombo = existing.keyCombo();
		}
		focus(existing == null ? text : name);
	}

	boolean isClosed() {
		return frame.isClosed();
	}

	void close() {
		if (!frame.closing()) {
			focus(null);
			frame.close();
		}
	}

	/** Focuses a text field, or none with null, and stops capturing keys; the others let go of text input first. */
	private void focus(@Nullable UiTextField target) {
		capturing = false;
		for (UiTextField field : List.of(name, text)) {
			if (field != target && field.focused()) {
				field.setFocused(false);
			}
		}
		if (target != null && !target.focused()) {
			target.setFocused(true);
		}
	}

	private void captureKeys() {
		focus(null);
		capturing = true;
	}

	/** The shortcut that already uses these keys, if any. */
	private Optional<CommandShortcut> duplicate() {
		return EMUtilsClient.commandShortcuts().store().duplicateOf(keyCombo, existing == null ? null : existing.id());
	}

	private boolean valid() {
		return !text.text().isBlank() && keyCombo != null && duplicate().isEmpty();
	}

	private void save() {
		if (!valid()) {
			return;
		}
		CommandShortcut shortcut = existing == null
			? CommandShortcut.create(name.text(), text.text(), keyCombo)
			: existing.with(name.text(), text.text(), keyCombo);
		EMUtilsClient.commandShortcuts().store().put(shortcut);
		EMUtilsClient.commandShortcuts().reload();
		onSaved.run();
		close();
	}

	// ---- drawing --------------------------------------------------------------------------------

	private void layout(int screenWidth, int screenHeight) {
		int labelBlock = UiText.lineHeight(font, UiText.Size.LABEL) + 6;
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING);
		nameY = PADDING + headerHeight + 16 + labelBlock;
		textY = nameY + FIELD_HEIGHT + 12 + labelBlock;
		keyY = textY + FIELD_HEIGHT + 6 + LINE_HEIGHT + 12 + labelBlock;
		footerY = keyY + KEY_HEIGHT + 8 + LINE_HEIGHT + 14;
		height = footerY + BUTTON_HEIGHT + PADDING;
		x = (screenWidth - WIDTH) / 2;
		y = (screenHeight - height) / 2;
	}

	void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		layout(screenWidth, screenHeight);
		Component title = Component.translatable(existing == null ? EMUtilsTexts.UI_SHORTCUT_NEW : EMUtilsTexts.UI_SHORTCUT_EDIT);
		if (frame.firstFrame()) {
			UiText.prepare(title, UiText.Size.HEADING);
		}
		if (!frame.begin(context, theme, screenWidth, screenHeight, x, y, WIDTH, height)) {
			return;
		}
		int hoverX = frame.closing() ? Integer.MIN_VALUE / 2 : mouseX;
		int hoverY = frame.closing() ? Integer.MIN_VALUE / 2 : mouseY;
		int left = x + PADDING;
		int right = x + WIDTH - PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, y + PADDING, theme.text());

		label(context, theme, Component.translatable(EMUtilsTexts.UI_SHORTCUT_NAME), left, y + nameY);
		field(context, theme, name, left, y + nameY, right - left, Component.translatable(EMUtilsTexts.UI_SHORTCUT_NAME_PLACEHOLDER));

		label(context, theme, Component.translatable(EMUtilsTexts.UI_SHORTCUT_TEXT), left, y + textY);
		field(context, theme, text, left, y + textY, right - left, Component.translatable(EMUtilsTexts.UI_SHORTCUT_TEXT_PLACEHOLDER));
		// What happens with the text: a command runs, anything else is sent as a chat message.
		boolean command = text.text().trim().startsWith("/");
		Component kind = text.text().isBlank()
			? Component.translatable(EMUtilsTexts.UI_SHORTCUT_TEXT_HINT)
			: Component.translatable(command ? EMUtilsTexts.UI_SHORTCUT_RUNS_COMMAND : EMUtilsTexts.UI_SHORTCUT_SENDS_MESSAGE);
		UiText.draw(context, font, kind, UiText.Size.BODY, left + 1, y + textY + FIELD_HEIGHT + 6, theme.muted());

		label(context, theme, Component.translatable(EMUtilsTexts.UI_SHORTCUT_KEYS), left, y + keyY);
		Optional<CommandShortcut> duplicate = duplicate();
		int border = capturing ? theme.accent() : duplicate.isPresent() ? theme.warning() : theme.line();
		int fill = capturing ? UiTheme.mix(theme.segmentBackground(), theme.accent(), 0.08F) : theme.segmentBackground();
		UiShapes.borderedRect(context, left, y + keyY, right - left, KEY_HEIGHT, 7, fill, border);
		int keyCenter = y + keyY + KEY_HEIGHT / 2;
		if (capturing || keyCombo == null) {
			Component prompt = Component.translatable(capturing ? EMUtilsTexts.UI_PRESS_KEY : EMUtilsTexts.UI_SHORTCUT_CLICK_TO_SET);
			UiText.drawCentered(context, font, prompt, UiText.Size.BODY, left + 10, keyCenter, capturing ? theme.textSecondary() : theme.muted());
		} else {
			Component keys = Component.literal(keyCombo.displayName());
			UiWidgets.keycap(context, font, theme, left + 8, keyCenter - UiWidgets.KEYCAP_HEIGHT / 2, keys, false, duplicate.isPresent(), 0.0F);
		}
		Component note = duplicate.isPresent()
			? Component.translatable(EMUtilsTexts.UI_SHORTCUT_KEYS_TAKEN, duplicate.get().displayName())
			: Component.translatable(EMUtilsTexts.UI_SHORTCUT_KEYS_HINT);
		UiText.draw(context, font, UiText.ellipsize(font, note, UiText.Size.BODY, right - left), UiText.Size.BODY, left + 1, y + keyY + KEY_HEIGHT + 8, duplicate.isPresent() ? theme.warning() : theme.muted());

		Component saveLabel = Component.translatable(existing == null ? EMUtilsTexts.UI_ADD : EMUtilsTexts.COMMAND_SHORTCUT_ACTION_SAVE);
		saveWidth = Math.max(60, UiWidgets.buttonWidth(font, saveLabel) + 12);
		saveX = right - saveWidth;
		cancelWidth = UiWidgets.buttonWidth(font, CommonComponents.GUI_CANCEL) + 4;
		cancelX = saveX - 6 - cancelWidth;
		UiWidgets.button(context, font, theme, cancelX, y + footerY, cancelWidth, BUTTON_HEIGHT, CommonComponents.GUI_CANCEL, UiWidgets.ButtonStyle.GHOST, contains(hoverX, hoverY, cancelX, y + footerY, cancelWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		boolean valid = valid();
		UiWidgets.button(context, font, theme, saveX, y + footerY, saveWidth, BUTTON_HEIGHT, saveLabel, valid ? UiWidgets.ButtonStyle.PRIMARY : UiWidgets.ButtonStyle.SURFACE, valid && contains(hoverX, hoverY, saveX, y + footerY, saveWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		frame.endBody(context);
		frame.end();
	}

	private void label(GuiGraphicsExtractor context, UiTheme theme, Component label, int left, int fieldTop) {
		UiText.draw(context, font, label, UiText.Size.LABEL, left + 1, fieldTop - UiText.lineHeight(font, UiText.Size.LABEL) - 6, theme.muted());
	}

	private void field(GuiGraphicsExtractor context, UiTheme theme, UiTextField field, int fieldX, int fieldY, int width, Component placeholder) {
		UiShapes.borderedRect(context, fieldX, fieldY, width, FIELD_HEIGHT, 6, theme.segmentBackground(), field.focused() ? theme.accent() : theme.line());
		field.draw(context, font, theme, fieldX + 8, fieldY + FIELD_HEIGHT / 2, width - 16, placeholder);
	}

	// ---- input ----------------------------------------------------------------------------------

	void mouseClicked(double mouseX, double mouseY) {
		if (frame.closing()) {
			return;
		}
		if (!contains(mouseX, mouseY, x, y, WIDTH, height) || contains(mouseX, mouseY, cancelX, y + footerY, cancelWidth, BUTTON_HEIGHT)) {
			close();
			return;
		}
		if (contains(mouseX, mouseY, saveX, y + footerY, saveWidth, BUTTON_HEIGHT)) {
			save();
			return;
		}
		int left = x + PADDING;
		int width = WIDTH - PADDING * 2;
		if (contains(mouseX, mouseY, left, y + nameY, width, FIELD_HEIGHT)) {
			focus(name);
			name.click(font, mouseX, false);
		} else if (contains(mouseX, mouseY, left, y + textY, width, FIELD_HEIGHT)) {
			focus(text);
			text.click(font, mouseX, false);
		} else if (contains(mouseX, mouseY, left, y + keyY, width, KEY_HEIGHT)) {
			captureKeys();
		} else {
			focus(null);
		}
	}

	/** While capturing, every key but Esc becomes the combination; otherwise Enter saves and Tab moves on. */
	void keyPressed(KeyEvent input) {
		if (frame.closing()) {
			return;
		}
		if (capturing) {
			if (input.isEscape()) {
				capturing = false;
				return;
			}
			StoredKeyCombo captured = StoredKeyCombo.from(input);
			if (captured != null) {
				keyCombo = captured;
				capturing = false;
			}
			return;
		}
		if (input.isEscape()) {
			close();
			return;
		}
		if (input.isConfirmation()) {
			save();
			return;
		}
		if (input.key() == InputConstants.KEY_TAB) {
			// Name → text → keys, and back with Shift.
			int current = name.focused() ? 0 : text.focused() ? 1 : 2;
			int next = Math.floorMod(current + (input.hasShiftDown() ? -1 : 1), 3);
			if (next == 2) {
				captureKeys();
			} else {
				UiTextField field = next == 0 ? name : text;
				focus(field);
				field.setText(field.text());
			}
			return;
		}
		if (!name.keyPressed(input, () -> {
		})) {
			text.keyPressed(input, () -> {
			});
		}
	}

	void charTyped(CharacterEvent input) {
		if (capturing || frame.closing()) {
			return;
		}
		if (!name.charTyped(input, () -> {
		})) {
			text.charTyped(input, () -> {
			});
		}
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
