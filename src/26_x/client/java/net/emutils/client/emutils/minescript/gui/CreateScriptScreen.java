package net.emutils.client.emutils.minescript.gui;

import java.util.function.Consumer;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

final class CreateScriptScreen extends Screen {
	private final Screen parent;
	private final String defaultFolder;
	private final Consumer<String> onCreate;
	private EditBox nameField;

	CreateScriptScreen(Screen parent, String defaultFolder, Consumer<String> onCreate) {
		super(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_NEW_SCRIPT));
		this.parent = parent;
		this.defaultFolder = defaultFolder == null ? "" : defaultFolder;
		this.onCreate = onCreate;
	}

	@Override
	protected void init() {
		LinearLayout layout = LinearLayout.vertical().spacing(8);
		layout.defaultCellSetting().alignHorizontallyCenter();
		layout.addChild(new StringWidget(title, font));
		layout.addChild(new StringWidget(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_CREATE_HINT), font));
		nameField = layout.addChild(new EditBox(font, 0, 0, 260, 20, title));
		nameField.setValue(defaultFolder.isBlank() ? "new_script.py" : defaultFolder + "/new_script.py");
		LinearLayout buttons = layout.addChild(LinearLayout.horizontal().spacing(8));
		buttons.addChild(Button.builder(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_CREATE), button -> create()).width(120).build());
		buttons.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).width(120).build());
		layout.visitWidgets(this::addRenderableWidget);
		layout.arrangeElements();
		layout.setX((width - layout.getWidth()) / 2);
		layout.setY(height / 3);
		setInitialFocus(nameField);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent input) {
		if (nameField != null && nameField.keyPressed(input)) {
			return true;
		}
		if (input.isConfirmation()) {
			create();
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
		return nameField != null && nameField.charTyped(input) || super.charTyped(input);
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	private void create() {
		onCreate.accept(nameField.getValue());
	}
}
