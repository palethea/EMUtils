package net.emutils.client.emutils.debug;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.EMUtilsHudElements;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.commandshortcuts.CommandShortcut;
import net.emutils.client.emutils.commandshortcuts.gui.CommandShortcutsScreen;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiFontRenderer;
import net.emutils.client.emutils.gui.ui.UiLoadingOverlay;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.hud.editor.HudEditorScreen;
import net.emutils.client.emutils.hud.layout.HudLayoutDraft;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.inventory.gui.MassDropItemsScreen;
import net.emutils.client.emutils.minescript.MinescriptKeyBinding;
import net.emutils.client.emutils.minescript.MinescriptKeybindStore;
import net.emutils.client.emutils.minescript.MinescriptPython;
import net.emutils.client.emutils.minescript.gui.ScriptsScreen;
import net.emutils.client.emutils.packs.PackType;
import net.emutils.client.emutils.packs.ResourcePackController;
import net.emutils.client.emutils.packs.gui.PacksScreen;
import net.emutils.client.emutils.screenshot.gui.GalleryScreen;
import net.emutils.client.emutils.spotify.SpotifyTrackState;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.versioned.VersionedScreens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

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
	/** {@code -Demutils.uiSnapshotFrom=N} (Gradle property {@code emutilsUiSnapshotFrom}) starts at step N. */
	private static int step = Integer.getInteger("emutils.uiSnapshotFrom", 0);
	private static int stepTicks;
	/** The screenshots the gallery showed the first time it opened. */
	private static List<Path> galleryShown = List.of();
	private static long configModifiedBefore;
	private static boolean configCheckPending;
	private static boolean spotifyWasPlaying;
	private static int hudTextures;

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
			case 52 -> {
				if (MinecraftClientCompat.screen(client) instanceof GalleryScreen screen) {
					galleryShown = screen.screenshotsForSnapshot();
				}
				capture(client, "gui scale 2, gallery");
			}
			case 53 -> {
				if (MinecraftClientCompat.screen(client) instanceof GalleryScreen screen) {
					screen.openPreviewForSnapshot(1);
				}
				next();
			}
			case 54 -> capture(client, "gui scale 2, gallery preview");
			case 55 -> {
				// The screenshot taken of the open gallery shows up in it without reopening.
				if (MinecraftClientCompat.screen(client) instanceof GalleryScreen screen) {
					List<Path> listed = screen.screenshotsForSnapshot();
					if (listed.size() > galleryShown.size() && listed.containsAll(galleryShown)) {
						EMUtilsClient.LOGGER.info("EMUtils UI snapshot check passed: an open gallery lists a new screenshot ({} -> {})", galleryShown.size(), listed.size());
					} else {
						EMUtilsClient.LOGGER.error("EMUtils UI snapshot check failed: an open gallery didn't list the new screenshot ({} -> {})", galleryShown.size(), listed.size());
					}
				}
				EMUtilsClient.config().setSettingsUiDark(false);
				client.gui.setScreen(new GalleryScreen(null));
				next();
			}
			case 56 -> {
				// Reopened, the gallery shows its thumbnails from the shared cache right away (#133).
				if (stepTicks == 2 && MinecraftClientCompat.screen(client) instanceof GalleryScreen screen) {
					// Screenshots taken since the first opening load now; the ones it showed must not.
					List<Path> loading = screen.loadingThumbnailsForSnapshot().stream().filter(galleryShown::contains).toList();
					if (galleryShown.isEmpty()) {
						EMUtilsClient.LOGGER.error("EMUtils UI snapshot check failed: the gallery showed no screenshots to cache");
					} else if (loading.isEmpty()) {
						EMUtilsClient.LOGGER.info("EMUtils UI snapshot check passed: a reopened gallery shows cached thumbnails at once");
					} else {
						EMUtilsClient.LOGGER.error("EMUtils UI snapshot check failed: a reopened gallery was still loading {}", loading);
					}
				}
				capture(client, "gui scale 2, gallery, light");
			}
			case 57 -> {
				if (MinecraftClientCompat.screen(client) instanceof GalleryScreen screen) {
					checkGalleryClickAfterRefresh(client, screen);
				}
				checkTextFieldSurrogates();
				// The theme change is saved after a short delay; step 60 checks it was written.
				configModifiedBefore = configModified();
				EMUtilsClient.config().setSettingsUiDark(true);
				check(configModified() == configModifiedBefore, "a settings change isn't written to disk straight away");
				configCheckPending = true;
				next();
			}
			case 58 -> {
				client.gui.setScreen(new PacksScreen(null));
				next();
			}
			case 59 -> capture(client, "gui scale 2, packs installed");
			case 60 -> {
				if (configCheckPending) {
					configCheckPending = false;
					Path file = EMUtilsPaths.configFile();
					check(configModified() != configModifiedBefore && !Files.exists(file.resolveSibling(file.getFileName() + ".tmp")), "a settings change is written about a second later, with no temporary file left");
				}
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
			// Minescript's Python warning (#122): break config.txt the way Minescript's Windows default does,
			// then let the banner find a working Python and fix it in one click.
			case 102 -> {
				client.gui.setScreen(null);
				savedMinescriptConfig = readMinescriptConfig();
				writeMinescriptConfig("python=\"%userprofile%\\AppData\\Local\\Microsoft\\WindowsApps\\python3.exe\"\n");
				client.gui.setScreen(new ScriptsScreen(null));
				next();
			}
			case 103 -> {
				if (stepTicks >= 5 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.openForSnapshot(TEST_SCRIPT_FOLDER + "/hello.py");
					next();
				}
			}
			case 104 -> {
				MinescriptPython.Result python = MinescriptPython.result();
				waitForCheck(python.problem() == MinescriptPython.Problem.NOT_WORKING && !python.searching() && python.suggestion() != null, 400, "a broken Python is noticed and a working one is found: " + (python.suggestion() == null ? "none" : python.suggestion().path()));
			}
			case 105 -> captureAfter(client, 20, "scripts, python warning");
			case 106 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					MinescriptPython.Python suggestion = MinescriptPython.result().suggestion();
					screen.fixPythonForSnapshot();
					check(suggestion != null && readMinescriptConfig().contains("python=\"" + suggestion.path() + "\""), "the fix writes the Python to config.txt");
					check(!MinescriptPython.result().broken(), "the warning goes away after the fix");
					deleteQuietly(testScript("tools/marker.marker"));
					screen.openForSnapshot(TEST_SCRIPT_FOLDER + "/tools/marker.py");
					screen.runForSnapshot();
				}
				next();
			}
			case 107 -> waitForCheck(Files.isRegularFile(testScript("tools/marker.marker")), 200, "scripts run with the fixed Python");
			case 108 -> captureAfter(client, 10, "scripts, python fixed");
			case 109 -> {
				if (savedMinescriptConfig != null) {
					writeMinescriptConfig(savedMinescriptConfig);
					MinescriptCompat.reloadConfig();
				}
				next();
			}
			// Finishing the editor (#125): editing keys, find, run errors and file management.
			case 110 -> {
				client.gui.setScreen(null);
				// A keybind on a script that gets moved below, so the move can check it comes along.
				MinescriptKeybindStore store = MinescriptKeybindStore.load();
				MinescriptKeyBinding binding = MinescriptKeyBinding.from(TEST_SCRIPT_FOLDER + "/tools/marker", new KeyEvent(InputConstants.KEY_K, 'k', InputConstants.MOD_CONTROL | InputConstants.MOD_ALT));
				if (binding != null) {
					store.put(binding);
				}
				client.gui.setScreen(new ScriptsScreen(null));
				next();
			}
			case 111 -> {
				if (stepTicks >= 10 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.openForSnapshot(TEST_SCRIPT_FOLDER + "/edit.py");
					// Brackets and quotes close themselves and typing the closer steps over it; Enter indents after
					// a colon and keeps the indentation; Shift+Tab outdents.
					type(screen, "def greet(name");
					type(screen, "):");
					press(screen, InputConstants.KEY_RETURN, 0);
					type(screen, "print(\"hi");
					type(screen, "\")");
					press(screen, InputConstants.KEY_RETURN, 0);
					press(screen, InputConstants.KEY_TAB, InputConstants.MOD_SHIFT);
					type(screen, "greet(1)");
					check(screen.editorTextForSnapshot().equals(EDIT_EXPECTED), "typing closes pairs, indents after a colon and Shift+Tab outdents: " + screen.editorTextForSnapshot().replace("\n", "\\n"));

					screen.keyPressed(selectAll());
					press(screen, InputConstants.KEY_TAB, 0);
					check(screen.editorTextForSnapshot().equals("    def greet(name):\n        print(\"hi\")\n    greet(1)"), "Tab indents every selected line");
					press(screen, InputConstants.KEY_TAB, InputConstants.MOD_SHIFT);
					check(screen.editorTextForSnapshot().equals(EDIT_EXPECTED), "Shift+Tab outdents every selected line");
					press(screen, InputConstants.KEY_SLASH, InputConstants.MOD_CONTROL);
					check(screen.editorTextForSnapshot().equals("# def greet(name):\n#     print(\"hi\")\n# greet(1)"), "Ctrl+/ comments the selected lines");
					press(screen, InputConstants.KEY_SLASH, InputConstants.MOD_CONTROL);
					check(screen.editorTextForSnapshot().equals(EDIT_EXPECTED), "Ctrl+/ again uncomments them");

					press(screen, InputConstants.KEY_END, 0);
					press(screen, InputConstants.KEY_D, InputConstants.MOD_CONTROL);
					check(screen.editorTextForSnapshot().equals(EDIT_EXPECTED + "\ngreet(1)"), "Ctrl+D duplicates the line");
					press(screen, InputConstants.KEY_UP, 0);
					press(screen, InputConstants.KEY_UP, 0);
					press(screen, InputConstants.KEY_END, 0);
					press(screen, InputConstants.KEY_RETURN, 0);
					check(screen.editorTextForSnapshot().split("\n", -1)[2].equals("    "), "Enter keeps the indentation");
					press(screen, InputConstants.KEY_BACKSPACE, 0);
					check(screen.editorTextForSnapshot().split("\n", -1)[2].isEmpty(), "Backspace removes a whole indentation level");
					press(screen, InputConstants.KEY_BACKSPACE, 0);
					check(screen.editorTextForSnapshot().equals(EDIT_EXPECTED + "\ngreet(1)"), "Backspace at the line start joins the lines");

					press(screen, InputConstants.KEY_F, InputConstants.MOD_CONTROL);
					type(screen, "greet");
					int[] found = screen.findForSnapshot();
					check(found[1] == 3 && found[0] > 0, "Ctrl+F finds every match and selects one: " + found[0] + " of " + found[1]);
					next();
				}
			}
			case 112 -> captureAfter(client, 15, "scripts, find");
			case 113 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					int before = screen.findForSnapshot()[0];
					press(screen, InputConstants.KEY_RETURN, 0);
					check(screen.findForSnapshot()[0] == before % 3 + 1, "Enter in the find bar goes to the next match");
					press(screen, InputConstants.KEY_ESCAPE, 0);
					screen.discardForSnapshot();
					screen.openForSnapshot(TEST_SCRIPT_FOLDER + "/broken.py");
					screen.runForSnapshot();
				}
				next();
			}
			case 114 -> {
				MinescriptCompat.ScriptError error = MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen ? screen.errorForSnapshot() : null;
				waitForCheck(error != null && error.line() == 2 && error.message().startsWith("NameError"), 200, "a failed run shows its line and error: " + error);
			}
			case 115 -> captureAfter(client, 15, "scripts, run error");
			case 116 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					type(screen, "x");
				}
				next();
			}
			case 117 -> {
				if (stepTicks >= 2 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					check(screen.errorForSnapshot() == null, "editing the script clears the error");
					screen.discardForSnapshot();
					screen.newFolderForSnapshot(TEST_SCRIPT_FOLDER);
					next();
				}
			}
			case 118 -> {
				if (stepTicks >= 5 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					// The dialog selects just "new_folder", so typing replaces the name and keeps the parent.
					type(screen, "lib");
					press(screen, InputConstants.KEY_RETURN, 0);
					next();
				}
			}
			case 119 -> {
				if (stepTicks >= 5 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					check(Files.isDirectory(testScript("lib")), "New Folder creates a folder inside the selected one");
					screen.renameForSnapshot(TEST_SCRIPT_FOLDER + "/tools/marker.py");
					next();
				}
			}
			case 120 -> captureAfter(client, 20, "scripts, rename dialog");
			case 121 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.keyPressed(selectAll());
					type(screen, TEST_SCRIPT_FOLDER + "/lib/marker_moved");
					press(screen, InputConstants.KEY_RETURN, 0);
				}
				next();
			}
			case 122 -> {
				if (stepTicks >= 5 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					check(Files.isRegularFile(testScript("lib/marker_moved.py")) && !Files.exists(testScript("tools/marker.py")), "renaming moves a script to another folder");
					check(MinescriptKeybindStore.load().get(TEST_SCRIPT_FOLDER + "/lib/marker_moved").isPresent(), "the keybind moves with the script");
					screen.renameForSnapshot(TEST_SCRIPT_FOLDER + "/lib");
					next();
				}
			}
			case 123 -> {
				if (stepTicks >= 5 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.keyPressed(selectAll());
					type(screen, TEST_SCRIPT_FOLDER + "/library");
					press(screen, InputConstants.KEY_RETURN, 0);
					next();
				}
			}
			case 124 -> {
				if (stepTicks >= 5 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					check(Files.isRegularFile(testScript("library/marker_moved.py")), "renaming a folder moves its scripts");
					check(MinescriptKeybindStore.load().get(TEST_SCRIPT_FOLDER + "/library/marker_moved").isPresent(), "keybinds follow a renamed folder");
					screen.menuForSnapshot(TEST_SCRIPT_FOLDER + "/library", 150, 150);
					next();
				}
			}
			case 125 -> captureAfter(client, 15, "scripts, folder menu");
			case 126 -> {
				escape(client);
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.deleteFolderForSnapshot(TEST_SCRIPT_FOLDER + "/library");
				}
				next();
			}
			case 127 -> captureAfter(client, 20, "scripts, delete folder");
			case 128 -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					press(screen, InputConstants.KEY_RETURN, 0);
				}
				next();
			}
			case 129 -> {
				if (stepTicks >= 5) {
					check(!Files.exists(testScript("library")), "Delete folder removes the folder and its scripts");
					check(MinescriptKeybindStore.load().get(TEST_SCRIPT_FOLDER + "/library/marker_moved").isEmpty(), "deleting a folder removes its scripts' keybinds");
					next();
				}
			}
			case SCRIPTS_CLEANUP_STEP -> {
				if (MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.discardForSnapshot();
				}
				client.gui.setScreen(null);
				MinescriptKeybindStore.load().removeFolder(TEST_SCRIPT_FOLDER);
				EMUtilsClient.minescriptKeybinds().reload();
				deleteTestScripts();
				next();
			}
			// Command Shortcuts (#131): add one through the sheet, try a second on the same keys, then the list.
			case 131 -> {
				shortcutsBefore = EMUtilsClient.commandShortcuts().store().shortcuts().stream().map(CommandShortcut::id).toList();
				client.options.guiScale().set(2);
				client.resizeGui();
				client.gui.setScreen(new CommandShortcutsScreen(null));
				next();
			}
			case 132 -> captureAfter(client, 25, "shortcuts, empty");
			case 133 -> {
				if (MinecraftClientCompat.screen(client) instanceof CommandShortcutsScreen screen) {
					screen.openSheetForSnapshot(null);
					// A new shortcut starts in the text field; Tab goes on to the keys, and again to the name.
					type(screen, "/home");
					press(screen, InputConstants.KEY_TAB, 0);
					screen.keyPressed(new KeyEvent(InputConstants.KEY_H, 'h', InputConstants.MOD_CONTROL));
					press(screen, InputConstants.KEY_TAB, 0);
					type(screen, "Home");
					press(screen, InputConstants.KEY_RETURN, 0);
					check(EMUtilsClient.commandShortcuts().store().shortcuts().stream().anyMatch(shortcut -> shortcut.displayName().equals("Home") && shortcut.displayText().equals("/home") && shortcut.keyCombo().ctrl()), "the sheet adds a shortcut with its name, command and keys");
				}
				next();
			}
			case 134 -> {
				if (stepTicks >= 15 && MinecraftClientCompat.screen(client) instanceof CommandShortcutsScreen screen) {
					screen.openSheetForSnapshot(null);
					type(screen, "hello everyone");
					press(screen, InputConstants.KEY_TAB, 0);
					screen.keyPressed(new KeyEvent(InputConstants.KEY_H, 'h', InputConstants.MOD_CONTROL));
					int before = EMUtilsClient.commandShortcuts().store().shortcuts().size();
					press(screen, InputConstants.KEY_RETURN, 0);
					check(EMUtilsClient.commandShortcuts().store().shortcuts().size() == before, "keys another shortcut uses aren't saved");
					next();
				}
			}
			case 135 -> captureAfter(client, 20, "shortcuts, keys taken");
			case 136 -> {
				if (MinecraftClientCompat.screen(client) instanceof CommandShortcutsScreen screen) {
					press(screen, InputConstants.KEY_TAB, InputConstants.MOD_SHIFT);
					press(screen, InputConstants.KEY_TAB, 0);
					screen.keyPressed(new KeyEvent(InputConstants.KEY_G, 'g', InputConstants.MOD_CONTROL));
					press(screen, InputConstants.KEY_RETURN, 0);
					check(EMUtilsClient.commandShortcuts().store().shortcuts().stream().anyMatch(shortcut -> shortcut.displayText().equals("hello everyone") && !shortcut.isCommand()), "other keys save it as a chat message");
				}
				next();
			}
			case 137 -> captureAfter(client, 25, "shortcuts, list");
			case 138 -> {
				if (MinecraftClientCompat.screen(client) instanceof CommandShortcutsScreen screen) {
					screen.openMenuForSnapshot(620, 150);
				}
				next();
			}
			case 139 -> captureAfter(client, 15, "shortcuts, menu");
			case 140 -> {
				escape(client);
				EMUtilsClient.config().setSettingsUiDark(false);
				client.options.guiScale().set(3);
				client.resizeGui();
				next();
			}
			case 141 -> captureAfter(client, 30, "shortcuts, gui scale 3, light");
			case 142 -> {
				// Opened from the settings screen, like from the hub: Run now closes both into the game.
				EMUtilsClient.config().setSettingsUiDark(true);
				client.options.guiScale().set(2);
				client.resizeGui();
				client.gui.setScreen(new CommandShortcutsScreen(new SettingsScreen(null)));
				next();
			}
			case 143 -> {
				if (stepTicks >= 10 && MinecraftClientCompat.screen(client) instanceof CommandShortcutsScreen screen) {
					screen.runFirstForSnapshot();
					next();
				}
			}
			case 144 -> waitForCheck(MinecraftClientCompat.screen(client) == null, 40, "Run now closes every menu, back to the game");
			case 145 -> {
				for (CommandShortcut shortcut : EMUtilsClient.commandShortcuts().store().shortcuts()) {
					if (!shortcutsBefore.contains(shortcut.id())) {
						EMUtilsClient.commandShortcuts().store().remove(shortcut.id());
					}
				}
				EMUtilsClient.commandShortcuts().reload();
				client.gui.setScreen(null);
				next();
			}
			// The Mass Drop list (#134): search, add an item, and see it in the list with how many you carry.
			case 146 -> {
				massDropBefore = EMUtilsClient.massDrop().store().itemIds();
				if (client.getConnection() != null) {
					client.getConnection().sendCommand("give @s minecraft:cobblestone 40");
				}
				client.gui.setScreen(new MassDropItemsScreen(null));
				next();
			}
			case 147 -> captureAfter(client, 25, "mass drop, list");
			case 148 -> {
				if (MinecraftClientCompat.screen(client) instanceof MassDropItemsScreen screen) {
					screen.searchForSnapshot("cobblestone");
					List<String> shown = screen.shownForSnapshot();
					check(!shown.isEmpty() && shown.contains("minecraft:cobblestone"), "searching finds items by name: " + shown.size() + " results");
					String first = shown.isEmpty() ? "" : shown.getFirst();
					if (!massDropBefore.contains(first)) {
						screen.toggleFirstForSnapshot();
					}
					check(EMUtilsClient.massDrop().store().contains(first), "clicking a result adds it to the list: " + first);
				}
				next();
			}
			case 149 -> captureAfter(client, 15, "mass drop, search");
			case 150 -> {
				if (MinecraftClientCompat.screen(client) instanceof MassDropItemsScreen screen) {
					screen.searchForSnapshot("");
					check(screen.shownForSnapshot().stream().allMatch(EMUtilsClient.massDrop().store()::contains), "with no search, the list shows the dropped items");
				}
				next();
			}
			case 151 -> captureAfter(client, 15, "mass drop, list with items");
			case 152 -> {
				for (String id : EMUtilsClient.massDrop().store().itemIds()) {
					if (!massDropBefore.contains(id)) {
						EMUtilsClient.massDrop().store().toggle(id);
					}
				}
				client.gui.setScreen(null);
				next();
			}
			// The HUD Layout Editor (#136): select an element, nudge it, and cancel without saving.
			case 153 -> {
				if (HudLayoutManager.beginEditorSession(EMUtilsClient.MOD_ID, client)) {
					client.gui.setScreen(new HudEditorScreen(null));
				}
				next();
			}
			case 154 -> captureAfter(client, 20, "hud editor");
			case 155 -> {
				if (MinecraftClientCompat.screen(client) instanceof HudEditorScreen screen) {
					screen.selectFirstForSnapshot();
					HudLayoutDraft before = screen.selectedDraftForSnapshot();
					for (int i = 0; i < 3; i++) {
						press(screen, InputConstants.KEY_RIGHT, 0);
					}
					HudLayoutDraft after = screen.selectedDraftForSnapshot();
					check(before != null && after != null && after.x() == before.x() + 3, "arrow keys nudge the selected element: " + (before == null ? "none" : before.x()) + " -> " + (after == null ? "none" : after.x()));
				}
				next();
			}
			case 156 -> captureAfter(client, 15, "hud editor, selected");
			// Dragging the Size slider over several frames: the card must stay put and the size only grow.
			case 157 -> {
				if (MinecraftClientCompat.screen(client) instanceof HudEditorScreen screen) {
					if (stepTicks == 1) {
						sliderCardX = screen.cardXForSnapshot();
						sliderScales.clear();
					}
					screen.dragSizeSliderForSnapshot(0.1F + stepTicks * 0.04F);
					HudLayoutDraft draft = screen.selectedDraftForSnapshot();
					sliderScales.add(draft == null ? -1 : draft.scale());
					boolean steady = screen.cardXForSnapshot() == sliderCardX;
					if (stepTicks >= 12 || !steady) {
						boolean rising = true;
						for (int i = 1; i < sliderScales.size(); i++) {
							rising &= sliderScales.get(i) >= sliderScales.get(i - 1);
						}
						check(steady && rising, "dragging the Size slider keeps the card still and the size steady: " + sliderScales);
						screen.dragSizeSliderForSnapshot(null);
						next();
					}
				}
			}
			case 158 -> captureAfter(client, 10, "hud editor, resized");
			case 159 -> {
				if (MinecraftClientCompat.screen(client) instanceof HudEditorScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
					check(MinecraftClientCompat.screen(client) instanceof HudEditorScreen, "Esc first deselects, keeping the editor open");
					press(screen, InputConstants.KEY_ESCAPE, 0);
				}
				check(MinecraftClientCompat.screen(client) == null && !HudLayoutManager.isEditing(), "Esc again cancels and closes the editor");
				next();
			}
			// The Spotify players (#129), with whatever the real Spotify app is playing on this machine.
			case 160 -> {
				EMUtilsClient.config().setSpotifyEnabled(true);
				EMUtilsClient.config().setSpotifyHudOverlay(true);
				EMUtilsClient.config().setSpotifyPlayerEnabled(true);
				EMUtilsClient.config().setSettingsUiDark(true);
				setGuiScale(client, 2);
				EMUtilsClient.spotify().refreshSoon();
				next();
			}
			case 161 -> {
				SpotifyTrackState state = EMUtilsClient.spotify().state();
				// Waits for the song, then a moment longer for its cover.
				if (stepTicks >= 100 || (state.hasTrack() && stepTicks >= 60)) {
					EMUtilsClient.LOGGER.info("EMUtils UI snapshot: Spotify state {} '{}' by '{}', {}/{} ms, cover {}", state.kind(), state.title(), state.artist(), state.positionMs(), state.durationMs(), state.artUrl().isEmpty() ? "none" : "found");
					next();
				}
			}
			case 162 -> captureAfter(client, 0, "spotify hud, gui scale 2, dark");
			case 163 -> {
				client.gui.setScreen(new PauseScreen(true));
				next();
			}
			case 164 -> captureAfter(client, 20, "spotify pause menu, gui scale 2, dark");
			case 165 -> {
				EMUtilsClient.config().setSettingsUiDark(false);
				setGuiScale(client, 3);
				next();
			}
			case 166 -> captureAfter(client, 20, "spotify pause menu, gui scale 3, light");
			case 167 -> {
				client.gui.setScreen(null);
				next();
			}
			case 168 -> captureAfter(client, 15, "spotify hud, gui scale 3, light");
			case 169 -> {
				EMUtilsClient.config().setSettingsUiDark(true);
				setGuiScale(client, 4);
				next();
			}
			case 170 -> captureAfter(client, 15, "spotify hud, gui scale 4, dark");
			case 171 -> {
				setGuiScale(client, 2);
				if (HudLayoutManager.beginEditorSession(EMUtilsClient.MOD_ID, client)) {
					HudLayoutManager.setDraftLayout(EMUtilsHudElements.SPOTIFY, 20, 20, 200, 60);
					client.gui.setScreen(new HudEditorScreen(null));
				}
				next();
			}
			case 172 -> captureAfter(client, 20, "spotify hud at 200% with a 60% background, hud editor");
			case 173 -> {
				if (MinecraftClientCompat.screen(client) instanceof HudEditorScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
					press(screen, InputConstants.KEY_ESCAPE, 0);
				}
				next();
			}
			// Play/pause shows right away and holds while Spotify catches up, then toggles back.
			case 174 -> {
				SpotifyTrackState before = EMUtilsClient.spotify().state();
				if (!before.hasTrack()) {
					EMUtilsClient.LOGGER.info("EMUtils UI snapshot: no Spotify track, skipping the play/pause check");
					step = 177;
					stepTicks = 0;
					return;
				}
				spotifyWasPlaying = before.playing();
				EMUtilsClient.spotify().playPause();
				check(EMUtilsClient.spotify().state().playing() != spotifyWasPlaying, "play/pause shows the new state right away");
				next();
			}
			case 175 -> {
				if (stepTicks >= 20) {
					check(EMUtilsClient.spotify().state().playing() != spotifyWasPlaying, "play/pause still shows the new state after Spotify's polls: " + EMUtilsClient.spotify().state().playing());
					EMUtilsClient.spotify().playPause();
					next();
				}
			}
			case 176 -> {
				if (stepTicks >= 20) {
					check(EMUtilsClient.spotify().state().playing() == spotifyWasPlaying, "play/pause again returns to the old state");
					next();
				}
			}
			// A small window: the pause menu card shrinks to fit below the menu's buttons.
			case 177 -> {
				setGuiScale(client, 4);
				client.gui.setScreen(new PauseScreen(true));
				next();
			}
			case 178 -> captureAfter(client, 20, "spotify pause menu, gui scale 4");
			// A title too long for the card scrolls through after a pause.
			case 179 -> {
				client.gui.setScreen(null);
				setGuiScale(client, 2);
				EMUtilsClient.spotify().setStateForSnapshot(SpotifyTrackState.track("A Real Hero (feat. Electric Youth) - Drive Original Soundtrack", "College, Electric Youth", true, "", 65_000L, 267_000L));
				next();
			}
			case 180 -> captureAfter(client, 10, "spotify hud, long title before scrolling");
			case 181 -> captureAfter(client, 60, "spotify hud, long title scrolling");
			case 182 -> {
				EMUtilsClient.config().setSpotifyHudScrollTitles(false);
				next();
			}
			case 183 -> captureAfter(client, 5, "spotify hud, long title with scrolling off");
			case 184 -> {
				EMUtilsClient.config().setSpotifyHudScrollTitles(true);
				EMUtilsClient.spotify().setStateForSnapshot(null);
				next();
			}
			// The HUD Overlay (#93) with every line on.
			case 185 -> {
				EMUtilsConfig config = EMUtilsClient.config();
				config.setSpotifyHudOverlay(false);
				config.setHudOverlay(true);
				config.setHudShowIcons(true);
				config.setHudShowCoordinates(true);
				config.setHudShowNetherCoordinates(true);
				config.setHudShowChunkRegion(true);
				config.setHudShowBiome(true);
				config.setHudShowPing(true);
				config.setHudShowFps(true);
				config.setHudShowFacing(true);
				config.setHudShowSpeed(true);
				config.setHudShowServerTime(true);
				config.setHudShowRealTime(true);
				config.setHudShowMemory(true);
				config.setSettingsUiDark(true);
				setGuiScale(client, 2);
				next();
			}
			case 186 -> captureAfter(client, 20, "hud overlay, gui scale 2, dark");
			// Values change every tick; they come from cached glyphs, so no new text textures appear.
			case 187 -> {
				if (stepTicks == 1) {
					hudTextures = UiFontRenderer.texturesMade();
				}
				if (stepTicks >= 60) {
					int made = UiFontRenderer.texturesMade() - hudTextures;
					check(made <= 2, "the HUD overlay's changing values make no new text textures: " + made + " in 3 s");
					next();
				}
			}
			case 188 -> {
				EMUtilsClient.config().setSettingsUiDark(false);
				setGuiScale(client, 3);
				next();
			}
			case 189 -> captureAfter(client, 15, "hud overlay, gui scale 3, light");
			case 190 -> {
				EMUtilsClient.config().setSettingsUiDark(true);
				setGuiScale(client, 4);
				next();
			}
			case 191 -> captureAfter(client, 15, "hud overlay, gui scale 4, dark");
			case 192 -> {
				setGuiScale(client, 2);
				if (HudLayoutManager.beginEditorSession(EMUtilsClient.MOD_ID, client)) {
					HudLayoutManager.setDraftLayout(EMUtilsHudElements.INFO_OVERLAY, 20, 20, 200, 20);
					client.gui.setScreen(new HudEditorScreen(null));
				}
				next();
			}
			case 193 -> captureAfter(client, 20, "hud overlay at 200% with a 20% background, hud editor");
			case 194 -> {
				if (MinecraftClientCompat.screen(client) instanceof HudEditorScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
					press(screen, InputConstants.KEY_ESCAPE, 0);
				}
				next();
			}
			default -> {
				EMUtilsClient.LOGGER.info("EMUtils UI snapshots done; stopping Minecraft.");
				enabled = false;
				client.stop();
			}
		}
	}

	private static void setGuiScale(Minecraft client, int guiScale) {
		if (client.options.guiScale().get() != guiScale) {
			client.options.guiScale().set(guiScale);
			client.resizeGui();
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
	private static final int SCRIPTS_CLEANUP_STEP = 130;
	private static List<String> shortcutsBefore = List.of();
	private static java.util.Set<String> massDropBefore = java.util.Set.of();
	private static int sliderCardX;
	private static final List<Integer> sliderScales = new java.util.ArrayList<>();
	private static final String EDIT_EXPECTED = "def greet(name):\n    print(\"hi\")\ngreet(1)";
	private static @Nullable String savedMinescriptConfig;

	private static String readMinescriptConfig() {
		try {
			return Files.readString(MinescriptCompat.scriptsDir().resolve("config.txt"));
		} catch (IOException exception) {
			return "";
		}
	}

	private static void writeMinescriptConfig(String text) {
		try {
			Files.writeString(MinescriptCompat.scriptsDir().resolve("config.txt"), text);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not write the snapshot Minescript config.", exception);
		}
	}

	private static void deleteQuietly(Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException ignored) {
			// Checked by the step that follows.
		}
	}

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
			Files.writeString(folder.resolve("edit.py"), "");
			Files.writeString(folder.resolve("broken.py"), "x = 1\nprint(undefined_name)\n");
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

	private static void press(Screen screen, int key, int modifiers) {
		screen.keyPressed(new KeyEvent(key, 0, modifiers));
	}

	private static void type(Screen screen, String text) {
		text.codePoints().forEach(codepoint -> screen.charTyped(new CharacterEvent(codepoint)));
	}

	/**
	 * A screenshot arriving between a frame and a click refreshes the gallery's list; clicking the first
	 * tile drawn in that frame must still open that screenshot, not whichever moved into its place (#143).
	 */
	private static void checkGalleryClickAfterRefresh(Minecraft client, GalleryScreen screen) {
		GalleryScreen.TileForSnapshot tile = screen.firstTileForSnapshot();
		if (tile == null) {
			check(false, "the gallery drew a tile to click");
			return;
		}
		Path copy = tile.path().resolveSibling("emutils-snapshot-newest.png");
		try {
			Files.copy(tile.path(), copy, StandardCopyOption.REPLACE_EXISTING);
			Files.setLastModifiedTime(copy, FileTime.fromMillis(System.currentTimeMillis() + 60_000L));
			screen.refreshForSnapshot();
			MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
			screen.mouseClicked(new MouseButtonEvent(tile.x(), tile.y(), left), false);
			screen.mouseReleased(new MouseButtonEvent(tile.x(), tile.y(), left));
			check(tile.path().equals(screen.previewForSnapshot()), "a gallery click right after a new screenshot opens the screenshot that was clicked (" + screen.previewForSnapshot() + ")");
		} catch (IOException exception) {
			check(false, "the gallery click check could set up its screenshot: " + exception);
		} finally {
			screen.closePreviewForSnapshot();
			try {
				Files.deleteIfExists(copy);
			} catch (IOException ignored) {
			}
			screen.refreshForSnapshot();
		}
	}

	/** Text fields keep emoji (surrogate pairs) whole when placing the caret, deleting and cutting (#143). */
	private static void checkTextFieldSurrogates() {
		String emoji = new String(Character.toChars(0x1F600));
		UiTextField field = new UiTextField(new Object(), 32);
		field.setFocused(true);
		field.setText("a" + emoji + "b");
		field.select(2, 2);
		field.charTyped(new CharacterEvent('X'), () -> {
		});
		check(field.text().equals("aX" + emoji + "b"), "a caret placed inside an emoji moves before it");
		field.select(5, 5);
		field.keyPressed(new KeyEvent(InputConstants.KEY_BACKSPACE, 0, 0), () -> {
		});
		field.keyPressed(new KeyEvent(InputConstants.KEY_BACKSPACE, 0, 0), () -> {
		});
		check(field.text().equals("aX"), "Backspace removes a whole emoji");
		field.setFocused(false);
		UiTextField short_ = new UiTextField(new Object(), 3);
		short_.setText(emoji + emoji);
		check(short_.text().equals(emoji), "the length limit doesn't cut an emoji in half");
	}

	private static long configModified() {
		try {
			return Files.getLastModifiedTime(EMUtilsPaths.configFile()).toMillis();
		} catch (IOException exception) {
			return -1L;
		}
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
