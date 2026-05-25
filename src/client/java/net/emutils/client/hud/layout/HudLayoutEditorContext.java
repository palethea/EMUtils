package net.emutils.client.hud.layout;

import net.emutils.client.gui.hud.HudLayoutEditorScreen;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class HudLayoutEditorContext {
	private HudLayoutEditorContext() {
	}

	public static boolean isActive(@Nullable MinecraftClient client) {
		return client != null && client.currentScreen instanceof HudLayoutEditorScreen;
	}

	public static void beginVanillaHudDim() {
		HudLayoutEditorVanillaDim.begin();
	}

	public static void endVanillaHudDim() {
		HudLayoutEditorVanillaDim.end();
	}
}
