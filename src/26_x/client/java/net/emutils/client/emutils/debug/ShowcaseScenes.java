package net.emutils.client.emutils.debug;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.settings.KeybindsScreen;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.hud.editor.HudEditorScreen;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.inventory.InventorySearch;
import net.emutils.client.emutils.map.WorldMapScreen;
import net.emutils.client.emutils.minescript.gui.ScriptsScreen;
import net.emutils.client.emutils.packs.gui.PacksScreen;
import net.emutils.client.emutils.profile.ProfileColor;
import net.emutils.client.emutils.profile.ProfileIcon;
import net.emutils.client.emutils.profile.ProfileManager;
import net.emutils.client.emutils.profile.gui.ProfilesScreen;
import net.emutils.client.emutils.screenshot.ScreenshotMessage;
import net.emutils.client.emutils.screenshot.ScreenshotPaths;
import net.emutils.client.emutils.screenshot.gui.GalleryScreen;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.emutils.client.mixin.HandledScreenAccessor;
import net.emutils.client.mixin.MouseAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Modrinth gallery's screenshots (#272), UI snapshot steps 2000 and up, run with {@code -PemutilsShowcase=true}
 * in a kept world. Each scene poses one feature and saves the frame to {@code screenshots/showcase}; the last step
 * frames every shot (rounded corners, a raised edge and a drop shadow) into {@code screenshots/showcase/framed}.
 */
final class ShowcaseScenes {
	static final int FIRST_STEP = 2000;

	/** The view the world scenes share: a savanna river with a mountain and a cave behind it, facing west. */
	private static final int HOME_X = 300;
	private static final int HOME_Z = 0;
	private static final int HOME_Y = 69;
	private static final int PREGEN_RADIUS = 320;
	private static final String PREGEN_DONE = "emutils-showcase-generated.txt";
	private static final String SCRIPT_FOLDER = "showcase";

	private interface Pose {
		void tick(Minecraft client, int t);
	}

	/** A scene: posed from tick 1, saved at {@code shootAt}. */
	private record Scene(String name, int shootAt, Pose pose) {
	}

	private static final List<Scene> SCENES = List.of(
		new Scene("01-settings", 40, ShowcaseScenes::settingsHub),
		new Scene("02a-hud-overlay", 40, ShowcaseScenes::hudOverlay),
		new Scene("02b-look-at-info", 40, ShowcaseScenes::lookAtInfo),
		new Scene("02c-keystrokes-armor", 40, ShowcaseScenes::keystrokesAndArmor),
		new Scene("03-world-map", 160, ShowcaseScenes::worldMap),
		new Scene("04-waypoints", 60, ShowcaseScenes::waypointsInWorld),
		new Scene("05-waypoint-list", 30, (client, t) -> open(client, t, new WaypointsScreen(null))),
		new Scene("06-hud-editor", 40, ShowcaseScenes::hudEditor),
		new Scene("07-hud-overlay-settings", 40, (client, t) -> sheet(client, t, "hud_overlay")),
		new Scene("08-settings-light", 40, ShowcaseScenes::settingsLight),
		new Scene("09-zoom", 60, ShowcaseScenes::zoom),
		new Scene("10-beacon-light-level", 140, ShowcaseScenes::beaconAndLightLevel),
		new Scene("11-inventory-search", 55, ShowcaseScenes::inventorySearch),
		new Scene("12-shulker-preview", 50, ShowcaseScenes::shulkerPreview),
		new Scene("13-clear-underwater", 50, ShowcaseScenes::clearUnderwater),
		new Scene("14-pack-manager", 120, ShowcaseScenes::packManager),
		new Scene("15-script-manager", 50, ShowcaseScenes::scriptManager),
		new Scene("16-profiles", 30, ShowcaseScenes::profiles),
		new Scene("17-keybinds", 30, (client, t) -> open(client, t, new KeybindsScreen(new SettingsScreen(null)))),
		new Scene("18-menus", 40, (client, t) -> sheet(client, t, "menus")),
		new Scene("19-gallery", 60, ShowcaseScenes::gallery),
		new Scene("20-screenshot-helper", 30, ShowcaseScenes::screenshotHelper),
		new Scene("21-auto-reconnect", 30, ShowcaseScenes::autoReconnect)
	);

	private static @Nullable BlockPos chest;
	/** The blocks the beacon scene replaced, as setblock arguments, to put back after it. */
	private static final List<String> beaconRestore = new java.util.ArrayList<>();

