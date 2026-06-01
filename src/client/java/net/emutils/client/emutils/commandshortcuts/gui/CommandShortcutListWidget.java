package net.emutils.client.emutils.commandshortcuts.gui;

import java.util.List;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.emutils.client.emutils.commandshortcuts.CommandShortcut;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;

public final class CommandShortcutListWidget extends AlwaysSelectedEntryListWidget<CommandShortcutListWidget.Entry> {
	private static final int ROW_HEIGHT = 56;
	private final CommandShortcutListScreen screen;

	public CommandShortcutListWidget(CommandShortcutListScreen screen, MinecraftClient client, int width, int height) {
		super(client, width, height, 0, ROW_HEIGHT);
		this.screen = screen;
		centerListVertically = false;
	}

	public void refreshEntries() {
		clearEntries();
		List<CommandShortcut> shortcuts = EMUtilsClient.commandShortcuts().store().shortcuts();
		if (shortcuts.isEmpty()) {
			addEntry(new EmptyEntry(client));
			return;
		}

		for (CommandShortcut shortcut : shortcuts) {
			addEntry(new ShortcutEntry(screen, client, shortcut));
		}
	}

	@Override
	public int getRowWidth() {
		return Math.min(width - 40, 560);
	}

	abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
	}

	private static final class EmptyEntry extends Entry {
		private final MinecraftClient client;

		private EmptyEntry(MinecraftClient client) {
			this.client = client;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			Text text = Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_NONE).formatted(Formatting.GRAY);
			int x = getContentMiddleX() - client.textRenderer.getWidth(text) / 2;
			context.drawTextWithShadow(client.textRenderer, text, x, getContentMiddleY() - 4, Colors.LIGHT_GRAY);
		}

		@Override
		public Text getNarration() {
			return Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_NONE);
		}
	}

	private static final class ShortcutEntry extends Entry {
		private static final int BUTTON_WIDTH = 92;
		private final CommandShortcutListScreen screen;
		private final MinecraftClient client;
		private final CommandShortcut shortcut;
		private final ButtonWidget editButton;
		private final ButtonWidget removeButton;

		private ShortcutEntry(CommandShortcutListScreen screen, MinecraftClient client, CommandShortcut shortcut) {
			this.screen = screen;
			this.client = client;
			this.shortcut = shortcut;
			editButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_ACTION_EDIT), button -> editShortcut())
				.size(BUTTON_WIDTH, 20)
				.build();
			removeButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_ACTION_DELETE), button -> removeShortcut())
				.size(BUTTON_WIDTH, 20)
				.build();
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int x = getContentX();
			int y = getContentY() + 3;
			int textWidth = Math.max(40, getContentRightEnd() - x - BUTTON_WIDTH - 18);

			int markerColor = shortcut.isCommand() ? 0xFF55FFFF : 0xFFFFFF55;
			context.fill(x, y + 2, x + 8, y + 10, markerColor);

			String name = client.textRenderer.trimToWidth(shortcut.displayName(), textWidth);
			context.drawTextWithShadow(client.textRenderer, Text.literal(name).formatted(Formatting.WHITE), x + 12, y, Colors.WHITE);

			Text command = Text.translatable(
				EMUtilsTexts.COMMAND_SHORTCUT_COMMAND_VALUE,
				client.textRenderer.trimToWidth(shortcut.displayText(), Math.max(20, textWidth - 64))
			).formatted(Formatting.AQUA);
			Text key = Text.translatable(EMUtilsTexts.COMMAND_SHORTCUT_KEY_VALUE, shortcut.keyCombo().displayName()).formatted(Formatting.GRAY);
			context.drawTextWithShadow(client.textRenderer, command, x + 12, y + 13, Colors.WHITE);
			context.drawTextWithShadow(client.textRenderer, key, x + 12, y + 28, Colors.LIGHT_GRAY);

			int buttonX = getContentRightEnd() - BUTTON_WIDTH;
			editButton.setPosition(buttonX, y);
			removeButton.setPosition(buttonX, y + 28);
			editButton.render(context, mouseX, mouseY, deltaTicks);
			removeButton.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			return editButton.mouseClicked(click, doubled)
				|| removeButton.mouseClicked(click, doubled);
		}

		@Override
		public boolean mouseReleased(Click click) {
			return editButton.mouseReleased(click)
				|| removeButton.mouseReleased(click);
		}

		@Override
		public void forEachChild(Consumer<ClickableWidget> consumer) {
			consumer.accept(editButton);
			consumer.accept(removeButton);
		}

		@Override
		public Text getNarration() {
			return Text.literal(shortcut.displayName() + " " + shortcut.keyCombo().displayName());
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
