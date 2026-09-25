package net.emutils.client.emutils.inventory.gui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/** Opens the Mass Drop list: the new UI when its dev-only preview is on (#134), the classic one otherwise. */
public final class MassDropScreens {
	private MassDropScreens() {
	}

	public static Screen list(@Nullable Screen parent) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().settingsUiPreview()) {
			return new MassDropItemsScreen(parent);
		}
		return new MassDropScreen(parent);
	}
}
