package net.emutils.client.emutils.debug;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.IrisCompat;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.render.BeaconRadiusRenderer;
import net.emutils.client.emutils.render.LightLevelOverlayRenderer;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.jspecify.annotations.Nullable;

/**
 * The visual compatibility scenes (#246), UI snapshot steps 1000 and up, run with Sodium, Iris and Mod Menu
 * ({@code -PemutilsCompat}) and without them, for the baseline. Launching isn't enough: what breaks with those
 * mods is usually how EMUtils draws, so each scene shows one feature off, on, and off again on a fixed stage
 * high above the world, and compares the frames. The two frames with it off say how much the picture changes
 * by itself (clouds, shader animation); the feature has to change clearly more than that. Each run writes the
 * numbers to {@code compat-results.tsv}, which {@code scripts/test-compat.ps1} compares with the baseline's,
 * and the frames, with a picture of what changed, to {@code screenshots/compat}.
 *
 * <p>With Iris, the scenes run a second time with the test shader pack on.
 */
final class CompatScenes {
	static final int FIRST_STEP = 1000;
	static final String SHADER_PACK = "EMUtils Test Shaders.zip";

	enum Outcome { WAIT, NEXT, FINISH }

	/** A pixel counts as changed when one of its channels moved by more than this. */
	private static final int PIXEL_DELTA = 32;
	/** A feature has to change at least this share of the frame, and this many times what changes by itself. */
	private static final double MIN_SHARE = 0.0008;
	private static final int NOISE_FACTOR = 3;
	private static final int SETTLE_TICKS = 40;
	/** After the time changes, the sky takes a few seconds to fade to its new colors. */
	private static final int NEW_TIME_SETTLE_TICKS = 140;

	private record Scene(String id, String time, BiConsumer<Minecraft, Boolean> toggle, int onTicks) {
	}

	/** The night scenes first, then the day ones, so the sky only has to settle once in between. */
	private static final List<Scene> SCENES = List.of(
		new Scene("fullbright", "midnight", (client, on) -> EMUtilsClient.config().setTweakFullbright(on), 20),
		new Scene("beacon-radius-outline", "midnight", (client, on) -> EMUtilsClient.config().setBeaconRadiusOutline(on), 20),
		new Scene("light-level-overlay", "midnight", (client, on) -> EMUtilsClient.config().setLightLevelOverlay(on), 40),
		new Scene("waypoint-beam", "midnight", CompatScenes::showWaypoint, 20),
		new Scene("no-fog", "noon", (client, on) -> EMUtilsClient.config().setTweakNoFog(on), 20),
		new Scene("minimap", "noon", (client, on) -> EMUtilsClient.config().setMinimap(on), 40),
		new Scene("hud-overlay", "noon", (client, on) -> EMUtilsClient.config().setHudOverlay(on), 20)
	);

	private static final int SCENES_START = 1;
	private static final int MENUS = SCENES_START + SCENES.size();
	private static final int SHADERS_ON = MENUS + 1;
	private static final int SHADER_SCENES_START = SHADERS_ON + 1;
	private static final int WRAP_UP = SHADER_SCENES_START + SCENES.size();

	private static BlockPos stage = BlockPos.ZERO;
	private static String time = "";
	private static int settle = SETTLE_TICKS;
	private static @Nullable Waypoint waypoint;
	private static boolean shaders;
	private static @Nullable ChatVisiblity chatBefore;
	private static @Nullable CloudStatus cloudsBefore;
	private static boolean bobbingBefore;
	private static double fovEffectsBefore;
	private static int renderDistanceBefore;
	private static final List<String> results = new ArrayList<>();
	/** The frames of the scene under way: off, on, off again; null until each comes back from the GPU. */
	private static final int[][] frames = new int[3][];
	private static int frameWidth;
	private static int frameHeight;

	private CompatScenes() {
	}

	static Outcome tick(Minecraft client, int index, int stepTicks) {
		if (index == 0) {
			return setUpStage(client, stepTicks);
		}
		if (index < MENUS) {
			return scene(client, SCENES.get(index - SCENES_START), stepTicks);
		}
		if (index == MENUS) {
			return menus(client, stepTicks);
		}
		if (index == SHADERS_ON) {
			return turnShadersOn(client, stepTicks);
		}
		if (index < WRAP_UP) {
			return shaders ? scene(client, SCENES.get(index - SHADER_SCENES_START), stepTicks) : Outcome.NEXT;
		}
		if (index == WRAP_UP) {
			wrapUp(client);
		}
		return Outcome.FINISH;
	}

