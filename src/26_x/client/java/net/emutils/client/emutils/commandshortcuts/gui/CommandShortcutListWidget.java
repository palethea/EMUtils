package net.emutils.client.emutils.commandshortcuts.gui;

import java.util.List;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.commandshortcuts.CommandShortcut;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;

public final class CommandShortcutListWidget extends ObjectSelectionList<CommandShortcutListWidget.Entry> {
	private static final int ROW_HEIGHT = 56;
	private final CommandShortcutListScreen screen;

	public CommandShortcutListWidget(CommandShortcutListScreen screen, Minecraft client, int width, int height) {
		super(client, width, height, 0, ROW_HEIGHT);
		this.screen = screen;
		centerListVertically = false;
	}

	public void refreshEntries() {
		clearEntries();
		List<CommandShortcut> shortcuts = EMUtilsClient.commandShortcuts().store().shortcuts();
		if (shortcuts.isEmpty()) {
			addEntry(new EmptyEntry(minecraft));
			return;
		}

		for (CommandShortcut shortcut : shortcuts) {
			addEntry(new ShortcutEntry(screen, minecraft, shortcut));
		}
	}

	@Override
	public int getRowWidth() {
		return Math.min(width - 40, 560);
	}

	abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
	}

	private static final class EmptyEntry extends Entry {
		private final Minecraft client;

		private EmptyEntry(Minecraft client) {
			this.client = client;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			Component text = Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_NONE).withStyle(ChatFormatting.GRAY);
			int x = getContentXMiddle() - client.font.width(text) / 2;
			context.text(client.font, text, x, getContentYMiddle() - 4, CommonColors.LIGHT_GRAY);
		}

		@Override
		public Component getNarration() {
			return Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_NONE);
		}
	}

	private static final class ShortcutEntry extends Entry {
		private static final int BUTTON_WIDTH = 92;
		private final CommandShortcutListScreen screen;
		private final Minecraft client;
		private final CommandShortcut shortcut;
		private final Button editButton;
		private final Button removeButton;

		private ShortcutEntry(CommandShortcutListScreen screen, Minecraft client, CommandShortcut shortcut) {
			this.screen = screen;
			this.client = client;
			this.shortcut = shortcut;
			editButton = Button.builder(Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_ACTION_EDIT), button -> editShortcut())
				.size(BUTTON_WIDTH, 20)
				.build();
			removeButton = Button.builder(Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_ACTION_DELETE), button -> removeShortcut())
				.size(BUTTON_WIDTH, 20)
				.build();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int x = getContentX();
			int y = getContentY() + 3;
			int textWidth = Math.max(40, getContentRight() - x - BUTTON_WIDTH - 18);

			int markerColor = shortcut.isCommand() ? 0xFF55FFFF : 0xFFFFFF55;
			context.fill(x, y + 2, x + 8, y + 10, markerColor);

			String name = client.font.plainSubstrByWidth(shortcut.displayName(), textWidth);
			context.text(client.font, Component.literal(name).withStyle(ChatFormatting.WHITE), x + 12, y, CommonColors.WHITE);

			Component command = Component.translatable(
				EMUtilsTexts.COMMAND_SHORTCUT_COMMAND_VALUE,
				client.font.plainSubstrByWidth(shortcut.displayText(), Math.max(20, textWidth - 64))
			).withStyle(ChatFormatting.AQUA);
			Component key = Component.translatable(EMUtilsTexts.COMMAND_SHORTCUT_KEY_VALUE, shortcut.keyCombo().displayName()).withStyle(ChatFormatting.GRAY);
			context.text(client.font, command, x + 12, y + 13, CommonColors.WHITE);
			context.text(client.font, key, x + 12, y + 28, CommonColors.LIGHT_GRAY);

			int buttonX = getContentRight() - BUTTON_WIDTH;
			editButton.setPosition(buttonX, y);
			removeButton.setPosition(buttonX, y + 28);
			editButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
			removeButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
			return editButton.mouseClicked(click, doubled)
				|| removeButton.mouseClicked(click, doubled);
		}

		@Override
		public boolean mouseReleased(MouseButtonEvent click) {
			return editButton.mouseReleased(click)
				|| removeButton.mouseReleased(click);
		}

		@Override
		public void visitWidgets(Consumer<AbstractWidget> consumer) {
			consumer.accept(editButton);
			consumer.accept(removeButton);
		}

		@Override
		public Component getNarration() {
			return Component.literal(shortcut.displayName() + " " + shortcut.keyCombo().displayName());
		}

		private void editShortcut() {
			client.setScreen(new CommandShortcutEditScreen(screen, shortcut));
		}

		private void removeShortcut() {
			EMUtilsClient.commandShortcuts().store().remove(shortcut.id());
			EMUtilsClient.commandShortcuts().reload();
			screen.refreshList();
		}
	}
}
