package net.emutils.client.emutils.debug;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Development aid: with {@code -Demutils.uiSnapshot=true} (Gradle property {@code emutilsUiSnapshot}),
 * the client enters a test world, opens the new settings screen, saves screenshots of it at GUI
 * scales 3 (dark and light), 2, 1 and 4, and quits. Screenshots land in the run directory.
 */
public final class UiSnapshotter {
	private static final String ENABLED_PROPERTY = "emutils.uiSnapshot";
	private static final int SETTLE_TICKS = 25;

	private static boolean enabled = Boolean.getBoolean(ENABLED_PROPERTY);
	private static int worldTicks;
	private static int step;
	private static int stepTicks;

	private UiSnapshotter() {
	}

	public static void tick(Minecraft client) {
		if (!enabled || client.level == null || client.player == null) {
			return;
		}

		worldTicks++;
		if (worldTicks < 40) {
			return;
		}
		stepTicks++;
		switch (step) {
			case 0 -> setup(client, 3, true);
			case 1 -> capture(client, "gui scale 3, dark");
			case 2 -> setup(client, 3, false);
			case 3 -> capture(client, "gui scale 3, light");
			case 4 -> setup(client, 2, true);
			case 5 -> capture(client, "gui scale 2, dark");
			case 6 -> setup(client, 1, true);
			case 7 -> capture(client, "gui scale 1, dark");
			case 8 -> setup(client, 4, true);
			case 9 -> capture(client, "gui scale 4, dark");
			default -> {
				EMUtilsClient.LOGGER.info("EMUtils UI snapshots done; stopping Minecraft.");
				enabled = false;
				client.stop();
			}
		}
	}

	private static void setup(Minecraft client, int guiScale, boolean dark) {
		EMUtilsClient.config().setSettingsUiDark(dark);
		if (client.options.guiScale().get() != guiScale) {
			client.options.guiScale().set(guiScale);
			client.resizeGui();
		}
		if (!(MinecraftClientCompat.screen(client) instanceof SettingsScreen)) {
			client.setScreenAndShow(new SettingsScreen(null));
		}
		next();
	}

	private static void capture(Minecraft client, String label) {
		if (stepTicks < SETTLE_TICKS) {
			return;
		}
		EMUtilsClient.LOGGER.info("EMUtils UI snapshot: {}", label);
		Screenshot.grab(client, false);
		next();
	}

	private static void next() {
		step++;
		stepTicks = 0;
	}
}
