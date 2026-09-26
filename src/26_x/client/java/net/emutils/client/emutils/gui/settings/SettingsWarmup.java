package net.emutils.client.emutils.gui.settings;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.ui.UiBlur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.Util;

/**
 * Renders the settings screen once, invisibly, right after the game has loaded, so its first real
 * open doesn't stall while every label, shape and icon is created (about 0.2 s otherwise). Those are
 * cached per GUI scale and cut to the window's width, so it runs again after the GUI scale or window size
 * changed. The drawing goes into a throwaway render state that is never shown.
 */
public final class SettingsWarmup {
	private static int preparedScale;
	private static int preparedWidth;
	private static int preparedHeight;
	/** How long a new window size has to stay before the screen is prepared for it again. */
	private static final long SETTLE_MILLIS = 500L;
	private static int pendingScale;
	private static int pendingWidth;
	private static int pendingHeight;
	private static long pendingSince;

	private SettingsWarmup() {
	}

	/** Call every client tick; it prepares the screen when needed, once loading has finished. */
	public static void tick(Minecraft client) {
		if (client.gui.overlay() != null || EMUtilsClient.config() == null) {
			return;
		}
		int scale = (int) Math.ceil(client.getWindow().getGuiScale());
		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		// Not while the screen is open: it's already rendered, and the preparation would stall it.
		if ((scale == preparedScale && width == preparedWidth && height == preparedHeight) || client.gui.screen() instanceof SettingsScreen) {
			return;
		}
		// Wait until the size has settled, so dragging the window's edge doesn't prepare it on every tick.
		long now = Util.getMillis();
		if (scale != pendingScale || width != pendingWidth || height != pendingHeight) {
			pendingScale = scale;
			pendingWidth = width;
			pendingHeight = height;
			pendingSince = now;
		}
		if (preparedScale != 0 && now - pendingSince < SETTLE_MILLIS) {
			return;
		}
		preparedScale = scale;
		preparedWidth = width;
		preparedHeight = height;
		long start = System.nanoTime();
		float blur = UiBlur.get();
		SettingsScreen screen = new SettingsScreen(null);
		screen.init(width, height);
		// Initializing prepares the blur for opening from gameplay; this screen never opens, so put back
		// what was there, which another screen may be fading right now.
		UiBlur.set(blur);
		screen.prerender(new GuiGraphicsExtractor(client, new GuiRenderState(), Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2));
		EMUtilsClient.LOGGER.debug("Prepared the EMUtils settings screen in {} ms.", (System.nanoTime() - start) / 1_000_000);
	}
}