	/**
	 * A stone platform high above spawn with a beacon on it, lit only by the sky: no terrain in sight but
	 * the platform, so every run sees the same thing. Time and weather stand still.
	 */
	private static Outcome setUpStage(Minecraft client, int stepTicks) {
		if (stepTicks == 1) {
			client.gui.setScreen(null);
			if (client.options.guiScale().get() != 2) {
				client.options.guiScale().set(2);
				client.resizeGui();
			}
			// Nothing on screen that the scenes don't show: no chat lines, no tutorial toasts, no clouds, and a
			// still camera. At a render distance of 2 the fog falls on the far side of the platform.
			chatBefore = client.options.chatVisibility().get();
			cloudsBefore = client.options.cloudStatus().get();
			bobbingBefore = client.options.bobView().get();
			fovEffectsBefore = client.options.fovEffectScale().get();
			renderDistanceBefore = client.options.renderDistance().get();
			client.options.chatVisibility().set(ChatVisiblity.HIDDEN);
			client.options.cloudStatus().set(CloudStatus.OFF);
			client.options.bobView().set(false);
			client.options.fovEffectScale().set(0.0);
			client.getTutorial().setStep(TutorialSteps.NONE);
			clearOldFrames(client);

			EMUtilsConfig config = EMUtilsClient.config();
			config.setMinimap(false);
			config.setWorldMap(false);
			config.setHudOverlay(false);
			config.setTweakFullbright(false);
			config.setTweakNoFog(false);
			config.resetBeaconRadiusDefaults();
			// A dense grid of thick lines, so the cage covers enough of the frame to compare.
			config.setBeaconRadiusGridSpacing(4);
			config.setBeaconRadiusLineWidth(3);
			config.resetLightLevelDefaults();
			config.setLightLevelOverlay(false);
			// The beacon has to be found once before its scene: its outline shows when it's on.
			config.setBeaconRadiusOutline(true);

			BlockPos spawn = client.player.blockPosition();
			stage = new BlockPos(spawn.getX(), Math.min(client.level.getMaxY() - 40, 260), spawn.getZ());
			// Iris keeps shaders on between launches; the first round of scenes is without them.
			setShaders(false);
			command(client, "difficulty peaceful");
			// Building the beacon would pop this advancement's toast into the frames.
			command(client, "advancement grant @s only minecraft:nether/create_beacon");
			command(client, "gamerule advance_time false");
			command(client, "gamerule advance_weather false");
			command(client, "weather clear");
			command(client, "time set midnight");
			time = "midnight";
			command(client, "fill " + at(-40, 0, -40) + " " + at(40, 0, 40) + " minecraft:smooth_stone");
			command(client, "fill " + at(-1, 1, 8) + " " + at(1, 1, 10) + " minecraft:iron_block");
			command(client, "setblock " + at(0, 2, 9) + " minecraft:beacon");
			command(client, "tp @s " + at(0, 1, 0) + " 180 0");
			waypoint = EMUtilsClient.waypoint().addCustom(client, null, "Compat", stage.getX() + 12, stage.getY() + 1, stage.getZ(), 0xFFFF5555, true, "");
			if (waypoint != null && !waypoint.hidden()) {
				EMUtilsClient.waypoint().toggleHidden(waypoint.id());
			}
			results.clear();
			shaders = false;
			return Outcome.WAIT;
		}
		// A beacon only gets its levels when it updates, every 80 ticks.
		boolean found = BeaconRadiusRenderer.outlinedBeaconsForSnapshot() >= 1;
		// The toast of the advancement granted above is gone after five seconds.
		if (found && stepTicks >= 120 || stepTicks >= 400) {
			check(found, "the compatibility stage's beacon is outlined before its scene (" + stepTicks + " ticks)");
			EMUtilsClient.config().setBeaconRadiusOutline(false);
			// Only now, so the platform was built while its chunks were all loaded.
			client.options.renderDistance().set(2);
			return Outcome.NEXT;
		}
		return Outcome.WAIT;
	}

