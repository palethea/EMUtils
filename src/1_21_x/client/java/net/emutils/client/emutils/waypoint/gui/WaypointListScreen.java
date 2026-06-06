package net.emutils.client.emutils.waypoint.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public final class WaypointListScreen extends Screen {
	private final Screen parent;
	private ThreePartsLayoutWidget layout;
	private WaypointListWidget list;
	private ButtonWidget clearButton;

	public WaypointListScreen(Screen parent) {
		super(Text.translatable(EMUtilsTexts.SCREEN_CURRENT_WAYPOINTS));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout = new ThreePartsLayoutWidget(this);
		layout.addHeader(title, textRenderer);

		DirectionalLayoutWidget footer = DirectionalLayoutWidget.horizontal().spacing(8);
		footer.add(ButtonWidget.builder(Text.translatable(EMUtilsTexts.OPTION_ADD_WAYPOINT), button -> {
			client.setScreen(new AddWaypointScreen(this));
		}).width(150).build());
		footer.add(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(100).build());
		clearButton = footer.add(ButtonWidget.builder(Text.translatable(EMUtilsTexts.OPTION_CLEAR_WAYPOINTS), button -> {
			EMUtilsClient.waypoint().clearForCurrentWorld(client);
			refreshList();
		}).width(150).build());
		layout.addFooter(footer);
		layout.forEachChild(this::addDrawableChild);

		list = addDrawableChild(new WaypointListWidget(this, client, width, height));
		refreshList();
		refreshWidgetPositions();
	}

	void refreshList() {
		if (list != null) {
			list.refreshEntries();
		}
		if (clearButton != null) {
			clearButton.active = EMUtilsClient.waypoint().hasWaypointForCurrentWorld(client);
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
}
