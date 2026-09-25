package net.emutils.client.emutils.hud.layout;

import net.emutils.client.emutils.hud.editor.HudEditorScreen;
import net.emutils.client.emutils.hud.editor.HudLayoutEditorScreen;
import net.emutils.client.emutils.hud.editor.HudLayoutEditorOverlay;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

public final class HudLayoutEditorContext {
	private HudLayoutEditorContext() {
	}

	public static boolean isActive(@Nullable Minecraft client) {
		return client != null && (MinecraftClientCompat.screen(client) instanceof HudLayoutEditorScreen || MinecraftClientCompat.screen(client) instanceof HudEditorScreen || HudLayoutEditorOverlay.isActive());
	}

	public static void beginVanillaHudDim() {
		HudLayoutEditorVanillaDim.begin();
	}

	public static void endVanillaHudDim() {
		HudLayoutEditorVanillaDim.end();
	}
}
