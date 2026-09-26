package net.emutils.client.emutils.gui.ui;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * EMUtils UI screens that were closed back into the game but are still fading out. The screen is
 * already gone, so the player can look around and move straight away, and its last fade is drawn on top
 * of the HUD instead.
 */
public final class UiClosingScreens {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "closing_screen");
	/** Longer than any close animation; a fade that isn't drawn (with the HUD hidden, say) is dropped after this. */
	private static final long MAX_NANOS = 1_000_000_000L;
	private static final List<Entry> SCREENS = new ArrayList<>();

	private UiClosingScreens() {
	}

	public static void register() {
		HudElementRegistry.addLast(ID, (context, tickCounter) -> render(context));
	}

	static void add(UiPanelScreen screen) {
		SCREENS.add(new Entry(screen, System.nanoTime()));
	}

	/** Drops any fading screens, for example when a new one opens. */
	static void clear() {
		if (!SCREENS.isEmpty()) {
			for (Entry entry : SCREENS) {
				entry.screen().finishFadingOnHud();
			}
			SCREENS.clear();
			UiBlur.reset();
		}
	}

	private static void render(GuiGraphicsExtractor context) {
		if (SCREENS.isEmpty()) {
			return;
		}
		long now = System.nanoTime();
		SCREENS.removeIf(entry -> {
			if (now - entry.startNanos() > MAX_NANOS || !entry.screen().extractClosingFrame(context)) {
				entry.screen().finishFadingOnHud();
				return true;
			}
			return false;
		});
		if (SCREENS.isEmpty()) {
			UiBlur.reset();
		}
	}

	private record Entry(UiPanelScreen screen, long startNanos) {
	}
}
