package net.emutils.client.emutils.gui.hub;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.capes.CapePreferredProvider;
import net.emutils.client.emutils.chat.ChatMentionAlerts;
import net.emutils.client.emutils.commandshortcuts.gui.CommandShortcutListScreen;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.waypoint.WaypointCoordinateFormat;
import net.emutils.client.emutils.minescript.gui.ScriptManagerScreen;
import net.emutils.client.emutils.packs.gui.PackManagerScreen;
import net.emhelpers.client.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.screenshot.ScreenshotGallerySort;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HubSettingsRegistry {
	private static final Map<HubCategory, Function<Runnable, List<HubSettingRow>>> ROWS = new EnumMap<>(HubCategory.class);
	private static final String[] HIGHLIGHT_STYLES = {"Bold", "Italic", "Underline", "Normal"};

	static {
		ROWS.put(HubCategory.CHAT, HubSettingsRegistry::chatRows);
		ROWS.put(HubCategory.DEATH_WAYPOINTS, HubSettingsRegistry::deathRows);
		ROWS.put(HubCategory.AUTO_RECONNECT, HubSettingsRegistry::reconnectRows);
		ROWS.put(HubCategory.SCREENSHOT, HubSettingsRegistry::screenshotRows);
		ROWS.put(HubCategory.SCREENSHOT_GALLERY, HubSettingsRegistry::screenshotGalleryRows);
		ROWS.put(HubCategory.MANAGERS, HubSettingsRegistry::managerRows);
		ROWS.put(HubCategory.HUD_OVERLAY, HubSettingsRegistry::hudRows);
		ROWS.put(HubCategory.FOOD_HUD, HubSettingsRegistry::foodHudRows);
		ROWS.put(HubCategory.ZOOM, HubSettingsRegistry::zoomRows);
		ROWS.put(HubCategory.FULLBRIGHT, HubSettingsRegistry::fullbrightRows);
		ROWS.put(HubCategory.CLEAR_WEATHER, HubSettingsRegistry::clearWeatherRows);
		ROWS.put(HubCategory.TWEAKS, HubSettingsRegistry::tweaksRows);
		ROWS.put(HubCategory.CAPES, HubSettingsRegistry::capesRows);
		ROWS.put(HubCategory.INVENTORY, HubSettingsRegistry::inventoryRows);
		ROWS.put(HubCategory.SPOTIFY, HubSettingsRegistry::spotifyRows);
	}

	private HubSettingsRegistry() {
	}

	public static List<HubSettingRow> rows(HubCategory category, Runnable refresh) {
		Function<Runnable, List<HubSettingRow>> supplier = ROWS.get(category);
		if (supplier == null) {
			return List.of();
		}

		return supplier.apply(refresh);
	}

	public static Runnable resetAction(HubCategory category, Runnable refresh) {
		EMUtilsConfig config = config();
		Runnable reset = switch (category) {
			case CHAT -> config::resetChatDefaults;
			case DEATH_WAYPOINTS -> config::resetDeathWaypointDefaults;
			case AUTO_RECONNECT -> config::resetAutoReconnectDefaults;
			case SCREENSHOT -> config::resetScreenshotHelperDefaults;
			case SCREENSHOT_GALLERY -> config::resetScreenshotGalleryDefaults;
			case MANAGERS -> config::resetManagerDefaults;
			case HUD_OVERLAY -> config::resetHudDefaults;
			case FOOD_HUD -> config::resetFoodHudDefaults;
			case ZOOM -> config::resetZoomDefaults;
			case FULLBRIGHT -> config::resetFullbrightDefaults;
			case CLEAR_WEATHER -> config::resetClearWeatherDefaults;
			case TWEAKS -> config::resetTweaksDefaults;
			case CAPES -> config::resetCapesDefaults;
			case INVENTORY -> config::resetInventoryToolsDefaults;
			case SPOTIFY -> config::resetSpotifyPlayerDefaults;
		};

		return () -> {
			reset.run();
			refresh.run();
		};
	}

	public static List<HubCategory> visibleCategories() {
		List<HubCategory> categories = new ArrayList<>();
		for (HubCategory category : HubCategory.values()) {
			categories.add(category);
		}

		return categories;
	}

	private static EMUtilsConfig config() {
		return EMUtilsClient.config();
	}

	private static List<HubSettingRow> chatRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_COPY_CHAT, config::copyChat, config::setCopyChat));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_COPY_CHAT_FORMATTING, config::copyChatFormatting, config::setCopyChatFormatting));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_COPY_CHAT_FEEDBACK, config::copyChatFeedback, config::setCopyChatFeedback));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CHAT_TIMESTAMPS, config::chatTimestamps, config::setChatTimestamps));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CHAT_TIMESTAMP_24_HOUR, config::chatTimestamp24Hour, config::setChatTimestamp24Hour));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SMART_CHAT_FILTERS, config::smartChatFilters, config::setSmartChatFilters));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_DUPLICATE_MESSAGE_TIME_WINDOW, config::duplicateMessageTimeWindow, config::setDuplicateMessageTimeWindow));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_DUPLICATE_MESSAGE_WINDOW,
			EMUtilsTexts.SUFFIX_SECONDS,
			EMUtilsConfig.DUPLICATE_MESSAGE_WINDOW_MIN,
			EMUtilsConfig.DUPLICATE_MESSAGE_WINDOW_MAX,
			config::duplicateMessageWindowSeconds,
			config::setDuplicateMessageWindowSeconds
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CHAT_MENTION_ALERTS, config::chatMentionAlerts, config::setChatMentionAlerts));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_CHAT_MENTION_ALERT_VOLUME,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.CHAT_MENTION_VOLUME_MIN,
			EMUtilsConfig.CHAT_MENTION_VOLUME_MAX,
			config::chatMentionAlertVolume,
			config::setChatMentionAlertVolume
		));
		rows.add(new HubSettingRow.Cycle<>(
			EMUtilsTexts.OPTION_CHAT_MENTION_ALERT_SOUND,
			() -> config.chatMentionAlertSound(),
			config::setChatMentionAlertSound,
			() -> (config.chatMentionAlertSound() + 1) % 7,
			() -> Component.literal(ChatMentionAlerts.soundNames()[config.chatMentionAlertSound()])
		));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CHAT_MENTION_HIGHLIGHT, config::chatMentionHighlight, config::setChatMentionHighlight));
		rows.add(new HubSettingRow.Rgb(
			EMUtilsTexts.OPTION_CHAT_MENTION_HIGHLIGHT_COLOR,
			config::chatMentionHighlightColor,
			config::setChatMentionHighlightColor
		));
		rows.add(new HubSettingRow.Cycle<>(
			EMUtilsTexts.OPTION_CHAT_MENTION_HIGHLIGHT_STYLE,
			() -> config.chatMentionHighlightStyle(),
			config::setChatMentionHighlightStyle,
			() -> (config.chatMentionHighlightStyle() + 1) % 4,
			() -> Component.literal(HIGHLIGHT_STYLES[config.chatMentionHighlightStyle()])
		));
		return rows;
	}

	private static List<HubSettingRow> deathRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_WAYPOINTS, config::waypointEnabled, config::setWaypointEnabled));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_WAYPOINT_AUTO_COPY, config::waypointAutoCopyCoords, config::setWaypointAutoCopyCoords));
		rows.add(new HubSettingRow.Cycle<>(
			EMUtilsTexts.OPTION_WAYPOINT_COORD_FORMAT,
			config::waypointCoordinateFormat,
			config::setWaypointCoordinateFormat,
			() -> config.waypointCoordinateFormat().next(),
			() -> Component.translatable(config.waypointCoordinateFormat().labelKey())
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Rgb(
			EMUtilsTexts.OPTION_WAYPOINT_DEFAULT_DEATH_COLOR,
			config::waypointDefaultDeathColor,
			config::setWaypointDefaultDeathColor
		));
		rows.add(new HubSettingRow.Rgb(
			EMUtilsTexts.OPTION_WAYPOINT_DEFAULT_CUSTOM_COLOR,
			config::waypointDefaultCustomColor,
			config::setWaypointDefaultCustomColor
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_WAYPOINT_OPACITY,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.DEATH_WAYPOINT_OPACITY_MIN,
			EMUtilsConfig.DEATH_WAYPOINT_OPACITY_MAX,
			config::waypointOpacity,
			config::setWaypointOpacity
		));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_WAYPOINT_SIZE,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.DEATH_WAYPOINT_SIZE_MIN,
			EMUtilsConfig.DEATH_WAYPOINT_SIZE_MAX,
			config::waypointSize,
			config::setWaypointSize
		));
		return rows;
	}

	private static List<HubSettingRow> reconnectRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_AUTO_RECONNECT, config::autoReconnect, config::setAutoReconnect));
		rows.add(divider());
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_RETRY_DELAY,
			EMUtilsTexts.SUFFIX_SECONDS,
			EMUtilsConfig.RECONNECT_DELAY_MIN,
			EMUtilsConfig.RECONNECT_DELAY_MAX,
			config::reconnectDelaySeconds,
			config::setReconnectDelaySeconds
		));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_AUTO_RECONNECT_UNLIMITED, config::autoReconnectUnlimitedTries, config::setAutoReconnectUnlimitedTries));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_AUTO_RECONNECT_MAX_TRIES,
			"",
			EMUtilsConfig.RECONNECT_MAX_TRIES_MIN,
			EMUtilsConfig.RECONNECT_MAX_TRIES_MAX,
			config::autoReconnectMaxTries,
			config::setAutoReconnectMaxTries
		));
		return rows;
	}

	private static List<HubSettingRow> screenshotRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SCREENSHOT_HELPER, config::screenshotHelper, config::setScreenshotHelper));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SCREENSHOT_AUTO_COPY, config::screenshotAutoCopy, config::setScreenshotAutoCopy));
		return rows;
	}

	private static List<HubSettingRow> screenshotGalleryRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Cycle<>(
			EMUtilsTexts.OPTION_SCREENSHOT_SORT,
			config::screenshotGallerySort,
			config::setScreenshotGallerySort,
			() -> config.screenshotGallerySort().next(),
			() -> Component.translatable(config.screenshotGallerySort().labelKey())
		));
		rows.add(new HubSettingRow.Toggle(
			EMUtilsTexts.OPTION_SCREENSHOT_DELETE_CONFIRMATION,
			config::screenshotGalleryDeleteConfirmation,
			config::setScreenshotGalleryDeleteConfirmation
		));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_SCREENSHOT_MAX_COUNT,
			"",
			EMUtilsConfig.SCREENSHOT_MAX_COUNT_MIN,
			EMUtilsConfig.SCREENSHOT_MAX_COUNT_MAX,
			config::screenshotGalleryMaxCount,
			config::setScreenshotGalleryMaxCount
		));
		return rows;
	}

	private static List<HubSettingRow> managerRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_PACK_MANAGER, config::packManagerEnabled, config::setPackManagerEnabled));
		rows.add(navRow(EMUtilsTexts.HUB_OPEN_PACK_MANAGER, PackManagerScreen::new));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_COMMAND_SHORTCUTS, config::commandShortcutsEnabled, config::setCommandShortcutsEnabled));
		rows.add(navRow(EMUtilsTexts.HUB_OPEN_COMMAND_SHORTCUTS, CommandShortcutListScreen::new));
		rows.add(divider());
		rows.add(navRow(EMUtilsTexts.HUB_OPEN_SCRIPT_MANAGER, ScriptManagerScreen::new, MinescriptCompat.isLoaded()));
		return rows;
	}

	private static List<HubSettingRow> hudRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_OVERLAY, config::hudOverlay, config::setHudOverlay));
		rows.add(new HubSettingRow.Action(
			Component.translatable(EMUtilsTexts.OPTION_HUD_LAYOUT_EDITOR),
			() -> {
				Minecraft client = Minecraft.getInstance();
				if (client != null) {
					HudLayoutManager.openEditor(EMUtilsClient.MOD_ID, client);
				}
			},
			true
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_SHOW_ICONS, config::hudShowIcons, config::setHudShowIcons));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_HIDE_WITH_DEBUG, config::hudHideWithDebug, config::setHudHideWithDebug));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_COORDINATES, config::hudShowCoordinates, config::setHudShowCoordinates));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_CHUNK_REGION, config::hudShowChunkRegion, config::setHudShowChunkRegion));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_BIOME, config::hudShowBiome, config::setHudShowBiome));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_FACING, config::hudShowFacing, config::setHudShowFacing));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_PING, config::hudShowPing, config::setHudShowPing));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_FPS, config::hudShowFps, config::setHudShowFps));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_MEMORY, config::hudShowMemory, config::setHudShowMemory));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_SERVER_TIME, config::hudShowServerTime, config::setHudShowServerTime));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_REAL_TIME, config::hudShowRealTime, config::setHudShowRealTime));
		return rows;
	}

	private static List<HubSettingRow> foodHudRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FOOD_HUD, config::foodHud, config::setFoodHud));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FOOD_HUD_SATURATION_OVERLAY, config::foodHudSaturationOverlay, config::setFoodHudSaturationOverlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FOOD_HUD_HELD_FOOD_OVERLAY, config::foodHudHeldFoodOverlay, config::setFoodHudHeldFoodOverlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FOOD_HUD_OFFHAND_OVERLAY, config::foodHudOffhandOverlay, config::setFoodHudOffhandOverlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FOOD_HUD_EXHAUSTION_UNDERLAY, config::foodHudExhaustionUnderlay, config::setFoodHudExhaustionUnderlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FOOD_HUD_VANILLA_ANIMATIONS, config::foodHudVanillaAnimations, config::setFoodHudVanillaAnimations));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FOOD_HUD_TOOLTIPS, config::foodHudTooltips, config::setFoodHudTooltips));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_FOOD_HUD_TOOLTIP_ALWAYS, config::foodHudTooltipAlways, config::setFoodHudTooltipAlways));
		return rows;
	}

	private static List<HubSettingRow> zoomRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ZOOM, config::zoomEnabled, config::setZoomEnabled));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_ZOOM_AMOUNT,
			"emutils.suffix.multiplier",
			EMUtilsConfig.ZOOM_AMOUNT_MIN,
			EMUtilsConfig.ZOOM_AMOUNT_MAX,
			config::zoomAmount,
			config::setZoomAmount
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ZOOM_SMOOTH_TRANSITION, config::zoomSmoothTransition, config::setZoomSmoothTransition));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_ZOOM_TRANSITION_SPEED,
			"emutils.suffix.zoom_speed",
			EMUtilsConfig.ZOOM_TRANSITION_SPEED_MIN,
			EMUtilsConfig.ZOOM_TRANSITION_SPEED_MAX,
			config::zoomTransitionSpeed,
			config::setZoomTransitionSpeed
		));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_ZOOM_OUT_SPEED,
			"emutils.suffix.zoom_out_speed",
			EMUtilsConfig.ZOOM_OUT_SPEED_MIN,
			EMUtilsConfig.ZOOM_OUT_SPEED_MAX,
			config::zoomOutSpeedMultiplierRaw,
			config::setZoomOutSpeedMultiplier
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ZOOM_CINEMATIC_CAMERA, config::zoomCinematicCamera, config::setZoomCinematicCamera));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ZOOM_HIDE_HAND, config::zoomHideHand, config::setZoomHideHand));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ZOOM_HIDE_HUD, config::zoomHideHud, config::setZoomHideHud));
		return rows;
	}

	private static List<HubSettingRow> fullbrightRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT, config::tweakFullbright, config::setTweakFullbright));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT_STRENGTH,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.FULLBRIGHT_STRENGTH_MIN,
			EMUtilsConfig.FULLBRIGHT_STRENGTH_MAX,
			config::tweakFullbrightStrength,
			config::setTweakFullbrightStrength
		));
		return rows;
	}

	private static List<HubSettingRow> tweaksRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT, config::tweakFullbright, config::setTweakFullbright));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_TWEAK_FULLBRIGHT_STRENGTH,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.FULLBRIGHT_STRENGTH_MIN,
			EMUtilsConfig.FULLBRIGHT_STRENGTH_MAX,
			config::tweakFullbrightStrength,
			config::setTweakFullbrightStrength
		));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER, config::tweakClearWeather, config::setTweakClearWeather));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_FIRE_OVERLAY, config::tweakNoFireOverlay, config::setTweakNoFireOverlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_LOW_FIRE_OVERLAY, config::tweakLowFireOverlay, config::setTweakLowFireOverlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_NAUSEA, config::tweakNoNausea, config::setTweakNoNausea));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_SPYGLASS_OVERLAY, config::tweakNoSpyglassOverlay, config::setTweakNoSpyglassOverlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_FOG, config::tweakNoFog, config::setTweakNoFog));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_UNDERWATER, config::tweakClearUnderwater, config::setTweakClearUnderwater));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_LAVA, config::tweakClearLava, config::setTweakClearLava));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_ENVIRONMENT_FOG, config::tweakNoEnvironmentFog, config::setTweakNoEnvironmentFog));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_HURT_CAM, config::tweakNoHurtCam, config::setTweakNoHurtCam));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_FAST_PLACE, config::tweakFastPlace, config::setTweakFastPlace));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_FREELOOK, config::tweakFreelook, config::setTweakFreelook));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_OWN_NAMETAG, config::tweakOwnNametag, config::setTweakOwnNametag));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_SHULKER_TOOLTIP_PREVIEW, config::tweakShulkerTooltipPreview, config::setTweakShulkerTooltipPreview));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_BUNDLE_TOOLTIP_PREVIEW, config::tweakBundleTooltipPreview, config::setTweakBundleTooltipPreview));
		return rows;
	}

	private static List<HubSettingRow> clearWeatherRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER, config::tweakClearWeather, config::setTweakClearWeather));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER_HIDE_RAIN, config::tweakClearWeatherHideRain, config::setTweakClearWeatherHideRain));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER_HIDE_SNOW, config::tweakClearWeatherHideSnow, config::setTweakClearWeatherHideSnow));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER_HIDE_RAIN_EFFECTS, config::tweakClearWeatherHideRainEffects, config::setTweakClearWeatherHideRainEffects));
		return rows;
	}

	private static List<HubSettingRow> capesRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CUSTOM_CAPES, config::customCapes, config::setCustomCapes));
		rows.add(new HubSettingRow.Cycle<>(
			EMUtilsTexts.OPTION_CAPE_PREFERRED_PROVIDER,
			config::capePreferredProvider,
			config::setCapePreferredProvider,
			() -> config.capePreferredProvider().next(),
			() -> Component.translatable(config.capePreferredProvider().labelKey())
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CAPE_OPTIFINE, config::capeOptifine, config::setCapeOptifine));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CAPE_LABYMOD, config::capeLabyMod, config::setCapeLabyMod));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CAPE_MINECRAFTCAPES, config::capeMinecraftCapes, config::setCapeMinecraftCapes));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CAPE_COSMETICA, config::capeCosmetica, config::setCapeCosmetica));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CAPE_CLOAKSPLUS, config::capeCloaksPlus, config::setCapeCloaksPlus));
		return rows;
	}

	private static List<HubSettingRow> inventoryRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_INVENTORY_TOOLS, config::inventoryToolsEnabled, config::setInventoryToolsEnabled));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SLOT_LOCKING, config::slotLockingEnabled, config::setSlotLockingEnabled));
		rows.add(new HubSettingRow.Rgb(
			EMUtilsTexts.OPTION_SLOT_LOCK_COLOR,
			config::slotLockOverlayColor,
			config::setSlotLockOverlayColor
		));
		rows.add(new HubSettingRow.Rgb(
			EMUtilsTexts.OPTION_BOUND_SLOT_COLOR,
			config::boundSlotOverlayColor,
			config::setBoundSlotOverlayColor
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SLOT_BINDING, config::slotBindingEnabled, config::setSlotBindingEnabled));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SLOT_BINDING_LOCK_BOUND_SLOTS, config::slotBindingLockBoundSlots, config::setSlotBindingLockBoundSlots));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_INVENTORY_PREVIEW, config::inventoryPreviewEnabled, config::setInventoryPreviewEnabled));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_PRESERVE_CONTAINER_CURSOR, config::preserveContainerCursor, config::setPreserveContainerCursor));
		return rows;
	}

	private static List<HubSettingRow> spotifyRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SPOTIFY_PLAYER, config::spotifyPlayerEnabled, config::setSpotifyPlayerEnabled));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SPOTIFY_HUD_OVERLAY, config::spotifyHudOverlay, config::setSpotifyHudOverlay));
		return rows;
	}

	private static HubSettingRow navRow(String labelKey, Function<Screen, Screen> screenFactory) {
		return navRow(labelKey, screenFactory, true);
	}

	private static HubSettingRow navRow(String labelKey, Function<Screen, Screen> screenFactory, boolean enabled) {
		return new HubSettingRow.Action(
			Component.translatable(labelKey),
			() -> {
				net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
				if (client != null) {
					Screen parent = client.screen;
					client.setScreen(screenFactory.apply(parent));
				}
			},
			enabled
		);
	}

	private static HubSettingRow divider() {
		return new HubSettingRow.Divider();
	}
}