	private static Outcome scene(Minecraft client, Scene scene, int stepTicks) {
		if (stepTicks == 1) {
			settle = scene.time().equals(time) ? SETTLE_TICKS : NEW_TIME_SETTLE_TICKS;
			time = scene.time();
		}
		int on = settle + 1;
		int offAgain = on + scene.onTicks() + 1;
		int last = offAgain + 20;
		if (stepTicks == 1) {
			client.gui.setScreen(null);
			scene.toggle().accept(client, false);
			command(client, "time set " + scene.time());
			place(client, scene.id());
			java.util.Arrays.fill(frames, null);
		} else if (stepTicks == settle) {
			grab(client, 0, scene, "off");
		} else if (stepTicks == on) {
			scene.toggle().accept(client, true);
		} else if (stepTicks == on + scene.onTicks()) {
			grab(client, 1, scene, "on");
		} else if (stepTicks == offAgain) {
			scene.toggle().accept(client, false);
		} else if (stepTicks == last) {
			grab(client, 2, scene, "off-again");
		} else if (stepTicks > last) {
			if (frames[0] != null && frames[1] != null && frames[2] != null) {
				compare(client, scene);
				return Outcome.NEXT;
			}
			if (stepTicks > last + 200) {
				check(false, name(scene) + ": the frames came back from the GPU");
				return Outcome.NEXT;
			}
		}
		return Outcome.WAIT;
	}

	/** Where on the platform each scene looks from. */
	private static void place(Minecraft client, String scene) {
		String where = switch (scene) {
			// Up at the wall of the beacon's cage, 11 blocks to the north.
			case "beacon-radius-outline" -> at(0, 1, 0) + " 180 -25";
			// Down at the platform, where every spot is dark enough for the overlay.
			case "light-level-overlay" -> at(0, 1, 0) + " 180 60";
			// The waypoint stands 12 blocks to the east.
			case "waypoint-beam" -> at(0, 1, 0) + " -90 0";
			case "fullbright" -> at(0, 1, 0) + " 90 35";
			// Along the platform into the fog, which starts a few blocks before the render distance ends.
			case "no-fog" -> at(0, 1, 0) + " 90 8";
			default -> at(0, 1, 0) + " 90 10";
		};
		command(client, "tp @s " + where);
	}

	private static void showWaypoint(Minecraft client, boolean on) {
		if (waypoint != null && waypoint.hidden() == on) {
			EMUtilsClient.waypoint().toggleHidden(waypoint.id());
		}
	}

