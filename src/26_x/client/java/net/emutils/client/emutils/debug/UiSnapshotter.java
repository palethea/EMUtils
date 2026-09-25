package net.emutils.client.emutils.debug;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.gui.ui.UiLoadingOverlay;
import net.emutils.client.emutils.minescript.gui.ScriptsScreen;
import net.emutils.client.emutils.packs.PackType;
import net.emutils.client.emutils.packs.ResourcePackController;
import net.emutils.client.emutils.packs.gui.PacksScreen;
import net.emutils.client.emutils.screenshot.gui.GalleryScreen;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.emutils.client.versioned.VersionedScreens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
			case 58 -> {
				client.gui.setScreen(new PacksScreen(null));
				next();
			}
			case 59 -> capture(client, "gui scale 2, packs installed");
			case 60 -> {
				if (MinecraftClientCompat.screen(client) instanceof PacksScreen screen) {
					screen.searchModrinthForSnapshot("");
				}
				next();
			}
			case 61 -> captureAfter(client, 60, "gui scale 2, packs modrinth");
			case 62 -> {
				if (MinecraftClientCompat.screen(client) instanceof PacksScreen screen) {
					screen.searchModrinthForSnapshot("faithful");
				}
				next();
			}
			case 63 -> captureAfter(client, 60, "gui scale 2, packs search");
			// The loading card: a tiny resource pack is turned on and off like the Pack Manager does.
			case 64 -> {
				client.gui.setScreen(new PacksScreen(null));
				writeTestPack(client);
				next();
			}
			case 65 -> {
				if (stepTicks == 50) {
					EMUtilsClient.LOGGER.info("EMUtils UI snapshot: packs with the test pack installed");
					Screenshot.grab(client, false);
				}
				if (stepTicks >= 60) {
					EMUtilsClient.LOGGER.info("EMUtils UI snapshot: enable test pack: {}", ResourcePackController.setResourcePackEnabled(client, TEST_PACK, true, TEST_PACK_ICON).message());
					next();
				}
			}
			case 66 -> captureAfter(client, 3, "loading card, resource pack");
			case 67 -> captureAfter(client, 60, "after the resource pack loaded");
			case 68 -> {
				ResourcePackController.setResourcePackEnabled(client, TEST_PACK, false);
				next();
			}
			case 69 -> {
				if (stepTicks >= 60) {
					deleteTestPack(client);
					next();
				}
			}
			// Minecraft's Resource Packs button opening the Pack Manager (#114), and Back returning to Options.
			case 70 -> {
				EMUtilsClient.config().setPackManagerReplaceResourcePacksButton(true);
				client.gui.setScreen(VersionedScreens.options(null, client));
				next();
			}
			case 71 -> {
				if (stepTicks >= 10) {
					pressResourcePacks(client);
					next();
				}
			}
			case 72 -> captureAfter(client, 40, "resource packs button opened: " + screenName(client));
			case 73 -> {
				escape(client);
				next();
			}
			case 74 -> captureAfter(client, 20, "back from the pack manager: " + screenName(client));
			case 75 -> {
				EMUtilsClient.config().setPackManagerReplaceResourcePacksButton(false);
				client.gui.setScreen(null);
				next();
			}
			// The Script Manager (#118), with a few test scripts in the run folder's minescript folder.
			case 76 -> {
				if (client.options.guiScale().get() != 2) {
					client.options.guiScale().set(2);
					client.resizeGui();
				}
				writeTestScripts();
				client.gui.setScreen(new ScriptsScreen(null));
				next();
			}
			case 77 -> {
				if (stepTicks >= 15 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.openForSnapshot(TEST_SCRIPT_FOLDER + "/hello.py");
					next();
				}
			}
			case 78 -> captureAfter(client, 30, "scripts, gui scale 2, dark");
			case 79 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.keyPressed(new KeyEvent(InputConstants.KEY_DOWN, 0, 0));
					screen.keyPressed(new KeyEvent(InputConstants.KEY_DOWN, 0, 0));
					screen.keyPressed(new KeyEvent(InputConstants.KEY_END, 0, 0));
					" # edited".codePoints().forEach(codepoint -> screen.charTyped(new CharacterEvent(codepoint)));
					screen.keyPressed(new KeyEvent(InputConstants.KEY_UP, 0, InputConstants.MOD_SHIFT));
				}
				next();
			}
			case 80 -> captureAfter(client, 10, "scripts, edited");
			case 81 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.openKeybindForSnapshot();
				}
				next();
			}
			case 82 -> {
				if (stepTicks >= 10 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.keyPressed(new KeyEvent(InputConstants.KEY_K, 0, InputConstants.MOD_CONTROL));
					next();
				}
			}
			case 83 -> captureAfter(client, 15, "scripts, keybind dialog");
			case 84 -> {
				escape(client);
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.newScriptForSnapshot();
				}
				next();
			}
			case 85 -> captureAfter(client, 25, "scripts, new script dialog");
			case 86 -> {
				escape(client);
				EMUtilsClient.config().setSettingsUiDark(false);
				client.options.guiScale().set(3);
				client.resizeGui();
				next();
			}
			case 87 -> captureAfter(client, 30, "scripts, gui scale 3, light");
			case 88 -> {
				EMUtilsClient.config().setSettingsUiDark(true);
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.discardForSnapshot();
				}
				client.gui.setScreen(null);
				if (MinescriptCompat.isLoaded()) {
					client.options.guiScale().set(2);
					client.resizeGui();
					client.gui.setScreen(new ScriptsScreen(null));
					next();
				} else {
					EMUtilsClient.LOGGER.info("EMUtils UI snapshot: Minescript isn't installed, so no scripts are run");
					step = SCRIPTS_CLEANUP_STEP;
					stepTicks = 0;
				}
			}
			// With Minescript installed: run scripts in folders through the Run/Stop button (#118).
			case 89 -> {
				if (stepTicks >= 15 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.openForSnapshot(TEST_SCRIPT_FOLDER + "/tools/marker.py");
					screen.runForSnapshot();
					next();
				}
			}
			case 90 -> waitForCheck(Files.isRegularFile(testScript("tools/marker.marker")), 200, "a script in a folder runs");
			case 91 -> captureAfter(client, 5, "scripts, ran a script in a folder");
			case 92 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.openForSnapshot(TEST_SCRIPT_FOLDER + "/tools/auto_farm.py");
					screen.runForSnapshot();
				}
				next();
			}
			case 93 -> waitForCheck(MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen && screen.runningForSnapshot(), 200, "a running script in a folder shows as running");
			case 94 -> captureAfter(client, 25, "scripts, a script in a folder running");
			case 95 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.runForSnapshot();
				}
				next();
			}
			case 96 -> waitForCheck(MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen && !screen.runningForSnapshot() && MinescriptCompat.findActiveJobIdsForCommand(TEST_SCRIPT_FOLDER + "/tools/auto_farm").isEmpty(), 200, "Stop ends a script in a folder");
			case 97 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.newScriptForSnapshot();
				}
				next();
			}
			case 98 -> {
				if (stepTicks >= 10 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.keyPressed(selectAll());
					type(screen, TEST_SCRIPT_FOLDER + "/made/new_script");
					screen.keyPressed(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
					next();
				}
			}
			case 99 -> {
				if (stepTicks >= 5 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					check(Files.isRegularFile(testScript("made/new_script.py")), "the new script dialog creates a script in a new folder");
					check((TEST_SCRIPT_FOLDER + "/made/new_script.py").equals(screen.selectedForSnapshot()), "the new script opens in the editor");
					// Replace the template with a line that leaves a marker, save with Ctrl+S, and run it.
					screen.keyPressed(selectAll());
					type(screen, "open(__file__[:-3] + \".marker\", \"w\").write(\"ok\")");
					screen.keyPressed(new KeyEvent(InputConstants.KEY_S, 0, InputConstants.MOD_CONTROL));
					check(readTestScript("made/new_script.py").startsWith("open(__file__"), "Ctrl+S saves the edited script");
					screen.runForSnapshot();
					next();
				}
			}
			case 100 -> waitForCheck(Files.isRegularFile(testScript("made/new_script.marker")), 200, "a new script in a new folder runs");
			case 101 -> captureAfter(client, 5, "scripts, ran a new script in a new folder");
			case SCRIPTS_CLEANUP_STEP -> {
				client.gui.setScreen(null);
				deleteTestScripts();
				next();
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
	/** The mod's own icon stands in for a pack's Modrinth icon, so the card's icon is captured too (#115). */
	private static final UiLoadingOverlay.Icon TEST_PACK_ICON = new UiLoadingOverlay.Icon(HubIcons.PACKAGE, Identifier.fromNamespaceAndPath("emutils", "icon.png"), 128, 128);

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

	/** A folder of its own inside the minescript folder, so the test never touches real scripts. */
	private static final String TEST_SCRIPT_FOLDER = "emutils_snapshot";
	private static final int SCRIPTS_CLEANUP_STEP = 102;

	private static void writeTestScripts() {
		Path folder = MinescriptCompat.scriptsDir().resolve(TEST_SCRIPT_FOLDER);
		try {
			Files.createDirectories(folder.resolve("tools"));
			Files.writeString(folder.resolve("hello.py"), """
				import minescript

				# Greets the player and says where they stand.
				def greet(name):
				    x, y, z = minescript.player_position()
				    minescript.echo(f"Hello {name}! You're at {int(x)}, {int(y)}, {int(z)}.")
				    return True

				for step in range(3):
				\tgreet("world")  # a tab-indented line
				""");
			Files.writeString(folder.resolve("tools/fly_toggle.py"), "import minescript\n\nminescript.execute(\"/fly\")\n");
			Files.writeString(folder.resolve("tools/auto_farm.py"), "import time\n\nwhile True:\n    time.sleep(0.1)\n");
			// Leaves a marker next to itself, so the snapshot can check that it ran.
			Files.writeString(folder.resolve("tools/marker.py"), "open(__file__[:-3] + \".marker\", \"w\").write(\"ok\")\n");
			Files.writeString(folder.resolve("legacy.pyj"), "# read-only in EMUtils\n");
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not write the snapshot test scripts.", exception);
		}
	}

	private static void deleteTestScripts() {
		Path folder = MinescriptCompat.scriptsDir().resolve(TEST_SCRIPT_FOLDER);
		if (!Files.isDirectory(folder)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(folder)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not delete the snapshot test scripts.", exception);
		}
	}

	private static Path testScript(String relativePath) {
		return MinescriptCompat.scriptsDir().resolve(TEST_SCRIPT_FOLDER).resolve(relativePath);
	}

	private static String readTestScript(String relativePath) {
		try {
			return Files.readString(testScript(relativePath));
		} catch (IOException exception) {
			return "";
		}
	}

	/** Ctrl+A; 26.3 reads shortcuts from the key's layout character, so the event carries it too. */
	private static KeyEvent selectAll() {
		return new KeyEvent(InputConstants.KEY_A, 'a', InputConstants.MOD_CONTROL);
	}

	private static void type(Screen screen, String text) {
		text.codePoints().forEach(codepoint -> screen.charTyped(new CharacterEvent(codepoint)));
	}

	private static void check(boolean passed, String what) {
		if (passed) {
			EMUtilsClient.LOGGER.info("EMUtils UI snapshot check passed: {}", what);
		} else {
			EMUtilsClient.LOGGER.error("EMUtils UI snapshot check FAILED: {}", what);
		}
	}

	/** Moves on once the condition holds, or after the timeout with a failed check. */
	private static void waitForCheck(boolean condition, int timeoutTicks, String what) {
		if (condition || stepTicks >= timeoutTicks) {
			check(condition, what + " (" + stepTicks + " ticks)");
			next();
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

	/** Presses the Options screen's Resource Packs button with Enter, which runs its real press handler. */
	private static void pressResourcePacks(Minecraft client) {
		Screen screen = MinecraftClientCompat.screen(client);
		if (screen == null) {
			return;
		}
		Component label = Component.translatable("options.resourcepack");
		for (GuiEventListener child : screen.children()) {
			if (child instanceof Button button && button.getMessage().equals(label)) {
				screen.setFocused(button);
				screen.keyPressed(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
				return;
			}
		}
		EMUtilsClient.LOGGER.warn("EMUtils UI snapshot: no Resource Packs button on {}", screenName(client));
	}

	private static void escape(Minecraft client) {
		Screen screen = MinecraftClientCompat.screen(client);
		if (screen != null) {
			screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
		}
	}

	private static String screenName(Minecraft client) {
		Screen screen = MinecraftClientCompat.screen(client);
		return screen == null ? "no screen" : screen.getClass().getSimpleName();
	}

	private static void next() {
		step++;
		stepTicks = 0;
	}
}
