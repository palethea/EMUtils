package net.emutils.client.emutils.waypoint.gui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.screens.Screen;

/** Opens the waypoint screens: the new UI when its dev-only preview is on (#103), the classic ones otherwise. */
public final class WaypointScreens {
	private WaypointScreens() {
	}

	private static boolean newUi() {
		return EMUtilsClient.config() != null && EMUtilsClient.config().settingsUiPreview();
	}

	public static Screen list(Screen parent) {
		return newUi() ? new WaypointsScreen(parent) : new WaypointListScreen(parent);
	}

	/** The Add Waypoint form on its own, as the Add Waypoint keybind opens it. */
	public static Screen add(Screen parent) {
		return newUi() ? WaypointsScreen.addWaypoint(parent) : new AddWaypointScreen(parent);
	}
}