	private ShowcaseScenes() {
	}

	static CompatScenes.Outcome tick(Minecraft client, int index, int stepTicks) {
		if (index == 0) {
			return setUp(client, stepTicks);
		}
		if (index == 1) {
			return pregenerate(client, stepTicks);
		}
		if (index == 2) {
			return seed(client, stepTicks);
		}
		int scene = index - 3;
		if (scene < SCENES.size()) {
			return scene(client, SCENES.get(scene), stepTicks);
		}
		return frameAll(client, stepTicks);
	}

	private static CompatScenes.Outcome setUp(Minecraft client, int stepTicks) {
		if (stepTicks == 1) {
			client.gui.setScreen(null);
			command(client, "gamerule send_command_feedback false");
			command(client, "gamemode creative");
			command(client, "difficulty peaceful");
			command(client, "gamerule advance_time false");
			command(client, "gamerule advance_weather false");
			command(client, "gamerule do_mob_spawning false");
			command(client, "weather clear");
			command(client, "time set 1000");
			// No toasts for recipes or advancements popping into the frames.
			command(client, "recipe give @s *");
			command(client, "advancement grant @s everything");
			client.options.renderDistance().set(16);
			client.options.bobView().set(false);
			client.options.chatVisibility().set(ChatVisiblity.HIDDEN);
			client.getTutorial().setStep(TutorialSteps.NONE);
			setGuiScale(client, 3);
			clearOldShots(client);
		}
		return stepTicks >= 40 ? CompatScenes.Outcome.NEXT : CompatScenes.Outcome.WAIT;
	}

	/** Generates the land around the view once, with Chunky, so the maps have something to show. */
	private static CompatScenes.Outcome pregenerate(Minecraft client, int stepTicks) {
		IntegratedServer server = client.getSingleplayerServer();
		Path save = server == null ? null : server.getWorldPath(LevelResource.ROOT);
		if (save == null || Files.exists(save.resolve(PREGEN_DONE))) {
			return CompatScenes.Outcome.NEXT;
		}
		Object api = chunkyApi();
		if (api == null) {
			EMUtilsClient.LOGGER.error("EMUtils showcase: Chunky isn't installed, so the land isn't generated ahead");
			return CompatScenes.Outcome.NEXT;
		}
		try {
			if (stepTicks == 20) {
				boolean started = (Boolean) callChunky(api, "startTask", "minecraft:overworld", "square", (double) HOME_X, (double) HOME_Z, (double) PREGEN_RADIUS, (double) PREGEN_RADIUS, "region");
				if (!started) {
					callChunky(api, "continueTask", "minecraft:overworld");
				}
			}
			if (stepTicks > 60 && stepTicks % 100 == 0) {
				boolean running = (Boolean) callChunky(api, "isRunning", "minecraft:overworld");
				EMUtilsClient.LOGGER.info("EMUtils showcase: generating, {} s, Chunky {}", stepTicks / 20, running ? "running" : "done");
				if (!running) {
					Files.writeString(save.resolve(PREGEN_DONE), "Generated by Chunky, radius " + PREGEN_RADIUS + " around " + HOME_X + ", " + HOME_Z + "\n");
					return CompatScenes.Outcome.NEXT;
				}
			}
		} catch (ReflectiveOperationException | IOException exception) {
			EMUtilsClient.LOGGER.error("EMUtils showcase: Chunky couldn't generate the land", exception);
			return CompatScenes.Outcome.NEXT;
		}
		return CompatScenes.Outcome.WAIT;
	}

