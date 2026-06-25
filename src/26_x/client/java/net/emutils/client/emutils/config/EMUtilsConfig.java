package net.emutils.client.emutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.emutils.client.emutils.chat.ChatFeaturesRefresher;
import net.emutils.client.emutils.capes.CapePreferredProvider;
import net.emutils.client.emutils.capes.CustomCapeManager;
import net.emutils.client.emutils.waypoint.WaypointCoordinateFormat;
import net.emhelpers.client.hud.HudOverlayAnchor;
import net.emhelpers.client.hud.layout.HudCustomLayoutEntry;
import net.emhelpers.client.hud.layout.HudElementId;
import net.emhelpers.client.hud.layout.HudLayoutConfig;
import net.emhelpers.client.hud.layout.HudLayoutManager;
import net.emhelpers.client.hud.layout.HudLayoutMode;
import net.emutils.client.emutils.inventory.BoundSlotColor;
import net.emutils.client.emutils.inventory.InventorySortSpeed;
import net.emutils.client.emutils.inventory.SlotLockColor;
import net.emutils.client.emutils.screenshot.ScreenshotGallerySort;
import net.emutils.client.emutils.tweaks.AutoToolMode;
import net.emutils.client.emutils.util.EMUtilsPaths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import org.jspecify.annotations.Nullable;

public final class EMUtilsConfig implements HudLayoutConfig {
	public static final int RECONNECT_DELAY_MIN = 5;
	public static final int RECONNECT_DELAY_MAX = 15;
	public static final int RECONNECT_MAX_TRIES_MIN = 3;
	public static final int RECONNECT_MAX_TRIES_MAX = 15;
	public static final int SCREENSHOT_MAX_COUNT_MIN = 50;
	public static final int SCREENSHOT_MAX_COUNT_MAX = 500;
	public static final int DEATH_WAYPOINT_OPACITY_MIN = 25;
	public static final int DEATH_WAYPOINT_OPACITY_MAX = 100;
	public static final int DEATH_WAYPOINT_SIZE_MIN = 25;
	public static final int DEATH_WAYPOINT_SIZE_MAX = 100;
	public static final int HUD_BACKGROUND_OPACITY_MIN = 25;
	public static final int HUD_BACKGROUND_OPACITY_MAX = 100;
	public static final int INVENTORY_PREVIEW_OPACITY_MIN = 25;
	public static final int INVENTORY_PREVIEW_OPACITY_MAX = 100;
	public static final int HUD_LAYOUT_SCALE_MIN = HudLayoutManager.LAYOUT_SCALE_MIN;
	public static final int HUD_LAYOUT_SCALE_MAX = HudLayoutManager.LAYOUT_SCALE_MAX;
	public static final int ZOOM_AMOUNT_MIN = 2;
	public static final int ZOOM_AMOUNT_MAX = 10;
	public static final int ZOOM_SCROLL_AMOUNT_MAX = 100;
	public static final int ZOOM_TRANSITION_SPEED_MIN = 4;
	public static final int ZOOM_TRANSITION_SPEED_MAX = 40;
	public static final int ZOOM_OUT_SPEED_MIN = 10;
	public static final int ZOOM_OUT_SPEED_MAX = 30;
	public static final int CHAT_MENTION_VOLUME_MIN = 0;
	public static final int CHAT_MENTION_VOLUME_MAX = 100;
	public static final int DUPLICATE_MESSAGE_WINDOW_MIN = 30;
	public static final int DUPLICATE_MESSAGE_WINDOW_MAX = 120;
	public static final int PACK_MANAGER_SEARCH_LIMIT_MIN = 5;
	public static final int PACK_MANAGER_SEARCH_LIMIT_MAX = 50;
	public static final int FULLBRIGHT_STRENGTH_MIN = 1;
	public static final int FULLBRIGHT_STRENGTH_MAX = 100;
	public static final int FREE_CAMERA_BOOST_MULTIPLIER_MIN = 1;
	public static final int FREE_CAMERA_BOOST_MULTIPLIER_MAX = 10;
	public static final int HOTBAR_SLOT_MIN = 1;
	public static final int HOTBAR_SLOT_MAX = 9;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int DEFAULT_DEATH_WAYPOINT_SIZE = 50;
	private static final float LEGACY_SIZE_REFERENCE_PERCENT = 15.0F;

	private Boolean autoReconnect = Boolean.TRUE;
	private Boolean autoReconnectUnlimitedTries = Boolean.FALSE;
	private Boolean screenshotHelper = Boolean.TRUE;
	private Boolean screenshotAutoCopy = Boolean.FALSE;
	private Boolean screenshotMetadataSaver = Boolean.TRUE;
	private Boolean screenshotGalleryDeleteConfirmation = Boolean.TRUE;
	private String screenshotGallerySort = ScreenshotGallerySort.NEWEST_FIRST.name();
	private Boolean copyChat = Boolean.TRUE;
	private Boolean copyChatFormatting = Boolean.FALSE;
	private Boolean copyChatFeedback = Boolean.TRUE;
	private Boolean chatTimestamps = Boolean.FALSE;
	private Boolean chatTimestamp24Hour = Boolean.TRUE;
	private Boolean smartChatFilters = Boolean.FALSE;
	private Boolean duplicateMessageTimeWindow = Boolean.FALSE;
	private Boolean chatMentionAlerts = Boolean.FALSE;
	private Boolean chatMentionHighlight = Boolean.FALSE;
	private Boolean commandShortcutsEnabled = Boolean.TRUE;
	private Integer chatMentionHighlightColor = 0xFF7289DA;
	private Integer chatMentionHighlightStyle = 0;
	private Integer chatMentionAlertVolume = 100;
	private Integer chatMentionAlertSound = 0;
	private Boolean hudOverlay = Boolean.FALSE;
	private String hudOverlayAnchor = HudOverlayAnchor.TOP_LEFT.name();
	private String hudLayoutMode = HudLayoutMode.ANCHOR.name();
	private Map<String, HudCustomLayoutEntry> hudCustomLayout = new LinkedHashMap<>();
	private Boolean hudShowCoordinates = Boolean.TRUE;
	private Boolean hudShowNetherCoordinates = Boolean.FALSE;
	private Boolean hudShowChunkRegion = Boolean.TRUE;
	private Boolean hudShowBiome = Boolean.TRUE;
	private Boolean hudShowPing = Boolean.TRUE;
	private Boolean hudShowFps = Boolean.TRUE;
	private Boolean hudShowMemory = Boolean.TRUE;
	private Boolean hudShowServerTime = Boolean.TRUE;
	private Boolean hudShowRealTime = Boolean.TRUE;
	private Boolean hudShowIcons = Boolean.TRUE;
	private Boolean hudHideWithDebug = Boolean.TRUE;
	private Boolean hudShowFacing = Boolean.TRUE;
	private Boolean foodHud = Boolean.TRUE;
	private Boolean foodHudSaturationOverlay = Boolean.TRUE;
	private Boolean foodHudHeldFoodOverlay = Boolean.TRUE;
	private Boolean foodHudOffhandOverlay = Boolean.TRUE;
	private Boolean foodHudExhaustionUnderlay = Boolean.TRUE;
	private Boolean foodHudTooltips = Boolean.TRUE;
	private Boolean foodHudTooltipAlways = Boolean.TRUE;
	private Boolean foodHudVanillaAnimations = Boolean.TRUE;
	private Boolean deathWaypoint = Boolean.TRUE;
	private Boolean deathWaypointAutoCopyCoords = Boolean.FALSE;
	private String deathWaypointCoordinateFormat = WaypointCoordinateFormat.PLAIN.name();
	private Integer reconnectDelaySeconds = 8;
	private Integer autoReconnectMaxTries = 5;
	private Integer screenshotGalleryMaxCount = 200;
	private Integer duplicateMessageWindowSeconds = DUPLICATE_MESSAGE_WINDOW_MIN;
	private Integer deathWaypointOpacity = 75;
	private Integer deathWaypointSize = DEFAULT_DEATH_WAYPOINT_SIZE;
	private Integer waypointDefaultDeathColor = 0xFFFF5555;
	private Integer waypointDefaultCustomColor = 0xFF55FF55;
	private Integer hudBackgroundOpacity = 100;
	private Integer hudScale = 100;
	private Boolean zoomEnabled = Boolean.TRUE;
	private Boolean zoomCinematicCamera = Boolean.TRUE;
	private Boolean zoomHideHand = Boolean.FALSE;
	private Boolean zoomSmoothTransition = Boolean.TRUE;
	private Boolean zoomHideHud = Boolean.FALSE;
	private Boolean tweakFullbright = Boolean.FALSE;
	private Integer tweakFullbrightStrength = 100;
	private Boolean tweakNoFog = Boolean.FALSE;
	private Boolean tweakClearUnderwater = Boolean.TRUE;
	private Boolean tweakClearLava = Boolean.TRUE;
	private Boolean tweakNoEnvironmentFog = Boolean.TRUE;
	private Boolean tweakNoNetherParticles = Boolean.FALSE;
	@Deprecated
	private Boolean tweakNoCaveFog;
	@Deprecated
	private Boolean tweakNoNetherFog;
	@Deprecated
	private Boolean tweakNoEndFog;
	@Deprecated
	private Boolean tweakNoRainFog;
	@Deprecated
	private Boolean tweakNoWaterFog;
	@Deprecated
	private Boolean tweakNoLavaFog;
	@Deprecated
	private Boolean tweakClearFluidOverlay;
	private Boolean tweakNoHurtCam = Boolean.FALSE;
	private Boolean tweakFreelook = Boolean.FALSE;
	private Boolean beaconRadiusOutline = Boolean.FALSE;
	private Boolean lightLevelOverlay = Boolean.FALSE;
	private Boolean tweakShulkerTooltipPreview = Boolean.TRUE;
	private Boolean tweakBundleTooltipPreview = Boolean.TRUE;
	private Boolean tweakClearWeather = Boolean.FALSE;
	private Boolean tweakClearWeatherHideRain = Boolean.TRUE;
	private Boolean tweakClearWeatherHideSnow = Boolean.TRUE;
	private Boolean tweakClearWeatherHideRainEffects = Boolean.TRUE;
	private Boolean tweakNoFireOverlay = Boolean.FALSE;
	private Boolean tweakLowFireOverlay = Boolean.FALSE;
	private Boolean tweakNoNausea = Boolean.FALSE;
	private Boolean tweakNoSpyglassOverlay = Boolean.FALSE;
	private Boolean tweakFastPlace = Boolean.FALSE;
	private Boolean tweakFastUse = Boolean.FALSE;
	private Boolean tweakAntiDurabilityBreak = Boolean.FALSE;
	private Boolean tweakSafeWalk = Boolean.FALSE;
	private Boolean tweakPlaceBelow = Boolean.FALSE;
	private Boolean tweakLockedYPlacement = Boolean.FALSE;
	private Boolean tweakFreeCamera = Boolean.FALSE;
	private Integer freeCameraBoostMultiplier = 3;
	private Boolean autoFlightGearEnabled;
	private Boolean tweakAutoSwitchElytra = Boolean.FALSE;
	private Boolean tweakAutoSwitchRockets = Boolean.FALSE;
	private Boolean autoFlightDoubleJump = Boolean.FALSE;
	private Boolean autoFlightIgnoreShortFalls = Boolean.TRUE;
	private Integer autoSwitchRocketsHotbarSlot = 9;
	private Boolean autoToolEnabled = Boolean.FALSE;
	private String autoToolMode = AutoToolMode.LEGIT.name();
	private Boolean autoToolReturnToPreviousItem = Boolean.FALSE;
	private Boolean tweakOwnNametag = Boolean.FALSE;
	private Boolean packManagerEnabled = Boolean.TRUE;
	private Boolean packManagerShowShadersWithoutIris = Boolean.TRUE;
	private Boolean customCapes = Boolean.TRUE;
	private Boolean capeOptifine = Boolean.TRUE;
	private Boolean capeLabyMod = Boolean.TRUE;
	private Boolean capeMinecraftCapes = Boolean.TRUE;
	private Boolean capeCosmetica = Boolean.TRUE;
	private Boolean capeCloaksPlus = Boolean.TRUE;
	private String capePreferredProvider = CapePreferredProvider.AUTO.name();
	private Boolean spotifyPlayerEnabled = Boolean.FALSE;
	private Boolean spotifyHudOverlay = Boolean.FALSE;
	private String spotifyHudAnchor = HudOverlayAnchor.BOTTOM_RIGHT.name();
	private Integer spotifyHudBackgroundOpacity = 100;
	private Integer spotifyHudScale = 100;
	private Boolean inventoryToolsEnabled = Boolean.TRUE;
	private Boolean slotLockingEnabled = Boolean.TRUE;
	private Boolean slotBindingEnabled = Boolean.TRUE;
	private Boolean slotBindingShowIcons = Boolean.TRUE;
	private Boolean slotBindingLockBoundSlots = Boolean.TRUE;
	private Boolean hoverTransferEnabled = Boolean.TRUE;
	private Boolean hoverTransferGlobal = Boolean.FALSE;
	private Boolean sortButtonsEnabled = Boolean.TRUE;
	private String sortSpeed = InventorySortSpeed.NORMAL.name();
	private Boolean quickStackEnabled = Boolean.TRUE;
	private String quickStackSpeed = InventorySortSpeed.NORMAL.name();
	private Boolean inventoryPreviewEnabled = Boolean.FALSE;
	private Boolean preserveContainerCursor = Boolean.TRUE;
	private String slotLockColor = SlotLockColor.RED.name();
	private String boundSlotColor = BoundSlotColor.GRAY.name();
	private Integer slotLockArgb;
	private Integer boundSlotArgb;
	private Integer inventoryPreviewOpacity = 75;
	private Integer zoomAmount = 4;
	private Integer zoomTransitionSpeed = 24;
	private Integer zoomOutSpeedMultiplier = 18;
	private Integer packManagerSearchLimit = 20;

