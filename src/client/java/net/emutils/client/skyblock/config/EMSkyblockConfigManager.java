package net.emutils.client.skyblock.config;

import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigPlatform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.hud.layout.HudLayoutManager;
import net.emutils.client.skyblock.tracker.FishingTrackerStorage;
import net.emutils.client.util.EMUtilsPaths;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class EMSkyblockConfigManager {
	private static final Gson SNAPSHOT_GSON = new GsonBuilder().create();
	private static @Nullable ManagedConfig<EMSkyblockConfig> managed;
	private static @Nullable String lastSavedSnapshot;

	private EMSkyblockConfigManager() {
	}

	public static void init(EMUtilsConfig legacyConfig) {
		if (MoulConfigPlatform.instance == null) {
			new MoulConfigPlatform();
		}

		var file = EMUtilsPaths.emSkyblockConfigFile().toFile();
		try {
			Files.createDirectories(file.getParentFile().toPath());
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to create EMSkyblock config directory.", exception);
		}

		boolean freshFile = !file.exists();
		var builder = new io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder<EMSkyblockConfig>(file, EMSkyblockConfig.class);
		builder.setSaveFailed((managedFile, exception) -> {
			net.emutils.client.EMUtilsClient.LOGGER.error("MoulConfig SAVE FAILED!", exception);
		});
		builder.setLoadFailed((managedFile, exception) -> {
			net.emutils.client.EMUtilsClient.LOGGER.error("MoulConfig LOAD FAILED!", exception);
		});
		builder.jsonMapper(mapper -> {
			mapper.setDoNotRequireExposed(true);
			return kotlin.Unit.INSTANCE;
		});
		managed = new io.github.notenoughupdates.moulconfig.managed.ManagedConfig<>(builder);
		if (freshFile) {
			managed.getInstance().applyLegacy(legacyConfig);
			save();
		}

		wireActions(managed.getInstance());
		lastSavedSnapshot = snapshot(managed.getInstance());
	}

	public static ManagedConfig<EMSkyblockConfig> managed() {
		if (managed == null) {
			throw new IllegalStateException("EMSkyblock config has not been initialized.");
		}

		return managed;
	}

	public static EMSkyblockConfig config() {
		return managed().getInstance();
	}

	public static void save() {
		if (managed != null) {
			managed.saveToFile();
			lastSavedSnapshot = snapshot(managed.getInstance());
		} else {
			net.emutils.client.EMUtilsClient.LOGGER.warn("Attempted to save EMSkyblock config but manager is null!");
		}
	}

	public static void saveIfDirty() {
		if (managed == null) {
			return;
		}

		String currentSnapshot = snapshot(managed.getInstance());
		if (!currentSnapshot.equals(lastSavedSnapshot)) {
			save();
		}
	}

	public static void resetToDefaults() {
		EMSkyblockConfig config = config();
		config.applyDefaults();
		wireActions(config);
		managed().rebuildConfigProcessor();
		save();
	}

	private static void wireActions(EMSkyblockConfig config) {
		config.eiv.openHudLayoutEditor = () -> HudLayoutManager.openEditor(MinecraftClient.getInstance());
		config.statsHud.openHudLayoutEditor = () -> HudLayoutManager.openEditor(MinecraftClient.getInstance());
		config.fishing.hookDisplay.openHudLayoutEditor = () -> HudLayoutManager.openEditor(MinecraftClient.getInstance());
		config.fishing.seaCreatureTracker.openHudLayoutEditor = () -> HudLayoutManager.openEditor(MinecraftClient.getInstance());
		config.fishing.fishingProfitTracker.openHudLayoutEditor = () -> HudLayoutManager.openEditor(MinecraftClient.getInstance());
		config.fishing.seaCreatureTracker.resetSession = FishingTrackerStorage::resetSessionSeaCreature;
		config.fishing.seaCreatureTracker.resetAllTime = FishingTrackerStorage::resetAllTimeSeaCreature;
		config.fishing.fishingProfitTracker.resetSession = FishingTrackerStorage::resetSessionProfit;
		config.fishing.fishingProfitTracker.resetAllTime = FishingTrackerStorage::resetAllTimeProfit;
		config.actions.resetDefaults = EMSkyblockConfigManager::resetToDefaults;
	}

	private static String snapshot(EMSkyblockConfig config) {
		return SNAPSHOT_GSON.toJson(config);
	}
}
