package net.emutils.client.emutils.minescript.gui;

import java.util.function.Consumer;
import net.emutils.client.emutils.minescript.MinescriptKeyBinding;
import net.emutils.client.emutils.minescript.MinescriptKeybindStore;
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

final class ScriptKeybindScreen extends Screen {
	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 168;
	private static final int BUTTON_WIDTH = 120;
	private static final int BUTTON_GAP = 8;

	private final Minecraft client = Minecraft.getInstance();
	private final Screen parent;
	private final String command;
	private final MinescriptKeybindStore keybindStore;
	private final Consumer<MinescriptKeyBinding> onSave;
	private MinescriptKeyBinding draft;
	private Button saveButton;
	private Button cancelButton;
	private Font textRenderer;

	ScriptKeybindScreen(
		Screen parent,
		String command,
		MinescriptKeyBinding existing,
		MinescriptKeybindStore keybindStore,
		Consumer<MinescriptKeyBinding> onSave
	) {
		super(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_TITLE));
		this.parent = parent;
		this.command = command;
		this.draft = existing;
		this.keybindStore = keybindStore;
		this.onSave = onSave;
	}

	@Override
	protected void init() {
		textRenderer = font;
		saveButton = addRenderableWidget(Button.builder(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_SAVE), button -> save()).width(BUTTON_WIDTH).build());
		cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).width(BUTTON_WIDTH).build());
		repositionElements();
		updateSaveButton();
	}

	@Override
	protected void repositionElements() {
		int panelX = panelX();
		int panelY = panelY();
		int buttonsWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
		int buttonX = panelX + (PANEL_WIDTH - buttonsWidth) / 2;
		int buttonY = panelY + PANEL_HEIGHT - 28;
		saveButton.setPosition(buttonX, buttonY);
		cancelButton.setPosition(buttonX + BUTTON_WIDTH + BUTTON_GAP, buttonY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		context.fill(0, 0, width, height, 0xA0000000);
		int panelX = panelX();
		int panelY = panelY();
		context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xF0101010);
		context.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF555555);
		context.centeredText(textRenderer, title, width / 2, panelY + 12, CommonColors.WHITE);
		context.centeredText(
			textRenderer,
			Component.literal("\\" + command).withStyle(ChatFormatting.GRAY),
			width / 2,
			panelY + 28,
			CommonColors.LIGHT_GRAY
		);
		context.centeredText(
			textRenderer,
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_HINT).withStyle(ChatFormatting.GRAY),
			width / 2,
			panelY + 48,
			CommonColors.LIGHT_GRAY
		);
		Component preview = draft == null
			? Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_UNSET).withStyle(ChatFormatting.DARK_GRAY)
			: Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_PREVIEW, draft.displayName()).withStyle(ChatFormatting.YELLOW);
		context.centeredText(textRenderer, preview, width / 2, panelY + 78, CommonColors.WHITE);
		super.extractRenderState(context, mouseX, mouseY, deltaTicks);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (input.isEscape()) {
			onClose();
			return true;
		}
		if (input.isConfirmation()) {
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
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
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
				Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DUPLICATE_TITLE),
				Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DUPLICATE_MESSAGE, duplicate.command()),
				Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_REPLACE_KEYBIND),
				CommonComponents.GUI_CANCEL
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
