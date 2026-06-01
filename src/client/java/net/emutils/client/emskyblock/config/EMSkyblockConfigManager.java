package net.emutils.client.emskyblock.config;

import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigPlatform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emskyblock.config.ConfigVersionDisplay;
import net.emutils.client.emskyblock.config.GuiOptionEditorVersion;
import net.emutils.client.emskyblock.features.fishing.trackercommon.FishingTrackerStorage;
import net.emutils.client.emhelpers.util.EMUtilsPaths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
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
		boolean hasCategoryLayout = configFileContainsCategoryLayout(file.toPath());
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
		builder.customProcessor(ConfigVersionDisplay.class, (option, annotation) -> new GuiOptionEditorVersion(option));
		managed = new io.github.notenoughupdates.moulconfig.managed.ManagedConfig<>(builder);
		if (freshFile) {
			managed.getInstance().applyLegacy(legacyConfig);
		}
		managed.getInstance().migrateCategoryLayout(!hasCategoryLayout);
		if (freshFile || !hasCategoryLayout) {
			save();
		}

		migrateLegacyGeneralToggle(managed.getInstance());
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

	private static boolean configFileContainsCategoryLayout(java.nio.file.Path file) {
		try {
			return Files.exists(file) && Files.readString(file).contains("\"inventory\"");
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to inspect EMSkyblock config layout version.", exception);
			return false;
		}
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
		config.slayer.openHudLayoutEditor = () -> HudLayoutManager.openEditor(MinecraftClient.getInstance());
		config.slayer.resetSession = net.emutils.client.emskyblock.features.slayer.slayertracker.SlayerTrackerStorage::resetSessionAll;
		config.slayer.resetAllTime = net.emutils.client.emskyblock.features.slayer.slayertracker.SlayerTrackerStorage::resetAllTimeAll;
		config.actions.resetDefaults = EMSkyblockConfigManager::resetToDefaults;
		config.about.licenses.moulConfig = () -> openUrl("https://github.com/NotEnoughUpdates/MoulConfig");
		config.about.licenses.fabricLoader = () -> openUrl("https://github.com/FabricMC/fabric-loader");
		config.about.licenses.fabricApi = () -> openUrl("https://github.com/FabricMC/fabric-api");
		config.about.licenses.mixin = () -> openUrl("https://github.com/FabricMC/Mixin");
		config.about.licenses.mixinExtras = () -> openUrl("https://github.com/LlamaLad7/MixinExtras");
		config.about.licenses.skyHanniRepo = () -> openUrl("https://github.com/hannibal002/SkyHanni-REPO");
	}

	private static void migrateLegacyGeneralToggle(EMSkyblockConfig config) {
		if (config.general.enabled) {
			config.about.enabled = true;
			config.general.enabled = false;
			save();
		}
	}

	private static String snapshot(EMSkyblockConfig config) {
		return SNAPSHOT_GSON.toJson(config);
	}

	private static void openUrl(String url) {
		Util.getOperatingSystem().open(url);
	}
}
