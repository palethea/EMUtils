package net.emutils.client.emutils.debug;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.accessor.KeyBindingAccess;
import net.emutils.client.emutils.capes.CapeAnimations;
import net.emutils.client.emutils.capes.CapeSource;
import net.emutils.client.emutils.capes.CustomCapeManager;
import net.emutils.client.emutils.gui.ui.UiCodeFont;
import net.emutils.client.emutils.gui.ui.UiFontFamily;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiMotion;
import net.emutils.client.emutils.gui.hub.HubFeature;
import net.emutils.client.emutils.profile.Profile;
import net.emutils.client.emutils.profile.ProfileColor;
import net.emutils.client.emutils.profile.ProfileIcon;
import net.emutils.client.emutils.profile.ProfileManager;
import net.emutils.client.emutils.profile.gui.ProfilesScreen;
import net.emutils.client.EMUtilsHudElements;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.commandshortcuts.CommandShortcut;
import net.emutils.client.emutils.commandshortcuts.gui.CommandShortcutsScreen;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.settings.KeybindsScreen;
import net.emutils.client.emutils.hud.ArmorStatusDisplay;
import net.emutils.client.emutils.hud.ArmorStatusRenderer;
import net.emutils.client.emutils.hud.ClickCounter;
import net.emutils.client.emutils.tweaks.FreeCameraManager;
import net.emutils.client.emutils.hud.HudOverlayData;
import net.emutils.client.emutils.hud.HudOverlayLine;
import net.emutils.client.emutils.hud.HudOverlayRenderer;
import net.emutils.client.emutils.hud.HudTextShadow;
import net.emutils.client.emutils.hud.HudTpsTracker;
import net.emutils.client.emutils.hud.KeystrokesStyle;
import net.emutils.client.emutils.hud.LookAtInfoData;
import net.emutils.client.emutils.gui.settings.SettingsIconButton;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.config.ConfigTransfer;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiFontRenderer;
import net.emutils.client.emutils.gui.ui.UiLoadingOverlay;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.hud.editor.HudEditorScreen;
import net.emutils.client.emutils.hud.layout.HudLayoutDraft;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.inventory.InventorySearch;
import net.emutils.client.emutils.inventory.gui.MassDropItemsScreen;
import net.emutils.client.emutils.inventory.InventorySortMode;
import net.emutils.client.emutils.inventory.InventorySortSpeed;
import net.emutils.client.emutils.minescript.MinescriptKeyBinding;
import net.emutils.client.emutils.minescript.MinescriptKeybindStore;
import net.emutils.client.emutils.minescript.MinescriptPython;
import net.emutils.client.emutils.minescript.gui.ScriptsScreen;
import net.emutils.client.emutils.packs.PackType;
import net.emutils.client.emutils.packs.ResourcePackController;
import net.emutils.client.emutils.packs.gui.PacksScreen;
import net.emutils.client.emutils.screenshot.gui.GalleryScreen;
import net.emutils.client.emutils.spotify.SpotifyTrackState;
import net.emutils.client.emutils.render.BeaconRadiusRenderer;
import net.emutils.client.emutils.render.LightLevelOverlayRenderer;
import net.emutils.client.emutils.tweaks.AntiDurabilityBreak;
import net.emutils.client.emutils.tweaks.AntiDurabilityUnit;
import net.emutils.client.emutils.tweaks.FreelookManager;
import net.emutils.client.emutils.tweaks.SkyFlashAccess;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.emutils.client.emutils.waypoint.WaypointCoordinateFormat;
import net.emutils.client.emutils.waypoint.WaypointCoordinates;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.versioned.VersionedScreens;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.emutils.client.mixin.MouseAccess;
import net.emutils.client.mixin.HandledScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Development aid: with {@code -Demutils.uiSnapshot=true} (Gradle property {@code emutilsUiSnapshot}),
 * the client enters a test world, opens the new settings screen, saves screenshots of it at GUI
 * scales 3 (dark and light), 2, 1 and 4, then a few settings sheets and the color picker at each
 * scale, closes the screen, and quits. Screenshots land in the run directory's {@code screenshots}
 * folder, named after their step and label (for example {@code 151-mass-drop-list-with-items.png}),
 * so a rerun replaces the old ones.
 *
 * <p>To check only a few screens, run a range of steps with {@code emutilsUiSnapshotFrom} and
 * {@code emutilsUiSnapshotTo} (both inclusive). Include the section's setup and cleanup steps, since
 * the run stops right after the last one.
 */
public final class UiSnapshotter {
	private static final String ENABLED_PROPERTY = "emutils.uiSnapshot";
	private static final int SETTLE_TICKS = 25;

	private static boolean enabled = Boolean.getBoolean(ENABLED_PROPERTY);
	private static int worldTicks;
	/** {@code -Demutils.uiSnapshotFrom=N} (Gradle property {@code emutilsUiSnapshotFrom}) starts at step N. */
	private static int step = Integer.getInteger("emutils.uiSnapshotFrom", 0);
	/** {@code -Demutils.uiSnapshotTo=N} (Gradle property {@code emutilsUiSnapshotTo}) stops after step N. */
	private static final int LAST_STEP = Integer.getInteger("emutils.uiSnapshotTo", Integer.MAX_VALUE);
	private static int stepTicks;
	/** The screenshots the gallery showed the first time it opened. */
	private static List<Path> galleryShown = List.of();
	private static long configModifiedBefore;
	private static boolean configCheckPending;
	private static boolean leftWorld;
	private static boolean spotifyWasPlaying;
	private static int hudTextures;
	/** Where the player stood when the quick wins section began; its test blocks are placed around it. */
	private static BlockPos quickWinsOrigin = BlockPos.ZERO;
	private static BlockPos sortChest = BlockPos.ZERO;
	private static int armorStatusSounds;
	private static Vec3 freeCameraStart = Vec3.ZERO;
	private static int freeCameraWallX;
	private static int freeCameraArrivedTick = -1;
	private static BlockPos searchChest = BlockPos.ZERO;
	private static int warningsBefore;
	private static int lightLevelSpotsBefore;
	private static int lightningSeenAt = -1;
	private static int beaconLinesBefore;

	private UiSnapshotter() {
	}