	public static EMUtilsConfig load() {
		EMUtilsConfig config = null;

		if (Files.exists(EMUtilsPaths.configFile())) {
			try (Reader reader = Files.newBufferedReader(EMUtilsPaths.configFile())) {
				config = GSON.fromJson(reader, EMUtilsConfig.class);
			} catch (IOException | JsonParseException | IllegalStateException ignored) {
			}
		}

		if (config == null) {
			config = new EMUtilsConfig();
		}

		config.applyDefaults();
		config.save();
		return config;
	}

	public boolean autoReconnect() {
		return autoReconnect == null || autoReconnect;
	}

	public void setAutoReconnect(boolean enabled) {
		autoReconnect = enabled;
		save();
	}

	public boolean autoReconnectUnlimitedTries() {
		return autoReconnectUnlimitedTries != null && autoReconnectUnlimitedTries;
	}

	public void setAutoReconnectUnlimitedTries(boolean enabled) {
		autoReconnectUnlimitedTries = enabled;
		save();
	}

	public int autoReconnectMaxTries() {
		return clampReconnectMaxTries(autoReconnectMaxTries == null ? 5 : autoReconnectMaxTries);
	}

	public void setAutoReconnectMaxTries(int tries) {
		autoReconnectMaxTries = clampReconnectMaxTries(tries);
		save();
	}

	public boolean screenshotHelper() {
		return screenshotHelper == null || screenshotHelper;
	}

	public void setScreenshotHelper(boolean enabled) {
		screenshotHelper = enabled;
		save();
	}

	public boolean screenshotAutoCopy() {
		return screenshotAutoCopy != null && screenshotAutoCopy;
	}

	public void setScreenshotAutoCopy(boolean enabled) {
		screenshotAutoCopy = enabled;
		save();
	}

	public boolean screenshotMetadataSaver() {
		return screenshotMetadataSaver == null || screenshotMetadataSaver;
	}

	public void setScreenshotMetadataSaver(boolean enabled) {
		screenshotMetadataSaver = enabled;
		save();
	}

	public boolean screenshotGalleryDeleteConfirmation() {
		return screenshotGalleryDeleteConfirmation == null || screenshotGalleryDeleteConfirmation;
	}

	public void setScreenshotGalleryDeleteConfirmation(boolean enabled) {
		screenshotGalleryDeleteConfirmation = enabled;
		save();
	}

	public ScreenshotGallerySort screenshotGallerySort() {
		return ScreenshotGallerySort.fromName(screenshotGallerySort);
	}

	public void setScreenshotGallerySort(ScreenshotGallerySort sort) {
		screenshotGallerySort = (sort == null ? ScreenshotGallerySort.NEWEST_FIRST : sort).name();
		save();
	}

	public int screenshotGalleryMaxCount() {
		return clampScreenshotMaxCount(screenshotGalleryMaxCount == null ? 200 : screenshotGalleryMaxCount);
	}

	public void setScreenshotGalleryMaxCount(int maxCount) {
		screenshotGalleryMaxCount = clampScreenshotMaxCount(maxCount);
		save();
	}

	public boolean copyChat() {
		return copyChat == null || copyChat;
	}

	public void setCopyChat(boolean enabled) {
		copyChat = enabled;
		save();
	}

	public boolean copyChatFormatting() {
		return copyChatFormatting != null && copyChatFormatting;
	}

	public void setCopyChatFormatting(boolean enabled) {
		copyChatFormatting = enabled;
		save();
	}

	public boolean copyChatFeedback() {
		return copyChatFeedback == null || copyChatFeedback;
	}

	public void setCopyChatFeedback(boolean enabled) {
		copyChatFeedback = enabled;
		save();
	}

	public boolean chatTimestamps() {
		return chatTimestamps != null && chatTimestamps;
	}

	public void setChatTimestamps(boolean enabled) {
		chatTimestamps = enabled;
		save();
		ChatFeaturesRefresher.onTimestampSettingsChanged();
	}

	public boolean chatTimestamp24Hour() {
		return chatTimestamp24Hour == null || chatTimestamp24Hour;
	}

	public void setChatTimestamp24Hour(boolean enabled) {
		chatTimestamp24Hour = enabled;
		save();
		ChatFeaturesRefresher.onTimestampSettingsChanged();
	}

	public boolean smartChatFilters() {
		return smartChatFilters != null && smartChatFilters;
	}

	public void setSmartChatFilters(boolean enabled) {
		smartChatFilters = enabled;
		save();
	}

	public boolean duplicateMessageTimeWindow() {
		return duplicateMessageTimeWindow != null && duplicateMessageTimeWindow;
	}

	public void setDuplicateMessageTimeWindow(boolean enabled) {
		duplicateMessageTimeWindow = enabled;
		save();
	}

	public boolean chatMentionAlerts() {
		return chatMentionAlerts != null && chatMentionAlerts;
	}

	public void setChatMentionAlerts(boolean enabled) {
		chatMentionAlerts = enabled;
		save();
	}

	public boolean chatMentionHighlight() {
		return chatMentionHighlight != null && chatMentionHighlight;
	}

	public void setChatMentionHighlight(boolean enabled) {
		chatMentionHighlight = enabled;
		save();
		ChatFeaturesRefresher.onTimestampSettingsChanged();
	}

	public boolean commandShortcutsEnabled() {
		return commandShortcutsEnabled == null || commandShortcutsEnabled;
	}

	public void setCommandShortcutsEnabled(boolean enabled) {
		commandShortcutsEnabled = enabled;
		save();
	}

	public int chatMentionHighlightColor() {
		return chatMentionHighlightColor == null ? 0xFF7289DA : chatMentionHighlightColor;
	}

	public void setChatMentionHighlightColor(int color) {
		chatMentionHighlightColor = color;
		save();
		ChatFeaturesRefresher.onTimestampSettingsChanged();
	}

	public int chatMentionHighlightStyle() {
		return chatMentionHighlightStyle == null ? 0 : Math.max(0, Math.min(3, chatMentionHighlightStyle));
	}

	public void setChatMentionHighlightStyle(int style) {
		chatMentionHighlightStyle = Math.max(0, Math.min(3, style));
		save();
		ChatFeaturesRefresher.onTimestampSettingsChanged();
	}

	public int chatMentionAlertVolume() {
		return chatMentionAlertVolume == null ? 100 : Math.max(CHAT_MENTION_VOLUME_MIN, Math.min(CHAT_MENTION_VOLUME_MAX, chatMentionAlertVolume));
	}

	public void setChatMentionAlertVolume(int volume) {
		chatMentionAlertVolume = Math.max(CHAT_MENTION_VOLUME_MIN, Math.min(CHAT_MENTION_VOLUME_MAX, volume));
		save();
	}

	public int chatMentionAlertSound() {
		return chatMentionAlertSound == null ? 0 : Math.max(0, Math.min(6, chatMentionAlertSound));
	}

	public void setChatMentionAlertSound(int sound) {
		chatMentionAlertSound = Math.max(0, Math.min(6, sound));
		save();
	}

	public boolean hudOverlay() {
		return hudOverlay != null && hudOverlay;
	}

	public void setHudOverlay(boolean enabled) {
		hudOverlay = enabled;
		save();
	}

	public HudOverlayAnchor hudOverlayAnchor() {
		return HudOverlayAnchor.fromName(hudOverlayAnchor);
	}

