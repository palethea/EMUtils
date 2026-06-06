package net.emutils.client.emutils.minescript.gui;

import java.util.function.Consumer;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

final class CreateScriptScreen extends Screen {
	private final Screen parent;
	private final String defaultFolder;
	private final Consumer<String> onCreate;
	private TextFieldWidget nameField;

	CreateScriptScreen(Screen parent, String defaultFolder, Consumer<String> onCreate) {
		super(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_NEW_SCRIPT));
		this.parent = parent;
		this.defaultFolder = defaultFolder == null ? "" : defaultFolder;
		this.onCreate = onCreate;
	}

	@Override
	protected void init() {
		DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical().spacing(8);
		layout.getMainPositioner().alignHorizontalCenter();
		layout.add(new TextWidget(title, textRenderer));
		layout.add(new TextWidget(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_CREATE_HINT), textRenderer));
		nameField = layout.add(new TextFieldWidget(textRenderer, 0, 0, 260, 20, title));
		nameField.setText(defaultFolder.isBlank() ? "new_script.py" : defaultFolder + "/new_script.py");
		DirectionalLayoutWidget buttons = layout.add(DirectionalLayoutWidget.horizontal().spacing(8));
		buttons.add(ButtonWidget.builder(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_CREATE), button -> create()).width(120).build());
		buttons.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close()).width(120).build());
		layout.forEachChild(this::addDrawableChild);
		layout.refreshPositions();
		layout.setPosition((width - layout.getWidth()) / 2, height / 3);
		setInitialFocus(nameField);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		if (nameField != null && nameField.keyPressed(input)) {
			return true;
		}
		if (input.isEnter()) {
			create();
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		return nameField != null && nameField.charTyped(input) || super.charTyped(input);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void create() {
		onCreate.accept(nameField.getText());
	}
}
