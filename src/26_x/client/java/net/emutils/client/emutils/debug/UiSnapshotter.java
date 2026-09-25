package net.emutils.client.emutils.debug;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Development aid: with {@code -Demutils.uiSnapshot=true} (Gradle property {@code emutilsUiSnapshot}),
 * the client enters a test world, opens the new settings screen, saves screenshots of it at GUI
 * scales 3 (dark and light), 2, 1 and 4, then a few settings sheets and the color picker at each
 * scale, closes the screen, and quits. Screenshots land in the run directory.
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
			case 10 -> sheet(client, "zoom", 2);
			case 11 -> capture(client, "gui scale 2, zoom sheet");
			case 12 -> sheet(client, "auto_tool", 2);
			case 13 -> capture(client, "gui scale 2, auto tool sheet");
			case 14 -> sheet(client, "chat", 2);
			case 15 -> capture(client, "gui scale 2, chat sheet");
			case 16 -> sheet(client, "inventory", 2);
			case 17 -> pickColor(client);
			case 18 -> capture(client, "gui scale 2, color picker");
			case 19 -> sheet(client, "inventory", 1);
			case 20 -> pickColor(client);
			case 21 -> capture(client, "gui scale 1, color picker");
			case 22 -> {
				EMUtilsClient.config().setSettingsUiDark(false);
				sheet(client, "inventory", 3);
			}
			case 23 -> pickColor(client);
			case 24 -> capture(client, "gui scale 3, light, color picker");
			case 25 -> {
				EMUtilsClient.config().setSettingsUiDark(true);
				sheet(client, "inventory", 4);
			}
			case 26 -> pickColor(client);
			case 27 -> capture(client, "gui scale 4, color picker");
			case 28 -> closeScreen(client);
			case 29 -> waitForClose(client);
			case 30 -> {
				client.gui.setScreen(new SettingsScreen(null));
				next();
			}
			case 31 -> captureAfter(client, 2, "opening, mid-animation");
			case 32 -> capture(client, "opened");
			case 33 -> search(client, "manager");
			case 34 -> capture(client, "gui scale 2, search manager");
			case 35 -> {
				search(client, "zoom");
				if (MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					screen.listenForKey("zoom");
				}
			}
			case 36 -> capture(client, "gui scale 2, zoom keycap listening");
			case 37 -> sheet(client, "freelook", 2);
			case 38 -> capture(client, "gui scale 2, freelook sheet with keybind");
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
			client.gui.setScreen(new SettingsScreen(null));
		}
		next();
	}

	private static void sheet(Minecraft client, String featureId, int guiScale) {
		if (client.options.guiScale().get() != guiScale) {
			client.options.guiScale().set(guiScale);
			client.resizeGui();
		}
		if (MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
			screen.openSheet(featureId);
		}
		next();
	}

	private static void search(Minecraft client, String text) {
		if (client.options.guiScale().get() != 2) {
			client.options.guiScale().set(2);
			client.resizeGui();
		}
		if (MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
			screen.searchFor(text);
		}
		next();
	}

	private static void pickColor(Minecraft client) {
		if (stepTicks < SETTLE_TICKS) {
			return;
		}
		if (MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
			screen.openColorPickerInSheet();
		}
		next();
	}

	private static void closeScreen(Minecraft client) {
		if (MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
			screen.onClose();
		}
		next();
	}

	/** Checks that the close animation hands back to the game instead of leaving the screen open. */
	private static void waitForClose(Minecraft client) {
		if (MinecraftClientCompat.screen(client) instanceof SettingsScreen) {
			if (stepTicks > 40) {
				EMUtilsClient.LOGGER.error("EMUtils UI snapshot: the settings screen did not close");
				next();
			}
			return;
		}
		EMUtilsClient.LOGGER.info("EMUtils UI snapshot: settings screen closed after {} ticks", stepTicks);
		next();
	}

	private static void capture(Minecraft client, String label) {
		captureAfter(client, SETTLE_TICKS, label);
	}

	private static void captureAfter(Minecraft client, int ticks, String label) {
		if (stepTicks < ticks) {
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