	/** Goes to the view, gears up, and sets out the waypoints and profiles the scenes show. */
	private static CompatScenes.Outcome seed(Minecraft client, int stepTicks) {
		if (stepTicks == 1) {
			home(client);
			EMUtilsConfig config = EMUtilsClient.config();
			config.setWorldMap(true);
			config.setMinimap(true);
			command(client, "clear @s");
			// Earlier runs of the beacon scene left its base on the bank.
			command(client, "fill " + (HOME_X - 30) + " 50 " + (HOME_Z - 10) + " " + (HOME_X - 18) + " 90 " + (HOME_Z + 2) + " minecraft:air replace minecraft:beacon");
			command(client, "fill " + (HOME_X - 30) + " 50 " + (HOME_Z - 10) + " " + (HOME_X - 18) + " 90 " + (HOME_Z + 2) + " minecraft:grass_block replace minecraft:iron_block");
			command(client, "fill " + (HOME_X - 30) + " 50 " + (HOME_Z - 10) + " " + (HOME_X - 18) + " 90 " + (HOME_Z + 2) + " minecraft:air replace minecraft:torch");
			command(client, "item replace entity @s armor.head with minecraft:diamond_helmet[minecraft:damage=40,minecraft:enchantments={\"minecraft:protection\":4}]");
			command(client, "item replace entity @s armor.chest with minecraft:diamond_chestplate[minecraft:enchantments={\"minecraft:protection\":4}]");
			command(client, "item replace entity @s armor.legs with minecraft:diamond_leggings[minecraft:damage=180,minecraft:enchantments={\"minecraft:protection\":4}]");
			command(client, "item replace entity @s armor.feet with minecraft:diamond_boots[minecraft:enchantments={\"minecraft:feather_falling\":4}]");
			command(client, "item replace entity @s hotbar.0 with minecraft:diamond_sword[minecraft:enchantments={\"minecraft:sharpness\":5}]");
			command(client, "item replace entity @s hotbar.1 with minecraft:diamond_pickaxe[minecraft:damage=900,minecraft:enchantments={\"minecraft:efficiency\":5}]");
			command(client, "item replace entity @s hotbar.2 with minecraft:bow");
			command(client, "item replace entity @s hotbar.3 with minecraft:cooked_beef 32");
			command(client, "item replace entity @s hotbar.4 with minecraft:oak_planks 64");
			command(client, "item replace entity @s hotbar.5 with minecraft:torch 48");
			command(client, "item replace entity @s hotbar.6 with minecraft:water_bucket");
			command(client, "item replace entity @s hotbar.7 with minecraft:ender_pearl 16");
			command(client, "item replace entity @s hotbar.8 with minecraft:golden_apple 6");
			command(client, "item replace entity @s inventory.0 with minecraft:purple_shulker_box[minecraft:custom_name=\"Mining Kit\",minecraft:container=["
				+ "{slot:0,item:{id:\"minecraft:diamond_pickaxe\",count:1}},{slot:1,item:{id:\"minecraft:diamond_shovel\",count:1}},"
				+ "{slot:2,item:{id:\"minecraft:torch\",count:64}},{slot:3,item:{id:\"minecraft:torch\",count:64}},"
				+ "{slot:4,item:{id:\"minecraft:cooked_beef\",count:48}},{slot:5,item:{id:\"minecraft:water_bucket\",count:1}},"
				+ "{slot:6,item:{id:\"minecraft:diamond\",count:23}},{slot:7,item:{id:\"minecraft:raw_iron\",count:41}},"
				+ "{slot:8,item:{id:\"minecraft:redstone\",count:64}},{slot:9,item:{id:\"minecraft:lapis_lazuli\",count:37}},"
				+ "{slot:10,item:{id:\"minecraft:emerald\",count:12}},{slot:11,item:{id:\"minecraft:ender_pearl\",count:16}},"
				+ "{slot:12,item:{id:\"minecraft:golden_carrot\",count:32}},{slot:13,item:{id:\"minecraft:cobblestone\",count:64}},"
				+ "{slot:14,item:{id:\"minecraft:obsidian\",count:10}},{slot:15,item:{id:\"minecraft:flint_and_steel\",count:1}}]]");
			command(client, "item replace entity @s inventory.1 with minecraft:oak_log 64");
			command(client, "item replace entity @s inventory.2 with minecraft:iron_ingot 27");
			command(client, "item replace entity @s inventory.3 with minecraft:diamond 9");
			command(client, "item replace entity @s inventory.10 with minecraft:bread 20");
			command(client, "item replace entity @s inventory.12 with minecraft:arrow 64");
			command(client, "item replace entity @s inventory.20 with minecraft:cobblestone 64");
		}
		if (stepTicks == 120) {
			// Waypoints only take new ones while they are on; the scenes turn them off in between.
			EMUtilsClient.config().setWaypointEnabled(true);
			EMUtilsClient.waypoint().clearForCurrentWorld(client);
			waypoint(client, "Home", HOME_X - 34, HOME_Z + 26, 0xFF55FF55, true, "");
			waypoint(client, "Cave", HOME_X - 60, HOME_Z - 18, 0xFFFFAA55, false, "Mining");
			waypoint(client, "Diamonds", HOME_X - 140, HOME_Z - 70, 0xFF55FFFF, true, "Mining");
			waypoint(client, "Iron farm", HOME_X - 210, HOME_Z + 90, 0xFFDDDDDD, false, "Mining");
			waypoint(client, "Village", HOME_X + 150, HOME_Z - 160, 0xFFFF55FF, true, "");
			waypoint(client, "Portal", HOME_X - 20, HOME_Z + 240, 0xFFAA55FF, false, "");

			ProfileManager profiles = EMUtilsClient.profiles();
			if (profiles.byName("Hypixel") == null) {
				profiles.create("Hypixel", ProfileIcon.SWORDS, ProfileColor.ORANGE, List.of("hypixel.net"), false, true);
			}
			if (profiles.byName("Building") == null) {
				profiles.create("Building", ProfileIcon.HAMMER, ProfileColor.BLUE, List.of("play.example.net"), false, true);
			}
			if (profiles.byName("Singleplayer") == null) {
				profiles.create("Singleplayer", ProfileIcon.HOUSE, ProfileColor.GREEN, List.of(), true, true);
			}
			writeScripts();
		}
		return stepTicks >= 140 ? CompatScenes.Outcome.NEXT : CompatScenes.Outcome.WAIT;
	}

