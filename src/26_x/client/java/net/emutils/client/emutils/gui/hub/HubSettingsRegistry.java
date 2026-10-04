package net.emutils.client.emutils.gui.hub;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.capes.CapeSource;
import net.emutils.client.emutils.inventory.InventorySortSpeed;
import net.emutils.client.emutils.tweaks.FreeCameraHudMode;
import net.emutils.client.emutils.tweaks.AutoToolEnchantment;
import net.emutils.client.emutils.tweaks.AutoToolMode;
import net.emutils.client.emutils.chat.ChatMentionAlerts;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiCodeFont;
import net.emutils.client.emutils.gui.ui.UiFontFamily;
import net.emutils.client.emutils.gui.ui.UiMotion;
import net.emutils.client.emutils.inventory.gui.MassDropItemsScreen;
import net.emutils.client.emutils.waypoint.WaypointCoordinateFormat;
import net.emutils.client.emutils.waypoint.WaypointReachAction;
import net.emutils.client.emutils.waypoint.WaypointShareFormat;
import net.emutils.client.emutils.hud.ArmorStatusDisplay;
import net.emutils.client.emutils.hud.HudTextShadow;
import net.emutils.client.emutils.hud.KeystrokesStyle;
import net.emutils.client.emutils.map.MinimapShape;
import net.emutils.client.emutils.map.MinimapZoom;
import net.emutils.client.emutils.hud.HudFont;
import net.emutils.client.emutils.hud.HudStyle;
import net.emutils.client.emutils.hud.ScoreboardTitleAlignment;
import net.emutils.client.emutils.hud.TabListPing;
import net.emutils.client.emutils.hud.TabListSort;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.screenshot.ScreenshotGallerySort;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.fabricmc.loader.api.FabricLoader;
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
		ROWS.put(HubCategory.PACK_MANAGER, HubSettingsRegistry::packManagerRows);
		ROWS.put(HubCategory.HUD_OVERLAY, HubSettingsRegistry::hudRows);
		ROWS.put(HubCategory.FOOD_HUD, HubSettingsRegistry::foodHudRows);
		ROWS.put(HubCategory.LOOK_AT_INFO, HubSettingsRegistry::lookAtInfoRows);
		ROWS.put(HubCategory.KEYSTROKES, HubSettingsRegistry::keystrokesRows);
		ROWS.put(HubCategory.MINIMAP, HubSettingsRegistry::minimapRows);
		ROWS.put(HubCategory.ARMOR_STATUS, HubSettingsRegistry::armorStatusRows);
		ROWS.put(HubCategory.SCOREBOARD, HubSettingsRegistry::scoreboardRows);
		ROWS.put(HubCategory.TAB_LIST, HubSettingsRegistry::tabListRows);
		ROWS.put(HubCategory.ZOOM, HubSettingsRegistry::zoomRows);
		ROWS.put(HubCategory.FULLBRIGHT, HubSettingsRegistry::fullbrightRows);
		ROWS.put(HubCategory.CLEAR_WEATHER, HubSettingsRegistry::clearWeatherRows);
		ROWS.put(HubCategory.TWEAKS, HubSettingsRegistry::tweaksRows);
		ROWS.put(HubCategory.AUTO_FLIGHT, HubSettingsRegistry::autoFlightRows);
		ROWS.put(HubCategory.AUTO_TOOL, HubSettingsRegistry::autoToolRows);
		ROWS.put(HubCategory.CAPES, HubSettingsRegistry::capesRows);
		ROWS.put(HubCategory.INVENTORY, HubSettingsRegistry::inventoryRows);
		ROWS.put(HubCategory.SPOTIFY, HubSettingsRegistry::spotifyRows);
		ROWS.put(HubCategory.MENUS, HubSettingsRegistry::menuRows);
	}

	/** Accent presets for the menus: saturated and dark enough for white text in both themes. */
	private static final List<Integer> ACCENT_PRESETS = List.of(
		0xFF16A058, 0xFF12877F, 0xFF2F6FD6, 0xFF5B5BD6, 0xFF7B4FD0, 0xFFC23D7A, 0xFFD0453A, 0xFFC77700
	);

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
			case PACK_MANAGER -> config::resetPackManagerDefaults;
			case HUD_OVERLAY -> config::resetHudDefaults;
			case FOOD_HUD -> config::resetFoodHudDefaults;
			case LOOK_AT_INFO -> config::resetLookAtInfoDefaults;
			case KEYSTROKES -> config::resetKeystrokesDefaults;
			case MINIMAP -> config::resetMinimapDefaults;
			case ARMOR_STATUS -> config::resetArmorStatusDefaults;
			case SCOREBOARD -> config::resetScoreboardDefaults;
			case TAB_LIST -> config::resetTabListDefaults;
			case ZOOM -> config::resetZoomDefaults;
			case FULLBRIGHT -> config::resetFullbrightDefaults;
			case CLEAR_WEATHER -> config::resetClearWeatherDefaults;
			case TWEAKS -> config::resetTweaksDefaults;
			case AUTO_FLIGHT -> config::resetAutoFlightDefaults;
			case AUTO_TOOL -> config::resetAutoToolDefaults;
			case CAPES -> config::resetCapesDefaults;
			case INVENTORY -> config::resetInventoryToolsDefaults;
			case SPOTIFY -> config::resetSpotifyPlayerDefaults;
			case MENUS -> config::resetMenuSettings;
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

	/**
	 * How the menus look (#120): theme, accent and fonts. Shared by every profile; the accent can follow
	 * the active profile's color instead.
	 */
	private static List<HubSettingRow> menuRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		List<Boolean> themes = List.of(Boolean.TRUE, Boolean.FALSE);
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_MENUS_SECTION_COLORS));
		rows.add(new HubSettingRow.Cycle<>(
			EMUtilsTexts.UI_MENUS_THEME,
			config::settingsUiDark,
			config::setSettingsUiDark,
			() -> !config.settingsUiDark(),
			() -> Component.translatable(config.settingsUiDark() ? EMUtilsTexts.UI_MENUS_THEME_DARK : EMUtilsTexts.UI_MENUS_THEME_LIGHT),
			themes,
			dark -> Component.translatable(dark ? EMUtilsTexts.UI_MENUS_THEME_DARK : EMUtilsTexts.UI_MENUS_THEME_LIGHT)
		));
		rows.add(new HubSettingRow.Swatches(EMUtilsTexts.UI_MENUS_ACCENT, ACCENT_PRESETS, config::uiAccent, config::setUiAccent, () -> !config.uiAccentFromProfile()));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.UI_MENUS_ACCENT_FROM_PROFILE, config::uiAccentFromProfile, config::setUiAccentFromProfile));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.UI_MENUS_HIGH_CONTRAST, config::uiHighContrast, config::setUiHighContrast));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.UI_MENUS_CATEGORY_COLORS, config::uiCategoryColors, config::setUiCategoryColors));

		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_MENUS_SECTION_FONTS));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.UI_MENUS_FONT, config::uiFont, config::setUiFont, UiFontFamily.class, family -> Component.literal(family.displayName())));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.UI_MENUS_TEXT_SIZE, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.UI_TEXT_SIZE_MIN, EMUtilsConfig.UI_TEXT_SIZE_MAX, config::uiTextSize, config::setUiTextSize));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.UI_MENUS_CODE_FONT, config::uiCodeFont, config::setUiCodeFont, UiCodeFont.class, font -> Component.literal(font.displayName())));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.UI_MENUS_CODE_SIZE, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.UI_CODE_SIZE_MIN, EMUtilsConfig.UI_CODE_SIZE_MAX, config::uiCodeSize, config::setUiCodeSize));

		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_MENUS_SECTION_LAYOUT));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.UI_MENUS_COMPACT_CARDS, config::uiCompactCards, config::setUiCompactCards));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.UI_MENUS_REMEMBER_POSITION, config::uiRememberPosition, config::setUiRememberPosition));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.UI_MENUS_ROUNDNESS, EMUtilsTexts.SUFFIX_PERCENT, 0, 100, config::uiCornerRoundness, config::setUiCornerRoundness));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.UI_MENUS_PANEL_OPACITY, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.UI_PANEL_OPACITY_MIN, 100, config::uiPanelOpacity, config::setUiPanelOpacity));

		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_MENUS_SECTION_EFFECTS));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.UI_MENUS_MOTION, config::uiMotion, config::setUiMotion, UiMotion.class, motion -> Component.translatable(EMUtilsTexts.UI_MENUS_MOTION + "." + motion.name().toLowerCase(Locale.ROOT))));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.UI_MENUS_BLUR, EMUtilsTexts.SUFFIX_PERCENT, 0, 100, config::uiBackgroundBlur, config::setUiBackgroundBlur));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.UI_MENUS_DIM, EMUtilsTexts.SUFFIX_PERCENT, 0, 100, config::uiBackgroundDim, config::setUiBackgroundDim));
		return rows;
	}

	private static List<HubSettingRow> chatRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CHAT_FEATURES, config::chatFeaturesEnabled, config::setChatFeaturesEnabled));
		rows.add(divider());
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
		rows.add(HubSettingRow.Cycle.ofNames(
			EMUtilsTexts.OPTION_CHAT_MENTION_ALERT_SOUND,
			config::chatMentionAlertSound,
			config::setChatMentionAlertSound,
			ChatMentionAlerts.soundNames()
		));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CHAT_MENTION_HIGHLIGHT, config::chatMentionHighlight, config::setChatMentionHighlight));
		rows.add(new HubSettingRow.Rgb(
			EMUtilsTexts.OPTION_CHAT_MENTION_HIGHLIGHT_COLOR,
			config::chatMentionHighlightColor,
			config::setChatMentionHighlightColor
		));
		rows.add(HubSettingRow.Cycle.ofNames(
			EMUtilsTexts.OPTION_CHAT_MENTION_HIGHLIGHT_STYLE,
			config::chatMentionHighlightStyle,
			config::setChatMentionHighlightStyle,
			HIGHLIGHT_STYLES
		));
		return rows;
	}

	private static List<HubSettingRow> deathRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_WAYPOINTS, config::waypointEnabled, config::setWaypointEnabled));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_WAYPOINTS_SECTION_GENERAL));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_WAYPOINT_AUTO_COPY, config::waypointAutoCopyCoords, config::setWaypointAutoCopyCoords));
		rows.add(HubSettingRow.Cycle.ofEnum(
			EMUtilsTexts.OPTION_WAYPOINT_COORD_FORMAT,
			config::waypointCoordinateFormat,
			config::setWaypointCoordinateFormat,
			WaypointCoordinateFormat.class,
			value -> Component.translatable(value.labelKey())
		));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_COPY_COORDINATES_FEEDBACK, config::copyCoordinatesFeedback, config::setCopyCoordinatesFeedback));
		rows.add(HubSettingRow.Cycle.ofEnum(
			EMUtilsTexts.OPTION_WAYPOINT_REACH_ACTION,
			config::waypointReachAction,
			config::setWaypointReachAction,
			WaypointReachAction.class,
			value -> Component.translatable(value.labelKey())
		));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_DEATH_WAYPOINT_KEEP,
			EMUtilsTexts.SUFFIX_COUNT,
			EMUtilsConfig.DEATH_WAYPOINT_KEEP_MIN,
			EMUtilsConfig.DEATH_WAYPOINT_KEEP_MAX,
			config::deathWaypointKeep,
			config::setDeathWaypointKeep
		));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_WAYPOINT_CHAT_PROMPT, config::waypointChatPrompt, config::setWaypointChatPrompt));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_WAYPOINT_CHAT_AUTO_CREATE, config::waypointChatAutoCreate, config::setWaypointChatAutoCreate));
		rows.add(HubSettingRow.Cycle.ofEnum(
			EMUtilsTexts.OPTION_WAYPOINT_SHARE_FORMAT,
			config::waypointShareFormat,
			config::setWaypointShareFormat,
			WaypointShareFormat.class,
			value -> Component.translatable(value.labelKey())
		));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_WAYPOINTS_SECTION_APPEARANCE));
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
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_WAYPOINT_FONT, config::waypointFont, config::setWaypointFont, HudFont.class, font -> Component.translatable(font.labelKey())));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_WAYPOINT_LABEL_BACKGROUND,
			EMUtilsTexts.SUFFIX_PERCENT,
			0,
			100,
			config::waypointLabelBackground,
			config::setWaypointLabelBackground
		));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_WAYPOINTS_SECTION_DISPLAY));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_WAYPOINT_EDGE_PIN, config::waypointEdgePin, config::setWaypointEdgePin));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_WAYPOINT_SHOW_OTHER_DIMENSIONS, config::waypointShowOtherDimensions, config::setWaypointShowOtherDimensions));
		FabricLoader loader = FabricLoader.getInstance();
		if (loader.isModLoaded("xaerominimap") || loader.isModLoaded("xaeroworldmap")) {
			rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_WAYPOINT_XAERO, config::waypointXaero, config::setWaypointXaero));
		}
		rows.add(new HubSettingRow.Cycle<>(
			EMUtilsTexts.OPTION_WAYPOINT_MAX_DISTANCE,
			config::waypointMaxDistance,
			config::setWaypointMaxDistance,
			() -> {
				List<Integer> choices = EMUtilsConfig.WAYPOINT_MAX_DISTANCES;
				int index = choices.indexOf(config.waypointMaxDistance());
				return choices.get((index + 1) % choices.size());
			},
			() -> waypointMaxDistanceLabel(config.waypointMaxDistance()),
			EMUtilsConfig.WAYPOINT_MAX_DISTANCES,
			HubSettingsRegistry::waypointMaxDistanceLabel
		));
		return rows;
	}

	private static Component waypointMaxDistanceLabel(int blocks) {
		return blocks <= 0
			? Component.translatable(EMUtilsTexts.OPTION_WAYPOINT_MAX_DISTANCE_UNLIMITED)
			: Component.literal(Integer.toString(blocks)).append(Component.translatable(EMUtilsTexts.SUFFIX_BLOCKS));
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
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SCREENSHOT_METADATA, config::screenshotMetadataSaver, config::setScreenshotMetadataSaver));
		return rows;
	}

	private static List<HubSettingRow> screenshotGalleryRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(HubSettingRow.Cycle.ofEnum(
			EMUtilsTexts.OPTION_SCREENSHOT_SORT,
			config::screenshotGallerySort,
			config::setScreenshotGallerySort,
			ScreenshotGallerySort.class,
			value -> Component.translatable(value.labelKey())
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

	private static List<HubSettingRow> packManagerRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(
			EMUtilsTexts.OPTION_PACK_MANAGER_REPLACE_RESOURCE_PACKS_BUTTON,
			config::packManagerReplaceResourcePacksButton,
			config::setPackManagerReplaceResourcePacksButton
		));
		return rows;
	}

	private static List<HubSettingRow> hudRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_OVERLAY, config::hudOverlay, config::setHudOverlay));
		// Tabs, since the overlay has grown many lines (#48).
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_HUD_SECTION_GENERAL));
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
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_HUD_TEXT_SHADOW, config::hudTextShadow, config::setHudTextShadow, HudTextShadow.class, mode -> Component.translatable(mode.labelKey())));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_HIDE_WITH_DEBUG, config::hudHideWithDebug, config::setHudHideWithDebug));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_HIDE_IN_CONTAINERS, config::hudHideInContainers, config::setHudHideInContainers));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_HUD_SECTION_WORLD));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_COORDINATES, config::hudShowCoordinates, config::setHudShowCoordinates));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_NETHER_COORDINATES, config::hudShowNetherCoordinates, config::setHudShowNetherCoordinates));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_TARGET_BLOCK, config::hudShowTargetBlock, config::setHudShowTargetBlock));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_DIMENSION, config::hudShowDimension, config::setHudShowDimension));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_CHUNK_REGION, config::hudShowChunkRegion, config::setHudShowChunkRegion));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_SLIME_CHUNK, config::hudShowSlimeChunk, config::setHudShowSlimeChunk));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_BIOME, config::hudShowBiome, config::setHudShowBiome));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_FACING, config::hudShowFacing, config::setHudShowFacing));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_SPEED, config::hudShowSpeed, config::setHudShowSpeed));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_HUD_SECTION_PERFORMANCE));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_PING, config::hudShowPing, config::setHudShowPing));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_FPS, config::hudShowFps, config::setHudShowFps));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_MEMORY, config::hudShowMemory, config::setHudShowMemory));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_TPS, config::hudShowTps, config::setHudShowTps));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_TPS_COLORS, config::hudTpsColors, config::setHudTpsColors));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_TPS_COMPACT, config::hudTpsCompact, config::setHudTpsCompact));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_HUD_SECTION_TIME));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_SERVER_TIME, config::hudShowServerTime, config::setHudShowServerTime));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_DAY_NIGHT, config::hudShowDayNight, config::setHudShowDayNight));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_REAL_TIME, config::hudShowRealTime, config::setHudShowRealTime));
		return rows;
	}

	/** Look-At Info (#45), with a tab for its general look and one each for the block and entity lines. */
	private static List<HubSettingRow> lookAtInfoRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO, config::lookAtInfo, config::setLookAtInfo));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_HUD_SECTION_GENERAL));
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
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_SHOW_ID, config::lookAtInfoShowId, config::setLookAtInfoShowId));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_SHOW_ICONS, config::lookAtInfoShowIcons, config::setLookAtInfoShowIcons));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_HUD_TEXT_SHADOW, config::lookAtInfoTextShadow, config::setLookAtInfoTextShadow, HudTextShadow.class, mode -> Component.translatable(mode.labelKey())));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_HIDE_WITH_DEBUG, config::lookAtInfoHideWithDebug, config::setLookAtInfoHideWithDebug));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_HIDE_IN_CONTAINERS, config::lookAtInfoHideInContainers, config::setLookAtInfoHideInContainers));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_LOOK_AT_SECTION_BLOCK));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_BLOCKS, config::lookAtInfoBlocks, config::setLookAtInfoBlocks));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_POSITION, config::lookAtInfoPosition, config::setLookAtInfoPosition));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_HARDNESS, config::lookAtInfoHardness, config::setLookAtInfoHardness));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_BREAK_TIME, config::lookAtInfoBreakTime, config::setLookAtInfoBreakTime));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_TOOL, config::lookAtInfoTool, config::setLookAtInfoTool));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_HARVEST, config::lookAtInfoHarvest, config::setLookAtInfoHarvest));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_LOOK_AT_SECTION_ENTITY));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_ENTITIES, config::lookAtInfoEntities, config::setLookAtInfoEntities));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_HEALTH, config::lookAtInfoHealth, config::setLookAtInfoHealth));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_ARMOR, config::lookAtInfoArmor, config::setLookAtInfoArmor));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_LOOK_AT_INFO_EFFECTS, config::lookAtInfoEffects, config::setLookAtInfoEffects));
		return rows;
	}

	/** Minimap (#212): how the map looks, then what it shows. */
	private static List<HubSettingRow> minimapRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_MINIMAP, config::minimap, config::setMinimap));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_MINIMAP_SECTION_MAP));
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
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_MINIMAP_SHAPE, config::minimapShape, config::setMinimapShape, MinimapShape.class, shape -> Component.translatable(shape.labelKey())));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_MINIMAP_ROTATE, config::minimapRotate, config::setMinimapRotate));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_MINIMAP_ZOOM, config::minimapZoom, config::setMinimapZoom, MinimapZoom.class, zoom -> Component.literal(zoom.label())));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_MAP_CAVES, config::mapCaves, config::setMapCaves));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_MINIMAP_SECTION_SHOW));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_MINIMAP_COORDINATES, config::minimapCoordinates, config::setMinimapCoordinates));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_MINIMAP_WAYPOINTS, config::minimapWaypoints, config::setMinimapWaypoints));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_MINIMAP_WAYPOINTS_PINNED, config::minimapWaypointsPinned, config::setMinimapWaypointsPinned));
		return rows;
	}

	/** Keystrokes (#43): which keys show, then how they look. */
	private static List<HubSettingRow> keystrokesRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_KEYSTROKES, config::keystrokes, config::setKeystrokes));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_KEYSTROKES_SECTION_KEYS));
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
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_KEYSTROKES_MOUSE, config::keystrokesMouse, config::setKeystrokesMouse));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_KEYSTROKES_CPS, config::keystrokesCps, config::setKeystrokesCps));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_KEYSTROKES_SPACE, config::keystrokesSpace, config::setKeystrokesSpace));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_KEYSTROKES_SNEAK, config::keystrokesSneak, config::setKeystrokesSneak));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_KEYSTROKES_HIDE_IN_CONTAINERS, config::keystrokesHideInContainers, config::setKeystrokesHideInContainers));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_KEYSTROKES_SECTION_STYLE));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_KEYSTROKES_STYLE, config::keystrokesStyle, config::setKeystrokesStyle, KeystrokesStyle.class, style -> Component.translatable(style.labelKey())));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_KEYSTROKES_MENU_ACCENT, config::keystrokesMenuAccent, config::setKeystrokesMenuAccent));
		rows.add(new HubSettingRow.Swatches(EMUtilsTexts.OPTION_KEYSTROKES_PRESSED_COLOR, ACCENT_PRESETS, config::keystrokesPressedColor, config::setKeystrokesPressedColor, () -> !config.keystrokesMenuAccent()));
		return rows;
	}

	/** Armor Status (#44): which items show, then how durability shows and warns when low. */
	private static List<HubSettingRow> armorStatusRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS, config::armorStatus, config::setArmorStatus));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_ARMOR_STATUS_SECTION_ITEMS));
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
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_HELMET, config::armorStatusHelmet, config::setArmorStatusHelmet));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_CHESTPLATE, config::armorStatusChestplate, config::setArmorStatusChestplate));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_LEGGINGS, config::armorStatusLeggings, config::setArmorStatusLeggings));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_BOOTS, config::armorStatusBoots, config::setArmorStatusBoots));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_MAIN_HAND, config::armorStatusMainHand, config::setArmorStatusMainHand));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_OFF_HAND, config::armorStatusOffHand, config::setArmorStatusOffHand));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_FIREWORKS, config::armorStatusFireworks, config::setArmorStatusFireworks));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_HIDE_IN_CONTAINERS, config::armorStatusHideInContainers, config::setArmorStatusHideInContainers));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_ARMOR_STATUS_SECTION_DISPLAY));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_ARMOR_STATUS_DISPLAY, config::armorStatusDisplay, config::setArmorStatusDisplay, ArmorStatusDisplay.class, display -> Component.translatable(display.labelKey())));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_BAR, config::armorStatusBar, config::setArmorStatusBar));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.OPTION_ARMOR_STATUS_LOW_PERCENT, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.ARMOR_STATUS_LOW_PERCENT_MIN, EMUtilsConfig.ARMOR_STATUS_LOW_PERCENT_MAX, config::armorStatusLowPercent, config::setArmorStatusLowPercent));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_FLASH, config::armorStatusFlash, config::setArmorStatusFlash));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ARMOR_STATUS_SOUND, config::armorStatusSound, config::setArmorStatusSound));
		return rows;
	}

	/** Custom Scoreboard (#169): how the sidebar looks, then what it shows. */
	private static List<HubSettingRow> scoreboardRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SCOREBOARD, config::scoreboard, config::setScoreboard));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_SCOREBOARD_SECTION_LOOK));
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
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_SCOREBOARD_STYLE, config::scoreboardStyle, config::setScoreboardStyle, HudStyle.class, style -> Component.translatable(style.labelKey())));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_SCOREBOARD_FONT, config::scoreboardFont, config::setScoreboardFont, HudFont.class, font -> Component.translatable(font.labelKey())));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_HUD_TEXT_SHADOW, config::scoreboardTextShadow, config::setScoreboardTextShadow, HudTextShadow.class, mode -> Component.translatable(mode.labelKey())));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_SCOREBOARD_SECTION_CONTENT));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SCOREBOARD_SHOW_NUMBERS, config::scoreboardShowNumbers, config::setScoreboardShowNumbers));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_SCOREBOARD_TITLE_ALIGNMENT, config::scoreboardTitleAlignment, config::setScoreboardTitleAlignment, ScoreboardTitleAlignment.class, alignment -> Component.translatable(alignment.labelKey())));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SCOREBOARD_TITLE_BOLD, config::scoreboardTitleBold, config::setScoreboardTitleBold));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HUD_HIDE_WITH_DEBUG, config::scoreboardHideWithDebug, config::setScoreboardHideWithDebug));
		return rows;
	}

	/** Custom Tab List (#170): how it looks, which players it lists and how, and how ping shows. */
	private static List<HubSettingRow> tabListRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TAB_LIST, config::tabList, config::setTabList));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_TAB_LIST_SECTION_LOOK));
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
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_TAB_LIST_STYLE, config::tabListStyle, config::setTabListStyle, HudStyle.class, style -> Component.translatable(style.labelKey())));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_TAB_LIST_FONT, config::tabListFont, config::setTabListFont, HudFont.class, font -> Component.translatable(font.labelKey())));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_HUD_TEXT_SHADOW, config::tabListTextShadow, config::setTabListTextShadow, HudTextShadow.class, mode -> Component.translatable(mode.labelKey())));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TAB_LIST_ANIMATION, config::tabListAnimation, config::setTabListAnimation));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TAB_LIST_CENTERED, config::tabListCentered, config::setTabListCentered));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_TAB_LIST_SECTION_PLAYERS));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_TAB_LIST_SORT, config::tabListSort, config::setTabListSort, TabListSort.class, sort -> Component.translatable(sort.labelKey())));
		List<Integer> columnChoices = new ArrayList<>();
		for (int columns = 0; columns <= EMUtilsConfig.TAB_LIST_MAX_COLUMNS_MAX; columns++) {
			columnChoices.add(columns);
		}
		Function<Integer, Component> columnLabel = columns -> columns == 0 ? Component.translatable(EMUtilsTexts.OPTION_TAB_LIST_COLUMNS_AUTO) : Component.literal(Integer.toString(columns));
		rows.add(new HubSettingRow.Cycle<>(
			EMUtilsTexts.OPTION_TAB_LIST_COLUMNS,
			config::tabListMaxColumns,
			config::setTabListMaxColumns,
			() -> (config.tabListMaxColumns() + 1) % (EMUtilsConfig.TAB_LIST_MAX_COLUMNS_MAX + 1),
			() -> columnLabel.apply(config.tabListMaxColumns()),
			columnChoices,
			columnLabel
		));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.OPTION_TAB_LIST_ROW_HEIGHT, EMUtilsTexts.SUFFIX_PIXELS, EMUtilsConfig.TAB_LIST_ROW_HEIGHT_MIN, EMUtilsConfig.TAB_LIST_ROW_HEIGHT_MAX, config::tabListRowHeight, config::setTabListRowHeight));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TAB_LIST_HEADS, config::tabListHeads, config::setTabListHeads));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TAB_LIST_HIGHLIGHT_SELF, config::tabListHighlightSelf, config::setTabListHighlightSelf));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_TAB_LIST_SECTION_PING));
		rows.add(HubSettingRow.Cycle.ofEnum(EMUtilsTexts.OPTION_TAB_LIST_PING, config::tabListPing, config::setTabListPing, TabListPing.class, ping -> Component.translatable(ping.labelKey())));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TAB_LIST_PING_COLORS, config::tabListPingColors, config::setTabListPingColors));
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
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_ZOOM_SCALE_SENSITIVITY, config::zoomScaleSensitivity, config::setZoomScaleSensitivity));
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
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_LOW_SHIELD, config::tweakLowShield, config::setTweakLowShield));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.OPTION_TWEAK_LOW_SHIELD_AMOUNT, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.LOW_SHIELD_AMOUNT_MIN, EMUtilsConfig.LOW_SHIELD_AMOUNT_MAX, config::lowShieldAmount, config::setLowShieldAmount));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_LOW_TOTEM, config::tweakLowTotem, config::setTweakLowTotem));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.OPTION_TWEAK_LOW_TOTEM_AMOUNT, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.LOW_TOTEM_AMOUNT_MIN, EMUtilsConfig.LOW_TOTEM_AMOUNT_MAX, config::lowTotemAmount, config::setLowTotemAmount));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_SMALL_TOTEM, config::tweakSmallTotem, config::setTweakSmallTotem));
		rows.add(new HubSettingRow.Slider(EMUtilsTexts.OPTION_TWEAK_SMALL_TOTEM_SIZE, EMUtilsTexts.SUFFIX_PERCENT, EMUtilsConfig.SMALL_TOTEM_SIZE_MIN, EMUtilsConfig.SMALL_TOTEM_SIZE_MAX, config::smallTotemSize, config::setSmallTotemSize));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_NAUSEA, config::tweakNoNausea, config::setTweakNoNausea));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_SPYGLASS_OVERLAY, config::tweakNoSpyglassOverlay, config::setTweakNoSpyglassOverlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_PUMPKIN_OVERLAY, config::tweakNoPumpkinOverlay, config::setTweakNoPumpkinOverlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_FOG, config::tweakNoFog, config::setTweakNoFog));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_UNDERWATER, config::tweakClearUnderwater, config::setTweakClearUnderwater));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_LAVA, config::tweakClearLava, config::setTweakClearLava));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_ENVIRONMENT_FOG, config::tweakNoEnvironmentFog, config::setTweakNoEnvironmentFog));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_NETHER_PARTICLES, config::tweakNoNetherParticles, config::setTweakNoNetherParticles));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_FALLING_LEAF_PARTICLES, config::tweakNoFallingLeafParticles, config::setTweakNoFallingLeafParticles));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_NO_HURT_CAM, config::tweakNoHurtCam, config::setTweakNoHurtCam));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_FAST_PLACE, config::tweakFastPlace, config::setTweakFastPlace));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_FAST_USE, config::tweakFastUse, config::setTweakFastUse));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_ANTI_DURABILITY_BREAK, config::tweakAntiDurabilityBreak, config::setTweakAntiDurabilityBreak));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_SAFE_WALK, config::tweakSafeWalk, config::setTweakSafeWalk));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_FREELOOK, config::tweakFreelook, config::setTweakFreelook));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_FREE_CAMERA, config::tweakFreeCamera, config::setTweakFreeCamera));
		rows.add(HubSettingRow.Cycle.ofEnum(
			EMUtilsTexts.OPTION_FREE_CAMERA_HUD_MODE,
			config::freeCameraHudMode,
			config::setFreeCameraHudMode,
			FreeCameraHudMode.class,
			value -> Component.translatable(value.labelKey())
		));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_FREE_CAMERA_BOOST_MULTIPLIER,
			EMUtilsTexts.SUFFIX_MULTIPLIER,
			EMUtilsConfig.FREE_CAMERA_BOOST_MULTIPLIER_MIN,
			EMUtilsConfig.FREE_CAMERA_BOOST_MULTIPLIER_MAX,
			config::freeCameraBoostMultiplier,
			config::setFreeCameraBoostMultiplier
		));
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
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER_HIDE_THUNDER_FLASH, config::tweakClearWeatherHideThunderFlash, config::setTweakClearWeatherHideThunderFlash));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_TWEAK_CLEAR_WEATHER_HIDE_LIGHTNING_BOLTS, config::tweakClearWeatherHideLightningBolts, config::setTweakClearWeatherHideLightningBolts));
		return rows;
	}

	private static List<HubSettingRow> autoToolRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_AUTO_TOOL, config::autoToolEnabled, config::setAutoToolEnabled));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_AUTO_TOOL_SECTION_GENERAL));
		rows.add(HubSettingRow.Cycle.ofEnum(
			EMUtilsTexts.OPTION_AUTO_TOOL_MODE,
			config::autoToolMode,
			config::setAutoToolMode,
			AutoToolMode.class,
			value -> Component.translatable(value.labelKey())
		));
		rows.add(new HubSettingRow.Toggle(
			EMUtilsTexts.OPTION_AUTO_TOOL_RETURN_TO_PREVIOUS_ITEM,
			config::autoToolReturnToPreviousItem,
			config::setAutoToolReturnToPreviousItem
		));
		// The enchantment priority list (#50): between tools that can mine a block, the first enchantment
		// that tells them apart decides. Moving an entry rebuilds the rows in the new order.
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_AUTO_TOOL_SECTION_ENCHANTMENTS));
		List<AutoToolEnchantment> order = config.autoToolEnchantmentOrder();
		for (int i = 0; i < order.size(); i++) {
			AutoToolEnchantment enchantment = order.get(i);
			rows.add(new HubSettingRow.Ranked(
				enchantment.labelKey(),
				i + 1,
				() -> config.autoToolEnchantmentEnabled(enchantment),
				enabled -> config.setAutoToolEnchantmentEnabled(enchantment, enabled),
				i == 0 ? null : () -> {
					config.moveAutoToolEnchantment(enchantment, -1);
					refresh.run();
				},
				i == order.size() - 1 ? null : () -> {
					config.moveAutoToolEnchantment(enchantment, 1);
					refresh.run();
				}
			));
		}
		// Hotbar slots Auto Tool may pick tools from, so a slot kept for a weapon or a block stays as is.
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_AUTO_TOOL_SECTION_HOTBAR));
		for (int slot = 0; slot < EMUtilsConfig.HOTBAR_SLOT_MAX; slot++) {
			int index = slot;
			rows.add(new HubSettingRow.Toggle(
				EMUtilsTexts.OPTION_AUTO_TOOL_HOTBAR_SLOT_PREFIX + (slot + 1),
				() -> config.autoToolHotbarSlot(index),
				enabled -> config.setAutoToolHotbarSlot(index, enabled)
			));
		}
		return rows;
	}

	private static List<HubSettingRow> autoFlightRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(
			EMUtilsTexts.OPTION_TWEAK_AUTO_SWITCH_ELYTRA,
			config::tweakAutoSwitchElytra,
			config::setTweakAutoSwitchElytra
		));
		rows.add(new HubSettingRow.Toggle(
			EMUtilsTexts.OPTION_TWEAK_AUTO_SWITCH_ROCKETS,
			config::tweakAutoSwitchRockets,
			config::setTweakAutoSwitchRockets
		));
		rows.add(new HubSettingRow.Toggle(
			EMUtilsTexts.OPTION_AUTO_FLIGHT_DOUBLE_JUMP,
			config::autoFlightDoubleJump,
			config::setAutoFlightDoubleJump
		));
		rows.add(new HubSettingRow.Toggle(
			EMUtilsTexts.OPTION_AUTO_FLIGHT_IGNORE_SHORT_FALLS,
			config::autoFlightIgnoreShortFalls,
			config::setAutoFlightIgnoreShortFalls
		));
		rows.add(new HubSettingRow.Slider(
			EMUtilsTexts.OPTION_AUTO_SWITCH_ROCKETS_HOTBAR_SLOT,
			"",
			EMUtilsConfig.HOTBAR_SLOT_MIN,
			EMUtilsConfig.HOTBAR_SLOT_MAX,
			config::autoSwitchRocketsHotbarSlot,
			config::setAutoSwitchRocketsHotbarSlot
		));
		return rows;
	}

	private static List<HubSettingRow> capesRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_CUSTOM_CAPES, config::customCapes, config::setCustomCapes));
		rows.add(divider());
		// The priority list (#52): the first entry with a cape for a player wins, Minecraft being the
		// official cape. Moving an entry rebuilds the rows in the new order.
		List<CapeSource> order = config.capeOrder();
		for (int i = 0; i < order.size(); i++) {
			CapeSource source = order.get(i);
			rows.add(new HubSettingRow.Ranked(
				source.labelKey(),
				i + 1,
				() -> source.enabled(config),
				enabled -> source.setEnabled(config, enabled),
				i == 0 ? null : () -> {
					config.moveCapeSource(source, -1);
					refresh.run();
				},
				i == order.size() - 1 ? null : () -> {
					config.moveCapeSource(source, 1);
					refresh.run();
				}
			));
		}
		return rows;
	}

	private static List<HubSettingRow> inventoryRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_INVENTORY_TOOLS, config::inventoryToolsEnabled, config::setInventoryToolsEnabled));
		// Tabs, since the tools have grown many settings (#46).
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_INVENTORY_SECTION_SLOTS));
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
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_INVENTORY_SECTION_MOVING));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HOVER_TRANSFER, config::hoverTransferEnabled, config::setHoverTransferEnabled));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_HOVER_TRANSFER_GLOBAL, config::hoverTransferGlobal, config::setHoverTransferGlobal));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SORT_BUTTONS, config::sortButtonsEnabled, config::setSortButtonsEnabled));
		rows.add(HubSettingRow.Cycle.ofEnum(
			EMUtilsTexts.OPTION_SORT_SPEED,
			config::sortSpeed,
			config::setSortSpeed,
			InventorySortSpeed.class,
			value -> Component.translatable(value.labelKey())
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_QUICK_STACK, config::quickStackEnabled, config::setQuickStackEnabled));
		rows.add(HubSettingRow.Cycle.ofEnum(
			EMUtilsTexts.OPTION_QUICK_STACK_SPEED,
			config::quickStackSpeed,
			config::setQuickStackSpeed,
			InventorySortSpeed.class,
			value -> Component.translatable(value.labelKey())
		));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_AUTO_REFILL, config::autoRefillEnabled, config::setAutoRefillEnabled));
		rows.add(divider());
		rows.add(new HubSettingRow.Action(
			Component.translatable("emutils.mass_drop.manage"),
			() -> {
				Minecraft client = Minecraft.getInstance();
				client.gui.setScreen(new MassDropItemsScreen(net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client)));
			},
			true
		));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_INVENTORY_SECTION_SEARCH));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_INVENTORY_SEARCH, config::inventorySearch, config::setInventorySearch));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_INVENTORY_SEARCH_DIM, config::inventorySearchDim, config::setInventorySearchDim));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_INVENTORY_SEARCH_SHULKERS, config::inventorySearchShulkers, config::setInventorySearchShulkers));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_INVENTORY_SEARCH_REMEMBER, config::inventorySearchRemember, config::setInventorySearchRemember));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_INVENTORY_SEARCH_CREATIVE, config::inventorySearchCreative, config::setInventorySearchCreative));
		rows.add(new HubSettingRow.Section(EMUtilsTexts.UI_INVENTORY_SECTION_MORE));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_INVENTORY_PREVIEW, config::inventoryPreviewEnabled, config::setInventoryPreviewEnabled));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_PRESERVE_CONTAINER_CURSOR, config::preserveContainerCursor, config::setPreserveContainerCursor));
		return rows;
	}

	private static List<HubSettingRow> spotifyRows(Runnable refresh) {
		EMUtilsConfig config = config();
		List<HubSettingRow> rows = new ArrayList<>();
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SPOTIFY, config::spotifyEnabled, config::setSpotifyEnabled));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SPOTIFY_PLAYER, config::spotifyPlayerEnabled, config::setSpotifyPlayerEnabled));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SPOTIFY_PLAYER_SCROLL_TITLES, config::spotifyPlayerScrollTitles, config::setSpotifyPlayerScrollTitles));
		rows.add(divider());
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SPOTIFY_HUD_OVERLAY, config::spotifyHudOverlay, config::setSpotifyHudOverlay));
		rows.add(new HubSettingRow.Toggle(EMUtilsTexts.OPTION_SPOTIFY_HUD_SCROLL_TITLES, config::spotifyHudScrollTitles, config::setSpotifyHudScrollTitles));
		return rows;
	}

	private static HubSettingRow divider() {
		return new HubSettingRow.Divider();
	}
}
