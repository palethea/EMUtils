package net.emutils.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.emutils.client.chat.ChatFeaturesRefresher;
import net.emutils.client.util.EMUtilsPaths;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;

public final class EMUtilsConfig {
	public static final int RECONNECT_DELAY_MIN = 5;
	public static final int RECONNECT_DELAY_MAX = 15;
	public static final int DEATH_WAYPOINT_OPACITY_MIN = 25;
	public static final int DEATH_WAYPOINT_OPACITY_MAX = 100;
	public static final int DEATH_WAYPOINT_SIZE_MIN = 25;
	public static final int DEATH_WAYPOINT_SIZE_MAX = 100;
	public static final int DUPLICATE_MESSAGE_WINDOW_MIN = 30;
	public static final int DUPLICATE_MESSAGE_WINDOW_MAX = 120;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int DEFAULT_DEATH_WAYPOINT_SIZE = 50;
	private static final float LEGACY_SIZE_REFERENCE_PERCENT = 15.0F;

	private Boolean autoReconnect = Boolean.TRUE;
	private Boolean screenshotHelper = Boolean.TRUE;
	private Boolean screenshotAutoCopy = Boolean.FALSE;
	private Boolean copyChat = Boolean.TRUE;
	private Boolean copyChatFormatting = Boolean.FALSE;
	private Boolean copyChatFeedback = Boolean.TRUE;
	private Boolean chatTimestamps = Boolean.FALSE;
	private Boolean chatTimestamp24Hour = Boolean.TRUE;
	private Boolean smartChatFilters = Boolean.FALSE;
	private Boolean duplicateMessageTimeWindow = Boolean.FALSE;
	private Boolean chatMentionAlerts = Boolean.FALSE;
	private Boolean deathWaypoint = Boolean.TRUE;
	private Integer reconnectDelaySeconds = 8;
	private Integer duplicateMessageWindowSeconds = DUPLICATE_MESSAGE_WINDOW_MIN;
	private Integer deathWaypointOpacity = 75;
	private Integer deathWaypointSize = DEFAULT_DEATH_WAYPOINT_SIZE;

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

	public boolean deathWaypoint() {
		return deathWaypoint == null || deathWaypoint;
	}

	public void setDeathWaypoint(boolean enabled) {
		deathWaypoint = enabled;
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
		if (screenshotHelper == null) {
			screenshotHelper = Boolean.TRUE;
		}
		if (screenshotAutoCopy == null) {
			screenshotAutoCopy = Boolean.FALSE;
		}
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
		if (deathWaypoint == null) {
			deathWaypoint = Boolean.TRUE;
		}
		reconnectDelaySeconds = reconnectDelaySeconds();
		duplicateMessageWindowSeconds = duplicateMessageWindowSeconds();
		deathWaypointOpacity = deathWaypointOpacity();
		deathWaypointSize = migrateDeathWaypointSize(deathWaypointSize);
		deathWaypointSize = deathWaypointSize();
	}

	private static int clampDelay(int seconds) {
		return Math.max(RECONNECT_DELAY_MIN, Math.min(RECONNECT_DELAY_MAX, seconds));
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