	private static CompatScenes.Outcome scene(Minecraft client, Scene scene, int stepTicks) {
		if (stepTicks == 1) {
			baseline(client);
		}
		if (stepTicks < scene.shootAt()) {
			scene.pose().tick(client, stepTicks);
		}
		if (stepTicks == scene.shootAt() - 1) {
			MinecraftClientCompat.toastManager(client).clear();
		}
		if (stepTicks == scene.shootAt()) {
			save(client, scene.name());
		}
		return stepTicks >= scene.shootAt() + 5 ? CompatScenes.Outcome.NEXT : CompatScenes.Outcome.WAIT;
	}

	/** Every scene starts from the same place: at the view, at morning, with only the scene's features drawn. */
	private static void baseline(Minecraft client) {
		HudLayoutManager.cancelEditor(EMUtilsClient.config());
		client.gui.setScreen(null);
		setGuiScale(client, 3);
		releaseKeys(client);
		client.options.chatVisibility().set(ChatVisiblity.HIDDEN);
		MinecraftClientCompat.chat(client).clearMessages(false);
		EMUtilsConfig config = EMUtilsClient.config();
		config.setSettingsUiDark(true);
		config.setHudOverlay(false);
		config.setMinimap(false);
		config.setKeystrokes(false);
		config.setArmorStatus(false);
		config.setLookAtInfo(false);
		config.setBeaconRadiusOutline(false);
		config.setLightLevelOverlay(false);
		config.setWaypointEnabled(false);
		config.setTweakClearUnderwater(false);
		command(client, "gamemode creative");
		command(client, "time set 1000");
		// Into the void, so no puff of smoke shows where they were.
		command(client, "tp @e[type=!minecraft:player] ~ -300 ~");
		// The pointer out of the way, so nothing shows a hover.
		((MouseAccess) client.mouseHandler).emutils$setX(0.0D);
		((MouseAccess) client.mouseHandler).emutils$setY(0.0D);
		// The beacon scene leaves the land as it found it.
		for (String block : beaconRestore) {
			command(client, "setblock " + block);
		}
		beaconRestore.clear();
		if (chest != null) {
			command(client, "setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " minecraft:air");
			chest = null;
		}
		home(client);
	}

	private static void settingsHub(Minecraft client, int t) {
		if (t == 1) {
			client.gui.setScreen(new SettingsScreen(null));
		}
	}

	private static void settingsLight(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsClient.config().setSettingsUiDark(false);
			client.gui.setScreen(new SettingsScreen(null));
		}
	}

