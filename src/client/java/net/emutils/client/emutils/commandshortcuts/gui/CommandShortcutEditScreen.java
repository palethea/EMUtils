package net.emutils.client.emutils.commandshortcuts.gui;

import net.emutils.client.EMUtilsClient;
import net.emhelpers.client.input.StoredKeyCombo;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.commandshortcuts.CommandShortcut;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;

public final class CommandShortcutEditScreen extends Screen {
	private static final int FIELD_WIDTH = 280;
	private static final int BUTTON_WIDTH = 136;

	private final CommandShortcutListScreen parent;
	private final CommandShortcut existing;
	private TextFieldWidget nameField;
	private TextFieldWidget textField;
	private ButtonWidget keyButton;
	private ButtonWidget saveButton;
	private StoredKeyCombo keyCombo;
	private boolean capturingKey;
	private Text status = Text.empty();

	CommandShortcutEditScreen(CommandShortcutListScreen parent, CommandShortcut existing) {
		super(Text.translatable(existing == null ? EMUtilsTexts.SCREEN_ADD_COMMAND_SHORTCUT : EMUtilsTexts.SCREEN_EDIT_COMMAND_SHORTCUT));
		this.parent = parent;
		this.existing = existing;
		this.keyCombo = existing == null ? null : existing.keyCombo();
	}

	@Override
	protected void init() {
		DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical().spacing(8);
		layout.getMainPositioner().alignHorizontalCenter();

		layout.add(new TextWidget(title, textRenderer));
		layout.add(new TextWidget(Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_FIELD_NAME), textRenderer));
		nameField = layout.add(new TextFieldWidget(textRenderer, 0, 0, FIELD_WIDTH, 20, Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_FIELD_NAME)));
		nameField.setPlaceholder(Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_NAME_PLACEHOLDER));
		if (existing != null) {
			nameField.setText(existing.name());
		}
		nameField.setChangedListener(ignored -> updateSaveButton());

		layout.add(new TextWidget(Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_FIELD_TEXT), textRenderer));
		textField = layout.add(new TextFieldWidget(textRenderer, 0, 0, FIELD_WIDTH, 20, Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_FIELD_TEXT)));
		textField.setPlaceholder(Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_TEXT_PLACEHOLDER));
		if (existing != null) {
			textField.setText(existing.text());
		}
		textField.setChangedListener(ignored -> updateSaveButton());

		keyButton = layout.add(ButtonWidget.builder(keyLabel(), button -> beginKeyCapture()).width(FIELD_WIDTH).build());

		DirectionalLayoutWidget buttons = layout.add(DirectionalLayoutWidget.horizontal().spacing(8));
		saveButton = buttons.add(ButtonWidget.builder(Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_ACTION_SAVE), button -> save()).width(BUTTON_WIDTH).build());
		buttons.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close()).width(BUTTON_WIDTH).build());

		layout.forEachChild(this::addDrawableChild);
		layout.refreshPositions();
		layout.setPosition((width - layout.getWidth()) / 2, height / 6);
		setInitialFocus(nameField);
		updateSaveButton();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		if (status != null && !status.getString().isEmpty()) {
			context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 48, Colors.LIGHT_GRAY);
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (capturingKey) {
			if (input.isEscape()) {
				capturingKey = false;
				status = Text.empty();
				keyButton.setMessage(keyLabel());
				return true;
			}
			StoredKeyCombo captured = StoredKeyCombo.from(input);
			if (captured != null) {
				keyCombo = captured;
				capturingKey = false;
				status = Text.empty();
				keyButton.setMessage(keyLabel());
				updateSaveButton();
				return true;
			}
			return true;
		}

		if (nameField != null && nameField.keyPressed(input)) {
			return true;
		}
		if (textField != null && textField.keyPressed(input)) {
			return true;
		}
		if (input.isEnter()) {
			save();
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (capturingKey) {
			return true;
		}

		return (nameField != null && nameField.charTyped(input))
			|| (textField != null && textField.charTyped(input))
			|| super.charTyped(input);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void beginKeyCapture() {
		capturingKey = true;
		status = Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_CAPTURE_KEY).formatted(Formatting.YELLOW);
		keyButton.setMessage(Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_CAPTURE_KEY));
	}

	private void save() {
		String text = textField == null ? "" : textField.getText().trim();
		if (text.isEmpty()) {
			status = Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_TEXT_REQUIRED).formatted(Formatting.RED);
			return;
		}
		if (keyCombo == null) {
			status = Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_KEY_REQUIRED).formatted(Formatting.RED);
			return;
		}

		String currentId = existing == null ? null : existing.id();
		if (EMUtilsClient.commandShortcuts().store().duplicateOf(keyCombo, currentId).isPresent()) {
			status = Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_DUPLICATE_KEY).formatted(Formatting.RED);
			return;
		}

		String name = nameField == null ? "" : nameField.getText();
		CommandShortcut shortcut = existing == null
			? CommandShortcut.create(name, text, keyCombo)
			: existing.with(name, text, keyCombo);
		EMUtilsClient.commandShortcuts().store().put(shortcut);
		EMUtilsClient.commandShortcuts().reload();
		parent.refreshList();
		client.setScreen(parent);
	}

	private void updateSaveButton() {
		if (saveButton != null) {
			saveButton.active = textField != null && !textField.getText().trim().isEmpty() && keyCombo != null;
		}
	}

	private Text keyLabel() {
		if (keyCombo == null) {
			return Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_KEY_UNSET);
		}
		return Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_KEY_VALUE, keyCombo.displayName());
	}
}