	public void setHudOverlayAnchor(HudOverlayAnchor anchor) {
		hudOverlayAnchor = (anchor == null ? HudOverlayAnchor.TOP_LEFT : anchor).name();
		save();
	}

	public HudLayoutMode hudLayoutMode() {
		return HudLayoutMode.fromName(hudLayoutMode);
	}

	public void setHudLayoutMode(HudLayoutMode mode) {
		hudLayoutMode = (mode == null ? HudLayoutMode.ANCHOR : mode).name();
		save();
	}

	@Nullable
	public HudCustomLayoutEntry hudCustomLayoutEntry(HudElementId id) {
		if (hudCustomLayout == null) {
			hudCustomLayout = new LinkedHashMap<>();
		}

		return hudCustomLayout.get(id.configKey());
	}

	public void setHudCustomLayoutEntry(HudElementId id, int x, int y, int scale, int opacity) {
		if (hudCustomLayout == null) {
			hudCustomLayout = new LinkedHashMap<>();
		}

		HudCustomLayoutEntry entry = hudCustomLayout.computeIfAbsent(id.configKey(), ignored -> new HudCustomLayoutEntry());
		entry.setX(x);
		entry.setY(y);
		entry.setScale(HudLayoutManager.clampLayoutScale(scale));
		entry.setOpacity(HudLayoutManager.clampLayoutOpacity(opacity));
	}

	public int legacyHudScale() {
		return hudScale == null ? 100 : hudScale;
	}

	public int legacySpotifyHudScale() {
		return spotifyHudScale == null ? 100 : spotifyHudScale;
	}

	public Map<String, HudCustomLayoutEntry> hudCustomLayout() {
		if (hudCustomLayout == null) {
			hudCustomLayout = new LinkedHashMap<>();
		}

		return hudCustomLayout;
	}

	public boolean hudShowCoordinates() {
		return hudShowCoordinates == null || hudShowCoordinates;
	}

	public void setHudShowCoordinates(boolean enabled) {
		hudShowCoordinates = enabled;
		save();
	}

	public boolean hudShowNetherCoordinates() {
		return hudShowNetherCoordinates != null && hudShowNetherCoordinates;
	}

	public void setHudShowNetherCoordinates(boolean enabled) {
		hudShowNetherCoordinates = enabled;
		save();
	}

	public boolean hudShowChunkRegion() {
		return hudShowChunkRegion == null || hudShowChunkRegion;
	}

	public void setHudShowChunkRegion(boolean enabled) {
		hudShowChunkRegion = enabled;
		save();
	}

	public boolean hudShowBiome() {
		return hudShowBiome == null || hudShowBiome;
	}

	public void setHudShowBiome(boolean enabled) {
		hudShowBiome = enabled;
		save();
	}

	public boolean hudShowPing() {
		return hudShowPing == null || hudShowPing;
	}

	public void setHudShowPing(boolean enabled) {
		hudShowPing = enabled;
		save();
	}

	public boolean hudShowFps() {
		return hudShowFps == null || hudShowFps;
	}

	public void setHudShowFps(boolean enabled) {
		hudShowFps = enabled;
		save();
	}

	public boolean hudShowMemory() {
		return hudShowMemory == null || hudShowMemory;
	}

	public void setHudShowMemory(boolean enabled) {
		hudShowMemory = enabled;
		save();
	}

	public boolean hudShowServerTime() {
		return hudShowServerTime == null || hudShowServerTime;
	}

	public void setHudShowServerTime(boolean enabled) {
		hudShowServerTime = enabled;
		save();
	}

	public boolean hudShowRealTime() {
		return hudShowRealTime == null || hudShowRealTime;
	}

	public void setHudShowRealTime(boolean enabled) {
		hudShowRealTime = enabled;
		save();
	}

	public boolean hudShowIcons() {
		return hudShowIcons == null || hudShowIcons;
	}

	public void setHudShowIcons(boolean enabled) {
		hudShowIcons = enabled;
		save();
	}

	public boolean hudHideWithDebug() {
		return hudHideWithDebug == null || hudHideWithDebug;
	}

	public void setHudHideWithDebug(boolean enabled) {
		hudHideWithDebug = enabled;
		save();
	}

	public boolean hudShowFacing() {
		return hudShowFacing == null || hudShowFacing;
	}

	public void setHudShowFacing(boolean enabled) {
		hudShowFacing = enabled;
		save();
	}

	public boolean foodHud() {
		return foodHud == null || foodHud;
	}

	public void setFoodHud(boolean enabled) {
		foodHud = enabled;
		save();
	}

	public boolean foodHudSaturationOverlay() {
		return foodHudSaturationOverlay == null || foodHudSaturationOverlay;
	}

	public void setFoodHudSaturationOverlay(boolean enabled) {
		foodHudSaturationOverlay = enabled;
		save();
	}

	public boolean foodHudHeldFoodOverlay() {
		return foodHudHeldFoodOverlay == null || foodHudHeldFoodOverlay;
	}

	public void setFoodHudHeldFoodOverlay(boolean enabled) {
		foodHudHeldFoodOverlay = enabled;
		save();
	}

	public boolean foodHudOffhandOverlay() {
		return foodHudOffhandOverlay == null || foodHudOffhandOverlay;
	}

	public void setFoodHudOffhandOverlay(boolean enabled) {
		foodHudOffhandOverlay = enabled;
		save();
	}

	public boolean foodHudExhaustionUnderlay() {
		return foodHudExhaustionUnderlay == null || foodHudExhaustionUnderlay;
	}

	public void setFoodHudExhaustionUnderlay(boolean enabled) {
		foodHudExhaustionUnderlay = enabled;
		save();
	}

	public boolean foodHudTooltips() {
		return foodHudTooltips == null || foodHudTooltips;
	}

	public void setFoodHudTooltips(boolean enabled) {
		foodHudTooltips = enabled;
		save();
	}

	public boolean foodHudTooltipAlways() {
		return foodHudTooltipAlways == null || foodHudTooltipAlways;
	}

	public void setFoodHudTooltipAlways(boolean enabled) {
		foodHudTooltipAlways = enabled;
		save();
	}

	public boolean foodHudVanillaAnimations() {
		return foodHudVanillaAnimations == null || foodHudVanillaAnimations;
	}

	public void setFoodHudVanillaAnimations(boolean enabled) {
		foodHudVanillaAnimations = enabled;
		save();
	}

	public int hudBackgroundOpacity() {
		return clampHudBackgroundOpacity(hudBackgroundOpacity == null ? 100 : hudBackgroundOpacity);
	}

	public void setHudBackgroundOpacity(int opacity) {
		hudBackgroundOpacity = clampHudBackgroundOpacity(opacity);
		save();
	}

	public boolean deathWaypoint() {
		return deathWaypoint == null || deathWaypoint;
	}

	public void setDeathWaypoint(boolean enabled) {
		deathWaypoint = enabled;
		save();
	}

	public boolean deathWaypointAutoCopyCoords() {
		return deathWaypointAutoCopyCoords != null && deathWaypointAutoCopyCoords;
	}

	public void setDeathWaypointAutoCopyCoords(boolean enabled) {
		deathWaypointAutoCopyCoords = enabled;
		save();
	}

	public WaypointCoordinateFormat deathWaypointCoordinateFormat() {
		return WaypointCoordinateFormat.fromName(deathWaypointCoordinateFormat);
	}

	public void setDeathWaypointCoordinateFormat(WaypointCoordinateFormat format) {
		deathWaypointCoordinateFormat = (format == null ? WaypointCoordinateFormat.PLAIN : format).name();
		save();
	}

	public int deathWaypointOpacity() {
		return clampOpacity(deathWaypointOpacity == null ? 75 : deathWaypointOpacity);
	}

	public void setDeathWaypointOpacity(int opacity) {
		deathWaypointOpacity = clampOpacity(opacity);
		save();
	}

	public int deathWaypointSize() {
		return clampSize(deathWaypointSize == null ? DEFAULT_DEATH_WAYPOINT_SIZE : deathWaypointSize);
	}

	public void setDeathWaypointSize(int sizePercent) {
		deathWaypointSize = clampSize(sizePercent);
		save();
	}

	public boolean zoomEnabled() {
		return zoomEnabled == null || zoomEnabled;
	}

	public void setZoomEnabled(boolean enabled) {
		zoomEnabled = enabled;
		save();
	}

	public boolean zoomCinematicCamera() {
		return zoomCinematicCamera == null || zoomCinematicCamera;
	}

	public void setZoomCinematicCamera(boolean enabled) {
		zoomCinematicCamera = enabled;
		save();
	}

	public int zoomAmount() {
		return clampZoomAmount(zoomAmount == null ? 4 : zoomAmount);
	}

	public void setZoomAmount(int amount) {
		zoomAmount = clampZoomAmount(amount);
		save();
	}

	public int zoomTransitionSpeed() {
		return clampZoomTransitionSpeed(zoomTransitionSpeed == null ? 24 : zoomTransitionSpeed);
	}

	public void setZoomTransitionSpeed(int speed) {
		zoomTransitionSpeed = clampZoomTransitionSpeed(speed);
		save();
	}

	public boolean zoomHideHand() {
		return zoomHideHand != null && zoomHideHand;
	}

	public void setZoomHideHand(boolean enabled) {
		zoomHideHand = enabled;
		save();
	}

	public boolean zoomSmoothTransition() {
		return zoomSmoothTransition == null || zoomSmoothTransition;
	}

	public void setZoomSmoothTransition(boolean enabled) {
		zoomSmoothTransition = enabled;
		save();
	}

	public boolean zoomHideHud() {
		return zoomHideHud != null && zoomHideHud;
	}

	public void setZoomHideHud(boolean enabled) {
		zoomHideHud = enabled;
		save();
	}

	public float zoomOutSpeedMultiplier() {
		return (zoomOutSpeedMultiplier == null ? 18 : Math.max(ZOOM_OUT_SPEED_MIN, Math.min(ZOOM_OUT_SPEED_MAX, zoomOutSpeedMultiplier))) / 10.0F;
	}

	public int zoomOutSpeedMultiplierRaw() {
		return zoomOutSpeedMultiplier == null ? 18 : Math.max(ZOOM_OUT_SPEED_MIN, Math.min(ZOOM_OUT_SPEED_MAX, zoomOutSpeedMultiplier));
	}

	public void setZoomOutSpeedMultiplier(int value) {
		zoomOutSpeedMultiplier = Math.max(ZOOM_OUT_SPEED_MIN, Math.min(ZOOM_OUT_SPEED_MAX, value));
		save();
	}

	public boolean tweakFullbright() {
		return tweakFullbright != null && tweakFullbright;
	}

