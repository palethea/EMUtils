package net.emutils.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.emutils.client.util.EMUtilsPaths;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;

public final class EMUtilsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int MIN_RECONNECT_DELAY_SECONDS = 5;
	private static final int MAX_RECONNECT_DELAY_SECONDS = 15;

	private Boolean autoReconnect = Boolean.TRUE;
	private Integer reconnectDelaySeconds = 8;

	public static EMUtilsConfig load() {
		EMUtilsConfig config = null;

		if (Files.exists(EMUtilsPaths.configFile())) {
			try (Reader reader = Files.newBufferedReader(EMUtilsPaths.configFile())) {
				config = GSON.fromJson(reader, EMUtilsConfig.class);
			} catch (IOException ignored) {
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
		reconnectDelaySeconds = reconnectDelaySeconds();
	}

	private static int clampDelay(int seconds) {
		return Math.max(MIN_RECONNECT_DELAY_SECONDS, Math.min(MAX_RECONNECT_DELAY_SECONDS, seconds));
	}
}
