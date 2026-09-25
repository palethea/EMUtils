package net.emutils.client.emutils.packs.gui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.screens.Screen;

/** Opens the Pack Manager: the new UI when its dev-only preview is on (#110), the classic one otherwise. */
public final class PackScreens {
	private PackScreens() {
	}

	public static Screen manager(Screen parent) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().settingsUiPreview()) {
			return new PacksScreen(parent);
		}
		return new PackManagerScreen(parent);
	}
}