	public void setTweakFullbright(boolean enabled) {
		tweakFullbright = enabled;
		save();
	}

	public int tweakFullbrightStrength() {
		return clamp(tweakFullbrightStrength == null ? FULLBRIGHT_STRENGTH_MAX : tweakFullbrightStrength, FULLBRIGHT_STRENGTH_MIN, FULLBRIGHT_STRENGTH_MAX);
	}

	public float tweakFullbrightStrengthFactor() {
		if (!tweakFullbright()) {
			return 0.0F;
		}
		float rawFactor = tweakFullbrightStrength() / 100.0F;
		return (float) Math.pow(rawFactor, 6.0);
	}

	public void setTweakFullbrightStrength(int value) {
		tweakFullbrightStrength = clamp(value, FULLBRIGHT_STRENGTH_MIN, FULLBRIGHT_STRENGTH_MAX);
		save();
	}

	public boolean tweakNoFog() {
		return tweakNoFog != null && tweakNoFog;
	}

	public void setTweakNoFog(boolean enabled) {
		tweakNoFog = enabled;
		save();
	}

	public boolean tweakClearUnderwater() {
		return tweakClearUnderwater == null || tweakClearUnderwater;
	}

	public void setTweakClearUnderwater(boolean enabled) {
		tweakClearUnderwater = enabled;
		save();
	}

	public boolean tweakClearLava() {
		return tweakClearLava == null || tweakClearLava;
	}

	public void setTweakClearLava(boolean enabled) {
		tweakClearLava = enabled;
		save();
	}

	public boolean tweakNoEnvironmentFog() {
		return tweakNoEnvironmentFog == null || tweakNoEnvironmentFog;
	}

	public void setTweakNoEnvironmentFog(boolean enabled) {
		tweakNoEnvironmentFog = enabled;
		save();
	}

	public boolean tweakNoNetherParticles() {
		return tweakNoNetherParticles != null && tweakNoNetherParticles;
	}

	public void setTweakNoNetherParticles(boolean enabled) {
		tweakNoNetherParticles = enabled;
		save();
	}

	public boolean tweakNoHurtCam() {
		return tweakNoHurtCam != null && tweakNoHurtCam;
	}

	public void setTweakNoHurtCam(boolean enabled) {
		tweakNoHurtCam = enabled;
		save();
	}

	public boolean tweakFreelook() {
		return tweakFreelook != null && tweakFreelook;
	}

	public void setTweakFreelook(boolean enabled) {
		tweakFreelook = enabled;
		save();
	}

	public boolean beaconRadiusOutline() {
		return beaconRadiusOutline != null && beaconRadiusOutline;
	}

	public void setBeaconRadiusOutline(boolean enabled) {
		beaconRadiusOutline = enabled;
		save();
	}

	public boolean lightLevelOverlay() {
		return lightLevelOverlay != null && lightLevelOverlay;
	}

	public void setLightLevelOverlay(boolean enabled) {
		lightLevelOverlay = enabled;
		save();
	}

	public boolean tweakShulkerTooltipPreview() {
		return tweakShulkerTooltipPreview == null || tweakShulkerTooltipPreview;
	}

	public void setTweakShulkerTooltipPreview(boolean enabled) {
		tweakShulkerTooltipPreview = enabled;
		save();
	}

	public boolean tweakBundleTooltipPreview() {
		return tweakBundleTooltipPreview == null || tweakBundleTooltipPreview;
	}

	public void setTweakBundleTooltipPreview(boolean enabled) {
		tweakBundleTooltipPreview = enabled;
		save();
	}

	public boolean tweakClearWeather() {
		return tweakClearWeather != null && tweakClearWeather;
	}

	public void setTweakClearWeather(boolean enabled) {
		tweakClearWeather = enabled;
		save();
	}

	public boolean tweakClearWeatherHideRain() {
		return tweakClearWeatherHideRain == null || tweakClearWeatherHideRain;
	}

	public void setTweakClearWeatherHideRain(boolean enabled) {
		tweakClearWeatherHideRain = enabled;
		save();
	}

	public boolean tweakClearWeatherHideSnow() {
		return tweakClearWeatherHideSnow == null || tweakClearWeatherHideSnow;
	}

	public void setTweakClearWeatherHideSnow(boolean enabled) {
		tweakClearWeatherHideSnow = enabled;
		save();
	}

	public boolean tweakClearWeatherHideRainEffects() {
		return tweakClearWeatherHideRainEffects == null || tweakClearWeatherHideRainEffects;
	}

	public void setTweakClearWeatherHideRainEffects(boolean enabled) {
		tweakClearWeatherHideRainEffects = enabled;
		save();
	}

	public boolean shouldHideClearWeatherRain() {
		return tweakClearWeather() && tweakClearWeatherHideRain();
	}

	public boolean shouldHideClearWeatherSnow() {
		return tweakClearWeather() && tweakClearWeatherHideSnow();
	}

	public boolean shouldHideClearWeatherRainEffects() {
		return tweakClearWeather() && (tweakClearWeatherHideRain() || tweakClearWeatherHideRainEffects());
	}

	public boolean tweakNoFireOverlay() {
		return tweakNoFireOverlay != null && tweakNoFireOverlay;
	}

	public void setTweakNoFireOverlay(boolean enabled) {
		tweakNoFireOverlay = enabled;
		save();
	}

	public boolean tweakLowFireOverlay() {
		return tweakLowFireOverlay != null && tweakLowFireOverlay;
	}

	public void setTweakLowFireOverlay(boolean enabled) {
		tweakLowFireOverlay = enabled;
		save();
	}

	public boolean tweakNoNausea() {
		return tweakNoNausea != null && tweakNoNausea;
	}

	public void setTweakNoNausea(boolean enabled) {
		tweakNoNausea = enabled;
		save();
	}

	public boolean tweakNoSpyglassOverlay() {
		return tweakNoSpyglassOverlay != null && tweakNoSpyglassOverlay;
	}

	public void setTweakNoSpyglassOverlay(boolean enabled) {
		tweakNoSpyglassOverlay = enabled;
		save();
	}

	public boolean tweakFastPlace() {
		return tweakFastPlace != null && tweakFastPlace;
	}

	public void setTweakFastPlace(boolean enabled) {
		tweakFastPlace = enabled;
		save();
	}

	public boolean tweakFastUse() {
		return tweakFastUse != null && tweakFastUse;
	}

	public void setTweakFastUse(boolean enabled) {
		tweakFastUse = enabled;
		save();
	}

	public boolean tweakAntiDurabilityBreak() {
		return tweakAntiDurabilityBreak != null && tweakAntiDurabilityBreak;
	}

	public void setTweakAntiDurabilityBreak(boolean enabled) {
		tweakAntiDurabilityBreak = enabled;
		save();
	}

	public boolean tweakSafeWalk() {
		return tweakSafeWalk != null && tweakSafeWalk;
	}

	public void setTweakSafeWalk(boolean enabled) {
		tweakSafeWalk = enabled;
		save();
	}

	public boolean tweakPlaceBelow() {
		return tweakPlaceBelow != null && tweakPlaceBelow;
	}

	public void setTweakPlaceBelow(boolean enabled) {
		tweakPlaceBelow = enabled;
		save();
	}

	public boolean tweakLockedYPlacement() {
		return tweakLockedYPlacement != null && tweakLockedYPlacement;
	}

	public void setTweakLockedYPlacement(boolean enabled) {
		tweakLockedYPlacement = enabled;
		if (!enabled && net.emutils.client.EMUtilsClient.tweaks() != null) {
			net.emutils.client.EMUtilsClient.tweaks().lockedYPlacement().reset();
		}
		save();
	}

	public boolean tweakFreeCamera() {
		return tweakFreeCamera != null && tweakFreeCamera;
	}

	public void setTweakFreeCamera(boolean enabled) {
		tweakFreeCamera = enabled;
		save();
	}

	public int freeCameraBoostMultiplier() {
		return clamp(freeCameraBoostMultiplier == null ? 3 : freeCameraBoostMultiplier, FREE_CAMERA_BOOST_MULTIPLIER_MIN, FREE_CAMERA_BOOST_MULTIPLIER_MAX);
	}

	public void setFreeCameraBoostMultiplier(int multiplier) {
		freeCameraBoostMultiplier = clamp(multiplier, FREE_CAMERA_BOOST_MULTIPLIER_MIN, FREE_CAMERA_BOOST_MULTIPLIER_MAX);
		save();
	}

	public boolean tweakAutoSwitchElytra() {
		return tweakAutoSwitchElytra != null && tweakAutoSwitchElytra;
	}

	public boolean autoFlightGearEnabled() {
		return autoFlightGearEnabled != null && autoFlightGearEnabled;
	}

	public void setAutoFlightGearEnabled(boolean enabled) {
		autoFlightGearEnabled = enabled;
		if (enabled && !tweakAutoSwitchElytra() && !tweakAutoSwitchRockets()) {
			tweakAutoSwitchElytra = Boolean.TRUE;
			tweakAutoSwitchRockets = Boolean.TRUE;
		}
		save();
	}

	public void setTweakAutoSwitchElytra(boolean enabled) {
		tweakAutoSwitchElytra = enabled;
		save();
	}

	public boolean tweakAutoSwitchRockets() {
		return tweakAutoSwitchRockets != null && tweakAutoSwitchRockets;
	}

	public void setTweakAutoSwitchRockets(boolean enabled) {
		tweakAutoSwitchRockets = enabled;
		save();
	}

	public boolean autoFlightIgnoreShortFalls() {
		return autoFlightIgnoreShortFalls == null || autoFlightIgnoreShortFalls;
	}

	public boolean autoFlightDoubleJump() {
		return autoFlightDoubleJump != null && autoFlightDoubleJump;
	}

	public void setAutoFlightDoubleJump(boolean enabled) {
		autoFlightDoubleJump = enabled;
		save();
	}

	public void setAutoFlightIgnoreShortFalls(boolean enabled) {
		autoFlightIgnoreShortFalls = enabled;
		save();
	}

	public int autoSwitchRocketsHotbarSlot() {
		return autoSwitchRocketsHotbarSlot == null
			? HOTBAR_SLOT_MAX
			: clamp(autoSwitchRocketsHotbarSlot, HOTBAR_SLOT_MIN, HOTBAR_SLOT_MAX);
	}

	public void setAutoSwitchRocketsHotbarSlot(int slot) {
		autoSwitchRocketsHotbarSlot = clamp(slot, HOTBAR_SLOT_MIN, HOTBAR_SLOT_MAX);
		save();
	}

	public boolean autoToolEnabled() {
		return autoToolEnabled != null && autoToolEnabled;
	}

