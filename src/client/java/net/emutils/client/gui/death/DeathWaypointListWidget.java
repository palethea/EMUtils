package net.emutils.client.gui.death;

import java.util.List;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.death.DeathLocation;
import net.emutils.client.death.DeathWaypointCoordinates;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;

public final class DeathWaypointListWidget extends AlwaysSelectedEntryListWidget<DeathWaypointListWidget.Entry> {
	private static final int ROW_HEIGHT = 72;
	private final DeathWaypointListScreen screen;

	public DeathWaypointListWidget(DeathWaypointListScreen screen, MinecraftClient client, int width, int height) {
		super(client, width, height, 0, ROW_HEIGHT);
		this.screen = screen;
		centerListVertically = false;
	}

	public void refreshEntries() {
		clearEntries();
		List<DeathLocation> deaths = EMUtilsClient.deathWaypoint().deathsForCurrentWorld(client);
		if (deaths.isEmpty()) {
			addEntry(new EmptyEntry(client));
			return;
		}

		for (int index = 0; index < deaths.size(); index++) {
			addEntry(new WaypointEntry(screen, client, deaths.get(index), index));
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
			Text text = Text.translatable(EMUtilsTexts.DEATH_NONE_WORLD).formatted(Formatting.GRAY);
			int x = getContentMiddleX() - client.textRenderer.getWidth(text) / 2;
			context.drawTextWithShadow(client.textRenderer, text, x, getContentMiddleY() - 4, Colors.LIGHT_GRAY);
		}

		@Override
		public Text getNarration() {
			return Text.translatable(EMUtilsTexts.DEATH_NONE_WORLD);
		}
	}

	private static final class WaypointEntry extends Entry {
		private static final int BUTTON_WIDTH = 92;
		private final DeathWaypointListScreen screen;
		private final MinecraftClient client;
		private final DeathLocation location;
		private final int index;
		private final ButtonWidget copyButton;
		private final ButtonWidget removeButton;

		private WaypointEntry(DeathWaypointListScreen screen, MinecraftClient client, DeathLocation location, int index) {
			this.screen = screen;
			this.client = client;
			this.location = location;
			this.index = index;
			copyButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.DEATH_ACTION_COPY_COORDS), button -> copyCoordinates())
				.size(BUTTON_WIDTH, 20)
				.build();
			removeButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.DEATH_ACTION_REMOVE), button -> removeWaypoint())
				.size(BUTTON_WIDTH, 20)
				.build();
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int x = getContentX();
			int y = getContentY() + 3;
			Text label = index == 0
				? Text.translatable(EMUtilsTexts.DEATH_LABEL_LAST)
				: Text.translatable(EMUtilsTexts.DEATH_LABEL_NUMBERED, index + 1);
			context.drawTextWithShadow(client.textRenderer, label, x, y, Colors.WHITE);
			context.drawTextWithShadow(
				client.textRenderer,
				Text.literal(DeathWaypointCoordinates.plain(location)).formatted(Formatting.GRAY),
				x,
				y + 13,
				Colors.LIGHT_GRAY
			);
			context.drawTextWithShadow(
				client.textRenderer,
				Text.literal(location.dimension()).formatted(Formatting.DARK_GRAY),
				x,
				y + 26,
				Colors.GRAY
			);
			if (client.player != null && client.world != null) {
				context.drawTextWithShadow(
					client.textRenderer,
					Text.translatable(EMUtilsTexts.DEATH_DISTANCE, EMUtilsClient.deathWaypoint().distanceBlocks(client, location)).formatted(Formatting.AQUA),
					x,
					y + 39,
					Colors.WHITE
				);
			}

			int buttonX = getContentRightEnd() - BUTTON_WIDTH;
			copyButton.setPosition(buttonX, y);
			removeButton.setPosition(buttonX, y + 24);
			copyButton.render(context, mouseX, mouseY, deltaTicks);
			removeButton.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			return copyButton.mouseClicked(click, doubled) || removeButton.mouseClicked(click, doubled);
		}

		@Override
		public boolean mouseReleased(Click click) {
			return copyButton.mouseReleased(click) || removeButton.mouseReleased(click);
		}

		@Override
		public void forEachChild(Consumer<ClickableWidget> consumer) {
			consumer.accept(copyButton);
			consumer.accept(removeButton);
		}

		@Override
		public Text getNarration() {
			return Text.literal(DeathWaypointCoordinates.plain(location));
		}

		private void copyCoordinates() {
			client.keyboard.setClipboard(DeathWaypointCoordinates.plain(location));
			if (client.inGameHud != null) {
				client.inGameHud.getChatHud().addMessage(Text.translatable(EMUtilsTexts.DEATH_COORDS_COPIED).formatted(Formatting.GREEN));
			}
		}

		private void removeWaypoint() {
			EMUtilsClient.deathWaypoint().clear(client, location.deathTimestamp());
			screen.refreshList();
		}
	}
}
