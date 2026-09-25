package net.emutils.client.emutils.minescript.gui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.screens.Screen;

/** Opens the Script Manager: the new UI when its dev-only preview is on (#118), the classic one otherwise. */
public final class ScriptScreens {
	private ScriptScreens() {
	}

	public static Screen manager(Screen parent) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().settingsUiPreview()) {
			return new ScriptsScreen(parent);
		}
		return new ScriptManagerScreen(parent);
	}
}