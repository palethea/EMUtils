package net.emutils.client.emutils.waypoint.gui;

import java.util.List;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.WaypointCoordinates;
import net.emutils.client.emutils.waypoint.WaypointType;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;

public final class WaypointListWidget extends AlwaysSelectedEntryListWidget<WaypointListWidget.Entry> {
	private static final int ROW_HEIGHT = 72;
	private final WaypointListScreen screen;

	public WaypointListWidget(WaypointListScreen screen, MinecraftClient client, int width, int height) {
		super(client, width, height, 0, ROW_HEIGHT);
		this.screen = screen;
		centerListVertically = false;
	}

	public void refreshEntries() {
		clearEntries();
		List<Waypoint> waypoints = EMUtilsClient.waypoint().waypointsForCurrentWorld(client);
		if (waypoints.isEmpty()) {
			addEntry(new EmptyEntry(client));
			return;
		}

		for (Waypoint waypoint : waypoints) {
			addEntry(new WaypointEntry(screen, client, waypoint));
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
			Text text = Text.translatable(EMUtilsTexts.WAYPOINT_NONE_WORLD).formatted(Formatting.GRAY);
			int x = getContentMiddleX() - client.textRenderer.getWidth(text) / 2;
			context.drawTextWithShadow(client.textRenderer, text, x, getContentMiddleY() - 4, Colors.LIGHT_GRAY);
		}

		@Override
		public Text getNarration() {
			return Text.translatable(EMUtilsTexts.WAYPOINT_NONE_WORLD);
		}
	}

	private static final class WaypointEntry extends Entry {
		private static final int BUTTON_WIDTH = 92;
		private final WaypointListScreen screen;
		private final MinecraftClient client;
		private final Waypoint waypoint;
		private final ButtonWidget copyButton;
		private final ButtonWidget beaconButton;
		private final ButtonWidget removeButton;

		private WaypointEntry(WaypointListScreen screen, MinecraftClient client, Waypoint waypoint) {
			this.screen = screen;
			this.client = client;
			this.waypoint = waypoint;
			copyButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.WAYPOINT_ACTION_COPY_COORDS), button -> copyCoordinates())
				.size(BUTTON_WIDTH, 20)
				.build();
			beaconButton = ButtonWidget.builder(beaconLabel(), button -> toggleBeacon())
				.size(BUTTON_WIDTH, 20)
				.build();
			removeButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.WAYPOINT_ACTION_REMOVE), button -> removeWaypoint())
				.size(BUTTON_WIDTH, 20)
				.build();
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int x = getContentX();
			int y = getContentY() + 3;

			int colorDot = 0xFF000000 | (waypoint.color() & 0x00FFFFFF);
			context.fill(x, y + 2, x + 8, y + 10, colorDot);

			Text typeLabel = Text.translatable(waypoint.type().labelKey()).formatted(Formatting.DARK_GRAY);
			Text label = Text.literal(waypoint.label()).formatted(Formatting.WHITE);
			context.drawTextWithShadow(client.textRenderer, label, x + 12, y, Colors.WHITE);
			context.drawTextWithShadow(client.textRenderer, typeLabel, x + 12 + client.textRenderer.getWidth(label) + 6, y + 1, Colors.GRAY);

			context.drawTextWithShadow(
				client.textRenderer,
				Text.literal(WaypointCoordinates.plain(waypoint)).formatted(Formatting.GRAY),
				x + 12,
				y + 13,
				Colors.LIGHT_GRAY
			);
			context.drawTextWithShadow(
				client.textRenderer,
				Text.literal(waypoint.dimension()).formatted(Formatting.DARK_GRAY),
				x + 12,
				y + 26,
				Colors.GRAY
			);
			if (client.player != null && client.world != null) {
				context.drawTextWithShadow(
					client.textRenderer,
					Text.translatable(EMUtilsTexts.WAYPOINT_DISTANCE, EMUtilsClient.waypoint().distanceBlocks(client, waypoint)).formatted(Formatting.AQUA),
					x + 12,
					y + 39,
					Colors.WHITE
				);
			}

			int buttonX = getContentRightEnd() - BUTTON_WIDTH;
			copyButton.setPosition(buttonX, y);
			beaconButton.setPosition(buttonX, y + 24);
			removeButton.setPosition(buttonX, y + 48);
			copyButton.render(context, mouseX, mouseY, deltaTicks);
			beaconButton.render(context, mouseX, mouseY, deltaTicks);
			removeButton.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			return copyButton.mouseClicked(click, doubled)
				|| beaconButton.mouseClicked(click, doubled)
				|| removeButton.mouseClicked(click, doubled);
		}

		@Override
		public boolean mouseReleased(Click click) {
			return copyButton.mouseReleased(click)
				|| beaconButton.mouseReleased(click)
				|| removeButton.mouseReleased(click);
		}

		@Override
		public void forEachChild(Consumer<ClickableWidget> consumer) {
			consumer.accept(copyButton);
			consumer.accept(beaconButton);
			consumer.accept(removeButton);
		}

		@Override
		public Text getNarration() {
			return Text.literal(waypoint.label() + " " + WaypointCoordinates.plain(waypoint));
		}

		private void copyCoordinates() {
			EMUtilsClient.waypoint().copyCoordinates(client, waypoint.timestamp());
		}

		private void removeWaypoint() {
			EMUtilsClient.waypoint().clear(client, waypoint.timestamp());
			screen.refreshList();
		}

		private void toggleBeacon() {
			EMUtilsClient.waypoint().toggleBeacon(waypoint.timestamp());
			beaconButton.setMessage(beaconLabel());
		}

		private Text beaconLabel() {
			String key = waypoint.beaconEnabled() ? "emutils.waypoint.beacon_on" : "emutils.waypoint.beacon_off";
			return Text.translatable(key);
		}
	}
}
