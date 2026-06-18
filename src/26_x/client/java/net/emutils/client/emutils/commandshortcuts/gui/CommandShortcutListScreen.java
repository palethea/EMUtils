package net.emutils.client.emutils.commandshortcuts.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class CommandShortcutListScreen extends Screen {
	private final Screen parent;
	private HeaderAndFooterLayout layout;
	private CommandShortcutListWidget list;
	private Button clearButton;

	public CommandShortcutListScreen(Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_COMMAND_SHORTCUTS));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this);
		layout.addTitleHeader(title, font);

		LinearLayout footer = LinearLayout.horizontal().spacing(8);
		footer.addChild(Button.builder(Component.translatable(EMUtilsTexts.OPTION_ADD_COMMAND_SHORTCUT), button -> {
			minecraft.setScreenAndShow(new CommandShortcutEditScreen(this, null));
		}).width(150).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(100).build());
		clearButton = footer.addChild(Button.builder(Component.translatable(EMUtilsTexts.OPTION_CLEAR_COMMAND_SHORTCUTS), button -> clearShortcuts()).width(150).build());
		layout.addToFooter(footer);
		layout.visitWidgets(this::addRenderableWidget);

		list = addRenderableWidget(new CommandShortcutListWidget(this, minecraft, width, height));
		refreshList();
		repositionElements();
	}

	void refreshList() {
		if (list != null) {
			list.refreshEntries();
		}
		if (clearButton != null) {
			clearButton.active = !EMUtilsClient.commandShortcuts().store().shortcuts().isEmpty();
		}
	}

	@Override
	protected void repositionElements() {
		if (layout == null) {
			return;
		}

		layout.arrangeElements();
		if (list != null) {
			list.updateSizeAndPosition(width, layout.getContentHeight(), 0, layout.getHeaderHeight());
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	private void clearShortcuts() {
		minecraft.setScreenAndShow(new ConfirmScreen(
			confirmed -> {
				minecraft.setScreenAndShow(this);
				if (confirmed) {
					EMUtilsClient.commandShortcuts().store().clear();
					EMUtilsClient.commandShortcuts().reload();
					refreshList();
				}
			},
			Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_CLEAR_TITLE),
			Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_CLEAR_MESSAGE),
			Component.translatable(EMUtilsTexts.OPTION_CLEAR_COMMAND_SHORTCUTS),
			CommonComponents.GUI_CANCEL
		));
	}
}
