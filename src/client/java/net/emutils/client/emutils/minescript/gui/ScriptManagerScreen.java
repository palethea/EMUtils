package net.emutils.client.emutils.minescript.gui;

import java.io.IOException;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.screenshot.gui.GalleryIconButtonWidget;
import net.emutils.client.emutils.minescript.MinescriptKeyBinding;
import net.emutils.client.emutils.minescript.MinescriptKeybindStore;
import net.emutils.client.emutils.minescript.MinescriptScript;
import net.emutils.client.emutils.minescript.MinescriptScriptRepository;
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
import net.minecraft.util.Util;

public final class ScriptManagerScreen extends Screen {
	private static final int TITLE_HEIGHT = 28;
	private static final int TOOLBAR_HEIGHT = 24;
	private static final int TOOLBAR_GAP = 6;
	private static final int DONE_BUTTON_HEIGHT = 20;
	private static final int DONE_TOP_GAP = 10;
	private static final int BOTTOM_MARGIN = 8;
	private static final int STATUS_HEIGHT = 14;
	private static final int LEFT_WIDTH = 250;
	private static final int MARGIN = 10;
	private static final int ICON_BUTTON = 20;
	private static final int ICON_GAP = 4;

	private final Screen parent;
	private final MinescriptScriptRepository repository = new MinescriptScriptRepository();
	private final MinescriptKeybindStore keybindStore = MinescriptKeybindStore.load();
	private ScriptTreeWidget tree;
	private PythonScriptEditorWidget editor;
	private MinescriptScript selectedScript;
	private Text status = Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_READY).formatted(Formatting.GRAY);
	private GalleryIconButtonWidget openFolderButton;
	private GalleryIconButtonWidget newScriptButton;
	private GalleryIconButtonWidget refreshButton;
	private GalleryIconButtonWidget runButton;
	private GalleryIconButtonWidget saveButton;
	private GalleryIconButtonWidget setKeybindButton;
	private GalleryIconButtonWidget clearKeybindButton;
	private GalleryIconButtonWidget deleteButton;
	private ButtonWidget doneButton;

	public ScriptManagerScreen(Screen parent) {
		super(Text.translatable(EMUtilsTexts.SCREEN_SCRIPT_MANAGER));
		this.parent = parent;
	}

	@Override
	protected void init() {
		openFolderButton = addDrawableChild(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_OPEN_FOLDER,
			ScriptIcons.OPEN_FOLDER,
			button -> Util.getOperatingSystem().open(MinescriptCompat.scriptsDir().toFile())
		));
		newScriptButton = addDrawableChild(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_NEW_SCRIPT,
			ScriptIcons.NEW_SCRIPT,
			button -> openCreateScriptScreen()
		));
		refreshButton = addDrawableChild(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_REFRESH,
			ScriptIcons.REFRESH,
			button -> refreshScripts()
		));
		runButton = addDrawableChild(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_RUN,
			ScriptIcons.RUN,
			button -> runSelected()
		));
		saveButton = addDrawableChild(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_SAVE,
			ScriptIcons.SAVE,
			button -> saveSelected()
		));
		setKeybindButton = addDrawableChild(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_SET_KEYBIND,
			ScriptIcons.KEYBIND,
			button -> beginKeybindCapture()
		));
		clearKeybindButton = addDrawableChild(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_CLEAR_KEYBIND,
			ScriptIcons.CLEAR_KEYBIND,
			button -> clearKeybind()
		));
		deleteButton = addDrawableChild(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_DELETE,
			ScriptIcons.DELETE,
			button -> deleteSelected()
		));
		doneButton = addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(120).build());

		tree = addDrawableChild(new ScriptTreeWidget(client, LEFT_WIDTH, 100, this::selectScript));
		editor = addDrawableChild(new PythonScriptEditorWidget(client, 0, 0, 100, 100, () -> updateButtonState()));
		refreshWidgetPositions();
		refreshScripts();
		updateButtonState();
	}

	@Override
	protected void refreshWidgetPositions() {
		int toolbarY = TITLE_HEIGHT;
		layoutIconRow(MARGIN, LEFT_WIDTH, toolbarY, openFolderButton, newScriptButton, refreshButton);

		int editorX = MARGIN * 2 + LEFT_WIDTH;
		int editorWidth = width - editorX - MARGIN;
		layoutIconRow(editorX, editorWidth, toolbarY, runButton, saveButton, setKeybindButton, clearKeybindButton, deleteButton);

		int contentY = TITLE_HEIGHT + TOOLBAR_HEIGHT + TOOLBAR_GAP;
		int doneY = height - BOTTOM_MARGIN - STATUS_HEIGHT - DONE_TOP_GAP - DONE_BUTTON_HEIGHT;
		int contentBottom = doneY - DONE_TOP_GAP;
		int contentHeight = Math.max(40, contentBottom - contentY);
		if (tree != null) {
			tree.position(LEFT_WIDTH, contentHeight, MARGIN, contentY);
		}
		if (editor != null) {
			editor.setPosition(editorX, contentY);
			editor.setDimensions(editorWidth, contentHeight);
		}
		if (doneButton != null) {
			doneButton.setPosition(width / 2 - 60, doneY);
			doneButton.setHeight(DONE_BUTTON_HEIGHT);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, Colors.WHITE);
		context.drawTextWithShadow(textRenderer, status, MARGIN, height - BOTTOM_MARGIN - STATUS_HEIGHT + 2, Colors.LIGHT_GRAY);
		if (selectedScript != null) {
			keybindStore.get(selectedScript.commandName()).ifPresent(binding -> {
				Text key = Text.literal(binding.displayName()).formatted(Formatting.YELLOW);
				context.drawTextWithShadow(textRenderer, key, width - textRenderer.getWidth(key) - MARGIN, height - BOTTOM_MARGIN - STATUS_HEIGHT + 2, Colors.YELLOW);
			});
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.hasCtrlOrCmd() && input.key() == net.minecraft.client.util.InputUtil.GLFW_KEY_S) {
			saveSelected();
			return true;
		}
		return editor != null && editor.keyPressed(input) || super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		return editor != null && editor.charTyped(input) || super.charTyped(input);
	}

	@Override
	public void close() {
		if (editor != null && editor.dirty()) {
			confirmDiscard(() -> client.setScreen(parent));
			return;
		}
		client.setScreen(parent);
	}

	private GalleryIconButtonWidget iconButton(String labelKey, net.minecraft.util.Identifier icon, ButtonWidget.PressAction action) {
		return GalleryIconButtonWidget.create(Text.translatable(labelKey), icon, ScriptIcons.SIZE, action);
	}

	private void layoutIconRow(int regionX, int regionWidth, int y, GalleryIconButtonWidget... buttons) {
		int count = buttons.length;
		int totalWidth = count * ICON_BUTTON + Math.max(0, count - 1) * ICON_GAP;
		int startX = regionX + Math.max(0, (regionWidth - totalWidth) / 2);
		for (int index = 0; index < count; index++) {
			GalleryIconButtonWidget button = buttons[index];
			button.setPosition(startX + index * (ICON_BUTTON + ICON_GAP), y);
			button.setDimensions(ICON_BUTTON, ICON_BUTTON);
		}
	}

	private void refreshScripts() {
		if (!MinescriptCompat.isLoaded()) {
			setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_REQUIRES_MINESCRIPT).formatted(Formatting.RED));
			if (tree != null) {
				tree.setScripts(List.of());
			}
			return;
		}
		try {
			tree.setScripts(repository.scan());
			setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_READY).formatted(Formatting.GRAY));
		} catch (IOException exception) {
			setStatus(Text.literal(exception.getMessage()).formatted(Formatting.RED));
		}
	}

	private void selectScript(MinescriptScript script) {
		if (editor != null && editor.dirty()) {
			confirmDiscard(() -> loadScript(script));
			return;
		}
		loadScript(script);
	}

	private void loadScript(MinescriptScript script) {
		selectedScript = script;
		try {
			if (script.editable()) {
				editor.setText(repository.read(script));
				setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_LOADED, script.relativePath()).formatted(Formatting.GRAY));
			} else {
				editor.setText("# " + script.displayName() + " is runnable but read-only in EMUtils.\n");
				setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_READ_ONLY).formatted(Formatting.YELLOW));
			}
		} catch (IOException exception) {
			setStatus(Text.literal(exception.getMessage()).formatted(Formatting.RED));
		}
		updateButtonState();
	}

	private void saveSelected() {
		if (selectedScript == null || editor == null || !selectedScript.editable()) {
			return;
		}
		try {
			repository.write(selectedScript, editor.text());
			editor.markClean();
			setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_SAVED).formatted(Formatting.GREEN));
			refreshScripts();
		} catch (IOException exception) {
			setStatus(Text.literal(exception.getMessage()).formatted(Formatting.RED));
		}
		updateButtonState();
	}

	private void runSelected() {
		if (selectedScript == null || !selectedScript.runnable()) {
			return;
		}
		if (editor.dirty()) {
			saveSelected();
		}
		if (!repository.isSafeCommand(selectedScript.commandName())) {
			setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAFE_COMMAND).formatted(Formatting.RED));
			return;
		}
		switch (MinescriptCompat.toggleCommand(selectedScript.commandName())) {
			case STARTED -> setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_RUNNING, selectedScript.commandName()).formatted(Formatting.GREEN));
			case STOPPED -> setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_STOPPED, selectedScript.commandName()).formatted(Formatting.YELLOW));
			case FAILED -> {
			}
		}
	}

	private void deleteSelected() {
		if (selectedScript == null || selectedScript.directory() || !selectedScript.editable()) {
			return;
		}
		client.setScreen(new ConfirmScreen(
			confirmed -> {
				client.setScreen(this);
				if (!confirmed) {
					return;
				}
				try {
					repository.delete(selectedScript);
					selectedScript = null;
					editor.setText("");
					editor.markClean();
					refreshScripts();
					setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETED).formatted(Formatting.YELLOW));
				} catch (IOException exception) {
					setStatus(Text.literal(exception.getMessage()).formatted(Formatting.RED));
				}
				updateButtonState();
			},
			Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE_TITLE),
			Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE_MESSAGE, selectedScript.relativePath()),
			Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE),
			ScreenTexts.CANCEL
		));
	}

	private void openCreateScriptScreen() {
		client.setScreen(new CreateScriptScreen(this, tree == null ? "" : tree.selectedDirectory(), name -> {
			client.setScreen(this);
			try {
				MinescriptScript script = repository.createScript(name);
				refreshScripts();
				loadScript(script);
			} catch (IOException exception) {
				setStatus(Text.literal(exception.getMessage()).formatted(Formatting.RED));
			}
		}));
	}

	private void beginKeybindCapture() {
		if (selectedScript == null || selectedScript.commandName() == null) {
			return;
		}
		client.setScreen(new ScriptKeybindScreen(
			this,
			selectedScript.commandName(),
			keybindStore.get(selectedScript.commandName()).orElse(null),
			keybindStore,
			this::applyKeybind
		));
	}

	private void applyKeybind(MinescriptKeyBinding binding) {
		keybindStore.put(binding);
		EMUtilsClient.minescriptKeybinds().reload();
		setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_SET, binding.displayName()).formatted(Formatting.GREEN));
		updateButtonState();
	}

	private void clearKeybind() {
		if (selectedScript == null || selectedScript.commandName() == null) {
			return;
		}
		keybindStore.remove(selectedScript.commandName());
		EMUtilsClient.minescriptKeybinds().reload();
		setStatus(Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_CLEARED).formatted(Formatting.GREEN));
		updateButtonState();
	}

	private void confirmDiscard(Runnable action) {
		client.setScreen(new ConfirmScreen(
			confirmed -> {
				client.setScreen(this);
				if (confirmed) {
					action.run();
				}
			},
			Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAVED_TITLE),
			Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAVED_MESSAGE),
			Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_DISCARD),
			ScreenTexts.CANCEL
		));
	}

	private void updateButtonState() {
		boolean scriptSelected = selectedScript != null;
		if (runButton != null) {
			runButton.active = scriptSelected && selectedScript.runnable() && MinescriptCompat.isLoaded();
		}
		if (saveButton != null) {
			saveButton.active = scriptSelected && selectedScript.editable() && editor != null && editor.dirty();
		}
		if (setKeybindButton != null) {
			setKeybindButton.active = scriptSelected && selectedScript.runnable();
		}
		if (clearKeybindButton != null) {
			clearKeybindButton.active = scriptSelected && selectedScript.commandName() != null && keybindStore.get(selectedScript.commandName()).isPresent();
		}
		if (deleteButton != null) {
			deleteButton.active = scriptSelected && selectedScript.editable() && !selectedScript.directory();
		}
	}

	private void setStatus(Text status) {
		this.status = status;
	}
}
