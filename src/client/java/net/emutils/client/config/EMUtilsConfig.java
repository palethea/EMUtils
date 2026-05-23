package net.emutils.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.emutils.client.chat.ChatFeaturesRefresher;
import net.emutils.client.death.DeathWaypointCoordinateFormat;
import net.emutils.client.hud.HudOverlayAnchor;
import net.emutils.client.screenshot.ScreenshotGallerySort;
import net.emutils.client.util.EMUtilsPaths;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;

public final class EMUtilsConfig {
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
	public static final int HUD_SCALE_MIN = 50;
	public static final int HUD_SCALE_MAX = 150;
	public static final int DUPLICATE_MESSAGE_WINDOW_MIN = 30;
	public static final int DUPLICATE_MESSAGE_WINDOW_MAX = 120;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int DEFAULT_DEATH_WAYPOINT_SIZE = 50;
	private static final float LEGACY_SIZE_REFERENCE_PERCENT = 15.0F;

	private Boolean autoReconnect = Boolean.TRUE;
	private Boolean autoReconnectUnlimitedTries = Boolean.FALSE;
	private Boolean screenshotHelper = Boolean.TRUE;
	private Boolean screenshotAutoCopy = Boolean.FALSE;
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
	private Boolean hudOverlay = Boolean.FALSE;
	private String hudOverlayAnchor = HudOverlayAnchor.TOP_LEFT.name();
	private Boolean hudShowCoordinates = Boolean.TRUE;
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
	private Boolean deathWaypoint = Boolean.TRUE;
	private Boolean deathWaypointAutoCopyCoords = Boolean.FALSE;
	private String deathWaypointCoordinateFormat = DeathWaypointCoordinateFormat.PLAIN.name();
	private Integer reconnectDelaySeconds = 8;
	private Integer autoReconnectMaxTries = 5;
	private Integer screenshotGalleryMaxCount = 200;
	private Integer duplicateMessageWindowSeconds = DUPLICATE_MESSAGE_WINDOW_MIN;
	private Integer deathWaypointOpacity = 75;
	private Integer deathWaypointSize = DEFAULT_DEATH_WAYPOINT_SIZE;
	private Integer hudBackgroundOpacity = 100;
	private Integer hudScale = 100;

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

	public boolean hudShowCoordinates() {
		return hudShowCoordinates == null || hudShowCoordinates;
	}

	public void setHudShowCoordinates(boolean enabled) {
		hudShowCoordinates = enabled;
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

	public int hudBackgroundOpacity() {
		return clampHudBackgroundOpacity(hudBackgroundOpacity == null ? 100 : hudBackgroundOpacity);
	}

	public void setHudBackgroundOpacity(int opacity) {
		hudBackgroundOpacity = clampHudBackgroundOpacity(opacity);
		save();
	}

	public int hudScale() {
		return clampHudScale(hudScale == null ? 100 : hudScale);
	}

	public void setHudScale(int scale) {
		hudScale = clampHudScale(scale);
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

	public DeathWaypointCoordinateFormat deathWaypointCoordinateFormat() {
		return DeathWaypointCoordinateFormat.fromName(deathWaypointCoordinateFormat);
	}

	public void setDeathWaypointCoordinateFormat(DeathWaypointCoordinateFormat format) {
		deathWaypointCoordinateFormat = (format == null ? DeathWaypointCoordinateFormat.PLAIN : format).name();
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

	public float deathWaypointSizeMultiplier() {
		float displayPercent = deathWaypointSize() / 100.0F;
		float referencePercent = DEFAULT_DEATH_WAYPOINT_SIZE / 100.0F;
		return displayPercent * (LEGACY_SIZE_REFERENCE_PERCENT / 100.0F) / referencePercent;
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
		screenshotGallerySort = ScreenshotGallerySort.NEWEST_FIRST.name();
		screenshotGalleryDeleteConfirmation = Boolean.TRUE;
		screenshotGalleryMaxCount = 200;
		save();
	}

	public void resetDeathWaypointDefaults() {
		deathWaypoint = Boolean.TRUE;
		deathWaypointAutoCopyCoords = Boolean.FALSE;
		deathWaypointCoordinateFormat = DeathWaypointCoordinateFormat.PLAIN.name();
		deathWaypointOpacity = 75;
		deathWaypointSize = DEFAULT_DEATH_WAYPOINT_SIZE;
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
		save();
		ChatFeaturesRefresher.onTimestampSettingsChanged();
	}

	public void resetHudDefaults() {
		hudOverlay = Boolean.FALSE;
		hudOverlayAnchor = HudOverlayAnchor.TOP_LEFT.name();
		hudShowCoordinates = Boolean.TRUE;
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
		if (hudOverlay == null) {
			hudOverlay = Boolean.FALSE;
		}
		hudOverlayAnchor = hudOverlayAnchor().name();
		if (hudShowCoordinates == null) {
			hudShowCoordinates = Boolean.TRUE;
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
		hudBackgroundOpacity = hudBackgroundOpacity();
		hudScale = hudScale();
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

	private static int clampHudScale(int scale) {
		return Math.max(HUD_SCALE_MIN, Math.min(HUD_SCALE_MAX, scale));
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
