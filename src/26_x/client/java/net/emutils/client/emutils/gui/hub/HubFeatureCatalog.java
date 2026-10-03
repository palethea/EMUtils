package net.emutils.client.emutils.gui.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.commandshortcuts.gui.CommandShortcutsScreen;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.settings.KeybindsScreen;
import net.emutils.client.emutils.minescript.gui.ScriptsScreen;
import net.emutils.client.emutils.packs.gui.PacksScreen;
import net.emutils.client.emutils.profile.gui.ProfilesScreen;
import net.emutils.client.emutils.screenshot.gui.GalleryScreen;
import net.emutils.client.emutils.tweaks.AntiDurabilityUnit;
import net.emutils.client.emutils.tweaks.FreeCameraHudMode;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class HubFeatureCatalog {
	private HubFeatureCatalog() {
	}

	public static List<HubFeature> all() {
		EMUtilsConfig config = EMUtilsClient.config();
		List<HubFeature> features = new ArrayList<>(List.of(
			categoryFeature("fullbright", HubCategory.FULLBRIGHT, HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT, EMUtilsTexts.HUB_FEATURE_FULLBRIGHT_DESC, HubFeature.Icon.SUN, toggle(config::tweakFullbright, config::setTweakFullbright)),
			categoryFeature("clear_weather", HubCategory.CLEAR_WEATHER, HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER, EMUtilsTexts.HUB_FEATURE_CLEAR_WEATHER_DESC, HubFeature.Icon.CLOUD_SUN, toggle(config::tweakClearWeather, config::setTweakClearWeather)),
			leaf("no_fog", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_FOG, EMUtilsTexts.HUB_FEATURE_NO_FOG_DESC, HubFeature.Icon.CLOUD_OFF, toggle(config::tweakNoFog, config::setTweakNoFog), () -> config.setTweakNoFog(false)),
			leaf("clear_underwater", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_CLEAR_UNDERWATER, EMUtilsTexts.HUB_FEATURE_CLEAR_UNDERWATER_DESC, HubFeature.Icon.DROPLETS, toggle(config::tweakClearUnderwater, config::setTweakClearUnderwater), () -> config.setTweakClearUnderwater(true)),
			leaf("clear_lava", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_CLEAR_LAVA, EMUtilsTexts.HUB_FEATURE_CLEAR_LAVA_DESC, HubFeature.Icon.FLAME, toggle(config::tweakClearLava, config::setTweakClearLava), () -> config.setTweakClearLava(true)),
			leaf("no_fire_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_FIRE_OVERLAY, EMUtilsTexts.HUB_FEATURE_NO_FIRE_OVERLAY_DESC, HubFeature.Icon.FLAME, toggle(config::tweakNoFireOverlay, config::setTweakNoFireOverlay), () -> config.setTweakNoFireOverlay(false)),
			leaf("low_fire_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_LOW_FIRE_OVERLAY, EMUtilsTexts.HUB_FEATURE_LOW_FIRE_OVERLAY_DESC, HubFeature.Icon.FLAME, toggle(config::tweakLowFireOverlay, config::setTweakLowFireOverlay), () -> config.setTweakLowFireOverlay(false)),
			leaf("low_shield", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_LOW_SHIELD, EMUtilsTexts.HUB_FEATURE_LOW_SHIELD_DESC, HubFeature.Icon.SHIELD, toggle(config::tweakLowShield, config::setTweakLowShield), List.of(
				new HubSettingRow.Slider(EMUtilsTexts.OPTION_TWEAK_LOW_SHIELD_AMOUNT, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.LOW_SHIELD_AMOUNT_MIN, EMUtilsConfig.LOW_SHIELD_AMOUNT_MAX, config::lowShieldAmount, config::setLowShieldAmount)
			), config::resetLowShieldDefaults),
			leaf("low_totem", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_LOW_TOTEM, EMUtilsTexts.HUB_FEATURE_LOW_TOTEM_DESC, HubFeature.Icon.SPARKLES, toggle(config::tweakLowTotem, config::setTweakLowTotem), List.of(
				new HubSettingRow.Slider(EMUtilsTexts.OPTION_TWEAK_LOW_TOTEM_AMOUNT, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.LOW_TOTEM_AMOUNT_MIN, EMUtilsConfig.LOW_TOTEM_AMOUNT_MAX, config::lowTotemAmount, config::setLowTotemAmount)
			), config::resetLowTotemDefaults),
			leaf("small_totem", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_SMALL_TOTEM, EMUtilsTexts.HUB_FEATURE_SMALL_TOTEM_DESC, HubFeature.Icon.SPARKLES, toggle(config::tweakSmallTotem, config::setTweakSmallTotem), List.of(
				new HubSettingRow.Slider(EMUtilsTexts.OPTION_TWEAK_SMALL_TOTEM_SIZE, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.SMALL_TOTEM_SIZE_MIN, EMUtilsConfig.SMALL_TOTEM_SIZE_MAX, config::smallTotemSize, config::setSmallTotemSize)
			), config::resetSmallTotemDefaults),
			leaf("no_nausea", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_NAUSEA, EMUtilsTexts.HUB_FEATURE_NO_NAUSEA_DESC, HubFeature.Icon.EYE, toggle(config::tweakNoNausea, config::setTweakNoNausea), () -> config.setTweakNoNausea(false)),
			leaf("hide_effects", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_HIDE_EFFECTS, EMUtilsTexts.HUB_FEATURE_HIDE_EFFECTS_DESC, HubFeature.Icon.EYE, toggle(config::tweakHideEffects, config::setTweakHideEffects), List.of(
				new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_HIDE_EFFECTS_HUD, config::tweakHideEffectsHud, config::setTweakHideEffectsHud),
				new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_HIDE_EFFECTS_INVENTORY, config::tweakHideEffectsInventory, config::setTweakHideEffectsInventory)
			), config::resetHideEffectsDefaults),
			leaf("no_spyglass_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_SPYGLASS_OVERLAY, EMUtilsTexts.HUB_FEATURE_NO_SPYGLASS_OVERLAY_DESC, HubFeature.Icon.ZOOM, toggle(config::tweakNoSpyglassOverlay, config::setTweakNoSpyglassOverlay), () -> config.setTweakNoSpyglassOverlay(false)),
			leaf("no_pumpkin_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_PUMPKIN_OVERLAY, EMUtilsTexts.HUB_FEATURE_NO_PUMPKIN_OVERLAY_DESC, HubFeature.Icon.EYE, toggle(config::tweakNoPumpkinOverlay, config::setTweakNoPumpkinOverlay), () -> config.setTweakNoPumpkinOverlay(false)),
			leaf("no_environment_fog", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_ENVIRONMENT_FOG, EMUtilsTexts.HUB_FEATURE_NO_ENVIRONMENT_FOG_DESC, HubFeature.Icon.CLOUD_OFF, toggle(config::tweakNoEnvironmentFog, config::setTweakNoEnvironmentFog), () -> config.setTweakNoEnvironmentFog(true)),
			leaf("no_nether_particles", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_NETHER_PARTICLES, EMUtilsTexts.HUB_FEATURE_NO_NETHER_PARTICLES_DESC, HubFeature.Icon.SPARKLES, toggle(config::tweakNoNetherParticles, config::setTweakNoNetherParticles), () -> config.setTweakNoNetherParticles(false)),
			leaf("no_falling_leaf_particles", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_FALLING_LEAF_PARTICLES, EMUtilsTexts.HUB_FEATURE_NO_FALLING_LEAF_PARTICLES_DESC, HubFeature.Icon.SPARKLES, toggle(config::tweakNoFallingLeafParticles, config::setTweakNoFallingLeafParticles), () -> config.setTweakNoFallingLeafParticles(false)),
			leaf("no_hurt_cam", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_HURT_CAM, EMUtilsTexts.HUB_FEATURE_NO_HURT_CAM_DESC, HubFeature.Icon.SHIELD, toggle(config::tweakNoHurtCam, config::setTweakNoHurtCam), () -> config.setTweakNoHurtCam(false)),
			leaf("freelook", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_FREELOOK, EMUtilsTexts.HUB_FEATURE_FREELOOK_DESC, HubFeature.Icon.EYE, toggle(config::tweakFreelook, config::setTweakFreelook), List.of(
				new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FREELOOK_KEEP_PERSPECTIVE, config::freelookKeepPerspective, config::setFreelookKeepPerspective)
			), config::resetFreelookDefaults).keys("key.emutils.freelook"),
			leaf("beacon_radius_outline", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_BEACON_RADIUS_OUTLINE, EMUtilsTexts.HUB_FEATURE_BEACON_RADIUS_DESC, HubFeature.Icon.SPARKLES, toggle(config::beaconRadiusOutline, config::setBeaconRadiusOutline), withXaeroRow(config, List.of(
				new HubSettingRow.Slider(EMUtilsTexts.OPTION_BEACON_RADIUS_RANGE, EMUtilsTexts.SUFFIX_CHUNKS, EMUtilsConfig.BEACON_RADIUS_RANGE_MIN, EMUtilsConfig.BEACON_RADIUS_RANGE_MAX, config::beaconRadiusRange, config::setBeaconRadiusRange),
				new HubSettingRow.Toggle(EMUtilsTexts.OPTION_BEACON_RADIUS_ACTIVE_ONLY, config::beaconRadiusActiveOnly, config::setBeaconRadiusActiveOnly),
				new HubSettingRow.Slider(EMUtilsTexts.OPTION_BEACON_RADIUS_GRID_SPACING, EMUtilsTexts.SUFFIX_BLOCKS, EMUtilsConfig.BEACON_RADIUS_GRID_SPACING_MIN, EMUtilsConfig.BEACON_RADIUS_GRID_SPACING_MAX, config::beaconRadiusGridSpacing, config::setBeaconRadiusGridSpacing),
				new HubSettingRow.Slider(EMUtilsTexts.OPTION_BEACON_RADIUS_LINE_WIDTH, EMUtilsTexts.SUFFIX_PIXELS, EMUtilsConfig.BEACON_RADIUS_LINE_WIDTH_MIN, EMUtilsConfig.BEACON_RADIUS_LINE_WIDTH_MAX, config::beaconRadiusLineWidth, config::setBeaconRadiusLineWidth)
			)), config::resetBeaconRadiusDefaults).keys("key.emutils.beacon_radius_outline"),
			leaf("light_level_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_LIGHT_LEVEL_OVERLAY, EMUtilsTexts.HUB_FEATURE_LIGHT_LEVEL_OVERLAY_DESC, HubFeature.Icon.SUN, toggle(config::lightLevelOverlay, config::setLightLevelOverlay), List.of(
				new HubSettingRow.Slider(EMUtilsTexts.OPTION_LIGHT_LEVEL_RANGE, EMUtilsTexts.SUFFIX_BLOCKS, EMUtilsConfig.LIGHT_LEVEL_RANGE_MIN, EMUtilsConfig.LIGHT_LEVEL_RANGE_MAX, config::lightLevelRange, config::setLightLevelRange),
				new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LIGHT_LEVEL_SPAWNABLE_ONLY, config::lightLevelSpawnableOnly, config::setLightLevelSpawnableOnly)
			), config::resetLightLevelDefaults).keys("key.emutils.light_level_overlay"),
			leaf("own_nametag", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_OWN_NAMETAG, EMUtilsTexts.HUB_FEATURE_OWN_NAMETAG_DESC, HubFeature.Icon.TAG, toggle(config::tweakOwnNametag, config::setTweakOwnNametag), () -> config.setTweakOwnNametag(false)),
			leaf("shulker_preview", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_SHULKER_TOOLTIP_PREVIEW, EMUtilsTexts.HUB_FEATURE_SHULKER_PREVIEW_DESC, HubFeature.Icon.BOX, toggle(config::tweakShulkerTooltipPreview, config::setTweakShulkerTooltipPreview), () -> config.setTweakShulkerTooltipPreview(true)),
			leaf("bundle_preview", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_BUNDLE_TOOLTIP_PREVIEW, EMUtilsTexts.HUB_FEATURE_BUNDLE_PREVIEW_DESC, HubFeature.Icon.PACKAGE_OPEN, toggle(config::tweakBundleTooltipPreview, config::setTweakBundleTooltipPreview), () -> config.setTweakBundleTooltipPreview(true)),
			categoryFeature("zoom", HubCategory.ZOOM, HubFeature.Group.RENDER, EMUtilsTexts.HUB_ZOOM, EMUtilsTexts.HUB_FEATURE_ZOOM_DESC, HubFeature.Icon.ZOOM, toggle(config::zoomEnabled, config::setZoomEnabled)).keys("key.emutils.zoom"),
			categoryFeature("capes", HubCategory.CAPES, HubFeature.Group.RENDER, EMUtilsTexts.HUB_CAPES, EMUtilsTexts.HUB_FEATURE_CAPES_DESC, HubFeature.Icon.CAPE, toggle(config::customCapes, config::setCustomCapes)),
			categoryFeature("hud_overlay", HubCategory.HUD_OVERLAY, HubFeature.Group.HUD, EMUtilsTexts.HUB_HUD_OVERLAY, EMUtilsTexts.HUB_FEATURE_HUD_DESC, HubFeature.Icon.HUD, toggle(config::hudOverlay, config::setHudOverlay)).keys("key.emutils.open_hud_layout_editor"),
			categoryFeature("food_hud", HubCategory.FOOD_HUD, HubFeature.Group.HUD, EMUtilsTexts.HUB_FOOD_HUD, EMUtilsTexts.HUB_FEATURE_FOOD_HUD_DESC, HubFeature.Icon.APPLE, toggle(config::foodHud, config::setFoodHud)),
			categoryFeature("look_at_info", HubCategory.LOOK_AT_INFO, HubFeature.Group.HUD, EMUtilsTexts.HUB_LOOK_AT_INFO, EMUtilsTexts.HUB_FEATURE_LOOK_AT_INFO_DESC, HubFeature.Icon.CROSSHAIR, toggle(config::lookAtInfo, config::setLookAtInfo)),
			categoryFeature("keystrokes", HubCategory.KEYSTROKES, HubFeature.Group.HUD, EMUtilsTexts.HUB_KEYSTROKES, EMUtilsTexts.HUB_FEATURE_KEYSTROKES_DESC, HubFeature.Icon.KEYBOARD, toggle(config::keystrokes, config::setKeystrokes)),
			categoryFeature("minimap", HubCategory.MINIMAP, HubFeature.Group.HUD, EMUtilsTexts.HUB_MINIMAP, EMUtilsTexts.HUB_FEATURE_MINIMAP_DESC, HubFeature.Icon.MAP, toggle(config::minimap, config::setMinimap)).keys("key.emutils.minimap_zoom_in", "key.emutils.minimap_zoom_out"),
			categoryFeature("armor_status", HubCategory.ARMOR_STATUS, HubFeature.Group.HUD, EMUtilsTexts.HUB_ARMOR_STATUS, EMUtilsTexts.HUB_FEATURE_ARMOR_STATUS_DESC, HubFeature.Icon.SHIELD, toggle(config::armorStatus, config::setArmorStatus)),
			categoryFeature("scoreboard", HubCategory.SCOREBOARD, HubFeature.Group.HUD, EMUtilsTexts.HUB_SCOREBOARD, EMUtilsTexts.HUB_FEATURE_SCOREBOARD_DESC, HubFeature.Icon.TROPHY, toggle(config::scoreboard, config::setScoreboard)),
			categoryFeature("tab_list", HubCategory.TAB_LIST, HubFeature.Group.HUD, EMUtilsTexts.HUB_TAB_LIST, EMUtilsTexts.HUB_FEATURE_TAB_LIST_DESC, HubFeature.Icon.USERS, toggle(config::tabList, config::setTabList)),
			categoryFeature("spotify", HubCategory.SPOTIFY, HubFeature.Group.HUD, EMUtilsTexts.HUB_SPOTIFY_PLAYER, EMUtilsTexts.HUB_FEATURE_SPOTIFY_DESC, HubFeature.Icon.MUSIC, toggle(config::spotifyEnabled, config::setSpotifyEnabled)),
			categoryFeature("auto_reconnect", HubCategory.AUTO_RECONNECT, HubFeature.Group.UTILITY, EMUtilsTexts.HUB_AUTO_RECONNECT, EMUtilsTexts.HUB_FEATURE_AUTO_RECONNECT_DESC, HubFeature.Icon.RECONNECT, toggle(config::autoReconnect, config::setAutoReconnect)),
			categoryFeature("screenshot_helper", HubCategory.SCREENSHOT, HubFeature.Group.UTILITY, EMUtilsTexts.HUB_SCREENSHOT_HELPER, EMUtilsTexts.HUB_FEATURE_SCREENSHOT_DESC, HubFeature.Icon.IMAGE, toggle(config::screenshotHelper, config::setScreenshotHelper)),
			leaf("world_map", HubFeature.Group.UTILITY, EMUtilsTexts.OPTION_WORLD_MAP, EMUtilsTexts.HUB_FEATURE_WORLD_MAP_DESC, HubFeature.Icon.MAP, toggle(config::worldMap, config::setWorldMap), config::resetWorldMapDefaults).keys("key.emutils.world_map"),
			categoryFeature("waypoints", HubCategory.DEATH_WAYPOINTS, HubFeature.Group.UTILITY, EMUtilsTexts.HUB_WAYPOINTS, EMUtilsTexts.HUB_FEATURE_WAYPOINTS_DESC, HubFeature.Icon.PIN, toggle(config::waypointEnabled, config::setWaypointEnabled)).keys("key.emutils.add_waypoint", "key.emutils.add_waypoint_at_crosshair", "key.emutils.copy_coordinates"),
			new HubFeature(
				"free_camera",
				null,
				HubFeature.Group.UTILITY,
				EMUtilsTexts.OPTION_TWEAK_FREE_CAMERA,
				EMUtilsTexts.HUB_FEATURE_FREE_CAMERA_DESC,
				HubFeature.Icon.EYE,
				toggle(config::tweakFreeCamera, config::setTweakFreeCamera),
				List.of(
					HubSettingRow.Cycle.ofEnum(
						EMUtilsTexts.OPTION_FREE_CAMERA_HUD_MODE,
						config::freeCameraHudMode,
						config::setFreeCameraHudMode,
						FreeCameraHudMode.class,
						mode -> Component.translatable(mode.labelKey())
					),
					new HubSettingRow.Slider(
						EMUtilsTexts.OPTION_FREE_CAMERA_BOOST_MULTIPLIER,
						EMUtilsTexts.SUFFIX_MULTIPLIER,
						EMUtilsConfig.FREE_CAMERA_BOOST_MULTIPLIER_MIN,
						EMUtilsConfig.FREE_CAMERA_BOOST_MULTIPLIER_MAX,
						config::freeCameraBoostMultiplier,
						config::setFreeCameraBoostMultiplier
					),
					new HubSettingRow.Slider(
						EMUtilsTexts.OPTION_FREE_CAMERA_HORIZONTAL_SPEED,
						EMUtilsTexts.SUFFIX_PERCENT,
						EMUtilsConfig.FREE_CAMERA_SPEED_MIN,
						EMUtilsConfig.FREE_CAMERA_SPEED_MAX,
						config::freeCameraHorizontalSpeed,
						config::setFreeCameraHorizontalSpeed
					),
					new HubSettingRow.Slider(
						EMUtilsTexts.OPTION_FREE_CAMERA_VERTICAL_SPEED,
						EMUtilsTexts.SUFFIX_PERCENT,
						EMUtilsConfig.FREE_CAMERA_SPEED_MIN,
						EMUtilsConfig.FREE_CAMERA_SPEED_MAX,
						config::freeCameraVerticalSpeed,
						config::setFreeCameraVerticalSpeed
					),
					new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FREE_CAMERA_COLLISION, config::freeCameraCollision, config::setFreeCameraCollision),
					new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FREE_CAMERA_DOUBLE_TAP, config::freeCameraDoubleTap, config::setFreeCameraDoubleTap),
					new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FREE_CAMERA_CUSTOM_FOV, config::freeCameraCustomFov, config::setFreeCameraCustomFov),
					new HubSettingRow.Slider(
						EMUtilsTexts.OPTION_FREE_CAMERA_FOV,
						EMUtilsTexts.SUFFIX_DEGREES,
						EMUtilsConfig.FREE_CAMERA_FOV_MIN,
						EMUtilsConfig.FREE_CAMERA_FOV_MAX,
						config::freeCameraFov,
						config::setFreeCameraFov
					)
				),
				null,
				true,
				() -> {
					config.setTweakFreeCamera(false);
					config.resetFreeCameraSettings();
				}
			).keys("key.emutils.free_camera"),
			actionFeature(
				"screenshot_gallery",
				HubCategory.SCREENSHOT_GALLERY,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.SCREEN_SCREENSHOT_GALLERY,
				EMUtilsTexts.HUB_FEATURE_SCREENSHOT_GALLERY_DESC,
				HubFeature.Icon.IMAGE,
				null,
				openScreenAction(GalleryScreen::new),
				true,
				categoryReset(HubCategory.SCREENSHOT_GALLERY)
			).keys("key.emutils.open_gallery"),
			actionFeature(
				"current_waypoints",
				null,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.SCREEN_CURRENT_WAYPOINTS,
				EMUtilsTexts.HUB_FEATURE_CURRENT_WAYPOINTS_DESC,
				HubFeature.Icon.PIN,
				null,
				openScreenAction(WaypointsScreen::new),
				true,
				null
			).keys("key.emutils.open_waypoints"),
			actionFeature(
				"pack_manager",
				HubCategory.PACK_MANAGER,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.OPTION_PACK_MANAGER,
				EMUtilsTexts.HUB_FEATURE_PACK_MANAGER_DESC,
				HubFeature.Icon.PACKAGE,
				toggle(config::packManagerEnabled, config::setPackManagerEnabled),
				openScreenAction(PacksScreen::new),
				true,
				config::resetPackManagerDefaults
			),
			actionFeature(
				"script_manager",
				null,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.SCREEN_SCRIPT_MANAGER,
				EMUtilsTexts.HUB_FEATURE_SCRIPT_MANAGER_DESC,
				HubFeature.Icon.SCRIPT,
				null,
				openScreenAction(ScriptsScreen::new),
				MinescriptCompat.isLoaded(),
				null
			).requiresMod("Minescript", MinescriptCompat.isLoaded()).keys("key.emutils.open_script_manager"),
			actionFeature(
				"command_shortcuts",
				null,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.OPTION_COMMAND_SHORTCUTS,
				EMUtilsTexts.HUB_FEATURE_COMMAND_SHORTCUTS_DESC,
				HubFeature.Icon.TOOL,
				toggle(config::commandShortcutsEnabled, config::setCommandShortcutsEnabled),
				openScreenAction(CommandShortcutsScreen::new),
				true,
				config::resetCommandShortcutsDefaults
			),
			actionFeature(
				"profiles",
				null,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.SCREEN_PROFILES,
				EMUtilsTexts.HUB_FEATURE_PROFILES_DESC,
				HubFeature.Icon.USERS,
				null,
				openScreenAction(ProfilesScreen::new),
				true,
				null
			).keys("key.emutils.next_profile"),
			actionFeature(
				"keybinds",
				null,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.SCREEN_KEYBINDS,
				EMUtilsTexts.HUB_FEATURE_KEYBINDS_DESC,
				HubFeature.Icon.KEYBOARD,
				null,
				openScreenAction(KeybindsScreen::new),
				true,
				null
			),
			categoryFeature("menus", HubCategory.MENUS, HubFeature.Group.MANAGEMENT, EMUtilsTexts.UI_MENUS_TITLE, EMUtilsTexts.HUB_FEATURE_MENUS_DESC, HubFeature.Icon.PALETTE, null),
			categoryFeature("chat", HubCategory.CHAT, HubFeature.Group.QOL, EMUtilsTexts.HUB_CHAT_FEATURES, EMUtilsTexts.HUB_FEATURE_CHAT_DESC, HubFeature.Icon.CHAT, toggle(config::chatFeaturesEnabled, config::setChatFeaturesEnabled)),
			categoryFeature("inventory", HubCategory.INVENTORY, HubFeature.Group.QOL, EMUtilsTexts.HUB_INVENTORY_TOOLS, EMUtilsTexts.HUB_FEATURE_INVENTORY_DESC, HubFeature.Icon.BAG, toggle(config::inventoryToolsEnabled, config::setInventoryToolsEnabled)).keys("key.emutils.slot_lock", "key.emutils.slot_bind", "key.emutils.quick_stack", "key.emutils.mass_drop"),
			categoryFeature("auto_tool", HubCategory.AUTO_TOOL, HubFeature.Group.QOL, EMUtilsTexts.OPTION_AUTO_TOOL, EMUtilsTexts.HUB_FEATURE_AUTO_TOOL_DESC, HubFeature.Icon.TOOL, toggle(config::autoToolEnabled, config::setAutoToolEnabled)),
			categoryFeature("auto_flight_gear", HubCategory.AUTO_FLIGHT, HubFeature.Group.QOL, EMUtilsTexts.OPTION_AUTO_FLIGHT_GEAR, EMUtilsTexts.HUB_FEATURE_AUTO_FLIGHT_DESC, HubFeature.Icon.CAPE, toggle(config::autoFlightGearEnabled, config::setAutoFlightGearEnabled)),
			leaf("fast_place", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_FAST_PLACE, EMUtilsTexts.HUB_FEATURE_FAST_PLACE_DESC, HubFeature.Icon.MOUSE_CLICK, toggle(config::tweakFastPlace, config::setTweakFastPlace), () -> config.setTweakFastPlace(false)),
			leaf("fast_use", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_FAST_USE, EMUtilsTexts.HUB_FEATURE_FAST_USE_DESC, HubFeature.Icon.MOUSE_CLICK, toggle(config::tweakFastUse, config::setTweakFastUse), () -> config.setTweakFastUse(false)),
			leaf("anti_durability_break", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_ANTI_DURABILITY_BREAK, EMUtilsTexts.HUB_FEATURE_ANTI_DURABILITY_BREAK_DESC, HubFeature.Icon.SHIELD, toggle(config::tweakAntiDurabilityBreak, config::setTweakAntiDurabilityBreak), List.of(
				HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_ANTI_DURABILITY_UNIT, config::antiDurabilityUnit, config::setAntiDurabilityUnit, AntiDurabilityUnit.class, unit -> Component.translatable(unit.labelKey())),
				new HubSettingRow.Slider(EMUtilsTexts.OPTION_ANTI_DURABILITY_PROTECT_AT, () -> config.antiDurabilityUnit().suffixKey(), EMUtilsConfig.ANTI_DURABILITY_THRESHOLD_MIN, EMUtilsConfig.ANTI_DURABILITY_THRESHOLD_MAX, config::antiDurabilityProtectAt, config::setAntiDurabilityProtectAt),
				new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ANTI_DURABILITY_WARNING, config::antiDurabilityWarning, config::setAntiDurabilityWarning),
				new HubSettingRow.Slider(EMUtilsTexts.OPTION_ANTI_DURABILITY_WARN_AT, () -> config.antiDurabilityUnit().suffixKey(), EMUtilsConfig.ANTI_DURABILITY_THRESHOLD_MIN, EMUtilsConfig.ANTI_DURABILITY_THRESHOLD_MAX, config::antiDurabilityWarnAt, config::setAntiDurabilityWarnAt)
			), config::resetAntiDurabilityBreakDefaults),
			leaf("safe_walk", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_SAFE_WALK, EMUtilsTexts.HUB_FEATURE_SAFE_WALK_DESC, HubFeature.Icon.SHIELD, toggle(config::tweakSafeWalk, config::setTweakSafeWalk), () -> config.setTweakSafeWalk(false)),
			leaf("place_below", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_PLACE_BELOW, EMUtilsTexts.HUB_FEATURE_PLACE_BELOW_DESC, HubFeature.Icon.MOUSE_CLICK, toggle(config::tweakPlaceBelow, config::setTweakPlaceBelow), () -> config.setTweakPlaceBelow(false)).keys("key.emutils.place_below"),
			leaf("locked_y_placement", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_LOCKED_Y_PLACEMENT, EMUtilsTexts.HUB_FEATURE_LOCKED_Y_PLACEMENT_DESC, HubFeature.Icon.MOUSE_CLICK, toggle(config::tweakLockedYPlacement, config::setTweakLockedYPlacement), () -> config.setTweakLockedYPlacement(false)).keys("key.emutils.locked_y_placement")
		));
		return features;
	}

	@Nullable
	public static HubFeature find(String query) {
		String normalized = HubFeature.normalize(query);
		if (normalized.isEmpty()) {
			return null;
		}

		List<HubFeature> features = all();
		for (HubFeature feature : features) {
			if (feature.id().equals(normalized)) {
				return feature;
			}
		}
		for (HubFeature feature : features) {
			if (feature.id().replace('_', ' ').equals(normalized)) {
				return feature;
			}
		}
		for (HubFeature feature : features) {
			if (HubFeature.normalize(feature.title().getString()).equals(normalized)) {
				return feature;
			}
		}
		for (HubFeature feature : features) {
			if (feature.id().contains(normalized) || HubFeature.normalize(feature.title().getString()).contains(normalized)) {
				return feature;
			}
		}
		return null;
	}

	public static List<String> toggleableIds() {
		List<String> ids = new ArrayList<>();
		for (HubFeature feature : all()) {
			if (feature.toggle() != null) {
				ids.add(feature.id());
			}
		}
		return ids;
	}

	public static List<String> resettableIds() {
		List<String> ids = new ArrayList<>();
		for (HubFeature feature : all()) {
			if (feature.resetAction() != null) {
				ids.add(feature.id());
			}
		}
		return ids;
	}

	public static String normalize(String value) {
		return HubFeature.normalize(value);
	}

	private static HubFeature categoryFeature(
		String id,
		HubCategory category,
		HubFeature.Group group,
		String titleKey,
		String descriptionKey,
		HubFeature.Icon icon,
		HubFeature.Toggle toggle
	) {
		return new HubFeature(id, category, group, titleKey, descriptionKey, icon, toggle, null, null, true, categoryReset(category));
	}

	private static HubFeature leaf(
		String id,
		HubFeature.Group group,
		String titleKey,
		String descriptionKey,
		HubFeature.Icon icon,
		HubFeature.Toggle toggle,
		Runnable resetAction
	) {
		return new HubFeature(id, null, group, titleKey, descriptionKey, icon, toggle, null, null, true, resetAction);
	}

	/** A feature with a card toggle and its own settings, but no category of its own. */
	private static HubFeature leaf(
		String id,
		HubFeature.Group group,
		String titleKey,
		String descriptionKey,
		HubFeature.Icon icon,
		HubFeature.Toggle toggle,
		List<HubSettingRow> rows,
		Runnable resetAction
	) {
		return new HubFeature(id, null, group, titleKey, descriptionKey, icon, toggle, rows, null, true, resetAction);
	}

	private static HubFeature actionFeature(
		String id,
		@Nullable HubCategory category,
		HubFeature.Group group,
		String titleKey,
		String descriptionKey,
		HubFeature.Icon icon,
		HubFeature.@Nullable Toggle toggle,
		Runnable action,
		boolean actionEnabled,
		@Nullable Runnable resetAction
	) {
		return new HubFeature(
			id,
			category,
			group,
			titleKey,
			descriptionKey,
			icon,
			toggle,
			category == null ? List.of() : null,
			action,
			actionEnabled,
			resetAction
		);
	}

	private static Runnable categoryReset(HubCategory category) {
		return () -> HubSettingsRegistry.resetAction(category, () -> {
		}).run();
	}

	private static Runnable openScreenAction(Function<Screen, Screen> screenFactory) {
		return () -> {
			Minecraft client = Minecraft.getInstance();
			if (client != null) {
				client.gui.setScreen(screenFactory.apply(MinecraftClientCompat.screen(client)));
			}
		};
	}

	private static HubFeature.Toggle toggle(Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return new HubFeature.Toggle(getter::get, setter);
	}

	/** The Beacon Radius Outline's rows, with the Xaero Map Integration switch when a Xaero map mod is installed. */
	private static List<HubSettingRow> withXaeroRow(EMUtilsConfig config, List<HubSettingRow> rows) {
		FabricLoader loader = FabricLoader.getInstance();
		if (!loader.isModLoaded("xaerominimap") && !loader.isModLoaded("xaeroworldmap")) {
			return rows;
		}
		List<HubSettingRow> all = new ArrayList<>(rows);
		all.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_BEACON_RADIUS_XAERO, config::beaconRadiusXaero, config::setBeaconRadiusXaero));
		return all;
	}
}