	public static void tick(Minecraft client) {
		// The last steps leave the world on purpose, to check screens opened from the title screen.
		if (!enabled || !leftWorld && (client.level == null || client.player == null)) {
			return;
		}

		worldTicks++;
		if (worldTicks < 40) {
			return;
		}
		if (worldTicks == 40) {
			clearOldSnapshots(client);
		}
		stepTicks++;
		if (step > LAST_STEP) {
			finish(client);
			return;
		}
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
					check(screen.clickMenusButton(), "a left click on the palette button opens the menu settings");
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
					// Compares files, not counts: the gallery lists at most its newest 200.
					if (listed.stream().anyMatch(file -> !galleryShown.contains(file))) {
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
					grab(client, "packs with the test pack installed");
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
			case 164 -> {
				if (stepTicks == 5 && MinecraftClientCompat.screen(client) instanceof PauseScreen pause) {
					checkIconRow(pause, "pause menu");
				}
				captureAfter(client, 20, "spotify pause menu, gui scale 2, dark");
			}
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
			// Profiles (#89): the switcher, the list and its sheets, then switching by hand and automatically.
			case 195 -> {
				setGuiScale(client, 2);
				EMUtilsClient.config().setSettingsUiDark(true);
				ProfileManager profiles = EMUtilsClient.profiles();
				if (profiles.byName("Hypixel") == null) {
					profiles.create("Hypixel", ProfileIcon.SWORDS, ProfileColor.ORANGE, List.of("hypixel.net"), false, true);
				}
				if (profiles.byName("Singleplayer") == null) {
					profiles.create("Singleplayer", ProfileIcon.HOUSE, ProfileColor.GREEN, List.of(), true, false);
				}
				SettingsScreen settings = new SettingsScreen(null);
				client.gui.setScreen(settings);
				settings.openProfileMenuForSnapshot();
				next();
			}
			case 196 -> captureAfter(client, 15, "settings, profile menu");
			case 197 -> {
				client.gui.setScreen(new ProfilesScreen(MinecraftClientCompat.screen(client)));
				next();
			}
			case 198 -> captureAfter(client, 15, "profiles, dark");
			case 199 -> {
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					screen.openSheetForSnapshot(null, "Building", "play.example.net, hypixel.net", false);
				}
				next();
			}
			case 200 -> captureAfter(client, 15, "profiles, new profile sheet taking a server from another profile");
			case 201 -> {
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
					screen.openSheetForSnapshot("Hypixel", null, "", false);
				}
				next();
			}
			case 202 -> captureAfter(client, 15, "profiles, edit sheet");
			case 203 -> {
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
				}
				checkProfileSwitching(client);
				next();
			}
			case 204 -> {
				setGuiScale(client, 3);
				EMUtilsClient.config().setSettingsUiDark(false);
				next();
			}
			case 205 -> captureAfter(client, 20, "profiles, gui scale 3, light, singleplayer profile active");
			case 206 -> {
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					screen.confirmDeleteForSnapshot("Hypixel");
				}
				next();
			}
			case 207 -> captureAfter(client, 15, "profiles, delete dialog");
			// The server list picker, a refused duplicate name, the two-column sheet, and a long profile list.
			case 208 -> {
				seedServerList(client);
				setGuiScale(client, 2);
				EMUtilsClient.config().setSettingsUiDark(true);
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
					screen.openSheetForSnapshot("Singleplayer", null, "", false);
				}
				next();
			}
			case 209 -> {
				if (stepTicks == 2 && MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					String picked = screen.pickFirstServerForSnapshot();
					check("mc.hypixel.net".equals(picked), "the server picker lists the multiplayer servers (" + picked + ")");
				}
				captureAfter(client, 15, "profiles, edit sheet with the server list open");
			}
			// A long server list: typing filters it, it scrolls, and it stays on a short screen.
			case 210 -> {
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					type(screen, "wynn");
					check(screen.pickerCountForSnapshot() == 1 && "play.wynncraft.com".equals(screen.pickerFirstForSnapshot()), "typing filters the server list (" + screen.pickerCountForSnapshot() + " shown)");
				}
				next();
			}
			case 211 -> captureAfter(client, 10, "profiles, server list filtered");
			case 212 -> {
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
					check(screen.pickerCountForSnapshot() == 27, "Esc clears the filter and keeps the list open (" + screen.pickerCountForSnapshot() + " shown)");
				}
				next();
			}
			case 213 -> {
				if (stepTicks == 3 && MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					check(screen.pickerScrollToEndForSnapshot(), "a long server list scrolls");
				}
				captureAfter(client, 25, "profiles, server list with 27 servers, scrolled to the end");
			}
			case 214 -> {
				setGuiScale(client, 4);
				next();
			}
			case 215 -> {
				if (stepTicks == 15 && MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					check(screen.pickerFitsForSnapshot(), "the server list stays on screen at gui scale 4");
				}
				captureAfter(client, 15, "profiles, server list at gui scale 4");
			}
			case 216 -> {
				setGuiScale(client, 2);
				next();
			}
			case 217 -> {
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
					press(screen, InputConstants.KEY_ESCAPE, 0);
					screen.openSheetForSnapshot(null, "hypixel ", "", false);
					check(screen.sheetNameTakenForSnapshot(), "a name another profile has is refused, ignoring case");
				}
				next();
			}
			case 218 -> captureAfter(client, 15, "profiles, new profile sheet with a taken name");
			case 219 -> {
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
					setGuiScale(client, 4);
					screen.openSheetForSnapshot(null, "Building", "", false);
				}
				next();
			}
			case 220 -> {
				if (stepTicks == 15 && MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					check(screen.sheetWideForSnapshot() && screen.sheetBottomForSnapshot() <= screen.height, "the sheet fits a short window in two columns (bottom " + screen.sheetBottomForSnapshot() + " of " + screen.height + ")");
				}
				captureAfter(client, 15, "profiles, new profile sheet at gui scale 4");
			}
			case 221 -> {
				if (MinecraftClientCompat.screen(client) instanceof ProfilesScreen screen) {
					press(screen, InputConstants.KEY_ESCAPE, 0);
				}
				ProfileManager profiles = EMUtilsClient.profiles();
				for (int i = 1; profiles.profiles().size() < 14; i++) {
					profiles.create("Test " + i, ProfileIcon.values()[i % ProfileIcon.values().length], ProfileColor.values()[i % ProfileColor.values().length], List.of(), false, false);
				}
				setGuiScale(client, 3);
				SettingsScreen settings = new SettingsScreen(null);
				client.gui.setScreen(settings);
				settings.openProfileMenuForSnapshot();
				next();
			}
			case 222 -> {
				if (stepTicks == 3 && MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					check(screen.scrollProfileMenuForSnapshot(), "a profile list taller than the window scrolls");
				}
				captureAfter(client, 25, "settings, profile menu with 14 profiles, scrolled to the end");
			}
			case 223 -> {
				checkProfileReset();
				next();
			}
			case 224 -> {
				ProfileManager profiles = EMUtilsClient.profiles();
				profiles.pick(profiles.profiles().getFirst());
				for (Profile profile : profiles.profiles()) {
					profiles.delete(profile);
				}
				check(profiles.profiles().size() == 1 && profiles.active().isDefault(), "profiles deleted, back on Default");
				setGuiScale(client, 2);
				EMUtilsClient.config().setSettingsUiDark(true);
				client.gui.setScreen(null);
				next();
			}
			// Free Camera: the world keeps rendering around the camera, not just around the player.
			case 225 -> {
				EMUtilsClient.config().setTweakFreeCamera(true);
				next();
			}
			case 226 -> {
				if (stepTicks == 5 && client.player != null) {
					check(EMUtilsClient.tweaks().freeCamera().isActive() && client.getCameraEntity() != client.player, "free camera takes over the view");
					EMUtilsClient.tweaks().freeCamera().placeForSnapshot(client.player.getX(), client.player.getY() + 30, client.player.getZ(), client.player.getYRot(), 35.0F);
				}
				captureAfter(client, 60, "free camera, 30 blocks above the player");
			}
			case 227 -> {
				if (stepTicks == 1 && client.player != null) {
					EMUtilsClient.tweaks().freeCamera().placeForSnapshot(client.player.getX() + 64, client.player.getY() + 20, client.player.getZ() + 64, 135.0F, 20.0F);
				}
				captureAfter(client, 80, "free camera, 90 blocks from the player, looking back");
			}
			case 228 -> {
				if (stepTicks == 1 && client.player != null) {
					EMUtilsClient.tweaks().freeCamera().placeForSnapshot(client.player.getX() - 48, client.player.getY() + 12, client.player.getZ(), -90.0F, 10.0F);
				}
				captureAfter(client, 80, "free camera, 48 blocks west, looking east");
			}
			case 229 -> {
				if (stepTicks == 1 && client.player != null) {
					EMUtilsClient.tweaks().freeCamera().placeForSnapshot(client.player.getX(), client.player.getY() - 6, client.player.getZ(), 0.0F, 0.0F);
				}
				captureAfter(client, 60, "free camera, inside the ground");
			}
			case 230 -> {
				EMUtilsClient.config().setTweakFreeCamera(false);
				next();
			}
			case 231 -> {
				if (stepTicks == 5) {
					check(!EMUtilsClient.tweaks().freeCamera().isActive() && client.getCameraEntity() == client.player, "turning free camera off gives the view back to the player");
					next();
				}
			}
			// Menu settings (#120): theme, accent (picked, custom, or the profile's color) and fonts.
			case 232 -> {
				setGuiScale(client, 2);
				EMUtilsClient.config().resetMenuSettings();
				SettingsScreen settings = new SettingsScreen(null);
				client.gui.setScreen(settings);
				settings.openSheet("menus");
				next();
			}
			case 233 -> captureAfter(client, 15, "menu settings, dark");
			case 234 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setUiAccent(0xFF2F6FD6);
					check(UiTheme.current().accent() == 0xFF2F6FD6, "the picked accent colors the menus");
				}
				captureAfter(client, 20, "menu settings, blue accent");
			}
			case 235 -> {
				if (stepTicks == 1) {
					ProfileManager profiles = EMUtilsClient.profiles();
					Profile orange = profiles.create("Accent test", ProfileIcon.SWORDS, ProfileColor.ORANGE, List.of(), false, true);
					if (orange != null) {
						profiles.pick(orange);
					}
					EMUtilsClient.config().setUiAccentFromProfile(true);
					check(UiTheme.accentColor() == ProfileColor.ORANGE.argb() && EMUtilsClient.config().uiAccent() == 0xFF2F6FD6, "the accent follows the profile, and the picked one is kept (and carried over to the new profile)");
				}
				captureAfter(client, 20, "menu settings, accent from the orange profile");
			}
			case 236 -> {
				if (stepTicks == 1) {
					ProfileManager profiles = EMUtilsClient.profiles();
					Profile test = profiles.byName("Accent test");
					profiles.pick(profiles.profiles().getFirst());
					if (test != null) {
						profiles.delete(test);
					}
					check(EMUtilsClient.config().uiAccentFromProfile(), "menu settings carry over when switching back to Default");
					EMUtilsClient.config().setUiAccentFromProfile(false);
					EMUtilsClient.config().setUiAccent(0xFFFFD84D);
					EMUtilsClient.config().setSettingsUiDark(false);
					check(UiTheme.current().onAccent() == 0xFF141817, "text on a light accent turns dark");
				}
				captureAfter(client, 25, "menu settings, light theme, yellow custom accent");
			}
			case 237 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					EMUtilsClient.config().resetMenuSettings();
					screen.searchFor("");
					Component sample = Component.literal("Every little job");
					int nunito = UiText.width(client.font, sample, UiText.Size.BODY);
					EMUtilsClient.config().setUiFont(UiFontFamily.INTER);
					int inter = UiText.width(client.font, sample, UiText.Size.BODY);
					check(nunito != inter, "switching the font changes how text measures (" + nunito + " to " + inter + ")");
				}
				captureAfter(client, 15, "settings, Inter");
			}
			case 238 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setUiFont(UiFontFamily.RUBIK);
				}
				captureAfter(client, 15, "settings, Rubik");
			}
			case 239 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setUiFont(UiFontFamily.FIGTREE);
				}
				captureAfter(client, 15, "settings, Figtree");
			}
			case 240 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setUiFont(UiFontFamily.MINECRAFT);
				}
				captureAfter(client, 15, "settings, Minecraft font");
			}
			case 241 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					screen.openSheet("menus");
				}
				captureAfter(client, 15, "menu settings, Minecraft font");
			}
			case 242 -> {
				EMUtilsClient.config().setUiFont(UiFontFamily.NUNITO);
				EMUtilsClient.config().setUiCodeFont(UiCodeFont.FIRA_CODE);
				EMUtilsClient.config().setUiCodeSize(130);
				writeTestScripts();
				client.gui.setScreen(new ScriptsScreen(null));
				next();
			}
			case 243 -> {
				if (stepTicks == 15 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
					screen.openForSnapshot(TEST_SCRIPT_FOLDER + "/hello.py");
				}
				captureAfter(client, 40, "scripts, Fira Code at 130%");
			}
			case 244 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setUiCodeFont(UiCodeFont.CASCADIA_CODE);
					EMUtilsClient.config().setUiCodeSize(100);
				}
				captureAfter(client, 20, "scripts, Cascadia Code");
			}
			// More menu settings (#149): text size, compact cards, corners, contrast, background and motion.
			case 245 -> {
				EMUtilsClient.config().resetMenuSettings();
				EMUtilsClient.config().setUiTextSize(125);
				client.gui.setScreen(new SettingsScreen(null));
				next();
			}
			case 246 -> captureAfter(client, 15, "settings, text at 125%");
			case 247 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setUiTextSize(100);
					EMUtilsClient.config().setUiCompactCards(true);
				}
				captureAfter(client, 15, "settings, compact cards");
			}
			case 248 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setUiCompactCards(false);
					EMUtilsClient.config().setUiCornerRoundness(0);
					EMUtilsClient.config().setUiHighContrast(true);
					EMUtilsClient.config().setUiCategoryColors(false);
					check(UiTheme.current().groupColor(HubFeature.Group.RENDER) == UiTheme.current().muted(), "category colors can be turned off");
					check(UiTheme.current().muted() != UiTheme.DARK.muted(), "high contrast strengthens secondary text");
				}
				captureAfter(client, 15, "settings, square corners, high contrast, no category colors");
			}
			case 249 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					screen.openSheet("menus");
				}
				captureAfter(client, 15, "menu settings, square corners, high contrast");
			}
			case 250 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					EMUtilsClient.config().resetMenuSettings();
					EMUtilsClient.config().setUiBackgroundBlur(0);
					EMUtilsClient.config().setUiBackgroundDim(30);
					EMUtilsClient.config().setUiPanelOpacity(70);
					screen.searchFor("");
				}
				captureAfter(client, 20, "settings, no blur, 30% dimming, 70% panel");
			}
			case 251 -> {
				EMUtilsClient.config().resetMenuSettings();
				EMUtilsClient.config().setUiMotion(UiMotion.OFF);
				UiAnim motion = new UiAnim();
				motion.transition("check", 0.0F, 0.2F, true);
				boolean instant = motion.transition("check", 1.0F, 0.2F, true) == 1.0F && motion.towards("hover", 0.0F, 16.0F) == 0.0F && motion.towards("hover", 1.0F, 16.0F) == 1.0F;
				EMUtilsClient.config().setUiMotion(UiMotion.FAST);
				UiAnim fast = new UiAnim();
				fast.transition("check", 0.0F, 10.0F);
				fast.transition("check", 1.0F, 10.0F);
				boolean moving = fast.transition("check", 1.0F, 10.0F) < 1.0F;
				check(instant && moving, "Animations Off jumps straight to the end, Fast still animates");
				EMUtilsClient.config().setUiMotion(UiMotion.NORMAL);
				next();
			}
			// The menu settings' tabs (#149).
			case 252 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().resetMenuSettings();
					SettingsScreen settings = new SettingsScreen(null);
					client.gui.setScreen(settings);
					settings.openSheet("menus");
					settings.selectSheetSectionForSnapshot(1);
				}
				captureAfter(client, 20, "menu settings, Fonts tab");
			}
			case 253 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					screen.selectSheetSectionForSnapshot(2);
				}
				captureAfter(client, 15, "menu settings, Layout tab");
			}
			case 254 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof SettingsScreen screen) {
					screen.selectSheetSectionForSnapshot(3);
				}
				captureAfter(client, 15, "menu settings, Effects tab");
			}
			// Quick wins (#40, #41, #42, #51), checked in the world with commands (the test world allows them).
			// Copy Coordinates (#42): the player's position in the Coord Format, or the free camera's.
			case 255 -> {
				client.gui.setScreen(null);
				setGuiScale(client, 2);
				// Start on the surface: the world is new every run and may spawn the player in a cave,
				// where beacon beams can't reach the sky.
				BlockPos spawn = client.player.blockPosition();
				quickWinsOrigin = new BlockPos(spawn.getX(), client.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ()), spawn.getZ());
				command(client, "difficulty peaceful");
				command(client, "time set day");
				command(client, "tp @s " + at(0, 0, 0) + " 0 0");
				EMUtilsClient.config().setWaypointCoordinateFormat(WaypointCoordinateFormat.COMMA);
				EMUtilsClient.config().setCopyCoordinatesFeedback(true);
				next();
			}
			case 256 -> {
				if (stepTicks == 10) {
					EMUtilsClient.waypoint().copyCurrentCoordinates(client);
					BlockPos pos = client.player.blockPosition();
					check(pos.equals(quickWinsOrigin), "the player stands on the surface for the quick wins checks: " + pos);
					String copied = client.keyboardHandler.getClipboard();
					check((pos.getX() + ", " + pos.getY() + ", " + pos.getZ()).equals(copied), "Copy Coordinates copies the player's position in the Coord Format (" + copied + ")");
				}
				captureAfter(client, 20, "copy coordinates, chat feedback");
			}
			case 257 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setTweakFreeCamera(true);
				}
				if (stepTicks == 10) {
					BlockPos camera = EMUtilsClient.tweaks().freeCamera().cameraBlockPosition();
					EMUtilsClient.waypoint().copyCurrentCoordinates(client);
					String copied = client.keyboardHandler.getClipboard();
					check(camera != null && WaypointCoordinates.format(camera.getX(), camera.getY(), camera.getZ(), WaypointCoordinateFormat.COMMA).equals(copied), "with Free Camera on, Copy Coordinates copies the camera's position (" + copied + ")");
					EMUtilsClient.config().setTweakFreeCamera(false);
					EMUtilsClient.config().setWaypointCoordinateFormat(WaypointCoordinateFormat.PLAIN);
					next();
				}
			}
			case 258 -> openSheetAndCapture(client, "waypoints", "waypoints sheet, copy coords feedback");
			// Anti Durability Break (#40): Protect At in durability and percent, and the low durability warning.
			case 259 -> {
				client.gui.setScreen(null);
				EMUtilsConfig config = EMUtilsClient.config();
				config.resetAntiDurabilityBreakDefaults();
				config.setTweakAntiDurabilityBreak(true);
				config.setAntiDurabilityWarning(true);
				warningsBefore = AntiDurabilityBreak.warningsForSnapshot();
				command(client, "item replace entity @s weapon.mainhand with minecraft:iron_pickaxe[damage=240]");
				next();
			}
			case 260 -> waitForCheck(client.player.getMainHandItem().is(Items.IRON_PICKAXE), 60, "the worn test pickaxe arrives in hand");
			case 261 -> {
				if (stepTicks == 1) {
					// 10 of 250 durability left.
					ItemStack pickaxe = client.player.getMainHandItem();
					EMUtilsConfig config = EMUtilsClient.config();
					check(AntiDurabilityBreak.warningsForSnapshot() > warningsBefore, "picking up a pickaxe below Warn At shows the low durability warning");
					check(!AntiDurabilityBreak.protects(pickaxe), "at the default Protect At of 5, a pickaxe with 10 left can still be used");
					config.setAntiDurabilityProtectAt(10);
					check(AntiDurabilityBreak.protects(pickaxe), "Protect At 10 protects a pickaxe with 10 left");
					config.setAntiDurabilityUnit(AntiDurabilityUnit.PERCENT);
					config.setAntiDurabilityProtectAt(4);
					boolean fourPercent = AntiDurabilityBreak.protects(pickaxe);
					config.setAntiDurabilityProtectAt(3);
					check(fourPercent && !AntiDurabilityBreak.protects(pickaxe), "in Percent, a pickaxe at 4% is protected at 4% but not at 3%");
				}
				captureAfter(client, 3, "anti durability, low durability warning");
			}
			case 262 -> openSheetAndCapture(client, "anti_durability_break", "anti durability sheet, percent");
			case 263 -> {
				EMUtilsClient.config().resetAntiDurabilityBreakDefaults();
				command(client, "item replace entity @s weapon.mainhand with minecraft:air");
				client.gui.setScreen(null);
				next();
			}
			// Clear Weather's Hide Thunder Flash and Hide Lightning Bolts (#41), with real lightning at night.
			case 264 -> {
				EMUtilsConfig config = EMUtilsClient.config();
				config.setTweakClearWeather(true);
				config.setTweakClearWeatherHideThunderFlash(true);
				SkyFlashAccess sky = (SkyFlashAccess) client.level;
				client.level.setSkyFlashTime(5);
				boolean hidden = sky.emutils$visibleSkyFlashTime() == 0;
				config.setTweakClearWeatherHideThunderFlash(false);
				boolean shown = sky.emutils$visibleSkyFlashTime() == 5 || client.options.hideLightningFlash().get();
				client.level.setSkyFlashTime(0);
				check(hidden && shown, "Hide Thunder Flash keeps the sky from flashing, and turning it off brings the flash back");
				config.resetClearWeatherDefaults();
				command(client, "time set midnight");
				command(client, "tp @s ~ ~ ~ 0 -15");
				next();
			}
			case 265 -> {
				if (stepTicks == 20) {
					command(client, "execute at @s anchored eyes run summon minecraft:lightning_bolt ^ ^ ^16");
				}
				if (stepTicks > 20) {
					captureLightning(client, "lightning, flash and bolt shown");
				}
			}
			case 266 -> {
				if (stepTicks == 1) {
					EMUtilsConfig config = EMUtilsClient.config();
					config.setTweakClearWeather(true);
					config.setTweakClearWeatherHideThunderFlash(true);
					config.setTweakClearWeatherHideLightningBolts(true);
				}
				// Let the first bolt fade before striking again.
				if (stepTicks == 60) {
					command(client, "execute at @s anchored eyes run summon minecraft:lightning_bolt ^ ^ ^16");
				}
				if (stepTicks > 60) {
					captureLightning(client, "lightning, flash and bolt hidden");
				}
			}
			case 267 -> openSheetAndCapture(client, "clear_weather", "clear weather sheet");
			case 268 -> {
				EMUtilsClient.config().resetClearWeatherDefaults();
				command(client, "time set day");
				client.gui.setScreen(null);
				next();
			}
			// Beacon Radius Outline and Light Level Overlay caps and filters (#51).
			case 269 -> {
				// Close enough to the player to open it: the pyramid floats two blocks up, beside them.
				EMUtilsClient.config().resetBeaconRadiusDefaults();
				EMUtilsClient.config().setBeaconRadiusOutline(true);
				command(client, "fill " + at(2, 2, -1) + " " + at(4, 2, 1) + " minecraft:iron_block");
				command(client, "setblock " + at(3, 3, 0) + " minecraft:beacon");
				// A second beacon next to it, so the two cages overlap.
				command(client, "fill " + at(2, 2, 3) + " " + at(4, 2, 5) + " minecraft:iron_block");
				command(client, "setblock " + at(3, 3, 4) + " minecraft:beacon");
				command(client, "tp @s ~ ~ ~ -90 -30");
				next();
			}
			case 270 -> {
				waitForCheck(BeaconRadiusRenderer.outlinedBeaconsForSnapshot() >= 2, 300, "beacons on iron pyramids get an outline");
				if (step != 270) {
					List<Integer> colors = BeaconRadiusRenderer.outlineColorsForSnapshot();
					check(colors.contains(0xFFFFFFFF) && colors.contains(0xFFB4B4B4), "of two touching beacons without an effect, one cage is white and the other light gray: " + hexColors(colors));
				}
			}
			case 271 -> {
				// Turn the camera inside the cage, looking up, where lines run behind the camera: each
				// frame should show the same still lines, not ones jumping across the screen.
				if (stepTicks >= 10) {
					client.player.setYRot(-90.0F + (stepTicks - 10) * 6.0F);
					client.player.setXRot(-55.0F);
				}
				if (stepTicks >= 12 && stepTicks % 2 == 0) {
					grab(client, "beacon radius outline, turning " + (stepTicks - 10) / 2);
				}
				if (stepTicks >= 20) {
					next();
				}
			}
			case 272 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setBeaconRadiusActiveOnly(true);
				}
				if (stepTicks == 3) {
					check(BeaconRadiusRenderer.outlinedBeaconsForSnapshot() == 0, "Only Active Beacons skips a beacon without an effect");
					command(client, "item replace entity @s weapon.mainhand with minecraft:iron_ingot");
					next();
				}
			}
			case 273 -> waitForCheck(client.player.getMainHandItem().is(Items.IRON_INGOT), 60, "an iron ingot to pay the beacon with arrives in hand");
			// Pick the effect the way a player does: open the beacon, pay, and confirm. The server doesn't
			// send the beacon's new effect back, so this is what the outline has to notice.
			case 274 -> {
				if (stepTicks == 1) {
					BlockPos beacon = quickWinsOrigin.offset(3, 3, 0);
					client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(beacon), Direction.WEST, beacon, false));
				}
				waitForCheck(MinecraftClientCompat.screen(client) instanceof BeaconScreen, 40, "right-clicking the beacon opens it");
			}
			case 275 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof BeaconScreen screen) {
					// Beacon menu slots: 0 is the payment, 28-36 the hotbar.
					int hand = 28 + client.player.getInventory().getSelectedSlot();
					client.gameMode.handleContainerInput(screen.getMenu().containerId, hand, 0, ContainerInput.PICKUP, client.player);
					client.gameMode.handleContainerInput(screen.getMenu().containerId, 0, 0, ContainerInput.PICKUP, client.player);
				}
				if (stepTicks == 5) {
					// What the beacon screen's Done button does.
					client.getConnection().send(new ServerboundSetBeaconPacket(Optional.of(MobEffects.HASTE), Optional.empty()));
					client.player.closeContainer();
					next();
				}
			}
			case 276 -> waitForCheck(BeaconRadiusRenderer.outlinedBeaconsForSnapshot() >= 1, 60, "Only Active Beacons outlines a beacon right after you pick its effect in the beacon screen");
			case 277 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setBeaconRadiusActiveOnly(false);
					client.player.setYRot(-60.0F);
					client.player.setXRot(-45.0F);
				}
				if (stepTicks == 3) {
					List<Integer> colors = BeaconRadiusRenderer.outlineColorsForSnapshot();
					int haste = 0xFF000000 | MobEffects.HASTE.value().getColor();
					check(colors.contains(haste) && colors.contains(0xFFFFFFFF), "the Haste beacon's cage is Haste's color and the other stays white: " + hexColors(colors));
				}
				if (stepTicks == 10) {
					grab(client, "beacon radius outline, haste next to no effect");
					EMUtilsClient.config().setBeaconRadiusRange(2);
				}
				if (stepTicks == 12) {
					check(BeaconRadiusRenderer.outlinedBeaconsForSnapshot() >= 1, "a beacon in the player's chunk stays outlined at the smallest Max Distance");
					next();
				}
			}
			case 278 -> {
				if (stepTicks == 1) {
					beaconLinesBefore = BeaconRadiusRenderer.lineCountForSnapshot();
					EMUtilsClient.config().setBeaconRadiusGridSpacing(4);
					EMUtilsClient.config().setBeaconRadiusLineWidth(3);
				}
				if (stepTicks == 3) {
					int lines = BeaconRadiusRenderer.lineCountForSnapshot();
					check(lines > beaconLinesBefore * 3, "a 4-block Grid Spacing draws a much tighter grid (" + lines + " lines instead of " + beaconLinesBefore + ")");
				}
				if (stepTicks == 10) {
					grab(client, "beacon radius outline, 4-block grid, 3 px lines");
					SettingsScreen settings = new SettingsScreen(null);
					client.gui.setScreen(settings);
					settings.openSheet("beacon_radius_outline");
				}
				captureAfter(client, 35, "beacon radius sheet");
			}
			case 279 -> {
				command(client, "fill " + at(2, 2, -1) + " " + at(4, 3, 5) + " minecraft:air");
				EMUtilsClient.config().resetBeaconRadiusDefaults();
				EMUtilsClient.config().resetLightLevelDefaults();
				EMUtilsClient.config().setLightLevelOverlay(true);
				// A torch where the player stands: lit numbers nearby, dark spots further out.
				command(client, "setblock " + at(0, 0, 0) + " minecraft:torch");
				command(client, "tp @s ~ ~ ~ -90 40");
				client.gui.setScreen(null);
				next();
			}
			case 280 -> waitForCheck(LightLevelOverlayRenderer.numberCountsForSnapshot()[0] > 0 && LightLevelOverlayRenderer.numberCountsForSnapshot()[1] > 0, 100, "the light level overlay numbers lit and dark spots");
			case 281 -> captureAfter(client, 10, "light level overlay, all spots");
			case 282 -> {
				if (stepTicks == 1) {
					lightLevelSpotsBefore = LightLevelOverlayRenderer.numberCountsForSnapshot()[0];
					EMUtilsClient.config().setLightLevelSpawnableOnly(true);
				}
				if (stepTicks == 3) {
					int[] counts = LightLevelOverlayRenderer.numberCountsForSnapshot();
					check(counts[0] > 0 && counts[1] == 0 && counts[0] < lightLevelSpotsBefore, "Only Spawnable Spots leaves only the spots at block light 0 (" + counts[0] + " of " + lightLevelSpotsBefore + ")");
					lightLevelSpotsBefore = counts[0];
				}
				captureAfter(client, 15, "light level overlay, only spawnable spots");
			}
			case 283 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setLightLevelRange(8);
				}
				if (stepTicks == 3) {
					int spots = LightLevelOverlayRenderer.numberCountsForSnapshot()[0];
					check(spots < lightLevelSpotsBefore, "a smaller Range scans fewer spots (" + spots + " instead of " + lightLevelSpotsBefore + ")");
					SettingsScreen settings = new SettingsScreen(null);
					client.gui.setScreen(settings);
					settings.openSheet("light_level_overlay");
				}
				captureAfter(client, 25, "light level sheet, range 8");
			}
			case 284 -> {
				command(client, "setblock " + at(0, 0, 0) + " minecraft:air");
				command(client, "tp @s ~ ~ ~ 0 0");
				EMUtilsClient.config().resetLightLevelDefaults();
				client.gui.setScreen(null);
				next();
			}
			// The Keybinds page (#155): every EMUtils key, the × that unbinds one, the hint while one waits
			// for a key, conflicts, and the link to a feature's sheet. Freelook is put on Q, like Drop Item.
			case 285 -> {
				if (stepTicks == 1) {
					setGuiScale(client, 2);
					EMUtilsClient.config().setSettingsUiDark(true);
					KeyMapping freelook = KeyMapping.get("key.emutils.freelook");
					freelook.setKey(InputConstants.getKey(new KeyEvent(InputConstants.KEY_Q, 0, 0)));
					KeyMapping.resetMapping();
					client.gui.setScreen(new KeybindsScreen(new SettingsScreen(null)));
				}
				if (stepTicks == 5 && MinecraftClientCompat.screen(client) instanceof KeybindsScreen screen) {
					List<String> general = screen.generalKeysForSnapshot();
					check(general.contains("key.emutils.open_settings_hub"), "Open Settings is listed under General on the Keybinds page " + general);
					check(!general.contains("key.emutils.zoom"), "keys a feature lists aren't under General");
				}
				captureAfter(client, 20, "keybinds page, freelook clashing with Drop Item");
			}
			case 286 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof KeybindsScreen screen) {
					screen.hoverClearForSnapshot("key.emutils.zoom");
				}
				captureAfter(client, 15, "keybinds page, the x on a hovered key");
			}
			case 287 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof KeybindsScreen screen) {
					screen.hoverClearForSnapshot(null);
					screen.listenForSnapshot("key.emutils.zoom");
				}
				captureAfter(client, 15, "keybinds page, waiting for a key with the hint");
			}
			case 288 -> {
				if (MinecraftClientCompat.screen(client) instanceof KeybindsScreen screen) {
					KeyMapping zoom = KeyMapping.get("key.emutils.zoom");
					screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
					check(!zoom.isUnbound() && MinecraftClientCompat.screen(client) == screen, "Esc cancels waiting for a key and keeps the page open");
					screen.clickClearForSnapshot("key.emutils.zoom");
					check(zoom.isUnbound(), "the x unbinds the key right away");
					screen.listenForSnapshot("key.emutils.zoom");
					MouseButtonInfo right = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0);
					screen.mouseClicked(new MouseButtonEvent(0, 0, right), false);
					check(zoom.isDefault(), "a right click while waiting for a key resets it to its default");

					// /emutils export and import (#157) carry the keybinds along with the settings.
					EMUtilsConfig config = EMUtilsClient.config();
					boolean fullbright = config.tweakFullbright();
					zoom.setKey(InputConstants.getKey(new KeyEvent(InputConstants.KEY_G, 0, 0)));
					KeyMapping.resetMapping();
					String exported = ConfigTransfer.export();
					zoom.setKey(zoom.getDefaultKey());
					KeyMapping.resetMapping();
					config.setTweakFullbright(!fullbright);
					boolean imported = ConfigTransfer.importFrom(exported);
					EMUtilsConfig saved = EMUtilsConfig.read(EMUtilsClient.config().file());
					check(imported && zoom.saveString().equals("key.keyboard.g"), "/emutils import binds the exported keys (zoom is " + zoom.saveString() + ")");
					check(EMUtilsClient.config().tweakFullbright() == fullbright && saved != null && saved.tweakFullbright() == fullbright, "/emutils import restores the settings and writes them to disk");
					check(!ConfigTransfer.importFrom("{\"keybinds\": {\"key.emutils.zoom\": \"key.keyboard.h\"}}") && zoom.saveString().equals("key.keyboard.g"), "text without EMUtils settings is refused and binds nothing");
					zoom.setKey(zoom.getDefaultKey());
					KeyMapping.resetMapping();
					client.options.save();
				}
				next();
			}
			case 289 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof KeybindsScreen screen) {
					screen.setConflictsOnlyForSnapshot(true);
				}
				if (stepTicks == 5 && MinecraftClientCompat.screen(client) instanceof KeybindsScreen screen) {
					// Only Freelook on Q clashes (with Drop Item): Zoom's C is shared with Save Hotbar Activator
					// and F3+C only as key combos, which don't count (#158).
					int conflicts = screen.conflictCountForSnapshot();
					check(conflicts == 1 && screen.visibleRowsForSnapshot() == 1, "Conflicts only shows just the clashing key, not vanilla key combos (" + screen.visibleRowsForSnapshot() + " rows, " + conflicts + " conflicts)");
				}
				captureAfter(client, 15, "keybinds page, conflicts only");
			}
			case 290 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof KeybindsScreen screen) {
					screen.setConflictsOnlyForSnapshot(false);
					screen.searchForSnapshot("waypoint");
				}
				captureAfter(client, 15, "keybinds page, search waypoint");
			}
			case 291 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof KeybindsScreen screen) {
					screen.searchForSnapshot("");
					check(screen.openFeatureSheetForSnapshot("key.emutils.freelook"), "the feature link opens Freelook's sheet over the Keybinds page");
				}
				captureAfter(client, 20, "keybinds page, freelook sheet from the link");
			}
			case 292 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof KeybindsScreen screen) {
					screen.closeSheetForSnapshot();
					setGuiScale(client, 3);
					EMUtilsClient.config().setSettingsUiDark(false);
				}
				captureAfter(client, 20, "keybinds page, gui scale 3, light");
			}
			case 293 -> {
				KeyMapping freelook = KeyMapping.get("key.emutils.freelook");
				freelook.setKey(freelook.getDefaultKey());
				KeyMapping.resetMapping();
				client.options.save();
				deleteTestScripts();
				EMUtilsClient.config().resetMenuSettings();
				client.gui.setScreen(null);
				next();
			}
			// The HUD Overlay's new lines (#47, #48): targeted block, dimension, slime chunk, TPS and day/night,
			// then the text shadow and hiding it in containers.
			case 294 -> {
				if (stepTicks == 1) {
					setGuiScale(client, 2);
					EMUtilsConfig config = EMUtilsClient.config();
					config.resetHudDefaults();
					config.setHudOverlay(true);
					config.setHudShowTargetBlock(true);
					config.setHudShowDimension(true);
					config.setHudShowSlimeChunk(true);
					config.setHudShowTps(true);
					config.setHudShowDayNight(true);
					command(client, "time set 12000");
					// Look down at the ground, so there's a block to target.
					client.player.setXRot(60.0F);
				}
				if (stepTicks == 40) {
					HudOverlayData data = HudOverlayRenderer.hudOverlayData();
					check(data.targetBlock().matches("-?\\d+ -?\\d+ -?\\d+"), "the Looking At line shows the targeted block (" + data.targetBlock() + ")");
					check(data.dimension().equals("Overworld"), "the Dimension line names the Overworld (" + data.dimension() + ")");
					check(data.slimeChunk().equals("Yes") || data.slimeChunk().equals("No"), "singleplayer knows the seed, so the Slime Chunk line says yes or no (" + data.slimeChunk() + ")");
					check(data.tps().value().matches("\\d+\\.\\d \u00b7 \\d+\\.\\d ms") && data.tps().health() == HudTpsTracker.Health.GOOD, "singleplayer TPS is exact, with milliseconds per tick (" + data.tps().value() + ")");
					check(data.dayNight().startsWith("Night in "), "at time 12000 the Day/Night line counts down to night (" + data.dayNight() + ")");
					// On a server TPS is estimated from the world time sent every 20 ticks.
					long[][] steady = {{0, 0}, {20, 1000}, {40, 2000}, {60, 3000}};
					long[][] halfSpeed = {{0, 0}, {20, 2000}, {40, 4000}};
					HudTpsTracker.Reading full = HudTpsTracker.estimateForSnapshot(steady, 3100);
					HudTpsTracker.Reading half = HudTpsTracker.estimateForSnapshot(halfSpeed, 4100);
					HudTpsTracker.Reading stalled = HudTpsTracker.estimateForSnapshot(steady, 9000);
					HudTpsTracker.Reading early = HudTpsTracker.estimateForSnapshot(new long[][] {{0, 0}, {20, 1000}}, 1100);
					check(full.value().equals("~20.0") && full.health() == HudTpsTracker.Health.GOOD, "a steady server is estimated at ~20 TPS (" + full.value() + ")");
					check(half.value().equals("~10.0") && half.health() == HudTpsTracker.Health.BAD, "a server at half speed is estimated at ~10 TPS, in red (" + half.value() + ")");
					check(stalled.value().equals("~6.7"), "with no time update for 6 s the estimate drifts down (" + stalled.value() + ")");
					check(early.value().equals("--"), "one second of updates is too little for an estimate (" + early.value() + ")");
				}
				captureAfter(client, 40, "hud overlay, new lines");
			}
			case 295 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setHudTextShadow(HudTextShadow.ON);
					EMUtilsClient.config().setHudTpsCompact(true);
					EMUtilsClient.config().setSettingsUiDark(false);
				}
				captureAfter(client, 10, "hud overlay, light, text shadow on, compact tps");
			}
			case 296 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setSettingsUiDark(true);
					EMUtilsClient.config().setHudHideInContainers(true);
					client.gui.setScreen(new InventoryScreen(client.player));
				}
				captureAfter(client, 15, "hud overlay hidden while the inventory is open");
			}
			case 297 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setHudHideInContainers(false);
					setGuiScale(client, 2);
					SettingsScreen settings = new SettingsScreen(null);
					client.gui.setScreen(settings);
					settings.openSheet("hud_overlay");
					settings.selectSheetSectionForSnapshot(2);
				}
				captureAfter(client, 20, "hud overlay sheet, performance tab");
			}
			// Freelook turns all the way around (#167): a full circle of mouse movement, through vanilla's own
			// turnPlayer, turns the camera 360 degrees while the player keeps facing the same way.
			case 298 -> {
				KeyMapping freelookKey = KeyMapping.get("key.emutils.freelook");
				if (stepTicks == 1) {
					client.gui.setScreen(null);
					EMUtilsClient.config().setTweakFreelook(true);
					client.options.setCameraType(CameraType.FIRST_PERSON);
					client.player.setYRot(0.0F);
					client.player.setXRot(0.0F);
					freelookKey.setDown(true);
				}
				if (stepTicks == 5) {
					FreelookManager freelook = EMUtilsClient.tweaks().freelook();
					MouseAccess mouse = (MouseAccess) client.mouseHandler;
					double sensitivity = Math.pow(client.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 8.0;
					float yawBefore = client.player.getYRot();
					// 24 moves of 15 degrees each, 360 in all.
					for (int i = 0; i < 24; i++) {
						mouse.emutils$setAccumulatedDX(100.0 / sensitivity);
						mouse.emutils$turnPlayer(0.0);
					}
					mouse.emutils$setAccumulatedDX(0.0);
					float turned = freelook.cameraYaw() - yawBefore;
					check(freelook.isActive() && Math.abs(turned - 360.0F) < 1.0F, "Freelook turns the camera a full 360 degrees (" + turned + ")");
					check(client.player.getYRot() == yawBefore, "the player keeps facing the same way while Freelook turns the camera");
					check(client.options.getCameraType() == CameraType.THIRD_PERSON_BACK, "by default Freelook switches from first to third person");
					freelookKey.setDown(false);
				}
				// Keep Perspective (#171): Freelook stays in first person.
				if (stepTicks == 8) {
					check(client.options.getCameraType() == CameraType.FIRST_PERSON, "letting go of Freelook goes back to first person");
					EMUtilsClient.config().setFreelookKeepPerspective(true);
					freelookKey.setDown(true);
				}
				if (stepTicks == 12) {
					FreelookManager freelook = EMUtilsClient.tweaks().freelook();
					MouseAccess mouse = (MouseAccess) client.mouseHandler;
					float yawBefore = freelook.cameraYaw();
					mouse.emutils$setAccumulatedDX(600.0 / (Math.pow(client.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 8.0));
					mouse.emutils$turnPlayer(0.0);
					mouse.emutils$setAccumulatedDX(0.0);
					check(freelook.isActive() && client.options.getCameraType() == CameraType.FIRST_PERSON, "with Keep Perspective, Freelook stays in first person");
					check(Math.abs(freelook.cameraYaw() - yawBefore - 90.0F) < 1.0F, "and still turns the camera in first person");
					freelookKey.setDown(false);
				}
				if (stepTicks == 15) {
					EMUtilsClient.config().resetFreelookDefaults();
					next();
				}
			}
			// Cosmetica 2 capes (#52), from api.cloaks.gg: real users from Cosmetica's leaderboard, one with an
			// animated cape and one with a still one, and Notch, who isn't a Cosmetica user. The old API gave
			// every player an "Update Cosmetica" placeholder cape. Needs the network, like the Pack Manager.
			case 299 -> {
				if (stepTicks == 1) {
					EMUtilsConfig config = EMUtilsClient.config();
					config.setCustomCapes(true);
					config.setCapeOptifine(false);
					config.setCapeLabyMod(false);
					config.setCapeMinecraftCapes(false);
					config.setCapeCloaksPlus(false);
					config.setCapeCosmetica(true);
					int animatedBefore = CapeAnimations.count();
					CompletableFuture.runAsync(() -> {
						String animated = CustomCapeManager.resolveForSnapshot(new GameProfile(UUID.fromString("cd19cb6e-c829-46b3-a6df-63bbe2c5a0dd"), "Lythogeor"));
						// Counted now: the setting changes below reload the capes, which clears animations.
						int animatedAfter = CapeAnimations.count();
						String still = CustomCapeManager.resolveForSnapshot(new GameProfile(UUID.fromString("4f507640-5bc3-4e87-acdd-896a9cf0fe6c"), "Redjie"));
						String notch = CustomCapeManager.resolveForSnapshot(new GameProfile(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch"));
						// The priority list (#52), with a stand-in for an official cape: by default a provider's cape
						// wins; with Minecraft first the official one does, and a provider's still shows for
						// players without one; with Minecraft off official capes are hidden.
						GameProfile user = new GameProfile(UUID.fromString("4f507640-5bc3-4e87-acdd-896a9cf0fe6c"), "Redjie");
						GameProfile notchProfile = new GameProfile(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch");
						Identifier standIn = Identifier.withDefaultNamespace("textures/entity/official_cape_stand_in.png");
						ClientAsset.Texture official = new ClientAsset.Texture() {
							@Override
							public Identifier id() {
								return standIn;
							}

							@Override
							public Identifier texturePath() {
								return standIn;
							}
						};
						boolean providerWins = CustomCapeManager.capeTextureFor(user, official) != official;
						EMUtilsClient.config().moveCapeSource(CapeSource.MINECRAFT, -10);
						CustomCapeManager.resolveForSnapshot(user);
						boolean officialWins = CustomCapeManager.capeTextureFor(user, official) == official;
						boolean fallsBack = CustomCapeManager.capeTextureFor(user, null) != null;
						EMUtilsClient.config().setCapeMinecraft(false);
						CustomCapeManager.resolveForSnapshot(user);
						CustomCapeManager.resolveForSnapshot(notchProfile);
						boolean hidden = CustomCapeManager.capeTextureFor(notchProfile, official) == null;
						boolean providerInstead = CustomCapeManager.capeTextureFor(user, official) != official && CustomCapeManager.capeTextureFor(user, official) != null;
						EMUtilsConfig migrated = EMUtilsConfig.fromJson("{\"capePreferredProvider\": \"LABYMOD\"}", EMUtilsClient.config().file());
						client.execute(() -> {
							check("Cosmetica".equals(animated) && "Cosmetica".equals(still), "Cosmetica 2 users get their capes (" + animated + ", " + still + ")");
							check(animatedAfter > animatedBefore, "an animated Cosmetica cape animates (" + animatedAfter + " animated)");
							check(notch == null, "a player who isn't a Cosmetica user gets no Cosmetica cape, and no placeholder (" + notch + ")");
							check(providerWins, "by default a provider's cape wins over the official one");
							check(officialWins && fallsBack, "with Minecraft first the official cape wins, and players without one still get a provider's");
							check(hidden && providerInstead, "with Minecraft off official capes are hidden, and a provider's shows instead");
							check(migrated != null && migrated.capeOrder().getFirst() == CapeSource.LABYMOD && migrated.capeOrder().getLast() == CapeSource.MINECRAFT, "an old Preferred Provider becomes the top of the priority list");
							EMUtilsClient.config().resetCapesDefaults();
							next();
						});
					});
				}
				if (stepTicks > 600) {
					check(false, "the Cosmetica capes load within 30 seconds");
					EMUtilsClient.config().resetCapesDefaults();
					next();
				}
			}
			// The Capes sheet's priority list (#52), with Minecraft moved up one and Cosmetica switched off.
			case 300 -> {
				if (stepTicks == 1) {
					setGuiScale(client, 2);
					EMUtilsClient.config().moveCapeSource(CapeSource.MINECRAFT, -1);
					EMUtilsClient.config().setCapeCosmetica(false);
				}
				openSheetAndCapture(client, "capes", "capes sheet, priority list");
				if (step != 300) {
					EMUtilsClient.config().resetCapesDefaults();
				}
			}
			// Look-At Info (#45): the card for a block, then for a mob with an effect, in the light theme on the
			// right side of the screen, and the sample the layout editor shows while nothing is targeted.
			case 301 -> {
				if (stepTicks == 1) {
					client.gui.setScreen(null);
					setGuiScale(client, 2);
					EMUtilsConfig config = EMUtilsClient.config();
					config.resetHudDefaults();
					config.resetLookAtInfoDefaults();
					config.setLookAtInfo(true);
					BlockPos feet = client.player.blockPosition();
					// Stand in the middle of the block, facing south and looking down at dirt right in front, then obsidian.
					command(client, "tp @s " + (feet.getX() + 0.5) + " " + feet.getY() + " " + (feet.getZ() + 0.5) + " 0 60");
					command(client, "setblock ~ ~ ~1 minecraft:dirt");
					command(client, "setblock ~ ~1 ~1 minecraft:air");
					command(client, "time set 6000");
					command(client, "item replace entity @s hotbar.8 with minecraft:air");
					client.player.getInventory().setSelectedSlot(8);
				}
				if (stepTicks == 5) {
					client.player.setYRot(0.0F);
					client.player.setXRot(60.0F);
				}
				if (stepTicks == 20) {
					// Dirt drops with anything, so there's no Can Harvest line (#45).
					LookAtInfoData data = LookAtInfoData.current();
					check(data != null && data.id().equals("minecraft:dirt") && lookAtValue(data, "emutils.hud.look_at.harvest").isEmpty(), "a block that drops with anything has no Can Harvest line");
					command(client, "setblock ~ ~ ~1 minecraft:obsidian");
				}
				if (stepTicks == 40) {
					LookAtInfoData data = LookAtInfoData.current();
					check(data != null && data.id().equals("minecraft:obsidian") && data.name().equals("Obsidian"), "Look-At Info names the targeted block (" + (data == null ? "nothing" : data.name() + ", " + data.id()) + ")");
					check(data != null && lookAtValue(data, "emutils.hud.look_at.hardness").equals("50"), "obsidian's hardness is 50");
					check(data != null && lookAtValue(data, "emutils.hud.look_at.tool").equals("Pickaxe · Diamond+"), "obsidian needs a diamond pickaxe (" + (data == null ? "" : lookAtValue(data, "emutils.hud.look_at.tool")) + ")");
					check(data != null && lookAtValue(data, "emutils.hud.look_at.harvest").equals("No"), "an empty hand can't harvest obsidian");
					check(data != null && lookAtValue(data, "emutils.hud.look_at.position").matches("-?\\d+ -?\\d+ -?\\d+"), "the Position line shows the block's coordinates");
					List<Map.Entry<Block, String>> tools = List.of(
						Map.entry(Blocks.STONE, "Pickaxe · Wood+"),
						Map.entry(Blocks.IRON_ORE, "Pickaxe · Stone+"),
						Map.entry(Blocks.DIAMOND_ORE, "Pickaxe · Iron+"),
						Map.entry(Blocks.OAK_LOG, "Axe"),
						Map.entry(Blocks.DIRT, "Shovel"),
						Map.entry(Blocks.HAY_BLOCK, "Hoe"),
						Map.entry(Blocks.GLASS, "Any"),
						Map.entry(Blocks.BEDROCK, "Any")
					);
					for (Map.Entry<Block, String> tool : tools) {
						String value = LookAtInfoData.toolForSnapshot(tool.getKey().defaultBlockState());
						check(value.equals(tool.getValue()), "the Tool line for " + tool.getKey().getName().getString() + " is " + tool.getValue() + " (" + value + ")");
					}
					String cobweb = LookAtInfoData.toolForSnapshot(Blocks.COBWEB.defaultBlockState());
					check(cobweb.startsWith("Sword") || cobweb.startsWith("Shears"), "a cobweb needs a sword or shears (" + cobweb + ")");
				}
				captureAfter(client, 40, "look-at info, block");
			}
			case 302 -> {
				if (stepTicks == 1) {
					// A clear stone floor, so grass or a slope doesn't get between the player and the mob.
					command(client, "fill ~-1 ~-1 ~1 ~1 ~-1 ~4 minecraft:stone");
					command(client, "fill ~-1 ~ ~1 ~1 ~2 ~4 minecraft:air");
					command(client, "summon minecraft:husk ~ ~ ~3 {NoAI:1b,Silent:1b,active_effects:[{id:\"minecraft:speed\",amplifier:1b,duration:2400}]}");
					command(client, "damage @e[type=minecraft:husk,limit=1,sort=nearest] 5 minecraft:generic");
					client.player.setXRot(10.0F);
				}
				if (stepTicks == 40) {
					LookAtInfoData data = LookAtInfoData.current();
					check(data != null && data.id().equals("minecraft:husk"), "Look-At Info names the targeted mob (" + (data == null ? "nothing" : data.id()) + ")");
					String health = data == null ? "" : lookAtValue(data, "emutils.hud.look_at.health");
					check(health.equals("15 / 20"), "the Health line shows health and max health (" + health + ")");
					String speed = data == null ? "" : lookAtValue(data, "effect.minecraft.speed");
					check(speed.startsWith("II · "), "in singleplayer the mob's effects show, with their level (" + speed + ")");
				}
				captureAfter(client, 40, "look-at info, mob with an effect");
			}
			case 303 -> {
				if (stepTicks == 1) {
					EMUtilsConfig config = EMUtilsClient.config();
					config.setSettingsUiDark(false);
					config.setLookAtInfoTextShadow(HudTextShadow.ON);
					int width = client.getWindow().getGuiScaledWidth();
					config.setHudCustomLayoutEntry(EMUtilsHudElements.LOOK_AT_INFO, width - 178, 40, 100, 100);
				}
				captureAfter(client, 10, "look-at info, light, right side lines up right");
			}
			case 304 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setSettingsUiDark(true);
					EMUtilsClient.config().resetLookAtInfoDefaults();
					EMUtilsClient.config().setLookAtInfo(true);
					client.player.setXRot(-90.0F);
				}
				if (stepTicks == 5 && HudLayoutManager.beginEditorSession(EMUtilsClient.MOD_ID, client)) {
					check(LookAtInfoData.current() == null, "looking at the sky targets nothing");
					client.gui.setScreen(new HudEditorScreen(null));
				}
				captureAfter(client, 25, "hud editor, look-at info sample");
			}
			case 305 -> {
				if (stepTicks == 1) {
					HudLayoutManager.cancelEditor(EMUtilsClient.config());
					SettingsScreen settings = new SettingsScreen(null);
					client.gui.setScreen(settings);
					settings.openSheet("look_at_info");
				}
				if (stepTicks == 15 && MinecraftClientCompat.screen(client) instanceof SettingsScreen settings) {
					settings.selectSheetSectionForSnapshot(1);
				}
				captureAfter(client, 35, "look-at info sheet, block tab");
				if (step != 305) {
					command(client, "kill @e[type=minecraft:husk]");
					EMUtilsClient.config().resetLookAtInfoDefaults();
					client.player.setXRot(0.0F);
					client.gui.setScreen(null);
				}
			}
			// Keystrokes (#43): held keys light up, clicks per second count every press of the attack key,
			// then a light, square variant in a color of its own, and the Style tab of its settings.
			case 306 -> {
				Options options = client.options;
				if (stepTicks == 1) {
					client.gui.setScreen(null);
					setGuiScale(client, 2);
					EMUtilsConfig config = EMUtilsClient.config();
					config.resetKeystrokesDefaults();
					config.setKeystrokes(true);
					config.setKeystrokesSneak(true);
					options.keyUp.setDown(true);
					options.keyJump.setDown(true);
					InputConstants.Key attack = ((KeyBindingAccess) options.keyAttack).emhelpers$getBoundKey();
					for (int i = 0; i < 6; i++) {
						KeyMapping.click(attack);
					}
				}
				if (stepTicks == 10) {
					check(ClickCounter.leftCps() == 6 && ClickCounter.rightCps() == 0, "six attack presses in a second are 6 left CPS and no right ones (" + ClickCounter.leftCps() + ", " + ClickCounter.rightCps() + ")");
					int sliding = ClickCounter.countForSnapshot(new long[] {0L, 300L, 600L, 900L, 1200L}, 1250L);
					check(sliding == 4, "CPS only counts the last second (" + sliding + ")");
				}
				captureAfter(client, 12, "keystrokes, forward and jump held, 6 cps");
				if (step != 306) {
					options.keyUp.setDown(false);
					options.keyJump.setDown(false);
				}
			}
			case 307 -> {
				if (stepTicks == 1) {
					EMUtilsConfig config = EMUtilsClient.config();
					config.setSettingsUiDark(false);
					config.setKeystrokesStyle(KeystrokesStyle.SQUARE);
					config.setKeystrokesMenuAccent(false);
					config.setKeystrokesPressedColor(0xFFC23D7A);
					client.options.keyLeft.setDown(true);
				}
				captureAfter(client, 10, "keystrokes, light, square, own color, left held");
				if (step != 307) {
					client.options.keyLeft.setDown(false);
				}
			}
			case 308 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setSettingsUiDark(true);
					SettingsScreen settings = new SettingsScreen(null);
					client.gui.setScreen(settings);
					settings.openSheet("keystrokes");
				}
				if (stepTicks == 15 && MinecraftClientCompat.screen(client) instanceof SettingsScreen settings) {
					settings.selectSheetSectionForSnapshot(1);
				}
				captureAfter(client, 35, "keystrokes sheet, style tab");
			}
			case 309 -> {
				EMUtilsClient.config().resetKeystrokesDefaults();
				client.gui.setScreen(null);
				next();
			}
			// Inventory Search (#46): a chest with a few items, a shulker box with diamonds inside, a barrel with dirt,
			// and a map named in its lore. Ctrl+F focuses the box, typing doesn't close the screen, and matches
			// are outlined, including the shulker box with diamonds inside.
			case 310 -> {
				if (stepTicks == 1) {
					client.gui.setScreen(null);
					setGuiScale(client, 2);
					EMUtilsConfig config = EMUtilsClient.config();
					config.setInventoryToolsEnabled(true);
					config.setInventorySearch(true);
					config.setInventorySearchDim(true);
					config.setInventorySearchShulkers(true);
					config.setInventorySearchRemember(true);
					InventorySearch.setQueryForSnapshot("");
					searchChest = client.player.blockPosition().offset(2, 0, 0);
					String chest = searchChest.getX() + " " + searchChest.getY() + " " + searchChest.getZ();
					command(client, "clear @s");
					command(client, "setblock " + chest + " minecraft:chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:5},{Slot:1b,id:\"minecraft:stone\",count:64},{Slot:2b,id:\"minecraft:oak_log\",count:12},{Slot:3b,id:\"minecraft:iron_ingot\",count:9}]}");
					command(client, "give @s minecraft:shulker_box[minecraft:container=[{slot:0,item:{id:\"minecraft:diamond\",count:3}}]]");
					command(client, "give @s minecraft:barrel[minecraft:container=[{slot:0,item:{id:\"minecraft:dirt\",count:32}}]]");
					command(client, "give @s minecraft:paper[minecraft:custom_name=\"Treasure Map\",minecraft:lore=[\"Buried near spawn\"]]");
				}
				if (stepTicks == 15) {
					client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(searchChest), Direction.UP, searchChest, false));
				}
				if (stepTicks == 30 && MinecraftClientCompat.screen(client) instanceof ContainerScreen screen) {
					press(screen, InputConstants.KEY_F, InputConstants.MOD_CONTROL);
					type(screen, "diamond");
					// The inventory key would close the screen; while typing it's just a letter.
					press(screen, InputConstants.KEY_E, 0);
				}
				if (stepTicks == 35) {
					Screen screen = MinecraftClientCompat.screen(client);
					check(screen instanceof ContainerScreen, "typing E into the search box doesn't close the chest");
					if (screen instanceof ContainerScreen chest) {
						check(searchMatch(chest, Items.DIAMOND) == InventorySearch.Match.ITEM, "diamonds in the chest match \"diamond\"");
						check(searchMatch(chest, Items.STONE) == InventorySearch.Match.MISS, "stone doesn't match \"diamond\"");
						check(searchMatch(chest, Items.SHULKER_BOX) == InventorySearch.Match.INSIDE, "the shulker box with diamonds inside is marked as holding a match");
						check(searchMatch(chest, Items.BARREL) == InventorySearch.Match.MISS, "a barrel with only dirt inside doesn't match");
					}
				}
				captureAfter(client, 40, "inventory search, diamond in a chest");
			}
			case 311 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof ContainerScreen chest) {
					InventorySearch.setQueryForSnapshot("buried spawn");
					check(searchMatch(chest, Items.PAPER) == InventorySearch.Match.ITEM, "every word is found in the map's lore");
					check(searchMatch(chest, Items.DIAMOND) == InventorySearch.Match.MISS, "diamonds don't match \"buried spawn\"");
					InventorySearch.setQueryForSnapshot("iron_ingot");
					check(searchMatch(chest, Items.IRON_INGOT) == InventorySearch.Match.ITEM, "an item ID matches");
					press(chest, InputConstants.KEY_RETURN, 0);
					InventorySearch.setQueryForSnapshot("diamond");
				}
				if (stepTicks == 5) {
					client.gui.setScreen(new InventoryScreen(client.player));
				}
				captureAfter(client, 20, "inventory search, own inventory keeps the text");
			}
			// With Search in Creative on, the creative inventory gets the box too, above its tabs, and it only
			// marks your own inventory's slots.
			case 312 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof InventoryScreen screen) {
					check(searchMatch(screen, Items.SHULKER_BOX) == InventorySearch.Match.INSIDE, "the search carries over to the inventory");
					client.gui.setScreen(null);
					command(client, "gamemode creative");
					EMUtilsClient.config().setInventorySearchCreative(true);
				}
				if (stepTicks == 15) {
					client.gui.setScreen(new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), client.options.operatorItemsTab().get()));
				}
				if (stepTicks == 25 && MinecraftClientCompat.screen(client) instanceof CreativeModeInventoryScreen screen) {
					press(screen, InputConstants.KEY_F, InputConstants.MOD_CONTROL);
					type(screen, "x");
					check(InventorySearch.queryForSnapshot().equals("diamondx"), "typing in the creative inventory goes to the search box, not the creative search tab (" + InventorySearch.queryForSnapshot() + ")");
					press(screen, InputConstants.KEY_BACKSPACE, 0);
					press(screen, InputConstants.KEY_RETURN, 0);
				}
				captureAfter(client, 35, "inventory search, creative inventory");
			}
			// The Inventory Tools sheet, split into tabs; the Search tab.
			case 313 -> {
				if (stepTicks == 1) {
					InventorySearch.setQueryForSnapshot("");
					EMUtilsClient.config().setInventorySearchCreative(false);
					command(client, "gamemode survival");
					command(client, "setblock " + searchChest.getX() + " " + searchChest.getY() + " " + searchChest.getZ() + " minecraft:air");
					command(client, "clear @s");
					client.gui.setScreen(null);
				}
				// After the game mode change has come back from the server, which closes the creative screen.
				if (stepTicks == 10) {
					SettingsScreen settings = new SettingsScreen(null);
					client.gui.setScreen(settings);
					settings.openSheet("inventory");
				}
				if (stepTicks == 25 && MinecraftClientCompat.screen(client) instanceof SettingsScreen settings) {
					settings.selectSheetSectionForSnapshot(2);
				}
				captureAfter(client, 45, "inventory tools sheet, search tab");
				if (step != 313) {
					client.gui.setScreen(null);
				}
			}
			// A left click on a container's sort button sorts it (#177): on 26.3 the left button is 1, not 0, so
			// the click is built from InputConstants like a real one.
			case 314 -> {
				if (stepTicks == 1) {
					client.gui.setScreen(null);
					setGuiScale(client, 2);
					EMUtilsConfig config = EMUtilsClient.config();
					config.setInventoryToolsEnabled(true);
					config.setSortButtonsEnabled(true);
					config.setSortSpeed(InventorySortSpeed.NORMAL);
					sortChest = client.player.blockPosition().offset(2, 0, 0);
					String chest = sortChest.getX() + " " + sortChest.getY() + " " + sortChest.getZ();
					command(client, "setblock " + chest + " minecraft:chest{Items:[{Slot:0b,id:\"minecraft:stone\",count:8},{Slot:1b,id:\"minecraft:apple\",count:3},{Slot:2b,id:\"minecraft:diamond\",count:2}]}");
				}
				if (stepTicks == 15) {
					client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(sortChest), Direction.UP, sortChest, false));
				}
				if (stepTicks == 30 && MinecraftClientCompat.screen(client) instanceof ContainerScreen screen) {
					int[] button = EMUtilsClient.inventoryTools().containerSortButtonForSnapshot(screen.getMenu(), client.player.getInventory(), InventorySortMode.NAME);
					check(button != null, "the chest has a sort by name button");
					if (button != null) {
						HandledScreenAccessor panel = (HandledScreenAccessor) screen;
						MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
						double x = panel.emutils$getLeftPos() + button[0];
						double y = panel.emutils$getTopPos() + button[1];
						screen.mouseClicked(new MouseButtonEvent(x, y, left), false);
						screen.mouseReleased(new MouseButtonEvent(x, y, left));
					}
				}
				if (stepTicks == 70 && MinecraftClientCompat.screen(client) instanceof ContainerScreen screen) {
					ItemStack first = screen.getMenu().getSlot(0).getItem();
					check(first.is(Items.APPLE), "a left click on the sort by name button sorts the chest, apples first (" + first + ")");
				}
				captureAfter(client, 72, "chest sorted by a left click on its sort button");
				if (step != 314) {
					client.gui.setScreen(null);
					command(client, "setblock " + sortChest.getX() + " " + sortChest.getY() + " " + sortChest.getZ() + " minecraft:air");
				}
			}
			// Hide Effects (#176): the effect list beside the inventory, then without it, then the HUD without
			// the effect icons in its top-right corner.
			case 315 -> {
				if (stepTicks == 1) {
					client.gui.setScreen(null);
					setGuiScale(client, 2);
					EMUtilsClient.config().resetHideEffectsDefaults();
					command(client, "effect give @s minecraft:speed 120 1");
					command(client, "effect give @s minecraft:resistance 120 0");
				}
				if (stepTicks == 15) {
					client.gui.setScreen(new InventoryScreen(client.player));
				}
				captureAfter(client, 30, "inventory with the effect list");
			}
			case 316 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setTweakHideEffects(true);
					check(EMUtilsClient.config().hideInventoryEffects() && EMUtilsClient.config().hideHudEffects(), "Hide Effects hides both the inventory list and the HUD icons by default");
				}
				captureAfter(client, 10, "inventory, effects hidden");
			}
			case 317 -> {
				if (stepTicks == 1) {
					client.gui.setScreen(null);
				}
				captureAfter(client, 15, "hud, effect icons hidden");
				if (step != 317) {
					command(client, "effect clear @s");
					EMUtilsClient.config().resetHideEffectsDefaults();
				}
			}
			// Armor Status (#44): worn armor with an elytra, a low helmet in the warning color with its sound,
			// the fireworks carried, then a light card with Remaining / Max, and the Display tab of its settings.
			case 318 -> {
				if (stepTicks == 1) {
					client.gui.setScreen(null);
					setGuiScale(client, 2);
					EMUtilsConfig config = EMUtilsClient.config();
					config.resetArmorStatusDefaults();
					config.setArmorStatus(true);
					config.setArmorStatusSound(true);
					config.setArmorStatusFlash(false);
					armorStatusSounds = ArmorStatusRenderer.soundsForSnapshot();
					command(client, "item replace entity @s armor.head with minecraft:diamond_helmet[damage=350]");
					command(client, "item replace entity @s armor.chest with minecraft:elytra[damage=120]");
					command(client, "item replace entity @s armor.legs with minecraft:iron_leggings");
					command(client, "item replace entity @s armor.feet with minecraft:netherite_boots[damage=90]");
					command(client, "item replace entity @s weapon.mainhand with minecraft:diamond_pickaxe[damage=700]");
					command(client, "item replace entity @s weapon.offhand with minecraft:torch 40");
					command(client, "give @s minecraft:firework_rocket 23");
				}
				if (stepTicks == 25) {
					check(ArmorStatusRenderer.soundsForSnapshot() == armorStatusSounds + 1, "putting on a low helmet plays the low durability sound once (" + (ArmorStatusRenderer.soundsForSnapshot() - armorStatusSounds) + ")");
				}
				captureAfter(client, 30, "armor status, low helmet, elytra with fireworks");
			}
			case 319 -> {
				if (stepTicks == 1) {
					EMUtilsConfig config = EMUtilsClient.config();
					config.setSettingsUiDark(false);
					config.setArmorStatusDisplay(ArmorStatusDisplay.REMAINING_MAX);
				}
				captureAfter(client, 10, "armor status, light, remaining out of max");
			}
			case 320 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setSettingsUiDark(true);
					SettingsScreen settings = new SettingsScreen(null);
					client.gui.setScreen(settings);
					settings.openSheet("armor_status");
				}
				if (stepTicks == 15 && MinecraftClientCompat.screen(client) instanceof SettingsScreen settings) {
					settings.selectSheetSectionForSnapshot(1);
				}
				captureAfter(client, 35, "armor status sheet, display tab");
			}
			case 321 -> {
				EMUtilsClient.config().resetArmorStatusDefaults();
				client.gui.setScreen(null);
				for (String slot : new String[] {"armor.head", "armor.chest", "armor.legs", "armor.feet", "weapon.mainhand", "weapon.offhand"}) {
					command(client, "item replace entity @s " + slot + " with minecraft:air");
				}
				command(client, "clear @s minecraft:firework_rocket");
				next();
			}
			// Free Camera settings (#49): Collision stops the camera at a wall and without it the camera flies
			// through; Double-Tap to Start; the camera keeps facing the same way through a dimension change;
			// Camera FOV.
			case 322 -> {
				if (stepTicks == 1 && client.player != null) {
					client.gui.setScreen(null);
					setGuiScale(client, 2);
					EMUtilsConfig config = EMUtilsClient.config();
					config.setTweakFreeCamera(false);
					config.resetFreeCameraSettings();
					config.setFreeCameraCollision(true);
					freeCameraStart = client.player.position();
					freeCameraWallX = client.player.blockPosition().getX() + 6;
					BlockPos base = client.player.blockPosition();
					command(client, "fill " + freeCameraWallX + " " + (base.getY() - 3) + " " + (base.getZ() - 4) + " " + freeCameraWallX + " " + (base.getY() + 8) + " " + (base.getZ() + 4) + " minecraft:stone");
					config.setTweakFreeCamera(true);
				}
				if (stepTicks == 10) {
					EMUtilsClient.tweaks().freeCamera().placeForSnapshot(freeCameraStart.x, freeCameraStart.y, freeCameraStart.z, -90.0F, 0.0F);
					client.options.keyUp.setDown(true);
				}
				if (stepTicks == 50) {
					client.options.keyUp.setDown(false);
					Vec3 camera = EMUtilsClient.tweaks().freeCamera().positionForSnapshot();
					check(camera != null && camera.x > freeCameraStart.x + 3 && camera.x + 0.25 <= freeCameraWallX + 0.001, "with Collision on, the camera flies up to the wall and stops there (" + (camera == null ? "none" : String.format("%.2f", camera.x)) + ", wall at " + freeCameraWallX + ")");
					next();
				}
			}
			case 323 -> {
				if (stepTicks == 1) {
					EMUtilsClient.config().setFreeCameraCollision(false);
					EMUtilsClient.tweaks().freeCamera().placeForSnapshot(freeCameraStart.x, freeCameraStart.y, freeCameraStart.z, -90.0F, 0.0F);
					client.options.keyUp.setDown(true);
				}
				if (stepTicks == 40) {
					client.options.keyUp.setDown(false);
					Vec3 camera = EMUtilsClient.tweaks().freeCamera().positionForSnapshot();
					check(camera != null && camera.x > freeCameraWallX + 2, "with Collision off, the camera flies through the wall (" + (camera == null ? "none" : String.format("%.2f", camera.x)) + ")");
					next();
				}
			}
			case 324 -> {
				EMUtilsConfig config = EMUtilsClient.config();
				FreeCameraManager freeCamera = EMUtilsClient.tweaks().freeCamera();
				if (stepTicks == 1) {
					config.setTweakFreeCamera(false);
					config.setFreeCameraDoubleTap(true);
				}
				if (stepTicks == 5) {
					freeCamera.pressKeyForSnapshot();
				}
				if (stepTicks == 8) {
					check(!config.tweakFreeCamera(), "with Double-Tap to Start, one press doesn't start Free Camera");
					freeCamera.pressKeyForSnapshot();
				}
				if (stepTicks == 10) {
					check(config.tweakFreeCamera() && freeCamera.isActive(), "a second press right after starts it");
					freeCamera.pressKeyForSnapshot();
				}
				if (stepTicks == 12) {
					check(!config.tweakFreeCamera() && !freeCamera.isActive(), "one press ends it");
					config.setFreeCameraDoubleTap(false);
					next();
				}
			}
			case 325 -> {
				FreeCameraManager freeCamera = EMUtilsClient.tweaks().freeCamera();
				if (stepTicks == 1) {
					EMUtilsClient.config().setTweakFreeCamera(true);
					command(client, "effect give @s minecraft:resistance 60 4");
					command(client, "effect give @s minecraft:fire_resistance 60 0");
				}
				if (stepTicks == 5) {
					freeCamera.placeForSnapshot(freeCameraStart.x, freeCameraStart.y + 8, freeCameraStart.z, 45.0F, 20.0F);
					command(client, "execute in minecraft:the_nether run tp @s " + (int) freeCameraStart.x + " 70 " + (int) freeCameraStart.z);
				}
				if (stepTicks == 1) {
					freeCameraArrivedTick = -1;
				}
				if (freeCameraArrivedTick < 0 && stepTicks > 5 && client.level != null && client.level.dimension() == Level.NETHER && freeCamera.dimensionForSnapshot() == Level.NETHER) {
					freeCameraArrivedTick = stepTicks;
					command(client, "fill ~-1 ~ ~-1 ~1 ~2 ~1 minecraft:air");
					check(Math.abs(freeCamera.yawForSnapshot() - 45.0F) < 0.01F, "after going to the Nether, the free camera is on in the Nether, facing the same way (" + freeCamera.yawForSnapshot() + ")");
				}
				if (freeCameraArrivedTick >= 0) {
					captureAfter(client, freeCameraArrivedTick + 40, "free camera in the nether, facing the same way");
				} else if (stepTicks == 400) {
					check(false, "went to the Nether with Free Camera on");
					next();
				}
			}
			case 326 -> {
				FreeCameraManager freeCamera = EMUtilsClient.tweaks().freeCamera();
				if (stepTicks == 1) {
					command(client, "execute in minecraft:overworld run tp @s " + freeCameraStart.x + " " + freeCameraStart.y + " " + freeCameraStart.z);
				}
				if (stepTicks > 1 && client.level != null && client.level.dimension() == Level.OVERWORLD && freeCamera.dimensionForSnapshot() == Level.OVERWORLD) {
					check(freeCamera.isActive() && Math.abs(freeCamera.yawForSnapshot() - 45.0F) < 0.01F, "back in the Overworld, the free camera is still on, facing the same way");
					next();
				} else if (stepTicks == 400) {
					check(false, "came back to the Overworld with Free Camera on");
					next();
				}
			}
			case 327 -> {
				if (stepTicks == 1 && client.player != null) {
					EMUtilsConfig config = EMUtilsClient.config();
					config.setFreeCameraCustomFov(true);
					config.setFreeCameraFov(30);
					EMUtilsClient.tweaks().freeCamera().placeForSnapshot(freeCameraStart.x - 12, freeCameraStart.y + 4, freeCameraStart.z, -90.0F, 10.0F);
				}
				if (stepTicks == 20) {
					float fov = client.gameRenderer.mainCamera().getFov();
					check(Math.abs(fov - 30.0F) < 0.5F, "with Camera FOV at 30, the detached camera's field of view is 30 (" + fov + ")");
				}
				captureAfter(client, 30, "free camera with a 30 degree field of view, looking at the wall");
			}
			case 328 -> {
				EMUtilsConfig config = EMUtilsClient.config();
				config.setTweakFreeCamera(false);
				config.resetFreeCameraSettings();
				BlockPos base = BlockPos.containing(freeCameraStart);
				command(client, "fill " + freeCameraWallX + " " + (base.getY() - 3) + " " + (base.getZ() - 4) + " " + freeCameraWallX + " " + (base.getY() + 8) + " " + (base.getZ() + 4) + " minecraft:air");
				command(client, "effect clear @s");
				next();
			}
			// Outside a world: the settings can be opened from the title screen, and so can their screens.
			case 329 -> {
				EMUtilsClient.config().resetHudDefaults();
				SmokeLaunchVerifier.stopEnteringTestWorld();
				leftWorld = true;
				client.disconnectFromWorld(Component.literal("EMUtils UI snapshots"));
				next();
			}
			case 330 -> {
				if (client.level == null && MinecraftClientCompat.screen(client) != null && stepTicks > 20) {
					client.gui.setScreen(new WaypointsScreen(MinecraftClientCompat.screen(client)));
					next();
				} else if (stepTicks > 400) {
					check(false, "left the world for the outside-a-world snapshots");
					next();
				}
			}
			case 331 -> {
				if (stepTicks == 1 && MinecraftClientCompat.screen(client) instanceof WaypointsScreen screen) {
					screen.openAddSheetForSnapshot();
					check(!screen.sheetOpenForSnapshot(), "Add waypoint doesn't open outside a world");
					// Vanilla caps menus outside a world at 60 FPS; EMUtils menus use the Max Framerate (#162).
					int limit = client.getFramerateLimitTracker().getFramerateLimit();
					check(limit == client.options.framerateLimit().get(), "an EMUtils menu outside a world uses the Max Framerate (" + limit + " FPS)");
				}
				capture(client, "waypoints, not in a world");
			}
			// The EMUtils icon on the title screen (#160), first in the row of small icons.
			case 332 -> {
				if (stepTicks == 1) {
					setGuiScale(client, 2);
					client.gui.setScreen(new TitleScreen());
				}
				if (stepTicks == 10 && MinecraftClientCompat.screen(client) instanceof TitleScreen title) {
					checkIconRow(title, "title screen");
				}
				captureAfter(client, 20, "title screen, EMUtils icon");
			}
			// Closing back to a vanilla screen shows it right away, with the panel fading out over it (#164).
			case 333 -> {
				if (stepTicks == 1) {
					setGuiScale(client, 2);
					TitleScreen title = new TitleScreen();
					client.gui.setScreen(title);
					client.gui.setScreen(new SettingsScreen(title));
				}
				if (stepTicks == 15) {
					Screen settings = MinecraftClientCompat.screen(client);
					settings.onClose();
					check(settings instanceof SettingsScreen && MinecraftClientCompat.screen(client) instanceof TitleScreen, "closing the settings goes back to the title screen right away");
					// A resize re-initializes the title screen, and with it its Fabric events; the fade goes on.
					client.resizeGui();
				}
				captureAfter(client, 16, "settings closing back to the title screen, mid-fade after a resize");
			}
			default -> finish(client);
		}
	}

	/**
	 * The EMUtils icon is the first of the screen's small icon buttons, in the same row, the row's icons
	 * don't overlap, and there's no EMUtils button in the top-left corner any more (#160).
	 */
	private static void checkIconRow(Screen screen, String where) {
		AbstractWidget emutils = null;
		List<AbstractWidget> others = new ArrayList<>();
		boolean cornerButton = false;
		for (GuiEventListener child : screen.children()) {
			if (SettingsIconButton.is(child)) {
				emutils = (AbstractWidget) child;
			} else if (child instanceof SpriteIconButton icon) {
				others.add(icon);
			} else if (child instanceof AbstractWidget widget && widget.getX() < 20 && widget.getY() < 20) {
				cornerButton = true;
			}
		}
		AbstractWidget icon = emutils;
		boolean first = icon != null && !others.isEmpty() && others.stream().allMatch(other -> other.getY() != icon.getY() || other.getX() >= icon.getX() + icon.getWidth());
		boolean inRow = icon != null && others.stream().anyMatch(other -> other.getY() == icon.getY());
		check(first && inRow, "the EMUtils icon is the first of the " + where + "'s icon buttons, without overlapping them");
		check(!cornerButton, "no EMUtils button is left in the " + where + "'s top-left corner");
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

	/** The cases from the PR review of profiles (#89): long names, overlapping domains, other JSON, damaged files. */
	private static void checkProfileReviewFixes(ProfileManager profiles, Profile hypixel) {
		Profile longName = profiles.create("Survival Hardcore Friends Server", ProfileIcon.STAR, ProfileColor.BLUE, List.of(), false, false);
		Profile copy = profiles.duplicate(longName);
		check(copy != null && !copy.name().getString().equalsIgnoreCase(longName.name().getString()), "a copy of a profile with a 32-character name gets a name of its own (" + (copy == null ? null : copy.name().getString()) + ")");
		profiles.delete(longName);
		if (copy != null) {
			profiles.delete(copy);
		}

		// Hypixel has hypixel.net and is higher in the list; the more specific rule still wins.
		Profile minigames = profiles.create("Minigames", ProfileIcon.GAMEPAD, ProfileColor.PURPLE, List.of("mc.hypixel.net"), false, false);
		check(profiles.profileForServer("mc.hypixel.net") == minigames && profiles.profileForServer("MC.Hypixel.net:25565") == minigames, "the most specific server rule wins over a domain higher in the list");
		check(profiles.profileForServer("play.hypixel.net") == hypixel && profiles.profileForServer("example.org") == null, "a domain still covers its other subdomains");
		profiles.delete(minigames);

		check(profiles.importProfile("{}") == null && profiles.importProfile("{\"someOtherMod\": true}") == null, "JSON that isn't an EMUtils config doesn't import");
		Profile plain = profiles.importProfile(EMUtilsClient.config().toJson());
		check(plain != null, "a plain /emutils export still imports");
		if (plain != null) {
			profiles.delete(plain);
		}

		// A damaged profile file is reported, not overwritten with the defaults.
		Profile damaged = profiles.create("Damaged", ProfileIcon.FLAME, ProfileColor.RED, List.of(), false, false);
		Path file = EMUtilsPaths.profilesDir().resolve(damaged.id() + ".json");
		try {
			Files.writeString(file, "not json");
			boolean reported = profiles.export(damaged) == null && profiles.duplicate(damaged) == null;
			check(reported && Files.readString(file).equals("not json"), "a damaged profile file is reported and left alone by export and duplicate");
		} catch (IOException exception) {
			check(false, "a damaged profile file can be written for the check: " + exception);
		}
		// A profile whose file is missing reads as the defaults, the way switching to it would load it.
		try {
			Files.deleteIfExists(file);
			check(profiles.export(damaged) != null, "a profile without a settings file exports as the defaults");
		} catch (IOException exception) {
			check(false, "a profile file can be removed for the check: " + exception);
		}
		profiles.delete(damaged);
	}

	/** Resetting a profile puts its settings back to the defaults and keeps it active (#89). */
	private static void checkProfileReset() {
		ProfileManager profiles = EMUtilsClient.profiles();
		Profile hypixel = profiles.byName("Hypixel");
		if (hypixel == null) {
			check(false, "the Hypixel profile exists for the reset check");
			return;
		}
		profiles.pick(hypixel);
		EMUtilsClient.config().setTweakFullbright(true);
		boolean reset = profiles.resetToDefaults(hypixel);
		check(reset && profiles.active() == hypixel && !EMUtilsClient.config().tweakFullbright(), "resetting the active profile puts its settings back to the defaults");
	}

	/** Fills the multiplayer list up to 27 servers, Hypixel and Wynncraft first, for the server picker. */
	private static void seedServerList(Minecraft client) {
		ServerList list = new ServerList(client);
		list.load();
		if (list.size() == 0) {
			list.add(new ServerData("Hypixel", "mc.hypixel.net", ServerData.Type.OTHER), false);
			list.add(new ServerData("Wynncraft", "play.wynncraft.com", ServerData.Type.OTHER), false);
		}
		// Enough servers that the picker has to scroll and filtering matters.
		for (int i = list.size() + 1; list.size() < 27; i++) {
			list.add(new ServerData("Survival server " + i, "play-" + i + ".example.net", ServerData.Type.OTHER), false);
		}
		list.save();
	}

	/**
	 * Switching profiles swaps every setting, keeps the settings UI's look, and loads profiles by
	 * themselves on joining a world (#89). Leaves the Singleplayer profile active.
	 */
	private static void checkProfileSwitching(Minecraft client) {
		ProfileManager profiles = EMUtilsClient.profiles();
		Profile defaults = profiles.profiles().getFirst();
		Profile singleplayer = profiles.byName("Singleplayer");
		Profile hypixel = profiles.byName("Hypixel");
		if (singleplayer == null || hypixel == null) {
			check(false, "snapshot profiles exist");
			return;
		}
		profiles.pick(defaults);
		boolean fullbrightBefore = EMUtilsClient.config().tweakFullbright();
		EMUtilsClient.config().setTweakFullbright(true);
		EMUtilsClient.config().setSettingsUiDark(true);
		profiles.pick(singleplayer);
		check(profiles.active() == singleplayer && !EMUtilsClient.config().tweakFullbright(), "a profile started from the defaults has its own settings");
		check(EMUtilsClient.config().settingsUiDark(), "the settings UI keeps its look across profiles");
		profiles.pick(defaults);
		check(EMUtilsClient.config().tweakFullbright(), "switching back restores the Default profile's settings");
		EMUtilsClient.config().setTweakFullbright(fullbrightBefore);

		// Joining this singleplayer world loads the Singleplayer profile; without the link, it goes back.
		profiles.onJoin(client);
		check(profiles.active() == singleplayer, "joining a singleplayer world loads the singleplayer profile");
		profiles.update(singleplayer, "Singleplayer", singleplayer.icon(), singleplayer.color(), List.of(), false);
		profiles.onJoin(client);
		check(profiles.active() == defaults, "joining a place no profile is linked to goes back to the picked profile");
		profiles.update(singleplayer, "Singleplayer", singleplayer.icon(), singleplayer.color(), List.of(), true);

		// Giving a server to one profile takes it from another.
		profiles.update(singleplayer, "Singleplayer", singleplayer.icon(), singleplayer.color(), List.of("hypixel.net"), true);
		check(hypixel.servers().isEmpty() && singleplayer.servers().equals(List.of("hypixel.net")), "a server loads only the profile it was last given to");
		profiles.update(singleplayer, "Singleplayer", singleplayer.icon(), singleplayer.color(), List.of(), true);
		profiles.update(hypixel, "Hypixel", hypixel.icon(), hypixel.color(), List.of("hypixel.net"), false);

		// Export and import round trip, and text that isn't a profile.
		Profile imported = profiles.importProfile(profiles.export(hypixel));
		check(imported != null && imported.name().getString().equals("Hypixel 2") && imported.icon() == ProfileIcon.SWORDS, "an exported profile imports with its icon, and a number since its name is taken");
		if (imported != null) {
			profiles.delete(imported);
		}
		check(profiles.importProfile("not a profile") == null, "text that isn't a profile doesn't import");
		check(ProfileManager.parseServers(" Play.Example.net:25565 ,hypixel.net  mc.x.org.").equals(List.of("play.example.net", "hypixel.net", "mc.x.org")), "typed servers are split and normalized");

		checkProfileReviewFixes(profiles, hypixel);

		profiles.onJoin(client);
		check(profiles.active() == singleplayer, "singleplayer profile active for the light snapshot");
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

	/** How the search sees the first slot of the screen holding {@code item}, or NONE if there's none. */
	private static InventorySearch.Match searchMatch(AbstractContainerScreen<?> screen, Item item) {
		for (Slot slot : screen.getMenu().slots) {
			if (slot.getItem().is(item)) {
				return InventorySearch.match(slot.getItem());
			}
		}
		return InventorySearch.Match.NONE;
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

	private static String hexColors(List<Integer> colors) {
		return colors.stream().map(color -> String.format(Locale.ROOT, "#%08X", color)).toList().toString();
	}

	/** Runs a command as the player; the test world is created with commands allowed. */
	private static void command(Minecraft client, String command) {
		if (client.getConnection() != null) {
			client.getConnection().sendCommand(command);
		}
	}

	/** Absolute coordinates, as command text, of a block offset from where the quick wins section began. */
	private static String at(int dx, int dy, int dz) {
		return (quickWinsOrigin.getX() + dx) + " " + (quickWinsOrigin.getY() + dy) + " " + (quickWinsOrigin.getZ() + dz);
	}

	private static void openSheetAndCapture(Minecraft client, String featureId, String label) {
		if (stepTicks == 1) {
			SettingsScreen settings = new SettingsScreen(null);
			client.gui.setScreen(settings);
			settings.openSheet(featureId);
		}
		captureAfter(client, 20, label);
	}

	/** Takes a screenshot the frame after a lightning bolt shows up, while it still flashes. */
	private static void captureLightning(Minecraft client, String label) {
		boolean bolt = false;
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof LightningBolt) {
				bolt = true;
				break;
			}
		}
		if (bolt && lightningSeenAt < 0) {
			lightningSeenAt = stepTicks;
		}
		if (lightningSeenAt >= 0 && stepTicks > lightningSeenAt) {
			lightningSeenAt = -1;
			grab(client, label);
			next();
		} else if (stepTicks > 200) {
			check(false, "a summoned lightning bolt showed up for: " + label);
			lightningSeenAt = -1;
			next();
		}
	}

	/** The value of the Look-At Info line labeled {@code labelKey}, or an empty string. */
	private static String lookAtValue(LookAtInfoData data, String labelKey) {
		for (HudOverlayLine line : data.lines()) {
			if (line.labelKey().equals(labelKey)) {
				return line.value();
			}
		}
		return "";
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
		grab(client, label);
		next();
	}

	/** Saves a screenshot named after the step and {@code label}, replacing one from an earlier run. */
	private static void grab(Minecraft client, String label) {
		String slug = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
		String name = String.format(Locale.ROOT, "%03d-%s.png", step, slug);
		EMUtilsClient.LOGGER.info("EMUtils UI snapshot: {} ({})", label, name);
		Screenshot.grab(client.gameDirectory, name, client.gameRenderer.mainRenderTarget(), 1, message -> {
		});
	}

	/**
	 * Deletes this range's screenshots from earlier runs, so each screen is taken fresh: the gallery
	 * check needs its screenshot to be new, not a file it already listed.
	 */
	private static void clearOldSnapshots(Minecraft client) {
		int first = step;
		Path folder = client.gameDirectory.toPath().resolve("screenshots");
		if (!Files.isDirectory(folder)) {
			return;
		}
		try (Stream<Path> files = Files.list(folder)) {
			for (Path file : files.toList()) {
				String name = file.getFileName().toString();
				if (!name.matches("\\d{3}-.*\\.png")) {
					continue;
				}
				int fileStep = Integer.parseInt(name.substring(0, 3));
				if (fileStep >= first && fileStep <= LAST_STEP) {
					Files.deleteIfExists(file);
				}
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils UI snapshot: couldn't clear old screenshots", exception);
		}
	}

	private static void finish(Minecraft client) {
		EMUtilsClient.LOGGER.info("EMUtils UI snapshots done; stopping Minecraft.");
		enabled = false;
		client.stop();
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
