package net.emutils.client.emutils.gui.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.commandshortcuts.gui.CommandShortcutListScreen;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.minescript.gui.ScriptManagerScreen;
import net.emutils.client.emutils.packs.gui.PackManagerScreen;
import net.emutils.client.emutils.screenshot.gui.ScreenshotGalleryScreen;
import net.emutils.client.emutils.tweaks.FreeCameraHudMode;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.waypoint.gui.WaypointListScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class HubFeatureCatalog {
	private HubFeatureCatalog() {
	}

	public static List<HubFeature> all() {
		EMUtilsConfig config = EMUtilsClient.config();
		return List.of(
			categoryFeature("fullbright", HubCategory.FULLBRIGHT, HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT, EMUtilsTexts.HUB_FEATURE_FULLBRIGHT_DESC, HubFeature.Icon.SUN, toggle(config::tweakFullbright, config::setTweakFullbright)),
			categoryFeature("clear_weather", HubCategory.CLEAR_WEATHER, HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER, EMUtilsTexts.HUB_FEATURE_CLEAR_WEATHER_DESC, HubFeature.Icon.CLOUD_SUN, toggle(config::tweakClearWeather, config::setTweakClearWeather)),
			leaf("no_fog", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_FOG, EMUtilsTexts.HUB_FEATURE_NO_FOG_DESC, HubFeature.Icon.CLOUD_OFF, toggle(config::tweakNoFog, config::setTweakNoFog), () -> config.setTweakNoFog(false)),
			leaf("clear_underwater", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_CLEAR_UNDERWATER, EMUtilsTexts.HUB_FEATURE_CLEAR_UNDERWATER_DESC, HubFeature.Icon.DROPLETS, toggle(config::tweakClearUnderwater, config::setTweakClearUnderwater), () -> config.setTweakClearUnderwater(true)),
			leaf("clear_lava", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_CLEAR_LAVA, EMUtilsTexts.HUB_FEATURE_CLEAR_LAVA_DESC, HubFeature.Icon.FLAME, toggle(config::tweakClearLava, config::setTweakClearLava), () -> config.setTweakClearLava(true)),
			leaf("no_fire_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_FIRE_OVERLAY, EMUtilsTexts.HUB_FEATURE_NO_FIRE_OVERLAY_DESC, HubFeature.Icon.FLAME, toggle(config::tweakNoFireOverlay, config::setTweakNoFireOverlay), () -> config.setTweakNoFireOverlay(false)),
			leaf("low_fire_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_LOW_FIRE_OVERLAY, EMUtilsTexts.HUB_FEATURE_LOW_FIRE_OVERLAY_DESC, HubFeature.Icon.FLAME, toggle(config::tweakLowFireOverlay, config::setTweakLowFireOverlay), () -> config.setTweakLowFireOverlay(false)),
			leaf("no_nausea", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_NAUSEA, EMUtilsTexts.HUB_FEATURE_NO_NAUSEA_DESC, HubFeature.Icon.EYE, toggle(config::tweakNoNausea, config::setTweakNoNausea), () -> config.setTweakNoNausea(false)),
			leaf("no_spyglass_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_SPYGLASS_OVERLAY, EMUtilsTexts.HUB_FEATURE_NO_SPYGLASS_OVERLAY_DESC, HubFeature.Icon.ZOOM, toggle(config::tweakNoSpyglassOverlay, config::setTweakNoSpyglassOverlay), () -> config.setTweakNoSpyglassOverlay(false)),
			leaf("no_pumpkin_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_PUMPKIN_OVERLAY, EMUtilsTexts.HUB_FEATURE_NO_PUMPKIN_OVERLAY_DESC, HubFeature.Icon.EYE, toggle(config::tweakNoPumpkinOverlay, config::setTweakNoPumpkinOverlay), () -> config.setTweakNoPumpkinOverlay(false)),
			leaf("no_environment_fog", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_ENVIRONMENT_FOG, EMUtilsTexts.HUB_FEATURE_NO_ENVIRONMENT_FOG_DESC, HubFeature.Icon.CLOUD_OFF, toggle(config::tweakNoEnvironmentFog, config::setTweakNoEnvironmentFog), () -> config.setTweakNoEnvironmentFog(true)),
			leaf("no_nether_particles", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_NETHER_PARTICLES, EMUtilsTexts.HUB_FEATURE_NO_NETHER_PARTICLES_DESC, HubFeature.Icon.SPARKLES, toggle(config::tweakNoNetherParticles, config::setTweakNoNetherParticles), () -> config.setTweakNoNetherParticles(false)),
			leaf("no_hurt_cam", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_NO_HURT_CAM, EMUtilsTexts.HUB_FEATURE_NO_HURT_CAM_DESC, HubFeature.Icon.SHIELD, toggle(config::tweakNoHurtCam, config::setTweakNoHurtCam), () -> config.setTweakNoHurtCam(false)),
			leaf("freelook", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_FREELOOK, EMUtilsTexts.HUB_FEATURE_FREELOOK_DESC, HubFeature.Icon.EYE, toggle(config::tweakFreelook, config::setTweakFreelook), () -> config.setTweakFreelook(false)),
			leaf("beacon_radius_outline", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_BEACON_RADIUS_OUTLINE, EMUtilsTexts.HUB_FEATURE_BEACON_RADIUS_DESC, HubFeature.Icon.SPARKLES, toggle(config::beaconRadiusOutline, config::setBeaconRadiusOutline), () -> config.setBeaconRadiusOutline(false)),
			leaf("light_level_overlay", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_LIGHT_LEVEL_OVERLAY, EMUtilsTexts.HUB_FEATURE_LIGHT_LEVEL_OVERLAY_DESC, HubFeature.Icon.SUN, toggle(config::lightLevelOverlay, config::setLightLevelOverlay), () -> config.setLightLevelOverlay(false)),
			leaf("own_nametag", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_OWN_NAMETAG, EMUtilsTexts.HUB_FEATURE_OWN_NAMETAG_DESC, HubFeature.Icon.TAG, toggle(config::tweakOwnNametag, config::setTweakOwnNametag), () -> config.setTweakOwnNametag(false)),
			leaf("shulker_preview", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_SHULKER_TOOLTIP_PREVIEW, EMUtilsTexts.HUB_FEATURE_SHULKER_PREVIEW_DESC, HubFeature.Icon.BOX, toggle(config::tweakShulkerTooltipPreview, config::setTweakShulkerTooltipPreview), () -> config.setTweakShulkerTooltipPreview(true)),
			leaf("bundle_preview", HubFeature.Group.RENDER, EMUtilsTexts.OPTION_TWEAK_BUNDLE_TOOLTIP_PREVIEW, EMUtilsTexts.HUB_FEATURE_BUNDLE_PREVIEW_DESC, HubFeature.Icon.PACKAGE_OPEN, toggle(config::tweakBundleTooltipPreview, config::setTweakBundleTooltipPreview), () -> config.setTweakBundleTooltipPreview(true)),
			categoryFeature("zoom", HubCategory.ZOOM, HubFeature.Group.RENDER, EMUtilsTexts.HUB_ZOOM, EMUtilsTexts.HUB_FEATURE_ZOOM_DESC, HubFeature.Icon.ZOOM, toggle(config::zoomEnabled, config::setZoomEnabled)),
			categoryFeature("capes", HubCategory.CAPES, HubFeature.Group.RENDER, EMUtilsTexts.HUB_CAPES, EMUtilsTexts.HUB_FEATURE_CAPES_DESC, HubFeature.Icon.CAPE, toggle(config::customCapes, config::setCustomCapes)),
			categoryFeature("hud_overlay", HubCategory.HUD_OVERLAY, HubFeature.Group.HUD, EMUtilsTexts.HUB_HUD_OVERLAY, EMUtilsTexts.HUB_FEATURE_HUD_DESC, HubFeature.Icon.HUD, toggle(config::hudOverlay, config::setHudOverlay)),
			categoryFeature("food_hud", HubCategory.FOOD_HUD, HubFeature.Group.HUD, EMUtilsTexts.HUB_FOOD_HUD, EMUtilsTexts.HUB_FEATURE_FOOD_HUD_DESC, HubFeature.Icon.APPLE, toggle(config::foodHud, config::setFoodHud)),
			categoryFeature("spotify", HubCategory.SPOTIFY, HubFeature.Group.HUD, EMUtilsTexts.HUB_SPOTIFY_PLAYER, EMUtilsTexts.HUB_FEATURE_SPOTIFY_DESC, HubFeature.Icon.MUSIC, toggle(config::spotifyPlayerEnabled, config::setSpotifyPlayerEnabled)),
			categoryFeature("auto_reconnect", HubCategory.AUTO_RECONNECT, HubFeature.Group.UTILITY, EMUtilsTexts.HUB_AUTO_RECONNECT, EMUtilsTexts.HUB_FEATURE_AUTO_RECONNECT_DESC, HubFeature.Icon.RECONNECT, toggle(config::autoReconnect, config::setAutoReconnect)),
			categoryFeature("screenshot_helper", HubCategory.SCREENSHOT, HubFeature.Group.UTILITY, EMUtilsTexts.HUB_SCREENSHOT_HELPER, EMUtilsTexts.HUB_FEATURE_SCREENSHOT_DESC, HubFeature.Icon.IMAGE, toggle(config::screenshotHelper, config::setScreenshotHelper)),
			categoryFeature("waypoints", HubCategory.DEATH_WAYPOINTS, HubFeature.Group.UTILITY, EMUtilsTexts.HUB_WAYPOINTS, EMUtilsTexts.HUB_FEATURE_WAYPOINTS_DESC, HubFeature.Icon.PIN, toggle(config::waypointEnabled, config::setWaypointEnabled)),
			new HubFeature(
				"free_camera",
				null,
				HubFeature.Group.UTILITY,
				EMUtilsTexts.OPTION_TWEAK_FREE_CAMERA,
				EMUtilsTexts.HUB_FEATURE_FREE_CAMERA_DESC,
				HubFeature.Icon.EYE,
				toggle(config::tweakFreeCamera, config::setTweakFreeCamera),
				List.of(
					new HubSettingRow.Cycle<>(
						EMUtilsTexts.OPTION_FREE_CAMERA_HUD_MODE,
						config::freeCameraHudMode,
						config::setFreeCameraHudMode,
						() -> config.freeCameraHudMode().next(),
						() -> Component.translatable(config.freeCameraHudMode().labelKey())
					),
					new HubSettingRow.Slider(
						EMUtilsTexts.OPTION_FREE_CAMERA_BOOST_MULTIPLIER,
						EMUtilsTexts.SUFFIX_MULTIPLIER,
						EMUtilsConfig.FREE_CAMERA_BOOST_MULTIPLIER_MIN,
						EMUtilsConfig.FREE_CAMERA_BOOST_MULTIPLIER_MAX,
						config::freeCameraBoostMultiplier,
						config::setFreeCameraBoostMultiplier
					)
				),
				null,
				true,
				() -> {
					config.setTweakFreeCamera(false);
					config.setFreeCameraHudMode(FreeCameraHudMode.SPECTATOR);
					config.setFreeCameraBoostMultiplier(3);
				}
			),
			actionFeature(
				"screenshot_gallery",
				HubCategory.SCREENSHOT_GALLERY,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.SCREEN_SCREENSHOT_GALLERY,
				EMUtilsTexts.HUB_FEATURE_SCREENSHOT_GALLERY_DESC,
				HubFeature.Icon.IMAGE,
				null,
				openScreenAction(ScreenshotGalleryScreen::new),
				true,
				categoryReset(HubCategory.SCREENSHOT_GALLERY)
			),
			actionFeature(
				"current_waypoints",
				null,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.SCREEN_CURRENT_WAYPOINTS,
				EMUtilsTexts.HUB_FEATURE_CURRENT_WAYPOINTS_DESC,
				HubFeature.Icon.PIN,
				null,
				openScreenAction(WaypointListScreen::new),
				true,
				null
			),
			actionFeature(
				"pack_manager",
				null,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.OPTION_PACK_MANAGER,
				EMUtilsTexts.HUB_FEATURE_PACK_MANAGER_DESC,
				HubFeature.Icon.PACKAGE,
				toggle(config::packManagerEnabled, config::setPackManagerEnabled),
				openScreenAction(PackManagerScreen::new),
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
				openScreenAction(ScriptManagerScreen::new),
				MinescriptCompat.isLoaded(),
				null
			),
			actionFeature(
				"command_shortcuts",
				null,
				HubFeature.Group.MANAGEMENT,
				EMUtilsTexts.OPTION_COMMAND_SHORTCUTS,
				EMUtilsTexts.HUB_FEATURE_COMMAND_SHORTCUTS_DESC,
				HubFeature.Icon.TOOL,
				toggle(config::commandShortcutsEnabled, config::setCommandShortcutsEnabled),
				openScreenAction(CommandShortcutListScreen::new),
				true,
				config::resetCommandShortcutsDefaults
			),
			categoryFeature("chat", HubCategory.CHAT, HubFeature.Group.QOL, EMUtilsTexts.HUB_CHAT_FEATURES, EMUtilsTexts.HUB_FEATURE_CHAT_DESC, HubFeature.Icon.CHAT, toggle(config::copyChat, config::setCopyChat)),
			categoryFeature("inventory", HubCategory.INVENTORY, HubFeature.Group.QOL, EMUtilsTexts.HUB_INVENTORY_TOOLS, EMUtilsTexts.HUB_FEATURE_INVENTORY_DESC, HubFeature.Icon.BAG, toggle(config::inventoryToolsEnabled, config::setInventoryToolsEnabled)),
			categoryFeature("auto_tool", HubCategory.AUTO_TOOL, HubFeature.Group.QOL, EMUtilsTexts.OPTION_AUTO_TOOL, EMUtilsTexts.HUB_FEATURE_AUTO_TOOL_DESC, HubFeature.Icon.TOOL, toggle(config::autoToolEnabled, config::setAutoToolEnabled)),
			categoryFeature("auto_flight_gear", HubCategory.AUTO_FLIGHT, HubFeature.Group.QOL, EMUtilsTexts.OPTION_AUTO_FLIGHT_GEAR, EMUtilsTexts.HUB_FEATURE_AUTO_FLIGHT_DESC, HubFeature.Icon.CAPE, toggle(config::autoFlightGearEnabled, config::setAutoFlightGearEnabled)),
			leaf("fast_place", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_FAST_PLACE, EMUtilsTexts.HUB_FEATURE_FAST_PLACE_DESC, HubFeature.Icon.MOUSE_CLICK, toggle(config::tweakFastPlace, config::setTweakFastPlace), () -> config.setTweakFastPlace(false)),
			leaf("fast_use", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_FAST_USE, EMUtilsTexts.HUB_FEATURE_FAST_USE_DESC, HubFeature.Icon.MOUSE_CLICK, toggle(config::tweakFastUse, config::setTweakFastUse), () -> config.setTweakFastUse(false)),
			leaf("anti_durability_break", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_ANTI_DURABILITY_BREAK, EMUtilsTexts.HUB_FEATURE_ANTI_DURABILITY_BREAK_DESC, HubFeature.Icon.SHIELD, toggle(config::tweakAntiDurabilityBreak, config::setTweakAntiDurabilityBreak), () -> config.setTweakAntiDurabilityBreak(false)),
			leaf("safe_walk", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_SAFE_WALK, EMUtilsTexts.HUB_FEATURE_SAFE_WALK_DESC, HubFeature.Icon.SHIELD, toggle(config::tweakSafeWalk, config::setTweakSafeWalk), () -> config.setTweakSafeWalk(false)),
			leaf("place_below", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_PLACE_BELOW, EMUtilsTexts.HUB_FEATURE_PLACE_BELOW_DESC, HubFeature.Icon.MOUSE_CLICK, toggle(config::tweakPlaceBelow, config::setTweakPlaceBelow), () -> config.setTweakPlaceBelow(false)),
			leaf("locked_y_placement", HubFeature.Group.QOL, EMUtilsTexts.OPTION_TWEAK_LOCKED_Y_PLACEMENT, EMUtilsTexts.HUB_FEATURE_LOCKED_Y_PLACEMENT_DESC, HubFeature.Icon.MOUSE_CLICK, toggle(config::tweakLockedYPlacement, config::setTweakLockedYPlacement), () -> config.setTweakLockedYPlacement(false))
		);
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
				client.setScreenAndShow(screenFactory.apply(MinecraftClientCompat.screen(client)));
			}
		};
	}

	private static HubFeature.Toggle toggle(Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return new HubFeature.Toggle(getter::get, setter);
	}
}
