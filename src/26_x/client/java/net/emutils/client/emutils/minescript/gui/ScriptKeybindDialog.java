package net.emutils.client.emutils.minescript.gui;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiSheetFrame;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.minescript.MinescriptKeyBinding;
import net.emutils.client.emutils.minescript.MinescriptKeybindStore;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Captures a key combination that runs a script (#118): press keys to see the combination, then save.
 * A combination another script already uses is shown with a warning, and saving moves it to this one.
 * A script that has a keybind can also remove it here. Esc cancels and Enter saves, like the classic
 * keybind screen.
 */
final class ScriptKeybindDialog {
	private static final int WIDTH = 300;
	private static final int PADDING = 16;
	private static final int BUTTON_HEIGHT = 20;
	private static final int KEY_HEIGHT = 30;
	private static final int LINE_HEIGHT = 11;

	private final Font font;
	private final String command;
	private final MinescriptKeybindStore store;
	private final @Nullable MinescriptKeyBinding existing;
	private final Consumer<MinescriptKeyBinding> onSave;
	private final Runnable onRemove;
	private final UiSheetFrame frame;
	private @Nullable MinescriptKeyBinding draft;
	private int x;
	private int y;
	private int height;
	private int buttonsY;
	private int saveX;
	private int saveWidth;
	private int cancelX;
	private int cancelWidth;
	private int removeX;
	private int removeWidth;

	ScriptKeybindDialog(
		Font font,
		UiAnim anim,
		String command,
		MinescriptKeybindStore store,
		Consumer<MinescriptKeyBinding> onSave,
		Runnable onRemove
	) {
		this.font = font;
		this.command = command;
		this.store = store;
		this.existing = store.get(command).orElse(null);
		this.draft = existing;
		this.onSave = onSave;
		this.onRemove = onRemove;
		this.frame = new UiSheetFrame(anim, "keybind:" + System.identityHashCode(this), 14);
	}

	boolean isClosed() {
		return frame.isClosed();
	}

	/** Another script's keybind that uses the same keys as the draft, if any. */
	private Optional<MinescriptKeyBinding> duplicate() {
		if (draft == null) {
			return Optional.empty();
		}
		return store.duplicateOf(draft).filter(binding -> !binding.command().equals(command));
	}

	void render(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		Component title = Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_TITLE);
		Component subtitle = Component.literal("\\" + command);
		Optional<MinescriptKeyBinding> duplicate = duplicate();
		Component note = duplicate.isPresent()
			? Component.translatable(EMUtilsTexts.UI_SCRIPT_KEYBIND_MOVES, "\\" + duplicate.get().command())
			: Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_HINT);
		List<Component> noteLines = UiText.wrap(font, note, UiText.Size.BODY, WIDTH - PADDING * 2);
		int titleHeight = UiText.lineHeight(font, UiText.Size.BOLD);
		height = PADDING + titleHeight + 8 + LINE_HEIGHT + 14 + KEY_HEIGHT + 12 + noteLines.size() * LINE_HEIGHT + 16 + BUTTON_HEIGHT + PADDING;
		x = (screenWidth - WIDTH) / 2;
		y = (screenHeight - height) / 2;
		if (frame.firstFrame()) {
			UiText.prepare(title, UiText.Size.BOLD);
			UiText.prepare(subtitle, UiText.Size.BODY);
		}
		if (!frame.begin(context, theme, screenWidth, screenHeight, x, y, WIDTH, height)) {
			return;
		}
		UiText.draw(context, font, title, UiText.Size.BOLD, x + PADDING, y + PADDING, theme.text());
		int subtitleTop = y + PADDING + titleHeight + 8;
		UiText.draw(context, font, UiText.ellipsize(font, subtitle, UiText.Size.BODY, WIDTH - PADDING * 2), UiText.Size.BODY, x + PADDING, subtitleTop, theme.muted());

		// The combination, in a field that shows it's waiting for keys.
		int keyY = subtitleTop + LINE_HEIGHT + 14;
		int keyWidth = WIDTH - PADDING * 2;
		// An opaque tint: the border is drawn under the fill, so a see-through fill would show it.
		UiShapes.borderedRect(context, x + PADDING, keyY, keyWidth, KEY_HEIGHT, 8, UiTheme.mix(theme.surface(), theme.accent(), 0.1F), duplicate.isPresent() ? theme.warning() : theme.accent());
		Component keys = draft == null ? Component.translatable(EMUtilsTexts.UI_PRESS_KEY) : Component.literal(draft.displayName());
		UiText.Size size = draft == null ? UiText.Size.BODY : UiText.Size.BOLD;
		int keysWidth = UiText.width(font, keys, size);
		UiText.drawCentered(context, font, keys, size, x + PADDING + (keyWidth - keysWidth) / 2, keyY + KEY_HEIGHT / 2, draft == null ? theme.muted() : duplicate.isPresent() ? theme.warning() : theme.text());

		int noteTop = keyY + KEY_HEIGHT + 12;
		for (int i = 0; i < noteLines.size(); i++) {
			UiText.draw(context, font, noteLines.get(i), UiText.Size.BODY, x + PADDING, noteTop + i * LINE_HEIGHT, duplicate.isPresent() ? theme.warning() : theme.textSecondary());
		}

		buttonsY = y + height - PADDING - BUTTON_HEIGHT;
		Component saveLabel = Component.translatable(duplicate.isPresent() ? EMUtilsTexts.SCRIPT_MANAGER_REPLACE_KEYBIND : EMUtilsTexts.SCRIPT_MANAGER_SAVE);
		saveWidth = UiWidgets.buttonWidth(font, saveLabel) + 8;
		saveX = x + WIDTH - PADDING - saveWidth;
		cancelWidth = UiWidgets.buttonWidth(font, CommonComponents.GUI_CANCEL) + 4;
		cancelX = saveX - 6 - cancelWidth;
		boolean canSave = draft != null;
		UiWidgets.button(context, font, theme, cancelX, buttonsY, cancelWidth, BUTTON_HEIGHT, CommonComponents.GUI_CANCEL, UiWidgets.ButtonStyle.GHOST, hover(mouseX, mouseY, cancelX, cancelWidth));
		UiWidgets.button(context, font, theme, saveX, buttonsY, saveWidth, BUTTON_HEIGHT, saveLabel, canSave ? UiWidgets.ButtonStyle.PRIMARY : UiWidgets.ButtonStyle.GHOST, canSave ? hover(mouseX, mouseY, saveX, saveWidth) : 0.0F);
		removeWidth = 0;
		if (existing != null) {
			Component removeLabel = Component.translatable(EMUtilsTexts.UI_SCRIPT_KEYBIND_REMOVE);
			removeWidth = UiWidgets.buttonWidth(font, removeLabel) + 4;
			removeX = x + PADDING;
			UiWidgets.button(context, font, theme, removeX, buttonsY, removeWidth, BUTTON_HEIGHT, removeLabel, UiWidgets.ButtonStyle.OUTLINE, hover(mouseX, mouseY, removeX, removeWidth));
		}
		frame.endBody(context);
		frame.end();
	}

	private float hover(int mouseX, int mouseY, int buttonX, int buttonWidth) {
		return !frame.closing() && contains(mouseX, mouseY, buttonX, buttonsY, buttonWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F;
	}

	/** Handles a left click; the dialog takes every click while it is open. */
	void mouseClicked(double mouseX, double mouseY) {
		if (frame.closing()) {
			return;
		}
		if (contains(mouseX, mouseY, saveX, buttonsY, saveWidth, BUTTON_HEIGHT)) {
			save();
		} else if (removeWidth > 0 && contains(mouseX, mouseY, removeX, buttonsY, removeWidth, BUTTON_HEIGHT)) {
			onRemove.run();
			frame.close();
		} else if (contains(mouseX, mouseY, cancelX, buttonsY, cancelWidth, BUTTON_HEIGHT) || !contains(mouseX, mouseY, x, y, WIDTH, height)) {
			frame.close();
		}
	}

	/** Esc cancels, Enter saves, and any other combination becomes the draft; the dialog takes every key. */
	void keyPressed(KeyEvent input) {
		if (frame.closing()) {
			return;
		}
		if (input.isEscape()) {
			frame.close();
			return;
		}
		if (input.isConfirmation()) {
			save();
			return;
		}
		MinescriptKeyBinding captured = MinescriptKeyBinding.from(command, input);
		if (captured != null) {
			draft = captured;
		}
	}

	private void save() {
		if (draft == null) {
			return;
		}
		duplicate().ifPresent(store::removeBinding);
		onSave.accept(draft);
		frame.close();
	}

	private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
