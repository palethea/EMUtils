package net.emutils.client.emutils.minescript.gui;

import java.util.function.Consumer;
import net.emutils.client.emutils.minescript.MinescriptKeyBinding;
import net.emutils.client.emutils.minescript.MinescriptKeybindStore;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;

final class ScriptKeybindScreen extends Screen {
	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 168;
	private static final int BUTTON_WIDTH = 120;
	private static final int BUTTON_GAP = 8;

	private final Screen parent;
	private final String command;
	private final MinescriptKeybindStore keybindStore;
	private final Consumer<MinescriptKeyBinding> onSave;
	private MinescriptKeyBinding draft;
	private ButtonWidget saveButton;
	private ButtonWidget cancelButton;

	ScriptKeybindScreen(
		Screen parent,
		String command,
		MinescriptKeyBinding existing,
		MinescriptKeybindStore keybindStore,
		Consumer<MinescriptKeyBinding> onSave
	) {
		super(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_TITLE));
		this.parent = parent;
		this.command = command;
		this.draft = existing;
		this.keybindStore = keybindStore;
		this.onSave = onSave;
	}

	@Override
	protected void init() {
		saveButton = addDrawableChild(ButtonWidget.builder(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_SAVE), button -> save()).width(BUTTON_WIDTH).build());
		cancelButton = addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close()).width(BUTTON_WIDTH).build());
		refreshWidgetPositions();
		updateSaveButton();
	}

	@Override
	protected void refreshWidgetPositions() {
		int panelX = panelX();
		int panelY = panelY();
		int buttonsWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
		int buttonX = panelX + (PANEL_WIDTH - buttonsWidth) / 2;
		int buttonY = panelY + PANEL_HEIGHT - 28;
		saveButton.setPosition(buttonX, buttonY);
		cancelButton.setPosition(buttonX + BUTTON_WIDTH + BUTTON_GAP, buttonY);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.fill(0, 0, width, height, 0xA0000000);
		int panelX = panelX();
		int panelY = panelY();
		context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xF0101010);
		context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF555555);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 12, Colors.WHITE);
		context.drawCenteredTextWithShadow(
			textRenderer,
			Text.literal("\\" + command).formatted(Formatting.GRAY),
			width / 2,
			panelY + 28,
			Colors.LIGHT_GRAY
		);
		context.drawCenteredTextWithShadow(
			textRenderer,
			Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_HINT).formatted(Formatting.GRAY),
			width / 2,
			panelY + 48,
			Colors.LIGHT_GRAY
		);
		Text preview = draft == null
			? Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_UNSET).formatted(Formatting.DARK_GRAY)
			: Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_PREVIEW, draft.displayName()).formatted(Formatting.YELLOW);
		context.drawCenteredTextWithShadow(textRenderer, preview, width / 2, panelY + 78, Colors.WHITE);
		super.render(context, mouseX, mouseY, deltaTicks);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEscape()) {
			close();
			return true;
		}
		if (input.isEnter()) {
			save();
			return true;
		}
		MinescriptKeyBinding captured = MinescriptKeyBinding.from(command, input);
		if (captured != null) {
			draft = captured;
			updateSaveButton();
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void save() {
		if (draft == null) {
			return;
		}
		keybindStore.duplicateOf(draft).ifPresentOrElse(
			duplicate -> client.setScreen(new ConfirmScreen(
				confirmed -> {
					client.setScreen(parent);
					if (confirmed) {
						keybindStore.removeBinding(duplicate);
						onSave.accept(draft);
					}
				},
				Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_DUPLICATE_TITLE),
				Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_DUPLICATE_MESSAGE, duplicate.command()),
				Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_REPLACE_KEYBIND),
				ScreenTexts.CANCEL
			)),
			() -> {
				client.setScreen(parent);
				onSave.accept(draft);
			}
		);
	}

	private void updateSaveButton() {
		if (saveButton != null) {
			saveButton.active = draft != null;
		}
	}

	private int panelX() {
		return (width - PANEL_WIDTH) / 2;
	}

	private int panelY() {
		return (height - PANEL_HEIGHT) / 2;
	}
}
