package net.emutils.client.emutils.gui.ui;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * EMUtils UI screens that were closed but are still fading out. The screen is already gone, so the
 * player can look around and move, or use the vanilla screen it went back to, straight away. Its last
 * fade is drawn on top of the HUD, or on top of that vanilla screen (#164).
 */
public final class UiClosingScreens {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "closing_screen");
	/** Longer than any close animation; a fade that isn't drawn (with the HUD hidden, say) is dropped after this. */
	private static final long MAX_NANOS = 1_000_000_000L;
	private static final List<Entry> SCREENS = new ArrayList<>();

	private UiClosingScreens() {
	}

	public static void register() {
		HudElementRegistry.addLast(ID, (context, tickCounter) -> renderOnHud(context));
		// Fabric recreates a screen's events whenever it's initialized again, such as on a window resize,
		// so the hook that draws fades over a screen is added every time, not once when the fade starts.
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
			ScreenEvents.afterExtract(screen).register((shown, context, mouseX, mouseY, delta) -> renderOver(shown, context))
		);
	}

	/** Fades {@code screen} out on the HUD, after closing it into the game. */
	static void add(UiPanelScreen screen) {
		SCREENS.add(new Entry(screen, null, System.nanoTime()));
	}

	/** Fades {@code screen} out over {@code over}, the vanilla screen it went back to. */
	static void addOver(UiPanelScreen screen, Screen over) {
		SCREENS.add(new Entry(screen, over, System.nanoTime()));
	}

	/** Drops any fading screens, for example when a new one opens. */
	static void clear() {
		if (!SCREENS.isEmpty()) {
			for (Entry entry : SCREENS) {
				entry.screen().finishFadingOut();
			}
			SCREENS.clear();
			UiBlur.reset();
		}
	}

	private static void renderOnHud(GuiGraphicsExtractor context) {
		if (SCREENS.stream().noneMatch(entry -> entry.over() == null)) {
			return;
		}
		// Another screen opened during the fade, such as the pause menu: it covers the fade, and its own
		// background blur would be a second blur in this frame, which Minecraft doesn't allow.
		if (MinecraftClientCompat.screen(Minecraft.getInstance()) != null) {
			SCREENS.removeIf(entry -> {
				if (entry.over() == null) {
					entry.screen().finishFadingOut();
					return true;
				}
				return false;
			});
			UiBlur.reset();
			return;
		}
		long now = System.nanoTime();
		SCREENS.removeIf(entry -> {
			if (entry.over() != null) {
				return false;
			}
			if (now - entry.startNanos() > MAX_NANOS || !entry.screen().extractClosingFrame(context)) {
				entry.screen().finishFadingOut();
				return true;
			}
			return false;
		});
		if (SCREENS.stream().noneMatch(entry -> entry.over() == null)) {
			UiBlur.reset();
		}
	}

	/**
	 * Draws the fades over {@code shown}, after it drew itself. Fades over a screen that's no longer shown
	 * are dropped, since whatever replaced it covers them.
	 */
	private static void renderOver(Screen shown, GuiGraphicsExtractor context) {
		if (SCREENS.isEmpty()) {
			return;
		}
		long now = System.nanoTime();
		SCREENS.removeIf(entry -> {
			if (entry.over() == null) {
				return false;
			}
			boolean done = entry.over() != shown
				|| now - entry.startNanos() > MAX_NANOS
				|| !entry.screen().extractFadeOver(context);
			if (done) {
				entry.screen().finishFadingOut();
			}
			return done;
		});
	}

	private record Entry(UiPanelScreen screen, @Nullable Screen over, long startNanos) {
	}
}