	/** The info overlay and the minimap, with the waypoints on it. */
	private static void hudOverlay(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsConfig config = EMUtilsClient.config();
			config.setHudOverlay(true);
			config.setMinimap(true);
			config.setWaypointEnabled(true);
		}
	}

	/** Look-At Info on a parrot over the river. */
	private static void lookAtInfo(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsClient.config().setLookAtInfo(true);
			command(client, "summon minecraft:parrot " + (HOME_X - 3) + ".0 " + (HOME_Y + 1.1D) + " " + HOME_Z + ".5 {NoAI:1b,Variant:0,Rotation:[120f,0f],CustomName:\"Polly\"}");
		}
		if (t == 20) {
			face(client, HOME_X - 3.0D, HOME_Y + 1.5D, HOME_Z + 0.5D);
		}
	}

	/** Keystrokes with forward held, and the armor's durability. */
	private static void keystrokesAndArmor(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsConfig config = EMUtilsClient.config();
			config.setKeystrokes(true);
			config.setArmorStatus(true);
		}
		if (t == 37) {
			// Pressed just before the shot, so the player doesn't fly off.
			client.options.keyUp.setDown(true);
		}
	}

	private static void worldMap(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsClient.config().setWaypointEnabled(true);
		}
		if (t == 5) {
			WorldMapScreen.open(client, null);
		}
	}

	/** The waypoints in the world at sunset, with their beams. */
	private static void waypointsInWorld(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsClient.config().setWaypointEnabled(true);
			command(client, "time set 12100");
			client.player.setYRot(75.0F);
			client.player.setXRot(-4.0F);
		}
	}

	private static void hudEditor(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsConfig config = EMUtilsClient.config();
			config.setHudOverlay(true);
			config.setMinimap(true);
			config.setKeystrokes(true);
			config.setArmorStatus(true);
			config.setLookAtInfo(true);
			config.setScoreboard(true);
			config.setTabList(true);
		}
		if (t == 5 && HudLayoutManager.beginEditorSession(EMUtilsClient.MOD_ID, client)) {
			client.gui.setScreen(new HudEditorScreen(null));
		}
	}

	/** Zoomed in on the cave in the mountain. */
	private static void zoom(Minecraft client, int t) {
		if (t == 1) {
			// From higher up, so the bank doesn't block the cave.
			command(client, "tp @s " + HOME_X + ".5 " + (HOME_Y + 8) + " " + HOME_Z + ".5 68 -8");
		}
		if (t == 20) {
			KeyMapping.get("key.emutils.zoom").setDown(true);
		}
	}

	/** At night: a beacon's range outlined, and the light level on the ground around a torch. */
	private static void beaconAndLightLevel(Minecraft client, int t) {
		if (t == 1) {
			int bx = HOME_X - 24;
			int bz = HOME_Z - 4;
			int by = ground(client, bx, bz);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					remember(client, new BlockPos(bx + dx, by - 1, bz + dz));
				}
			}
			remember(client, new BlockPos(bx, by, bz));
			int tx = bx + 7;
			remember(client, new BlockPos(tx, ground(client, tx, bz + 3), bz + 3));
			command(client, "fill " + (bx - 1) + " " + (by - 1) + " " + (bz - 1) + " " + (bx + 1) + " " + (by - 1) + " " + (bz + 1) + " minecraft:iron_block");
			command(client, "fill " + bx + " " + (by + 1) + " " + bz + " " + bx + " " + Math.min(by + 60, 300) + " " + bz + " minecraft:air");
			command(client, "setblock " + bx + " " + by + " " + bz + " minecraft:beacon{primary_effect:\"minecraft:haste\"}");
			command(client, "setblock " + tx + " " + ground(client, tx, bz + 3) + " " + (bz + 3) + " minecraft:torch");
			command(client, "time set 13600");
			EMUtilsConfig config = EMUtilsClient.config();
			config.resetBeaconRadiusDefaults();
			config.setBeaconRadiusOutline(true);
			config.resetLightLevelDefaults();
			config.setLightLevelOverlay(true);
			command(client, "tp @s " + (bx + 30) + ".5 " + (by + 12) + " " + bz + ".5 90 22");
		}
	}

	/** A chest searched for "diamond", with the sort buttons beside it. */
	private static void inventorySearch(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsConfig config = EMUtilsClient.config();
			config.setInventoryToolsEnabled(true);
			config.setInventorySearch(true);
			config.setInventorySearchDim(true);
			config.setInventorySearchShulkers(true);
			chest = new BlockPos(HOME_X - 2, HOME_Y, HOME_Z);
			command(client, "setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " minecraft:chest{Items:["
				+ "{Slot:0b,id:\"minecraft:diamond\",count:14},{Slot:1b,id:\"minecraft:stone\",count:64},{Slot:2b,id:\"minecraft:oak_log\",count:48},"
				+ "{Slot:3b,id:\"minecraft:iron_ingot\",count:31},{Slot:4b,id:\"minecraft:diamond_pickaxe\",count:1},{Slot:5b,id:\"minecraft:coal\",count:52},"
				+ "{Slot:6b,id:\"minecraft:wheat\",count:20},{Slot:9b,id:\"minecraft:diamond_block\",count:3},{Slot:10b,id:\"minecraft:gold_ingot\",count:17},"
				+ "{Slot:11b,id:\"minecraft:redstone\",count:64},{Slot:12b,id:\"minecraft:cobblestone\",count:64},{Slot:14b,id:\"minecraft:diamond_sword\",count:1},"
				+ "{Slot:16b,id:\"minecraft:bone\",count:9},{Slot:18b,id:\"minecraft:string\",count:22},{Slot:19b,id:\"minecraft:diamond_ore\",count:4},"
				+ "{Slot:21b,id:\"minecraft:emerald\",count:6},{Slot:22b,id:\"minecraft:blue_shulker_box\",count:1,components:{\"minecraft:container\":[{slot:0,item:{id:\"minecraft:diamond\",count:30}}]}},"
				+ "{Slot:24b,id:\"minecraft:glass\",count:40},{Slot:26b,id:\"minecraft:arrow\",count:64}]}");
		}
		if (t == 20) {
			client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(chest), Direction.UP, chest, false));
		}
		if (t == 35 && MinecraftClientCompat.screen(client) instanceof ContainerScreen screen) {
			screen.keyPressed(new KeyEvent(InputConstants.KEY_F, 0, InputConstants.MOD_CONTROL));
			"diamond".codePoints().forEach(codepoint -> screen.charTyped(new CharacterEvent(codepoint)));
		}
	}

	/** The inventory, with the pointer over a shulker box full of a mining kit. */
	private static void shulkerPreview(Minecraft client, int t) {
		if (t == 1) {
			InventorySearch.setQueryForSnapshot("");
			command(client, "gamemode survival");
		}
		if (t == 10) {
			client.gui.setScreen(new InventoryScreen(client.player));
		}
		if (t >= 12 && MinecraftClientCompat.screen(client) instanceof InventoryScreen screen) {
			HandledScreenAccessor access = (HandledScreenAccessor) screen;
			double guiScale = client.getWindow().getGuiScale();
			// The first slot of the main inventory, where the shulker box is.
			MouseAccess mouse = (MouseAccess) client.mouseHandler;
			mouse.emutils$setX((access.emutils$getLeftPos() + 8 + 9) * guiScale);
			mouse.emutils$setY((access.emutils$getTopPos() + 84 + 9) * guiScale);
		}
	}

	/** Underwater in the river, seeing all the way across with Clear Underwater. */
	private static void clearUnderwater(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsClient.config().setTweakClearUnderwater(true);
			BlockPos deep = deepWater(client);
			if (deep != null) {
				command(client, "tp @s " + deep.getX() + ".5 " + deep.getY() + " " + deep.getZ() + ".5 90 2");
			}
		}
	}

	private static void packManager(Minecraft client, int t) {
		if (t == 1) {
			PacksScreen screen = new PacksScreen(null);
			client.gui.setScreen(screen);
			screen.searchModrinthForSnapshot("");
		}
	}

	private static void scriptManager(Minecraft client, int t) {
		if (t == 1) {
			client.gui.setScreen(new ScriptsScreen(null));
		}
		if (t == 15 && MinecraftClientCompat.screen(client) instanceof ScriptsScreen screen) {
			screen.openForSnapshot(SCRIPT_FOLDER + "/tree_farm.py");
		}
	}

	private static void profiles(Minecraft client, int t) {
		if (t == 1) {
			client.gui.setScreen(new ProfilesScreen(new SettingsScreen(null)));
		}
	}

	/** The gallery, with a few of the world scenes copied into the screenshots folder. */
	private static void gallery(Minecraft client, int t) {
		if (t == 1) {
			copyIntoGallery(client);
			client.gui.setScreen(new GalleryScreen(null));
		}
		// Held off the tiles, so none shows its hover buttons.
		((MouseAccess) client.mouseHandler).emutils$setX(0.0D);
		((MouseAccess) client.mouseHandler).emutils$setY(0.0D);
	}

	private static void screenshotHelper(Minecraft client, int t) {
		if (t == 1) {
			client.options.chatVisibility().set(ChatVisiblity.FULL);
			client.player.setYRot(84.0F);
			client.player.setXRot(-2.0F);
			Path shot = ScreenshotPaths.screenshotsDir(client).resolve("2026-11-18_17.42.06.png");
			MinecraftClientCompat.chat(client).addClientSystemMessage(ScreenshotMessage.saved(shot.toFile()));
		}
	}

	private static void autoReconnect(Minecraft client, int t) {
		if (t == 1) {
			EMUtilsConfig config = EMUtilsClient.config();
			config.setAutoReconnect(true);
			config.setReconnectDelaySeconds(15);
			EMUtilsClient.autoReconnect().captureServer(new ServerData("Survival", "play.example.net", ServerData.Type.OTHER));
			client.gui.setScreen(new DisconnectedScreen(new TitleScreen(), Component.literal("Connection Lost"), Component.literal("Server closed")));
		}
		if (t == 29) {
			// Leaves before the countdown runs out, so it never tries to connect.
			EMUtilsClient.config().resetAutoReconnectDefaults();
		}
	}

	private static void sheet(Minecraft client, int t, String featureId) {
		if (t == 1) {
			SettingsScreen settings = new SettingsScreen(null);
			client.gui.setScreen(settings);
			settings.openSheet(featureId);
		}
	}

	private static void open(Minecraft client, int t, net.minecraft.client.gui.screens.Screen screen) {
		if (t == 1) {
			client.gui.setScreen(screen);
		}
	}

	/** Frames every shot into the framed folder, once the last one is written. */
	private static CompatScenes.Outcome frameAll(Minecraft client, int stepTicks) {
		if (stepTicks == 1) {
			baseline(client);
		}
		if (stepTicks < 40) {
			return CompatScenes.Outcome.WAIT;
		}
		Path folder = folder(client);
		Path framed = folder.resolve("framed");
		try {
			Files.createDirectories(framed);
			List<Path> shots;
			try (Stream<Path> files = Files.list(folder)) {
				shots = files.filter(file -> file.getFileName().toString().endsWith(".png")).sorted().toList();
			}
			for (Path shot : shots) {
				ShowcaseFrame.frame(shot, framed.resolve(shot.getFileName()));
			}
			EMUtilsClient.LOGGER.info("EMUtils showcase: framed {} screenshots into {}", shots.size(), framed);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.error("EMUtils showcase: could not frame the screenshots", exception);
		}
		return CompatScenes.Outcome.FINISH;
	}

	private static void home(Minecraft client) {
		client.player.getAbilities().flying = true;
		command(client, "tp @s " + HOME_X + ".5 " + HOME_Y + " " + HOME_Z + ".5 90 10");
	}

	private static void releaseKeys(Minecraft client) {
		client.options.keyUp.setDown(false);
		KeyMapping.get("key.emutils.zoom").setDown(false);
	}

	private static int ground(Minecraft client, int x, int z) {
		return client.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
	}

	private static void face(Minecraft client, double x, double y, double z) {
		double dx = x - client.player.getX();
		double dy = y - client.player.getEyeY();
		double dz = z - client.player.getZ();
		client.player.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
		client.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
	}

	private static void remember(Minecraft client, BlockPos pos) {
		beaconRestore.add(pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + BlockStateParser.serialize(client.level.getBlockState(pos)));
	}

	/** A spot three blocks under the river's surface, near the view, where the water is deepest. */
	private static @Nullable BlockPos deepWater(Minecraft client) {
		BlockPos best = null;
		int bestDepth = 0;
		for (int x = HOME_X - 10; x <= HOME_X + 40; x += 2) {
			for (int z = HOME_Z - 40; z <= HOME_Z + 40; z += 2) {
				int top = client.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
				int depth = 0;
				while (depth < 20 && client.level.getFluidState(new BlockPos(x, top - depth, z)).is(FluidTags.WATER)) {
					depth++;
				}
				if (depth > bestDepth) {
					bestDepth = depth;
					best = new BlockPos(x, top - Math.min(3, depth - 1), z);
				}
			}
		}
		return best;
	}

	private static void waypoint(Minecraft client, String label, int x, int z, int color, boolean beam, String set) {
		EMUtilsClient.waypoint().addCustom(client, label, x, ground(client, x, z), z, color, beam, set);
		try {
			// The list is ordered by when each was added, in milliseconds.
			Thread.sleep(3);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}

	private static void writeScripts() {
		Path folder = MinescriptCompat.scriptsDir().resolve(SCRIPT_FOLDER);
		try {
			Files.createDirectories(folder.resolve("tools"));
			Files.writeString(folder.resolve("tree_farm.py"), """
				import minescript
				import time

				# Plants a row of saplings in front of you and bone-meals them.
				SAPLING = "minecraft:oak_sapling"
				ROW = 8

				def plant_row(x, y, z):
				    for i in range(ROW):
				        minescript.execute(f"setblock {x + i * 3} {y} {z} {SAPLING}")
				        time.sleep(0.05)

				x, y, z = [int(v) for v in minescript.player_position()]
				plant_row(x + 2, y, z + 2)
				minescript.echo(f"Planted {ROW} saplings at {x}, {y}, {z}")
				""");
			Files.writeString(folder.resolve("tools/coords.py"), "import minescript\n\nx, y, z = minescript.player_position()\nminescript.echo(f\"{x:.0f} {y:.0f} {z:.0f}\")\n");
			Files.writeString(folder.resolve("tools/night_vision.py"), "import minescript\n\nminescript.execute(\"effect give @s night_vision infinite 0 true\")\n");
			Files.writeString(folder.resolve("auto_fish.py"), "import minescript\nimport time\n\nwhile True:\n    minescript.player_press_use(True)\n    time.sleep(0.1)\n    minescript.player_press_use(False)\n    time.sleep(20)\n");
			Files.writeString(folder.resolve("build_house.py"), "import minescript\n\n# Builds a small hut where you stand.\n");
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not write the showcase scripts.", exception);
		}
	}

	/** Copies the world scenes into the screenshots folder, a minute apart, so the gallery has something to show. */
	private static void copyIntoGallery(Minecraft client) {
		Path gallery = ScreenshotPaths.screenshotsDir(client);
		String[] names = {"02a-hud-overlay", "02b-look-at-info", "04-waypoints", "09-zoom", "10-beacon-light-level", "13-clear-underwater", "03-world-map", "11-inventory-search", "12-shulker-preview"};
		long now = System.currentTimeMillis();
		try {
			Files.createDirectories(gallery);
			for (int i = 0; i < names.length; i++) {
				Path from = folder(client).resolve(names[i] + ".png");
				if (Files.exists(from)) {
					Path to = gallery.resolve("2026-11-18_17." + (42 - i) + ".06.png");
					Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
					Files.setLastModifiedTime(to, FileTime.fromMillis(now - i * 60_000L));
				}
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not fill the showcase gallery.", exception);
		}
	}

	/** Clears the last run's shots: the scenes, their framed copies, and the gallery's copies. */
	private static void clearOldShots(Minecraft client) {
		for (Path dir : List.of(ScreenshotPaths.screenshotsDir(client), folder(client), folder(client).resolve("framed"))) {
			if (!Files.isDirectory(dir)) {
				continue;
			}
			try (Stream<Path> files = Files.list(dir)) {
				for (Path file : files.filter(file -> file.getFileName().toString().endsWith(".png")).toList()) {
					Files.deleteIfExists(file);
				}
			} catch (IOException exception) {
				EMUtilsClient.LOGGER.warn("Could not clear the old showcase screenshots in {}", dir, exception);
			}
		}
	}

	private static void setGuiScale(Minecraft client, int guiScale) {
		if (client.options.guiScale().get() != guiScale) {
			client.options.guiScale().set(guiScale);
			client.resizeGui();
		}
	}

	private static @Nullable Object chunkyApi() {
		try {
			Object chunky = Class.forName("org.popcraft.chunky.ChunkyProvider").getMethod("get").invoke(null);
			return chunky == null ? null : chunky.getClass().getMethod("getApi").invoke(chunky);
		} catch (ReflectiveOperationException | LinkageError exception) {
			return null;
		}
	}

	private static Object callChunky(Object api, String method, Object... arguments) throws ReflectiveOperationException {
		for (java.lang.reflect.Method candidate : api.getClass().getMethods()) {
			if (candidate.getName().equals(method) && candidate.getParameterCount() == arguments.length) {
				return candidate.invoke(api, arguments);
			}
		}
		throw new NoSuchMethodException(method);
	}

	private static void save(Minecraft client, String name) {
		Path file = folder(client).resolve(name + ".png");
		Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
			try (image) {
				image.writeToFile(file);
			} catch (IOException exception) {
				EMUtilsClient.LOGGER.warn("Could not save the showcase screenshot {}", file, exception);
			}
		});
	}

	private static Path folder(Minecraft client) {
		Path folder = client.gameDirectory.toPath().resolve("screenshots").resolve("showcase");
		try {
			Files.createDirectories(folder);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not make {}", folder, exception);
		}
		return folder;
	}

	private static void command(Minecraft client, String command) {
		if (client.getConnection() != null) {
			client.getConnection().sendCommand(command);
		}
	}
}
