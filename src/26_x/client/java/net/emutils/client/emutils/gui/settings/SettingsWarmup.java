package net.emutils.client.emutils.gui.settings;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

/**
 * Renders the new settings screen once, invisibly, right after the game has loaded, so its first real
 * open doesn't stall while every label, shape and icon is created (about 0.2 s otherwise). Those are
 * cached per GUI scale and cut to the window's width, so it runs again after the GUI scale or window size
 * changed. The drawing goes into a throwaway render state that is never shown.
 */
public final class SettingsWarmup {
	private static int preparedScale;
	private static int preparedWidth;
	private static int preparedHeight;

	private SettingsWarmup() {
	}

	/** Call every client tick; it prepares the screen when needed, once loading has finished, if the new UI is in use. */
	public static void tick(Minecraft client) {
		if (client.gui.overlay() != null || EMUtilsClient.config() == null || !EMUtilsClient.config().settingsUiPreview()) {
			return;
		}
		int scale = (int) Math.ceil(client.getWindow().getGuiScale());
		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		// Not while the screen is open: it's already rendered, and the preparation would stall it.
		if ((scale == preparedScale && width == preparedWidth && height == preparedHeight) || client.gui.screen() instanceof SettingsScreen) {
			return;
		}
		preparedScale = scale;
		preparedWidth = width;
		preparedHeight = height;
		long start = System.nanoTime();
		SettingsScreen screen = new SettingsScreen(null);
		screen.init(width, height);
		screen.prerender(new GuiGraphicsExtractor(client, new GuiRenderState(), Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2));
		EMUtilsClient.LOGGER.debug("Prepared the EMUtils settings screen in {} ms.", (System.nanoTime() - start) / 1_000_000);
	}
}
