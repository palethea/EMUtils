package net.emutils.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.emutils.client.util.EMUtilsPaths;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;

public final class EMUtilsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int MIN_RECONNECT_DELAY_SECONDS = 5;
	private static final int MAX_RECONNECT_DELAY_SECONDS = 15;
	private static final int MIN_DEATH_WAYPOINT_OPACITY = 25;
	private static final int MAX_DEATH_WAYPOINT_OPACITY = 100;
	private static final int MIN_DEATH_WAYPOINT_SIZE = 25;
	private static final int MAX_DEATH_WAYPOINT_SIZE = 100;
	private static final int DEFAULT_DEATH_WAYPOINT_SIZE = 50;
	private static final float LEGACY_SIZE_REFERENCE_PERCENT = 15.0F;

	private Boolean autoReconnect = Boolean.TRUE;
	private Boolean screenshotHelper = Boolean.TRUE;
	private Boolean copyChat = Boolean.TRUE;
	private Boolean deathWaypoint = Boolean.TRUE;
	private Integer reconnectDelaySeconds = 8;
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

	public boolean copyChat() {
		return copyChat == null || copyChat;
	}

	public void setCopyChat(boolean enabled) {
		copyChat = enabled;
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
		if (copyChat == null) {
			copyChat = Boolean.TRUE;
		}
		if (deathWaypoint == null) {
			deathWaypoint = Boolean.TRUE;
		}
		reconnectDelaySeconds = reconnectDelaySeconds();
		deathWaypointOpacity = deathWaypointOpacity();
		deathWaypointSize = migrateDeathWaypointSize(deathWaypointSize);
		deathWaypointSize = deathWaypointSize();
	}

	private static int clampDelay(int seconds) {
		return Math.max(MIN_RECONNECT_DELAY_SECONDS, Math.min(MAX_RECONNECT_DELAY_SECONDS, seconds));
	}

	private static int clampOpacity(int opacity) {
		return Math.max(MIN_DEATH_WAYPOINT_OPACITY, Math.min(MAX_DEATH_WAYPOINT_OPACITY, opacity));
	}

	private static int clampSize(int sizePercent) {
		return Math.max(MIN_DEATH_WAYPOINT_SIZE, Math.min(MAX_DEATH_WAYPOINT_SIZE, sizePercent));
	}

	private static Integer migrateDeathWaypointSize(Integer sizePercent) {
		if (sizePercent == null) {
			return DEFAULT_DEATH_WAYPOINT_SIZE;
		}

		if (sizePercent >= MIN_DEATH_WAYPOINT_SIZE && sizePercent <= MAX_DEATH_WAYPOINT_SIZE) {
			return sizePercent;
		}

		return clampSize((int) Math.round(sizePercent * (DEFAULT_DEATH_WAYPOINT_SIZE / LEGACY_SIZE_REFERENCE_PERCENT)));
	}
}
