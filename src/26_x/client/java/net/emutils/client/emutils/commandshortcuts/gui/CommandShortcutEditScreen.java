package net.emutils.client.emutils.commandshortcuts.gui;

import net.emutils.client.EMUtilsClient;
import net.emhelpers.client.input.StoredKeyCombo;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.commandshortcuts.CommandShortcut;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;

public final class CommandShortcutEditScreen extends Screen {
	private static final int FIELD_WIDTH = 280;
	private static final int BUTTON_WIDTH = 136;

	private final CommandShortcutListScreen parent;
	private final CommandShortcut existing;
	private EditBox nameField;
	private EditBox textField;
	private Button keyButton;
	private Button saveButton;
	private StoredKeyCombo keyCombo;
	private boolean capturingKey;
	private Component status = Component.empty();

	CommandShortcutEditScreen(CommandShortcutListScreen parent, CommandShortcut existing) {
		super(Component.translatable(existing == null ? EMUtilsTexts.SCREEN_ADD_COMMAND_SHORTCUT : EMUtilsTexts.SCREEN_EDIT_COMMAND_SHORTCUT));
		this.parent = parent;
		this.existing = existing;
		this.keyCombo = existing == null ? null : existing.keyCombo();
	}

	@Override
	protected void init() {
		LinearLayout layout = LinearLayout.vertical().spacing(8);
		layout.defaultCellSetting().alignHorizontallyCenter();

		layout.addChild(new StringWidget(title, font));
		layout.addChild(new StringWidget(Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_FIELD_NAME), font));
		nameField = layout.addChild(new EditBox(font, 0, 0, FIELD_WIDTH, 20, Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_FIELD_NAME)));
		nameField.setHint(Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_NAME_PLACEHOLDER));
		if (existing != null) {
			nameField.setValue(existing.name());
		}
		nameField.setResponder(ignored -> updateSaveButton());

		layout.addChild(new StringWidget(Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_FIELD_TEXT), font));
		textField = layout.addChild(new EditBox(font, 0, 0, FIELD_WIDTH, 20, Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_FIELD_TEXT)));
		textField.setHint(Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_TEXT_PLACEHOLDER));
		if (existing != null) {
			textField.setValue(existing.text());
		}
		textField.setResponder(ignored -> updateSaveButton());

		keyButton = layout.addChild(Button.builder(keyLabel(), button -> beginKeyCapture()).width(FIELD_WIDTH).build());

		LinearLayout buttons = layout.addChild(LinearLayout.horizontal().spacing(8));
		saveButton = buttons.addChild(Button.builder(Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_ACTION_SAVE), button -> save()).width(BUTTON_WIDTH).build());
		buttons.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).width(BUTTON_WIDTH).build());

		layout.visitWidgets(this::addRenderableWidget);
		layout.arrangeElements();
		layout.setX((width - layout.getWidth()) / 2);
		layout.setY(height / 6);
		setInitialFocus(nameField);
		updateSaveButton();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		super.extractRenderState(context, mouseX, mouseY, deltaTicks);
		if (status != null && !status.getString().isEmpty()) {
			context.centeredText(font, status, width / 2, height - 48, CommonColors.LIGHT_GRAY);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (capturingKey) {
			if (input.isEscape()) {
				capturingKey = false;
				status = Component.empty();
				keyButton.setMessage(keyLabel());
				return true;
			}
			StoredKeyCombo captured = StoredKeyCombo.from(input);
			if (captured != null) {
				keyCombo = captured;
				capturingKey = false;
				status = Component.empty();
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
		if (input.isConfirmation()) {
			save();
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (capturingKey) {
			return true;
		}

		return (nameField != null && nameField.charTyped(input))
			|| (textField != null && textField.charTyped(input))
			|| super.charTyped(input);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private void beginKeyCapture() {
		capturingKey = true;
		status = Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_CAPTURE_KEY).withStyle(ChatFormatting.YELLOW);
		keyButton.setMessage(Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_CAPTURE_KEY));
	}

	private void save() {
		String text = textField == null ? "" : textField.getValue().trim();
		if (text.isEmpty()) {
			status = Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_TEXT_REQUIRED).withStyle(ChatFormatting.RED);
			return;
		}
		if (keyCombo == null) {
			status = Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_KEY_REQUIRED).withStyle(ChatFormatting.RED);
			return;
		}

		String currentId = existing == null ? null : existing.id();
		if (EMUtilsClient.commandShortcuts().store().duplicateOf(keyCombo, currentId).isPresent()) {
			status = Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_DUPLICATE_KEY).withStyle(ChatFormatting.RED);
			return;
		}

		String name = nameField == null ? "" : nameField.getValue();
		CommandShortcut shortcut = existing == null
			? CommandShortcut.create(name, text, keyCombo)
			: existing.with(name, text, keyCombo);
		EMUtilsClient.commandShortcuts().store().put(shortcut);
		EMUtilsClient.commandShortcuts().reload();
		parent.refreshList();
		minecraft.setScreen(parent);
	}

	private void updateSaveButton() {
		if (saveButton != null) {
			saveButton.active = textField != null && !textField.getValue().trim().isEmpty() && keyCombo != null;
		}
	}

	private Component keyLabel() {
		if (keyCombo == null) {
			return Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_KEY_UNSET);
		}
		return Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_KEY_VALUE, keyCombo.displayName());
	}
}