	public void setAutoToolEnabled(boolean enabled) {
		autoToolEnabled = enabled;
		save();
	}

	public AutoToolMode autoToolMode() {
		return AutoToolMode.fromName(autoToolMode);
	}

	public void setAutoToolMode(AutoToolMode mode) {
		autoToolMode = (mode == null ? AutoToolMode.LEGIT : mode).name();
		save();
	}

	public boolean autoToolReturnToPreviousItem() {
		return autoToolReturnToPreviousItem != null && autoToolReturnToPreviousItem;
	}

	public void setAutoToolReturnToPreviousItem(boolean enabled) {
		autoToolReturnToPreviousItem = enabled;
		save();
	}

	public boolean tweakOwnNametag() {
		return tweakOwnNametag != null && tweakOwnNametag;
	}

	public void setTweakOwnNametag(boolean enabled) {
		tweakOwnNametag = enabled;
		save();
	}

	public boolean tweaksEnabled() {
		return tweakFullbright()
			|| tweakNoFog()
			|| tweakClearUnderwater()
			|| tweakClearLava()
			|| tweakNoEnvironmentFog()
			|| tweakNoNetherParticles()
			|| tweakNoHurtCam()
			|| tweakFreelook()
			|| beaconRadiusOutline()
			|| lightLevelOverlay()
			|| tweakShulkerTooltipPreview()
			|| tweakBundleTooltipPreview()
			|| tweakClearWeather()
			|| tweakNoFireOverlay()
			|| tweakLowFireOverlay()
			|| tweakNoNausea()
			|| tweakNoSpyglassOverlay()
			|| tweakFastPlace()
			|| tweakFastUse()
			|| tweakAntiDurabilityBreak()
			|| tweakSafeWalk()
			|| tweakPlaceBelow()
			|| tweakLockedYPlacement()
			|| tweakFreeCamera()
			|| autoFlightGearEnabled()
			|| autoToolEnabled()
			|| tweakOwnNametag();
	}

	public boolean packManagerEnabled() {
		return packManagerEnabled == null || packManagerEnabled;
	}

	public void setPackManagerEnabled(boolean enabled) {
		packManagerEnabled = enabled;
		save();
	}

	public boolean packManagerShowShadersWithoutIris() {
		return packManagerShowShadersWithoutIris == null || packManagerShowShadersWithoutIris;
	}

	public void setPackManagerShowShadersWithoutIris(boolean enabled) {
		packManagerShowShadersWithoutIris = enabled;
		save();
	}

	public int packManagerSearchLimit() {
		return clampPackManagerSearchLimit(packManagerSearchLimit == null ? 20 : packManagerSearchLimit);
	}

	public void setPackManagerSearchLimit(int limit) {
		packManagerSearchLimit = clampPackManagerSearchLimit(limit);
		save();
	}

	public boolean customCapes() {
		return customCapes == null || customCapes;
	}

	public void setCustomCapes(boolean enabled) {
		customCapes = enabled;
		save();
		CustomCapeManager.reload();
	}

	public boolean capeOptifine() {
		return capeOptifine == null || capeOptifine;
	}

	public void setCapeOptifine(boolean enabled) {
		capeOptifine = enabled;
		save();
		CustomCapeManager.reload();
	}

	public boolean capeLabyMod() {
		return capeLabyMod == null || capeLabyMod;
	}

	public void setCapeLabyMod(boolean enabled) {
		capeLabyMod = enabled;
		save();
		CustomCapeManager.reload();
	}

	public boolean capeMinecraftCapes() {
		return capeMinecraftCapes == null || capeMinecraftCapes;
	}

	public void setCapeMinecraftCapes(boolean enabled) {
		capeMinecraftCapes = enabled;
		save();
		CustomCapeManager.reload();
	}

	public boolean capeCosmetica() {
		return capeCosmetica == null || capeCosmetica;
	}

	public void setCapeCosmetica(boolean enabled) {
		capeCosmetica = enabled;
		save();
		CustomCapeManager.reload();
	}

	public boolean capeCloaksPlus() {
		return capeCloaksPlus == null || capeCloaksPlus;
	}

	public void setCapeCloaksPlus(boolean enabled) {
		capeCloaksPlus = enabled;
		save();
		CustomCapeManager.reload();
	}

	public CapePreferredProvider capePreferredProvider() {
		return CapePreferredProvider.fromName(capePreferredProvider);
	}

	public void setCapePreferredProvider(CapePreferredProvider preferred) {
		capePreferredProvider = preferred.name();
		save();
		CustomCapeManager.reload();
	}

	public boolean capesEnabled() {
		return customCapes()
			&& (capeOptifine()
				|| capeLabyMod()
				|| capeMinecraftCapes()
				|| capeCosmetica()
				|| capeCloaksPlus());
	}

	public boolean spotifyPlayerEnabled() {
		return spotifyPlayerEnabled != null && spotifyPlayerEnabled;
	}

	public void setSpotifyPlayerEnabled(boolean enabled) {
		spotifyPlayerEnabled = enabled;
		save();
	}

	public boolean spotifyHudOverlay() {
		return spotifyHudOverlay != null && spotifyHudOverlay;
	}

	public void setSpotifyHudOverlay(boolean enabled) {
		spotifyHudOverlay = enabled;
		save();
	}

	public HudOverlayAnchor spotifyHudAnchor() {
		return HudOverlayAnchor.fromName(spotifyHudAnchor);
	}

	public void setSpotifyHudAnchor(HudOverlayAnchor anchor) {
		spotifyHudAnchor = (anchor == null ? HudOverlayAnchor.BOTTOM_RIGHT : anchor).name();
		save();
	}

	public int spotifyHudBackgroundOpacity() {
		return clampHudBackgroundOpacity(spotifyHudBackgroundOpacity == null ? 100 : spotifyHudBackgroundOpacity);
	}

	public void setSpotifyHudBackgroundOpacity(int opacity) {
		spotifyHudBackgroundOpacity = clampHudBackgroundOpacity(opacity);
		save();
	}

	public boolean inventoryToolsEnabled() {
		return inventoryToolsEnabled == null || inventoryToolsEnabled;
	}

	public void setInventoryToolsEnabled(boolean enabled) {
		inventoryToolsEnabled = enabled;
		save();
	}

	public boolean slotLockingEnabled() {
		return slotLockingEnabled == null || slotLockingEnabled;
	}

	public void setSlotLockingEnabled(boolean enabled) {
		slotLockingEnabled = enabled;
		save();
	}

	public boolean slotBindingEnabled() {
		return slotBindingEnabled == null || slotBindingEnabled;
	}

	public void setSlotBindingEnabled(boolean enabled) {
		slotBindingEnabled = enabled;
		save();
	}

	public boolean slotBindingShowIcons() {
		return slotBindingShowIcons == null || slotBindingShowIcons;
	}

	public void setSlotBindingShowIcons(boolean enabled) {
		slotBindingShowIcons = enabled;
		save();
	}

	public boolean slotBindingLockBoundSlots() {
		return slotBindingLockBoundSlots == null || slotBindingLockBoundSlots;
	}

	public void setSlotBindingLockBoundSlots(boolean enabled) {
		slotBindingLockBoundSlots = enabled;
		save();
	}

	public boolean hoverTransferEnabled() {
		return hoverTransferEnabled == null || hoverTransferEnabled;
	}

	public void setHoverTransferEnabled(boolean enabled) {
		hoverTransferEnabled = enabled;
		save();
	}

	public boolean hoverTransferGlobal() {
		return hoverTransferGlobal != null && hoverTransferGlobal;
	}

	public void setHoverTransferGlobal(boolean enabled) {
		hoverTransferGlobal = enabled;
		save();
	}

	public boolean sortButtonsEnabled() {
		return sortButtonsEnabled == null || sortButtonsEnabled;
	}

	public void setSortButtonsEnabled(boolean enabled) {
		sortButtonsEnabled = enabled;
		save();
	}

	public InventorySortSpeed sortSpeed() {
		return InventorySortSpeed.fromName(sortSpeed);
	}

	public void setSortSpeed(InventorySortSpeed speed) {
		sortSpeed = (speed == null ? InventorySortSpeed.NORMAL : speed).name();
		save();
	}

	public boolean quickStackEnabled() {
		return quickStackEnabled == null || quickStackEnabled;
	}

	public void setQuickStackEnabled(boolean enabled) {
		quickStackEnabled = enabled;
		save();
	}

	public InventorySortSpeed quickStackSpeed() {
		return InventorySortSpeed.fromName(quickStackSpeed);
	}

	public void setQuickStackSpeed(InventorySortSpeed speed) {
		quickStackSpeed = (speed == null ? InventorySortSpeed.NORMAL : speed).name();
		save();
	}

	public boolean inventoryPreviewEnabled() {
		return inventoryPreviewEnabled != null && inventoryPreviewEnabled;
	}

	public void setInventoryPreviewEnabled(boolean enabled) {
		inventoryPreviewEnabled = enabled;
		save();
	}

	public boolean preserveContainerCursor() {
		return preserveContainerCursor == null || preserveContainerCursor;
	}

	public void setPreserveContainerCursor(boolean enabled) {
		preserveContainerCursor = enabled;
		save();
	}

	public SlotLockColor slotLockColor() {
		return SlotLockColor.fromName(slotLockColor);
	}

	public void setSlotLockColor(SlotLockColor color) {
		slotLockColor = (color == null ? SlotLockColor.RED : color).name();
		slotLockArgb = null;
		save();
	}

	public BoundSlotColor boundSlotColor() {
		return BoundSlotColor.fromName(boundSlotColor);
	}

	public void setBoundSlotColor(BoundSlotColor color) {
		boundSlotColor = (color == null ? BoundSlotColor.GRAY : color).name();
		boundSlotArgb = null;
		save();
	}

	public int slotLockOverlayColor() {
		if (slotLockArgb != null) {
			return slotLockArgb;
		}

		return slotLockColor().color();
	}

	public void setSlotLockOverlayColor(int argb) {
		slotLockArgb = argb;
		save();
	}

	public int boundSlotOverlayColor() {
		if (boundSlotArgb != null) {
			return boundSlotArgb;
		}

		return boundSlotColor().color();
	}

	public void setBoundSlotOverlayColor(int argb) {
		boundSlotArgb = argb;
		save();
	}

	public int inventoryPreviewOpacity() {
		return clampInventoryPreviewOpacity(inventoryPreviewOpacity == null ? 75 : inventoryPreviewOpacity);
	}

	public void setInventoryPreviewOpacity(int opacity) {
		inventoryPreviewOpacity = clampInventoryPreviewOpacity(opacity);
		save();
	}

