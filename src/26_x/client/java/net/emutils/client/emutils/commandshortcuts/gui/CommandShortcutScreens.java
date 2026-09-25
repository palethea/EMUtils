package net.emutils.client.emutils.commandshortcuts.gui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.screens.Screen;

/** Opens Command Shortcuts: the new UI when its dev-only preview is on (#131), the classic list otherwise. */
public final class CommandShortcutScreens {
	private CommandShortcutScreens() {
	}

	public static Screen list(Screen parent) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().settingsUiPreview()) {
			return new CommandShortcutsScreen(parent);
		}
		return new CommandShortcutListScreen(parent);
	}
}
