package net.emutils.client.emutils.waypoint.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class WaypointListScreen extends Screen {
	private final Screen parent;
	private HeaderAndFooterLayout layout;
	private WaypointListWidget list;
	private Button clearButton;

	public WaypointListScreen(Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_CURRENT_WAYPOINTS));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this);
		layout.addTitleHeader(title, font);

		LinearLayout footer = LinearLayout.horizontal().spacing(8);
		footer.addChild(Button.builder(Component.translatable(EMUtilsTexts.OPTION_ADD_WAYPOINT), button -> {
			minecraft.setScreenAndShow(new AddWaypointScreen(this));
		}).width(150).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(100).build());
		clearButton = footer.addChild(Button.builder(Component.translatable(EMUtilsTexts.OPTION_CLEAR_WAYPOINTS), button -> {
			EMUtilsClient.waypoint().clearForCurrentWorld(minecraft);
			refreshList();
		}).width(150).build());
		layout.addToFooter(footer);
		layout.visitWidgets(this::addRenderableWidget);

		list = addRenderableWidget(new WaypointListWidget(this, minecraft, width, height));
		refreshList();
		repositionElements();
	}

	void refreshList() {
		if (list != null) {
			list.refreshEntries();
		}
		if (clearButton != null) {
			clearButton.active = EMUtilsClient.waypoint().hasWaypointForCurrentWorld(minecraft);
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
}