	/** Reads back the frame on screen; it arrives a frame or two later. */
	private static void grab(Minecraft client, int slot, Scene scene, String state) {
		Path file = folder(client).resolve(name(scene) + "-" + state + ".png");
		Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
			try (image) {
				int width = image.getWidth();
				int height = image.getHeight();
				int[] pixels = new int[width * height];
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						pixels[y * width + x] = image.getPixel(x, y);
					}
				}
				frameWidth = width;
				frameHeight = height;
				frames[slot] = pixels;
				image.writeToFile(file);
			} catch (IOException exception) {
				EMUtilsClient.LOGGER.warn("Could not save the compatibility frame {}", file, exception);
			}
		});
	}

	private static void compare(Minecraft client, Scene scene) {
		int[] off = frames[0];
		int[] on = frames[1];
		int[] offAgain = frames[2];
		int total = off.length;
		boolean[] noiseMask = new boolean[total];
		int noise = changed(off, offAgain, noiseMask);
		boolean[] mask = new boolean[total];
		int toOn = changed(off, on, mask);
		int back = changed(offAgain, on, null);
		int signal = Math.min(toOn, back);
		int needed = Math.max((int) (total * MIN_SHARE), noise * NOISE_FACTOR);
		boolean passed = signal >= needed;
		String share = String.format(Locale.ROOT, "%.2f%%", 100.0 * signal / total);
		check(passed, name(scene) + " visibly changes the picture: " + signal + " pixels (" + share + "), "
			+ noise + " change by themselves, " + needed + " needed");
		results.add(name(scene) + "\t" + signal + "\t" + noise + "\t" + total + "\t" + (passed ? "pass" : "fail"));
		writeMask(client, name(scene) + "-changed", on, mask);
		if (noise > 0) {
			writeMask(client, name(scene) + "-changed-by-itself", offAgain, noiseMask);
		}
	}

	/** How many pixels differ between two frames; {@code mask}, when given, marks which. */
	private static int changed(int[] a, int[] b, boolean @Nullable [] mask) {
		int count = 0;
		for (int i = 0; i < a.length; i++) {
			int pa = a[i];
			int pb = b[i];
			int delta = Math.max(Math.abs((pa & 0xFF) - (pb & 0xFF)),
				Math.max(Math.abs((pa >> 8 & 0xFF) - (pb >> 8 & 0xFF)), Math.abs((pa >> 16 & 0xFF) - (pb >> 16 & 0xFF))));
			if (delta > PIXEL_DELTA) {
				count++;
				if (mask != null) {
					mask[i] = true;
				}
			}
		}
		return count;
	}

	/** The frame darkened, with the pixels that changed in magenta. */
	private static void writeMask(Minecraft client, String name, int[] frame, boolean[] mask) {
		Path file = folder(client).resolve(name + ".png");
		try (NativeImage image = new NativeImage(frameWidth, frameHeight, false)) {
			for (int y = 0; y < frameHeight; y++) {
				for (int x = 0; x < frameWidth; x++) {
					int i = y * frameWidth + x;
					int pixel = frame[i];
					int dimmed = 0xFF000000 | (pixel >> 2 & 0x3F3F3F);
					image.setPixel(x, y, mask[i] ? 0xFFFF00FF : dimmed);
				}
			}
			image.writeToFile(file);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not save the compatibility difference {}", file, exception);
		}
	}

	/**
	 * The pause menu, where EMUtils adds its settings button to the row Mod Menu also adds to: no two buttons
	 * may overlap. With Mod Menu, EMUtils is listed and its config button opens the EMUtils settings.
	 */
	private static Outcome menus(Minecraft client, int stepTicks) {
		boolean modMenu = FabricLoader.getInstance().isModLoaded("modmenu");
		if (stepTicks == 1) {
			client.gui.setScreen(new PauseScreen(true));
		} else if (stepTicks == 15) {
			Screen screen = MinecraftClientCompat.screen(client);
			List<AbstractWidget> widgets = new ArrayList<>();
			if (screen != null) {
				for (GuiEventListener child : screen.children()) {
					if (child instanceof AbstractWidget widget && widget.visible) {
						widgets.add(widget);
					}
				}
			}
			String overlaps = overlaps(widgets);
			check(screen instanceof PauseScreen && overlaps.isEmpty(), "no buttons overlap on the pause menu" + (overlaps.isEmpty() ? "" : ": " + overlaps));
			if (modMenu) {
				boolean modsButton = widgets.stream().anyMatch(widget -> widget.getMessage().getString().contains("Mods"));
				check(modsButton, "the pause menu keeps Mod Menu's Mods button next to EMUtils' settings button");
			}
			saveScreen(client, "pause-menu");
		} else if (stepTicks == 20 && modMenu) {
			ModMenuChecks.check(client);
		} else if (stepTicks == 40 && modMenu) {
			saveScreen(client, "mod-menu-list");
			client.gui.setScreen(ModMenuChecks.configScreen());
		} else if (stepTicks == 70 && modMenu) {
			check(MinecraftClientCompat.screen(client) instanceof SettingsScreen, "Mod Menu's config button for EMUtils opens the EMUtils settings");
			saveScreen(client, "mod-menu-config");
		} else if (stepTicks >= 80) {
			client.gui.setScreen(null);
			return Outcome.NEXT;
		}
		return Outcome.WAIT;
	}

	private static String overlaps(List<AbstractWidget> widgets) {
		List<String> found = new ArrayList<>();
		for (int i = 0; i < widgets.size(); i++) {
			for (int j = i + 1; j < widgets.size(); j++) {
				AbstractWidget a = widgets.get(i);
				AbstractWidget b = widgets.get(j);
				if (a.getX() < b.getRight() && b.getX() < a.getRight() && a.getY() < b.getBottom() && b.getY() < a.getBottom()) {
					found.add("'" + a.getMessage().getString() + "' and '" + b.getMessage().getString() + "'");
				}
			}
		}
		return String.join(", ", found);
	}

	private static void saveScreen(Minecraft client, String name) {
		Path file = folder(client).resolve(name + ".png");
		Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
			try (image) {
				image.writeToFile(file);
			} catch (IOException exception) {
				EMUtilsClient.LOGGER.warn("Could not save the compatibility screen {}", file, exception);
			}
		});
	}

	/** With Iris, turns the test shader pack on for the second round of scenes. */
	private static Outcome turnShadersOn(Minecraft client, int stepTicks) {
		if (!IrisCompat.isIrisLoaded()) {
			return Outcome.NEXT;
		}
		if (stepTicks == 1) {
			shaders = setShaders(true) && IrisCompat.isActiveShaderPack(SHADER_PACK);
			check(shaders, "Iris turns the test shader pack on (" + IrisCompat.currentShaderPack().orElse("none") + ")");
		}
		// Shader packs build their buffers over the first frames.
		return stepTicks >= 60 ? Outcome.NEXT : Outcome.WAIT;
	}

	/** Turns the test shader pack on or off when Iris is installed; true when Iris did it. */
	private static boolean setShaders(boolean on) {
		if (!IrisCompat.isIrisLoaded() || !on && !IrisCompat.areShadersEnabled()) {
			return !on;
		}
		if (!(on ? IrisCompat.prepareShaderPack(SHADER_PACK) : IrisCompat.prepareDisableShaders())) {
			return false;
		}
		try {
			IrisCompat.reloadShaders();
			return true;
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Iris could not turn the test shader pack {}", on ? "on" : "off", exception);
			return false;
		}
	}

	/** Puts things back as they were and writes the numbers for the script. */
	private static void wrapUp(Minecraft client) {
		setShaders(false);
		for (Scene scene : SCENES) {
			scene.toggle().accept(client, false);
		}
		EMUtilsClient.config().resetBeaconRadiusDefaults();
		if (waypoint != null) {
			EMUtilsClient.waypoint().clear(client, waypoint.id());
		}
		if (chatBefore != null) {
			client.options.chatVisibility().set(chatBefore);
		}
		if (cloudsBefore != null) {
			client.options.cloudStatus().set(cloudsBefore);
		}
		client.options.bobView().set(bobbingBefore);
		client.options.fovEffectScale().set(fovEffectsBefore);
		client.options.renderDistance().set(renderDistanceBefore);
		Path file = client.gameDirectory.toPath().resolve("compat-results.tsv");
		try {
			Files.write(file, results);
		} catch (IOException exception) {
			check(false, "the compatibility results could be written to " + file + ": " + exception);
		}
		EMUtilsClient.LOGGER.info("EMUtils compatibility scenes done ({}): {} results in {}", System.getProperty("emutils.compat", "no compat mods"), results.size(), file);
	}

	private static String name(Scene scene) {
		return scene.id() + (shaders ? "-shaders" : "");
	}

	private static Path folder(Minecraft client) {
		Path folder = client.gameDirectory.toPath().resolve("screenshots").resolve("compat");
		try {
			Files.createDirectories(folder);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not make {}", folder, exception);
		}
		return folder;
	}

	private static void clearOldFrames(Minecraft client) {
		Path folder = folder(client);
		try (Stream<Path> files = Files.list(folder)) {
			for (Path file : files.toList()) {
				Files.deleteIfExists(file);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not clear {}", folder, exception);
		}
	}

	private static String at(int dx, int dy, int dz) {
		return (stage.getX() + dx) + " " + (stage.getY() + dy) + " " + (stage.getZ() + dz);
	}

	private static void command(Minecraft client, String command) {
		if (client.getConnection() != null) {
			client.getConnection().sendCommand(command);
		}
	}

	private static void check(boolean passed, String what) {
		if (passed) {
			EMUtilsClient.LOGGER.info("EMUtils UI snapshot check passed: {}", what);
		} else {
			EMUtilsClient.LOGGER.error("EMUtils UI snapshot check FAILED: {}", what);
		}
	}

	/** Mod Menu's classes, in their own class so they only load when Mod Menu is installed. */
	private static final class ModMenuChecks {
		private ModMenuChecks() {
		}

		static void check(Minecraft client) {
			CompatScenes.check(com.terraformersmc.modmenu.ModMenu.MODS.containsKey(EMUtilsClient.MOD_ID), "Mod Menu lists EMUtils");
			CompatScenes.check(com.terraformersmc.modmenu.ModMenu.hasConfigScreen(EMUtilsClient.MOD_ID), "Mod Menu knows EMUtils has a config screen");
			com.terraformersmc.modmenu.gui.ModsScreen mods = new com.terraformersmc.modmenu.gui.ModsScreen(null);
			client.gui.setScreen(mods);
			CompatScenes.check(!mods.modScreenErrors.containsKey(EMUtilsClient.MOD_ID), "Mod Menu had no error opening the EMUtils config screen");
		}

		static Screen configScreen() {
			return com.terraformersmc.modmenu.ModMenu.getConfigScreen(EMUtilsClient.MOD_ID, null);
		}
	}
}