	public float deathWaypointSizeMultiplier() {
		float displayPercent = deathWaypointSize() / 100.0F;
		float referencePercent = DEFAULT_DEATH_WAYPOINT_SIZE / 100.0F;
		return displayPercent * (LEGACY_SIZE_REFERENCE_PERCENT / 100.0F) / referencePercent;
	}

	public boolean waypointEnabled() {
		return deathWaypoint == null || deathWaypoint;
	}

	public void setWaypointEnabled(boolean enabled) {
		deathWaypoint = enabled;
		save();
	}

	public boolean waypointAutoCopyCoords() {
		return deathWaypointAutoCopyCoords != null && deathWaypointAutoCopyCoords;
	}

	public void setWaypointAutoCopyCoords(boolean enabled) {
		deathWaypointAutoCopyCoords = enabled;
		save();
	}

	public WaypointCoordinateFormat waypointCoordinateFormat() {
		return WaypointCoordinateFormat.fromName(deathWaypointCoordinateFormat);
	}

	public void setWaypointCoordinateFormat(WaypointCoordinateFormat format) {
		deathWaypointCoordinateFormat = (format == null ? WaypointCoordinateFormat.PLAIN : format).name();
		save();
	}

	public int waypointOpacity() {
		return clampOpacity(deathWaypointOpacity == null ? 75 : deathWaypointOpacity);
	}

	public void setWaypointOpacity(int opacity) {
		deathWaypointOpacity = clampOpacity(opacity);
		save();
	}

	public int waypointSize() {
		return clampSize(deathWaypointSize == null ? DEFAULT_DEATH_WAYPOINT_SIZE : deathWaypointSize);
	}

	public void setWaypointSize(int sizePercent) {
		deathWaypointSize = clampSize(sizePercent);
		save();
	}

	public float waypointSizeMultiplier() {
		float displayPercent = waypointSize() / 100.0F;
		float referencePercent = DEFAULT_DEATH_WAYPOINT_SIZE / 100.0F;
		return displayPercent * (LEGACY_SIZE_REFERENCE_PERCENT / 100.0F) / referencePercent;
	}

	public int waypointDefaultDeathColor() {
		return waypointDefaultDeathColor == null ? 0xFFFF5555 : waypointDefaultDeathColor;
	}

	public void setWaypointDefaultDeathColor(int color) {
		waypointDefaultDeathColor = color;
		save();
	}

	public int waypointDefaultCustomColor() {
		return waypointDefaultCustomColor == null ? 0xFF55FF55 : waypointDefaultCustomColor;
	}

	public void setWaypointDefaultCustomColor(int color) {
		waypointDefaultCustomColor = color;
		save();
	}

	public int reconnectDelaySeconds() {
		return clampDelay(reconnectDelaySeconds == null ? 8 : reconnectDelaySeconds);
	}

	public void setReconnectDelaySeconds(int seconds) {
		reconnectDelaySeconds = clampDelay(seconds);
		save();
	}

	public int duplicateMessageWindowSeconds() {
		return clampDuplicateMessageWindow(duplicateMessageWindowSeconds == null ? DUPLICATE_MESSAGE_WINDOW_MIN : duplicateMessageWindowSeconds);
	}

	public void setDuplicateMessageWindowSeconds(int seconds) {
		duplicateMessageWindowSeconds = clampDuplicateMessageWindow(seconds);
		save();
	}

	public void resetAutoReconnectDefaults() {
		autoReconnect = Boolean.TRUE;
		reconnectDelaySeconds = 8;
		autoReconnectMaxTries = 5;
		autoReconnectUnlimitedTries = Boolean.FALSE;
		save();
	}

	public void resetScreenshotDefaults() {
		screenshotHelper = Boolean.TRUE;
		screenshotAutoCopy = Boolean.FALSE;
		screenshotMetadataSaver = Boolean.TRUE;
		screenshotGallerySort = ScreenshotGallerySort.NEWEST_FIRST.name();
		screenshotGalleryDeleteConfirmation = Boolean.TRUE;
		screenshotGalleryMaxCount = 200;
		save();
	}

	public void resetScreenshotHelperDefaults() {
		screenshotHelper = Boolean.TRUE;
		screenshotAutoCopy = Boolean.FALSE;
		screenshotMetadataSaver = Boolean.TRUE;
		save();
	}

	public void resetScreenshotGalleryDefaults() {
		screenshotGallerySort = ScreenshotGallerySort.NEWEST_FIRST.name();
		screenshotGalleryDeleteConfirmation = Boolean.TRUE;
		screenshotGalleryMaxCount = 200;
		save();
	}

	public void resetDeathWaypointDefaults() {
		deathWaypoint = Boolean.TRUE;
		deathWaypointAutoCopyCoords = Boolean.FALSE;
		deathWaypointCoordinateFormat = WaypointCoordinateFormat.PLAIN.name();
		deathWaypointOpacity = 75;
		deathWaypointSize = DEFAULT_DEATH_WAYPOINT_SIZE;
		waypointDefaultDeathColor = 0xFFFF5555;
		waypointDefaultCustomColor = 0xFF55FF55;
		save();
	}

	public void resetChatDefaults() {
		copyChat = Boolean.TRUE;
		copyChatFormatting = Boolean.FALSE;
		copyChatFeedback = Boolean.TRUE;
		chatTimestamps = Boolean.FALSE;
		chatTimestamp24Hour = Boolean.TRUE;
		smartChatFilters = Boolean.FALSE;
		duplicateMessageTimeWindow = Boolean.FALSE;
		duplicateMessageWindowSeconds = DUPLICATE_MESSAGE_WINDOW_MIN;
		chatMentionAlerts = Boolean.FALSE;
		chatMentionHighlight = Boolean.FALSE;
		chatMentionHighlightColor = 0xFF7289DA;
		chatMentionHighlightStyle = 0;
		chatMentionAlertVolume = 100;
		chatMentionAlertSound = 0;
		save();
		ChatFeaturesRefresher.onTimestampSettingsChanged();
	}

	public void resetCommandShortcutsDefaults() {
		commandShortcutsEnabled = Boolean.TRUE;
		save();
	}

	public void resetManagerDefaults() {
		commandShortcutsEnabled = Boolean.TRUE;
		packManagerEnabled = Boolean.TRUE;
		packManagerShowShadersWithoutIris = Boolean.TRUE;
		packManagerSearchLimit = 20;
		save();
	}

	public void resetHudDefaults() {
		hudOverlay = Boolean.FALSE;
		hudOverlayAnchor = HudOverlayAnchor.TOP_LEFT.name();
		hudCustomLayout = new LinkedHashMap<>();
		hudShowCoordinates = Boolean.TRUE;
		hudShowNetherCoordinates = Boolean.FALSE;
		hudShowChunkRegion = Boolean.TRUE;
		hudShowBiome = Boolean.TRUE;
		hudShowPing = Boolean.TRUE;
		hudShowFps = Boolean.TRUE;
		hudShowMemory = Boolean.TRUE;
		hudShowServerTime = Boolean.TRUE;
		hudShowRealTime = Boolean.TRUE;
		hudShowIcons = Boolean.TRUE;
		hudHideWithDebug = Boolean.TRUE;
		hudShowFacing = Boolean.TRUE;
		hudBackgroundOpacity = 100;
		hudScale = 100;
		save();
	}

	public void resetFoodHudDefaults() {
		foodHud = Boolean.TRUE;
		foodHudSaturationOverlay = Boolean.TRUE;
		foodHudHeldFoodOverlay = Boolean.TRUE;
		foodHudOffhandOverlay = Boolean.TRUE;
		foodHudExhaustionUnderlay = Boolean.TRUE;
		foodHudTooltips = Boolean.TRUE;
		foodHudTooltipAlways = Boolean.TRUE;
		foodHudVanillaAnimations = Boolean.TRUE;
		save();
	}

	public void resetZoomDefaults() {
		zoomEnabled = Boolean.TRUE;
		zoomAmount = 4;
		zoomTransitionSpeed = 24;
		zoomCinematicCamera = Boolean.TRUE;
		zoomHideHand = Boolean.FALSE;
		zoomSmoothTransition = Boolean.TRUE;
		zoomHideHud = Boolean.FALSE;
		save();
	}

	public void resetTweaksDefaults() {
		tweakFullbright = Boolean.FALSE;
		tweakFullbrightStrength = FULLBRIGHT_STRENGTH_MAX;
		tweakNoFog = Boolean.FALSE;
		tweakClearUnderwater = Boolean.TRUE;
		tweakClearLava = Boolean.TRUE;
		tweakNoEnvironmentFog = Boolean.TRUE;
		tweakNoNetherParticles = Boolean.FALSE;
		tweakNoHurtCam = Boolean.FALSE;
		tweakFreelook = Boolean.FALSE;
		beaconRadiusOutline = Boolean.FALSE;
		lightLevelOverlay = Boolean.FALSE;
		tweakShulkerTooltipPreview = Boolean.TRUE;
		tweakBundleTooltipPreview = Boolean.TRUE;
		tweakClearWeather = Boolean.FALSE;
		tweakClearWeatherHideRain = Boolean.TRUE;
		tweakClearWeatherHideSnow = Boolean.TRUE;
		tweakClearWeatherHideRainEffects = Boolean.TRUE;
		tweakNoFireOverlay = Boolean.FALSE;
		tweakLowFireOverlay = Boolean.FALSE;
		tweakNoNausea = Boolean.FALSE;
		tweakNoSpyglassOverlay = Boolean.FALSE;
		tweakFastPlace = Boolean.FALSE;
		tweakFastUse = Boolean.FALSE;
		tweakAntiDurabilityBreak = Boolean.FALSE;
		tweakSafeWalk = Boolean.FALSE;
		tweakPlaceBelow = Boolean.FALSE;
		tweakLockedYPlacement = Boolean.FALSE;
		tweakFreeCamera = Boolean.FALSE;
		freeCameraBoostMultiplier = 3;
		autoFlightGearEnabled = Boolean.FALSE;
		tweakAutoSwitchElytra = Boolean.FALSE;
		tweakAutoSwitchRockets = Boolean.FALSE;
		autoFlightDoubleJump = Boolean.FALSE;
		autoFlightIgnoreShortFalls = Boolean.TRUE;
		autoSwitchRocketsHotbarSlot = HOTBAR_SLOT_MAX;
		autoToolEnabled = Boolean.FALSE;
		autoToolMode = AutoToolMode.LEGIT.name();
		autoToolReturnToPreviousItem = Boolean.FALSE;
		tweakOwnNametag = Boolean.FALSE;
		save();
	}

