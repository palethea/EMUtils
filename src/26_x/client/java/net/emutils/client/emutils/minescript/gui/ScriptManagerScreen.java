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
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;
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

	private final Minecraft client = Minecraft.getInstance();
	private final Screen parent;
	private final MinescriptScriptRepository repository = new MinescriptScriptRepository();
	private final MinescriptKeybindStore keybindStore = MinescriptKeybindStore.load();
	private ScriptTreeWidget tree;
	private PythonScriptEditorWidget editor;
	private MinescriptScript selectedScript;
	private Component status = Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_READY).withStyle(ChatFormatting.GRAY);
	private GalleryIconButtonWidget openFolderButton;
	private GalleryIconButtonWidget newScriptButton;
	private GalleryIconButtonWidget refreshButton;
	private GalleryIconButtonWidget runButton;
	private GalleryIconButtonWidget saveButton;
	private GalleryIconButtonWidget setKeybindButton;
	private GalleryIconButtonWidget clearKeybindButton;
	private GalleryIconButtonWidget deleteButton;
	private Button doneButton;
	private Font textRenderer;

	public ScriptManagerScreen(Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_SCRIPT_MANAGER));
		this.parent = parent;
	}

	@Override
	protected void init() {
		textRenderer = font;
		openFolderButton = addRenderableWidget(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_OPEN_FOLDER,
			ScriptIcons.OPEN_FOLDER,
			button -> Util.getPlatform().openFile(MinescriptCompat.scriptsDir().toFile())
		));
		newScriptButton = addRenderableWidget(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_NEW_SCRIPT,
			ScriptIcons.NEW_SCRIPT,
			button -> openCreateScriptScreen()
		));
		refreshButton = addRenderableWidget(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_REFRESH,
			ScriptIcons.REFRESH,
			button -> refreshScripts()
		));
		runButton = addRenderableWidget(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_RUN,
			ScriptIcons.RUN,
			button -> runSelected()
		));
		saveButton = addRenderableWidget(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_SAVE,
			ScriptIcons.SAVE,
			button -> saveSelected()
		));
		setKeybindButton = addRenderableWidget(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_SET_KEYBIND,
			ScriptIcons.KEYBIND,
			button -> beginKeybindCapture()
		));
		clearKeybindButton = addRenderableWidget(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_CLEAR_KEYBIND,
			ScriptIcons.CLEAR_KEYBIND,
			button -> clearKeybind()
		));
		deleteButton = addRenderableWidget(iconButton(
			EMUtilsTexts.SCRIPT_MANAGER_DELETE,
			ScriptIcons.DELETE,
			button -> deleteSelected()
		));
		doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(120).build());

		tree = addRenderableWidget(new ScriptTreeWidget(client, LEFT_WIDTH, 100, this::selectScript));
		editor = addRenderableWidget(new PythonScriptEditorWidget(client, 0, 0, 100, 100, () -> updateButtonState()));
		repositionElements();
		refreshScripts();
		updateButtonState();
	}

	@Override
	protected void repositionElements() {
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
			tree.updateSizeAndPosition(LEFT_WIDTH, contentHeight, MARGIN, contentY);
		}
		if (editor != null) {
			editor.setPosition(editorX, contentY);
			editor.setSize(editorWidth, contentHeight);
		}
		if (doneButton != null) {
			doneButton.setPosition(width / 2 - 60, doneY);
			doneButton.setHeight(DONE_BUTTON_HEIGHT);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		super.extractRenderState(context, mouseX, mouseY, deltaTicks);
		context.centeredText(textRenderer, title, width / 2, 10, CommonColors.WHITE);
		context.text(textRenderer, status, MARGIN, height - BOTTOM_MARGIN - STATUS_HEIGHT + 2, CommonColors.LIGHT_GRAY);
		if (selectedScript != null) {
			keybindStore.get(selectedScript.commandName()).ifPresent(binding -> {
				Component key = Component.literal(binding.displayName()).withStyle(ChatFormatting.YELLOW);
				context.text(textRenderer, key, width - textRenderer.width(key) - MARGIN, height - BOTTOM_MARGIN - STATUS_HEIGHT + 2, CommonColors.YELLOW);
			});
		}
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if ((input.hasControlDown() || (input.modifiers() & com.mojang.blaze3d.platform.InputConstants.MOD_SUPER) != 0)
			&& input.key() == com.mojang.blaze3d.platform.InputConstants.KEY_S) {
			saveSelected();
			return true;
		}
		return editor != null && editor.keyPressed(input) || super.keyPressed(input);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
		return editor != null && editor.charTyped(input) || super.charTyped(input);
	}

	@Override
	public void onClose() {
		if (editor != null && editor.dirty()) {
			confirmDiscard(() -> client.setScreen(parent));
			return;
		}
		client.setScreen(parent);
	}

	private GalleryIconButtonWidget iconButton(String labelKey, net.minecraft.resources.Identifier icon, Button.OnPress action) {
		return GalleryIconButtonWidget.create(Component.translatable(labelKey), icon, ScriptIcons.SIZE, action);
	}

	private void layoutIconRow(int regionX, int regionWidth, int y, GalleryIconButtonWidget... buttons) {
		int count = buttons.length;
		int totalWidth = count * ICON_BUTTON + Math.max(0, count - 1) * ICON_GAP;
		int startX = regionX + Math.max(0, (regionWidth - totalWidth) / 2);
		for (int index = 0; index < count; index++) {
			GalleryIconButtonWidget button = buttons[index];
			button.setPosition(startX + index * (ICON_BUTTON + ICON_GAP), y);
			button.setSize(ICON_BUTTON, ICON_BUTTON);
		}
	}

	private void refreshScripts() {
		if (!MinescriptCompat.isLoaded()) {
			setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_REQUIRES_MINESCRIPT).withStyle(ChatFormatting.RED));
			if (tree != null) {
				tree.setScripts(List.of());
			}
			return;
		}
		try {
			tree.setScripts(repository.scan());
			setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_READY).withStyle(ChatFormatting.GRAY));
		} catch (IOException exception) {
			setStatus(Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED));
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
				setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_LOADED, script.relativePath()).withStyle(ChatFormatting.GRAY));
			} else {
				editor.setText("# " + script.displayName() + " is runnable but read-only in EMUtils.\n");
				setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_READ_ONLY).withStyle(ChatFormatting.YELLOW));
			}
		} catch (IOException exception) {
			setStatus(Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED));
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
			setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_SAVED).withStyle(ChatFormatting.GREEN));
			refreshScripts();
		} catch (IOException exception) {
			setStatus(Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED));
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
			setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAFE_COMMAND).withStyle(ChatFormatting.RED));
			return;
		}
		switch (MinescriptCompat.toggleCommand(selectedScript.commandName())) {
			case STARTED -> setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_RUNNING, selectedScript.commandName()).withStyle(ChatFormatting.GREEN));
			case STOPPED -> setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_STOPPED, selectedScript.commandName()).withStyle(ChatFormatting.YELLOW));
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
					setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETED).withStyle(ChatFormatting.YELLOW));
				} catch (IOException exception) {
					setStatus(Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED));
				}
				updateButtonState();
			},
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE_TITLE),
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE_MESSAGE, selectedScript.relativePath()),
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE),
			CommonComponents.GUI_CANCEL
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
				setStatus(Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED));
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
		setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_SET, binding.displayName()).withStyle(ChatFormatting.GREEN));
		updateButtonState();
	}

	private void clearKeybind() {
		if (selectedScript == null || selectedScript.commandName() == null) {
			return;
		}
		keybindStore.remove(selectedScript.commandName());
		EMUtilsClient.minescriptKeybinds().reload();
		setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_CLEARED).withStyle(ChatFormatting.GREEN));
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
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAVED_TITLE),
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAVED_MESSAGE),
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DISCARD),
			CommonComponents.GUI_CANCEL
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

	private void setStatus(Component status) {
		this.status = status;
	}
}
