package net.emutils.client.emutils.commandshortcuts.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public final class CommandShortcutListScreen extends Screen {
	private final Screen parent;
	private ThreePartsLayoutWidget layout;
	private CommandShortcutListWidget list;
	private ButtonWidget clearButton;

	public CommandShortcutListScreen(Screen parent) {
		super(Text.translatable(EMUtilsTexts.SCREEN_COMMAND_SHORTCUTS));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout = new ThreePartsLayoutWidget(this);
		layout.addHeader(title, textRenderer);

		DirectionalLayoutWidget footer = DirectionalLayoutWidget.horizontal().spacing(8);
		footer.add(ButtonWidget.builder(Text.translatable(EMUtilsTexts.OPTION_ADD_COMMAND_SHORTCUT), button -> {
			client.setScreen(new CommandShortcutEditScreen(this, null));
		}).width(150).build());
		footer.add(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(100).build());
		clearButton = footer.add(ButtonWidget.builder(Text.translatable(EMUtilsTexts.OPTION_CLEAR_COMMAND_SHORTCUTS), button -> clearShortcuts()).width(150).build());
		layout.addFooter(footer);
		layout.forEachChild(this::addDrawableChild);

		list = addDrawableChild(new CommandShortcutListWidget(this, client, width, height));
		refreshList();
		refreshWidgetPositions();
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
	protected void refreshWidgetPositions() {
		if (layout == null) {
			return;
		}

		layout.refreshPositions();
		if (list != null) {
			list.position(width, layout.getContentHeight(), 0, layout.getHeaderHeight());
		}
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void clearShortcuts() {
		client.setScreen(new ConfirmScreen(
			confirmed -> {
				client.setScreen(this);
				if (confirmed) {
					EMUtilsClient.commandShortcuts().store().clear();
					EMUtilsClient.commandShortcuts().reload();
					refreshList();
				}
			},
			Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_CLEAR_TITLE),
			Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_CLEAR_MESSAGE),
			Text.translatable(EMUtilsTexts.OPTION_CLEAR_COMMAND_SHORTCUTS),
			ScreenTexts.CANCEL
		));
	}
}
