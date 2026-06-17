package net.emutils.client.emutils.waypoint.gui;

import java.util.List;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.WaypointCoordinates;
import net.emutils.client.emutils.waypoint.WaypointType;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;

public final class WaypointListWidget extends ObjectSelectionList<WaypointListWidget.Entry> {
	private static final int ROW_HEIGHT = 72;
	private final WaypointListScreen screen;

	public WaypointListWidget(WaypointListScreen screen, Minecraft client, int width, int height) {
		super(client, width, height, 0, ROW_HEIGHT);
		this.screen = screen;
		centerListVertically = false;
	}

	public void refreshEntries() {
		clearEntries();
		List<Waypoint> waypoints = EMUtilsClient.waypoint().waypointsForCurrentWorld(minecraft);
		if (waypoints.isEmpty()) {
			addEntry(new EmptyEntry(minecraft));
			return;
		}

		for (Waypoint waypoint : waypoints) {
			addEntry(new WaypointEntry(screen, minecraft, waypoint));
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
			Component text = Component.translatable(EMUtilsTexts.WAYPOINT_NONE_WORLD).withStyle(ChatFormatting.GRAY);
			int x = getContentXMiddle() - client.font.width(text) / 2;
			context.text(client.font, text, x, getContentYMiddle() - 4, CommonColors.LIGHT_GRAY);
		}

		@Override
		public Component getNarration() {
			return Component.translatable(EMUtilsTexts.WAYPOINT_NONE_WORLD);
		}
	}

	private static final class WaypointEntry extends Entry {
		private static final int BUTTON_WIDTH = 92;
		private final WaypointListScreen screen;
		private final Minecraft client;
		private final Waypoint waypoint;
		private final Button copyButton;
		private final Button visibilityButton;
		private final Button beaconButton;
		private final Button removeButton;

		private WaypointEntry(WaypointListScreen screen, Minecraft client, Waypoint waypoint) {
			this.screen = screen;
			this.client = client;
			this.waypoint = waypoint;
			copyButton = Button.builder(Component.translatable(EMUtilsTexts.WAYPOINT_ACTION_COPY_COORDS), button -> copyCoordinates())
				.size(BUTTON_WIDTH, 20)
				.build();
			visibilityButton = Button.builder(visibilityLabel(), button -> toggleVisibility())
				.size(BUTTON_WIDTH, 20)
				.build();
			beaconButton = Button.builder(beaconLabel(), button -> toggleBeacon())
				.size(BUTTON_WIDTH, 20)
				.build();
			removeButton = Button.builder(Component.translatable(EMUtilsTexts.WAYPOINT_ACTION_REMOVE), button -> removeWaypoint())
				.size(BUTTON_WIDTH, 20)
				.build();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int x = getContentX();
			int y = getContentY() + 3;

			int colorDot = 0xFF000000 | (waypoint.color() & 0x00FFFFFF);
			context.fill(x, y + 2, x + 8, y + 10, colorDot);

			Component typeLabel = Component.translatable(waypoint.type().labelKey()).withStyle(ChatFormatting.DARK_GRAY);
			Component label = Component.literal(waypoint.label()).withStyle(ChatFormatting.WHITE);
			context.text(client.font, label, x + 12, y, CommonColors.WHITE);
			context.text(client.font, typeLabel, x + 12 + client.font.width(label) + 6, y + 1, CommonColors.GRAY);

			context.text(
				client.font,
				Component.literal(WaypointCoordinates.plain(waypoint)).withStyle(ChatFormatting.GRAY),
				x + 12,
				y + 13,
				CommonColors.LIGHT_GRAY
			);
			context.text(
				client.font,
				Component.literal(waypoint.dimension()).withStyle(ChatFormatting.DARK_GRAY),
				x + 12,
				y + 26,
				CommonColors.GRAY
			);
			if (client.player != null && client.level != null) {
				context.text(
					client.font,
					Component.translatable(EMUtilsTexts.WAYPOINT_DISTANCE, EMUtilsClient.waypoint().distanceBlocks(client, waypoint)).withStyle(ChatFormatting.AQUA),
					x + 12,
					y + 39,
					CommonColors.WHITE
				);
			}

			int rightButtonX = getContentRight() - BUTTON_WIDTH;
			int leftButtonX = rightButtonX - BUTTON_WIDTH - 4;
			copyButton.setPosition(leftButtonX, y);
			visibilityButton.setPosition(rightButtonX, y);
			beaconButton.setPosition(leftButtonX, y + 24);
			removeButton.setPosition(rightButtonX, y + 24);
			copyButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
			visibilityButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
			beaconButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
			removeButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
			return copyButton.mouseClicked(click, doubled)
				|| visibilityButton.mouseClicked(click, doubled)
				|| beaconButton.mouseClicked(click, doubled)
				|| removeButton.mouseClicked(click, doubled);
		}

		@Override
		public boolean mouseReleased(MouseButtonEvent click) {
			return copyButton.mouseReleased(click)
				|| visibilityButton.mouseReleased(click)
				|| beaconButton.mouseReleased(click)
				|| removeButton.mouseReleased(click);
		}

		@Override
		public void visitWidgets(Consumer<AbstractWidget> consumer) {
			consumer.accept(copyButton);
			consumer.accept(visibilityButton);
			consumer.accept(beaconButton);
			consumer.accept(removeButton);
		}

		@Override
		public Component getNarration() {
			return Component.literal(waypoint.label() + " " + WaypointCoordinates.plain(waypoint));
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

		private void toggleVisibility() {
			EMUtilsClient.waypoint().toggleHidden(waypoint.timestamp());
			visibilityButton.setMessage(visibilityLabel());
		}

		private Component visibilityLabel() {
			return Component.translatable(waypoint.hidden()
				? EMUtilsTexts.WAYPOINT_ACTION_SHOW
				: EMUtilsTexts.WAYPOINT_ACTION_HIDE);
		}

		private Component beaconLabel() {
			String key = waypoint.beaconEnabled() ? "emutils.waypoint.beacon_on" : "emutils.waypoint.beacon_off";
			return Component.translatable(key);
		}
	}
}
