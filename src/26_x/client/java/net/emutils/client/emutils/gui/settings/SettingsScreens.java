package net.emutils.client.emutils.gui.settings;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.minecraft.client.gui.screens.Screen;

/** Opens the EMUtils settings: the new UI when its dev-only preview is on, the current hub otherwise. */
public final class SettingsScreens {
	private SettingsScreens() {
	}

	public static Screen hub(Screen parent) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().settingsUiPreview()) {
			return new SettingsScreen(parent);
		}
		return new CustomHubScreen(parent);
	}
}
