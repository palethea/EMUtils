package net.emutils.client.emutils.debug;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.packs.PackType;
import net.emutils.client.emutils.packs.ResourcePackController;
import net.emutils.client.emutils.screenshot.gui.GalleryScreen;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;

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
			case 35 -> sheet(client, "freelook", 2);
			case 36 -> capture(client, "gui scale 2, freelook sheet with keybind");
			case 37 -> {
				if (MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					screen.listenForKeyInSheet();
				}
				next();
			}
			case 38 -> capture(client, "gui scale 2, freelook sheet waiting for a key");
			case 39 -> checkKeybindInput(client);
			case 40 -> {
				if (MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					screen.searchFor("");
					EMUtilsClient.LOGGER.info("EMUtils UI snapshot: a left click on the theme button switches it: {}", screen.clickThemeButton());
				}
				next();
			}
			case 41 -> {
				seedWaypoints(client);
				client.gui.setScreen(new WaypointsScreen(null));
				next();
			}
			case 42 -> capture(client, "gui scale 2, waypoints");
			case 43 -> {
				if (MinecraftClientCompat.screen(client) instanceof WaypointsScreen screen) {
					screen.openAddSheetForSnapshot();
				}
				next();
			}
			case 44 -> capture(client, "gui scale 2, add waypoint sheet");
			case 45 -> {
				WaypointsScreen screen = new WaypointsScreen(null);
				client.gui.setScreen(screen);
				screen.openClearDialogForSnapshot();
				next();
			}
			case 46 -> capture(client, "gui scale 2, clear waypoints dialog");
			case 47 -> {
				EMUtilsClient.config().setSettingsUiDark(false);
				client.gui.setScreen(new WaypointsScreen(null));
				next();
			}
			case 48 -> capture(client, "gui scale 2, waypoints, light");
			case 49 -> {
				EMUtilsClient.config().setSettingsUiDark(true);
				EMUtilsClient.waypoint().clearForCurrentWorld(client);
				client.gui.setScreen(new WaypointsScreen(null));
				next();
			}
			case 50 -> capture(client, "gui scale 2, waypoints, empty");
			// The gallery shows the screenshots this run took so far.
			case 51 -> {
				client.gui.setScreen(new GalleryScreen(null));
				next();
			}
			case 52 -> capture(client, "gui scale 2, gallery");
			case 53 -> {
				if (MinecraftClientCompat.screen(client) instanceof GalleryScreen screen) {
					screen.openPreviewForSnapshot(1);
				}
				next();
			}
			case 54 -> capture(client, "gui scale 2, gallery preview");
			case 55 -> {
				EMUtilsClient.config().setSettingsUiDark(false);
				client.gui.setScreen(new GalleryScreen(null));
				next();
			}
			case 56 -> capture(client, "gui scale 2, gallery, light");
			case 57 -> {
				EMUtilsClient.config().setSettingsUiDark(true);
				next();
			}
			// The loading card: a tiny resource pack is turned on and off like the Pack Manager does.
			case 58 -> {
				client.gui.setScreen(new SettingsScreen(null));
				writeTestPack(client);
				next();
			}
			case 59 -> {
				if (stepTicks >= 10) {
					EMUtilsClient.LOGGER.info("EMUtils UI snapshot: enable test pack: {}", ResourcePackController.setResourcePackEnabled(client, TEST_PACK, true).message());
					next();
				}
			}
			case 60 -> captureAfter(client, 3, "loading card, resource pack");
			case 61 -> captureAfter(client, 60, "after the resource pack loaded");
			case 62 -> {
				ResourcePackController.setResourcePackEnabled(client, TEST_PACK, false);
				next();
			}
			case 63 -> {
				if (stepTicks >= 60) {
					deleteTestPack(client);
					next();
				}
			}
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

	/**
	 * Presses keys while the Freelook sheet waits for a key, the way a player would: Esc keeps the key
	 * as it was, bound or not, and leaves the sheet open; Backspace unbinds; any other key binds.
	 */
	private static void checkKeybindInput(Minecraft client) {
		if (!(MinecraftClientCompat.screen(client) instanceof SettingsScreen screen)) {
			next();
			return;
		}
		KeyMapping freelook = KeyMapping.get("key.emutils.freelook");
		String before = freelook.saveString();
		screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
		boolean escKeepsBound = freelook.saveString().equals(before) && screen.sheetOpen();

		screen.listenForKeyInSheet();
		screen.keyPressed(new KeyEvent(InputConstants.KEY_BACKSPACE, 0, 0));
		boolean backspaceUnbinds = freelook.isUnbound();

		screen.listenForKeyInSheet();
		screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
		boolean escKeepsUnbound = freelook.isUnbound() && screen.sheetOpen();

		screen.listenForKeyInSheet();
		screen.keyPressed(new KeyEvent(InputConstants.KEY_G, 0, 0));
		boolean keyBinds = freelook.saveString().equals("key.keyboard.g");

		freelook.setKey(freelook.getDefaultKey());
		KeyMapping.resetMapping();
		client.options.save();
		EMUtilsClient.LOGGER.info(
			"EMUtils UI snapshot: keybind input (was {}): Esc keeps a bound key {}, Backspace unbinds {}, Esc keeps it unbound {}, G binds {}",
			before, escKeepsBound, backspaceUnbinds, escKeepsUnbound, keyBinds
		);
		next();
	}

	/** Adds a few waypoints around the player, one hidden and one with its beacon on, to show the list. */
	private static void seedWaypoints(Minecraft client) {
		if (client.player == null) {
			return;
		}
		int x = client.player.getBlockX();
		int y = client.player.getBlockY();
		int z = client.player.getBlockZ();
		String[] names = {"Home base", "Iron farm", "Stronghold", "Ancient city entrance with a long name"};
		int[] colors = {0xFF55FF55, 0xFFFFAA55, 0xFF55FFFF, 0xFFFF55FF};
		int[][] offsets = {{12, 0, -30}, {-220, -8, 140}, {1480, -40, -2210}, {-640, -52, 90}};
		for (int i = 0; i < names.length; i++) {
			EMUtilsClient.waypoint().addCustom(client, names[i], x + offsets[i][0], y + offsets[i][1], z + offsets[i][2], colors[i], i == 1);
			try {
				// Waypoints are told apart by their creation time in milliseconds.
				Thread.sleep(3);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
		}
		List<Waypoint> waypoints = EMUtilsClient.waypoint().waypointsForCurrentWorld(client);
		if (waypoints.size() > 2) {
			EMUtilsClient.waypoint().toggleHidden(waypoints.get(2).timestamp());
		}
	}

	private static final String TEST_PACK = "EMUtils Snapshot Pack";

	/** A resource pack folder with just a pack.mcmeta, enough for Minecraft to list and load it. */
	private static void writeTestPack(Minecraft client) {
		try {
			Path folder = ResourcePackController.folder(client, PackType.RESOURCE).resolve(TEST_PACK);
			Files.createDirectories(folder);
			Files.writeString(folder.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"EMUtils UI snapshot test\",\"min_format\":65,\"max_format\":999}}");
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not write the snapshot test pack.", exception);
		}
	}

	private static void deleteTestPack(Minecraft client) {
		try {
			Path folder = ResourcePackController.folder(client, PackType.RESOURCE).resolve(TEST_PACK);
			Files.deleteIfExists(folder.resolve("pack.mcmeta"));
			Files.deleteIfExists(folder);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not delete the snapshot test pack.", exception);
		}
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