	public void resetFullbrightDefaults() {
		tweakFullbright = Boolean.FALSE;
		tweakFullbrightStrength = FULLBRIGHT_STRENGTH_MAX;
		save();
	}

	public void resetAutoToolDefaults() {
		autoToolEnabled = Boolean.FALSE;
		autoToolMode = AutoToolMode.LEGIT.name();
		autoToolReturnToPreviousItem = Boolean.FALSE;
		save();
	}

	public void resetAutoFlightDefaults() {
		autoFlightGearEnabled = Boolean.FALSE;
		tweakAutoSwitchElytra = Boolean.FALSE;
		tweakAutoSwitchRockets = Boolean.FALSE;
		autoFlightDoubleJump = Boolean.FALSE;
		autoFlightIgnoreShortFalls = Boolean.TRUE;
		autoSwitchRocketsHotbarSlot = HOTBAR_SLOT_MAX;
		save();
	}

	public void resetClearWeatherDefaults() {
		tweakClearWeather = Boolean.FALSE;
		tweakClearWeatherHideRain = Boolean.TRUE;
		tweakClearWeatherHideSnow = Boolean.TRUE;
		tweakClearWeatherHideRainEffects = Boolean.TRUE;
		save();
	}

	public void resetPackManagerDefaults() {
		packManagerEnabled = Boolean.TRUE;
		packManagerShowShadersWithoutIris = Boolean.TRUE;
		packManagerSearchLimit = 20;
		save();
	}

	public void resetCapesDefaults() {
		customCapes = Boolean.TRUE;
		capeOptifine = Boolean.TRUE;
		capeLabyMod = Boolean.TRUE;
		capeMinecraftCapes = Boolean.TRUE;
		capeCosmetica = Boolean.TRUE;
		capeCloaksPlus = Boolean.TRUE;
		capePreferredProvider = CapePreferredProvider.AUTO.name();
		save();
		CustomCapeManager.reload();
	}

	public void resetSpotifyPlayerDefaults() {
		spotifyPlayerEnabled = Boolean.FALSE;
		spotifyHudOverlay = Boolean.FALSE;
		spotifyHudBackgroundOpacity = 100;
		spotifyHudScale = 100;
		save();
	}

	public void resetInventoryToolsDefaults() {
		inventoryToolsEnabled = Boolean.TRUE;
		slotLockingEnabled = Boolean.TRUE;
		slotBindingEnabled = Boolean.TRUE;
		slotBindingShowIcons = Boolean.TRUE;
		slotBindingLockBoundSlots = Boolean.TRUE;
		hoverTransferEnabled = Boolean.TRUE;
		hoverTransferGlobal = Boolean.FALSE;
		sortButtonsEnabled = Boolean.TRUE;
		sortSpeed = InventorySortSpeed.NORMAL.name();
		quickStackEnabled = Boolean.TRUE;
		quickStackSpeed = InventorySortSpeed.NORMAL.name();
		inventoryPreviewEnabled = Boolean.FALSE;
		preserveContainerCursor = Boolean.TRUE;
		slotLockColor = SlotLockColor.RED.name();
		boundSlotColor = BoundSlotColor.GRAY.name();
		slotLockArgb = null;
		boundSlotArgb = null;
		inventoryPreviewOpacity = 75;
		save();
	}

	public void save() {
		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			try (Writer writer = Files.newBufferedWriter(EMUtilsPaths.configFile())) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}

	private void applyDefaults() {
		if (autoReconnect == null) {
			autoReconnect = Boolean.TRUE;
		}
		if (autoReconnectUnlimitedTries == null) {
			autoReconnectUnlimitedTries = Boolean.FALSE;
		}
		if (screenshotHelper == null) {
			screenshotHelper = Boolean.TRUE;
		}
		if (screenshotAutoCopy == null) {
			screenshotAutoCopy = Boolean.FALSE;
		}
		if (screenshotMetadataSaver == null) {
			screenshotMetadataSaver = Boolean.TRUE;
		}
		if (screenshotGalleryDeleteConfirmation == null) {
			screenshotGalleryDeleteConfirmation = Boolean.TRUE;
		}
		screenshotGallerySort = screenshotGallerySort().name();
		if (copyChat == null) {
			copyChat = Boolean.TRUE;
		}
		if (copyChatFormatting == null) {
			copyChatFormatting = Boolean.FALSE;
		}
		if (copyChatFeedback == null) {
			copyChatFeedback = Boolean.TRUE;
		}
		if (chatTimestamps == null) {
			chatTimestamps = Boolean.FALSE;
		}
		if (chatTimestamp24Hour == null) {
			chatTimestamp24Hour = Boolean.TRUE;
		}
		if (smartChatFilters == null) {
			smartChatFilters = Boolean.FALSE;
		}
		if (duplicateMessageTimeWindow == null) {
			duplicateMessageTimeWindow = Boolean.FALSE;
		}
		if (chatMentionAlerts == null) {
			chatMentionAlerts = Boolean.FALSE;
		}
		if (chatMentionHighlight == null) {
			chatMentionHighlight = Boolean.FALSE;
		}
		if (commandShortcutsEnabled == null) {
			commandShortcutsEnabled = Boolean.TRUE;
		}
		if (chatMentionHighlightColor == null) {
			chatMentionHighlightColor = 0xFF7289DA;
		}
		if (hudOverlay == null) {
			hudOverlay = Boolean.FALSE;
		}
		hudOverlayAnchor = hudOverlayAnchor().name();
		hudLayoutMode = hudLayoutMode().name();
		if (hudCustomLayout == null) {
			hudCustomLayout = new LinkedHashMap<>();
		}
		if (hudShowCoordinates == null) {
			hudShowCoordinates = Boolean.TRUE;
		}
		if (hudShowNetherCoordinates == null) {
			hudShowNetherCoordinates = Boolean.FALSE;
		}
		if (hudShowChunkRegion == null) {
			hudShowChunkRegion = Boolean.TRUE;
		}
		if (hudShowBiome == null) {
			hudShowBiome = Boolean.TRUE;
		}
		if (hudShowPing == null) {
			hudShowPing = Boolean.TRUE;
		}
		if (hudShowFps == null) {
			hudShowFps = Boolean.TRUE;
		}
		if (hudShowMemory == null) {
			hudShowMemory = Boolean.TRUE;
		}
		if (hudShowServerTime == null) {
			hudShowServerTime = Boolean.TRUE;
		}
		if (hudShowRealTime == null) {
			hudShowRealTime = Boolean.TRUE;
		}
		if (hudShowIcons == null) {
			hudShowIcons = Boolean.TRUE;
		}
		if (hudHideWithDebug == null) {
			hudHideWithDebug = Boolean.TRUE;
		}
		if (hudShowFacing == null) {
			hudShowFacing = Boolean.TRUE;
		}
		if (foodHud == null) {
			foodHud = Boolean.TRUE;
		}
		if (foodHudSaturationOverlay == null) {
			foodHudSaturationOverlay = Boolean.TRUE;
		}
		if (foodHudHeldFoodOverlay == null) {
			foodHudHeldFoodOverlay = Boolean.TRUE;
		}
		if (foodHudOffhandOverlay == null) {
			foodHudOffhandOverlay = Boolean.TRUE;
		}
		if (foodHudExhaustionUnderlay == null) {
			foodHudExhaustionUnderlay = Boolean.TRUE;
		}
		if (foodHudTooltips == null) {
			foodHudTooltips = Boolean.TRUE;
		}
		if (foodHudTooltipAlways == null) {
			foodHudTooltipAlways = Boolean.TRUE;
		}
		if (foodHudVanillaAnimations == null) {
			foodHudVanillaAnimations = Boolean.TRUE;
		}
		if (deathWaypoint == null) {
			deathWaypoint = Boolean.TRUE;
		}
		if (deathWaypointAutoCopyCoords == null) {
			deathWaypointAutoCopyCoords = Boolean.FALSE;
		}
		deathWaypointCoordinateFormat = deathWaypointCoordinateFormat().name();
		reconnectDelaySeconds = reconnectDelaySeconds();
		autoReconnectMaxTries = autoReconnectMaxTries();
		screenshotGalleryMaxCount = screenshotGalleryMaxCount();
		duplicateMessageWindowSeconds = duplicateMessageWindowSeconds();
		deathWaypointOpacity = deathWaypointOpacity();
		deathWaypointSize = migrateDeathWaypointSize(deathWaypointSize);
		deathWaypointSize = deathWaypointSize();
		if (waypointDefaultDeathColor == null) {
			waypointDefaultDeathColor = 0xFFFF5555;
		}
		if (waypointDefaultCustomColor == null) {
			waypointDefaultCustomColor = 0xFF55FF55;
		}
		hudBackgroundOpacity = hudBackgroundOpacity();
		if (hudScale == null) {
			hudScale = 100;
		}
		if (zoomEnabled == null) {
			zoomEnabled = Boolean.TRUE;
		}
		if (zoomCinematicCamera == null) {
			zoomCinematicCamera = Boolean.TRUE;
		}
		if (zoomHideHand == null) {
			zoomHideHand = Boolean.FALSE;
		}
		if (zoomSmoothTransition == null) {
			zoomSmoothTransition = Boolean.TRUE;
		}
		if (zoomHideHud == null) {
		zoomHideHud = Boolean.FALSE;
		zoomOutSpeedMultiplier = 18;
		}
		if (tweakFullbright == null) {
			tweakFullbright = Boolean.FALSE;
		}
		if (tweakFullbrightStrength == null) {
			tweakFullbrightStrength = FULLBRIGHT_STRENGTH_MAX;
		} else {
			tweakFullbrightStrength = clamp(tweakFullbrightStrength, FULLBRIGHT_STRENGTH_MIN, FULLBRIGHT_STRENGTH_MAX);
		}
		if (tweakNoFog == null) {
			tweakNoFog = Boolean.FALSE;
		}
		migrateFogTweaks();
		if (tweakClearUnderwater == null) {
			tweakClearUnderwater = Boolean.TRUE;
		}
		if (tweakClearLava == null) {
			tweakClearLava = Boolean.TRUE;
		}
		if (tweakNoEnvironmentFog == null) {
			tweakNoEnvironmentFog = Boolean.TRUE;
		}
		if (tweakNoNetherParticles == null) {
			tweakNoNetherParticles = Boolean.FALSE;
		}
		if (tweakNoHurtCam == null) {
			tweakNoHurtCam = Boolean.FALSE;
		}
		if (tweakFreelook == null) {
			tweakFreelook = Boolean.FALSE;
		}
		if (beaconRadiusOutline == null) {
			beaconRadiusOutline = Boolean.FALSE;
		}
		if (lightLevelOverlay == null) {
			lightLevelOverlay = Boolean.FALSE;
		}
		if (tweakShulkerTooltipPreview == null) {
			tweakShulkerTooltipPreview = Boolean.TRUE;
		}
		if (tweakBundleTooltipPreview == null) {
			tweakBundleTooltipPreview = Boolean.TRUE;
		}
		if (tweakClearWeather == null) {
			tweakClearWeather = Boolean.FALSE;
		}
		if (tweakClearWeatherHideRain == null) {
			tweakClearWeatherHideRain = Boolean.TRUE;
		}
		if (tweakClearWeatherHideSnow == null) {
			tweakClearWeatherHideSnow = Boolean.TRUE;
		}
		if (tweakClearWeatherHideRainEffects == null) {
			tweakClearWeatherHideRainEffects = Boolean.TRUE;
		}
		if (tweakNoFireOverlay == null) {
			tweakNoFireOverlay = Boolean.FALSE;
		}
		if (tweakLowFireOverlay == null) {
			tweakLowFireOverlay = Boolean.FALSE;
		}
		if (tweakNoNausea == null) {
			tweakNoNausea = Boolean.FALSE;
		}
		if (tweakNoSpyglassOverlay == null) {
			tweakNoSpyglassOverlay = Boolean.FALSE;
		}
		if (tweakFastPlace == null) {
			tweakFastPlace = Boolean.FALSE;
		}
		if (tweakFastUse == null) {
			tweakFastUse = Boolean.FALSE;
		}
		if (tweakAntiDurabilityBreak == null) {
			tweakAntiDurabilityBreak = Boolean.FALSE;
		}
		if (tweakSafeWalk == null) {
			tweakSafeWalk = Boolean.FALSE;
		}
		if (tweakPlaceBelow == null) {
			tweakPlaceBelow = Boolean.FALSE;
		}
		if (tweakLockedYPlacement == null) {
			tweakLockedYPlacement = Boolean.FALSE;
		}
		if (tweakFreeCamera == null) {
			tweakFreeCamera = Boolean.FALSE;
		}
		freeCameraBoostMultiplier = freeCameraBoostMultiplier();
		if (tweakAutoSwitchElytra == null) {
			tweakAutoSwitchElytra = Boolean.FALSE;
		}
		if (tweakAutoSwitchRockets == null) {
			tweakAutoSwitchRockets = Boolean.FALSE;
		}
		if (autoFlightGearEnabled == null) {
			autoFlightGearEnabled = tweakAutoSwitchElytra() || tweakAutoSwitchRockets();
		}
		if (autoFlightIgnoreShortFalls == null) {
			autoFlightIgnoreShortFalls = Boolean.TRUE;
		}
		if (autoFlightDoubleJump == null) {
			autoFlightDoubleJump = Boolean.FALSE;
		}
		autoSwitchRocketsHotbarSlot = autoSwitchRocketsHotbarSlot();
		if (autoToolEnabled == null) {
			autoToolEnabled = Boolean.FALSE;
		}
		if (autoToolMode == null) {
			autoToolMode = AutoToolMode.LEGIT.name();
		}
		if (autoToolReturnToPreviousItem == null) {
			autoToolReturnToPreviousItem = Boolean.FALSE;
		}
		if (tweakOwnNametag == null) {
			tweakOwnNametag = Boolean.FALSE;
		}
		if (packManagerEnabled == null) {
			packManagerEnabled = Boolean.TRUE;
		}
		if (packManagerShowShadersWithoutIris == null) {
			packManagerShowShadersWithoutIris = Boolean.TRUE;
		}
		if (customCapes == null) {
			customCapes = Boolean.TRUE;
		}
		if (capeOptifine == null) {
			capeOptifine = Boolean.TRUE;
		}
		if (capeLabyMod == null) {
			capeLabyMod = Boolean.TRUE;
		}
		if (capeMinecraftCapes == null) {
			capeMinecraftCapes = Boolean.TRUE;
		}
		if (capeCosmetica == null) {
			capeCosmetica = Boolean.TRUE;
		}
		if (capeCloaksPlus == null) {
			capeCloaksPlus = Boolean.TRUE;
		}
		if (capePreferredProvider == null || capePreferredProvider.isBlank()) {
			capePreferredProvider = CapePreferredProvider.AUTO.name();
		}
		if (spotifyPlayerEnabled == null) {
			spotifyPlayerEnabled = Boolean.FALSE;
		}
		if (spotifyHudOverlay == null) {
			spotifyHudOverlay = Boolean.FALSE;
		}
		spotifyHudAnchor = spotifyHudAnchor().name();
		spotifyHudBackgroundOpacity = spotifyHudBackgroundOpacity();
		if (spotifyHudScale == null) {
			spotifyHudScale = 100;
		}
		if (inventoryToolsEnabled == null) {
			inventoryToolsEnabled = Boolean.TRUE;
		}
		if (slotLockingEnabled == null) {
			slotLockingEnabled = Boolean.TRUE;
		}
		if (slotBindingEnabled == null) {
			slotBindingEnabled = Boolean.TRUE;
		}
		if (slotBindingShowIcons == null) {
			slotBindingShowIcons = Boolean.TRUE;
		}
		if (slotBindingLockBoundSlots == null) {
			slotBindingLockBoundSlots = Boolean.TRUE;
		}
		if (hoverTransferEnabled == null) {
			hoverTransferEnabled = Boolean.TRUE;
		}
		if (hoverTransferGlobal == null) {
			hoverTransferGlobal = Boolean.FALSE;
		}
		if (sortButtonsEnabled == null) {
			sortButtonsEnabled = Boolean.TRUE;
		}
		if (sortSpeed == null) {
			sortSpeed = InventorySortSpeed.NORMAL.name();
		}
		if (quickStackEnabled == null) {
			quickStackEnabled = Boolean.TRUE;
		}
		if (quickStackSpeed == null) {
			quickStackSpeed = InventorySortSpeed.NORMAL.name();
		}
		if (inventoryPreviewEnabled == null) {
			inventoryPreviewEnabled = Boolean.FALSE;
		}
		if (preserveContainerCursor == null) {
			preserveContainerCursor = Boolean.TRUE;
		}
		slotLockColor = slotLockColor().name();
		boundSlotColor = boundSlotColor().name();
		inventoryPreviewOpacity = inventoryPreviewOpacity();
		zoomAmount = zoomAmount();
		zoomTransitionSpeed = zoomTransitionSpeed();
		packManagerSearchLimit = packManagerSearchLimit();
	}

