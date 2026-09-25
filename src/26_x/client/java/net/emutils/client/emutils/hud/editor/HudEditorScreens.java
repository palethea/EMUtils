package net.emutils.client.emutils.hud.editor;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/** Opens the HUD Layout Editor: the new UI when its dev-only preview is on (#136), the classic one otherwise. */
public final class HudEditorScreens {
	private HudEditorScreens() {
	}

	public static Screen editor(@Nullable Screen parent) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().settingsUiPreview()) {
			return new HudEditorScreen(parent);
		}
		return new HudLayoutEditorScreen(parent);
	}
}