	private void migrateFogTweaks() {
		if (tweakClearUnderwater != null && tweakClearLava != null && tweakNoEnvironmentFog != null) {
			return;
		}

		boolean legacyWaterFog = tweakNoWaterFog == null || tweakNoWaterFog;
		boolean legacyFluidOverlay = tweakClearFluidOverlay != null && tweakClearFluidOverlay;
		if (tweakClearUnderwater == null) {
			tweakClearUnderwater = legacyWaterFog || legacyFluidOverlay;
		}
		if (tweakClearLava == null) {
			tweakClearLava = tweakNoLavaFog == null || tweakNoLavaFog;
		}
		if (tweakNoEnvironmentFog == null) {
			boolean legacyCave = tweakNoCaveFog == null || tweakNoCaveFog;
			boolean legacyNether = tweakNoNetherFog == null || tweakNoNetherFog;
			boolean legacyEnd = tweakNoEndFog == null || tweakNoEndFog;
			boolean legacyRain = tweakNoRainFog == null || tweakNoRainFog;
			tweakNoEnvironmentFog = legacyCave || legacyNether || legacyEnd || legacyRain;
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int clampDelay(int seconds) {
		return Math.max(RECONNECT_DELAY_MIN, Math.min(RECONNECT_DELAY_MAX, seconds));
	}

	private static int clampReconnectMaxTries(int tries) {
		return Math.max(RECONNECT_MAX_TRIES_MIN, Math.min(RECONNECT_MAX_TRIES_MAX, tries));
	}

	private static int clampScreenshotMaxCount(int maxCount) {
		return Math.max(SCREENSHOT_MAX_COUNT_MIN, Math.min(SCREENSHOT_MAX_COUNT_MAX, maxCount));
	}

	private static int clampDuplicateMessageWindow(int seconds) {
		return Math.max(DUPLICATE_MESSAGE_WINDOW_MIN, Math.min(DUPLICATE_MESSAGE_WINDOW_MAX, seconds));
	}

	private static int clampOpacity(int opacity) {
		return Math.max(DEATH_WAYPOINT_OPACITY_MIN, Math.min(DEATH_WAYPOINT_OPACITY_MAX, opacity));
	}

	private static int clampSize(int sizePercent) {
		return Math.max(DEATH_WAYPOINT_SIZE_MIN, Math.min(DEATH_WAYPOINT_SIZE_MAX, sizePercent));
	}

	private static int clampHudBackgroundOpacity(int opacity) {
		return Math.max(HUD_BACKGROUND_OPACITY_MIN, Math.min(HUD_BACKGROUND_OPACITY_MAX, opacity));
	}

	private static int clampInventoryPreviewOpacity(int opacity) {
		return Math.max(INVENTORY_PREVIEW_OPACITY_MIN, Math.min(INVENTORY_PREVIEW_OPACITY_MAX, opacity));
	}

	private static int clampZoomAmount(int amount) {
		return Math.max(ZOOM_AMOUNT_MIN, Math.min(ZOOM_AMOUNT_MAX, amount));
	}

	private static int clampZoomTransitionSpeed(int speed) {
		return Math.max(ZOOM_TRANSITION_SPEED_MIN, Math.min(ZOOM_TRANSITION_SPEED_MAX, speed));
	}

	private static int clampZoomOutSpeed(int value) {
		return Math.max(ZOOM_OUT_SPEED_MIN, Math.min(ZOOM_OUT_SPEED_MAX, value));
	}

	private static int clampPackManagerSearchLimit(int limit) {
		return Math.max(PACK_MANAGER_SEARCH_LIMIT_MIN, Math.min(PACK_MANAGER_SEARCH_LIMIT_MAX, limit));
	}

	private static Integer migrateDeathWaypointSize(Integer sizePercent) {
		if (sizePercent == null) {
			return DEFAULT_DEATH_WAYPOINT_SIZE;
		}

		if (sizePercent >= DEATH_WAYPOINT_SIZE_MIN && sizePercent <= DEATH_WAYPOINT_SIZE_MAX) {
			return sizePercent;
		}

		return clampSize((int) Math.round(sizePercent * (DEFAULT_DEATH_WAYPOINT_SIZE / LEGACY_SIZE_REFERENCE_PERCENT)));
	}
}
