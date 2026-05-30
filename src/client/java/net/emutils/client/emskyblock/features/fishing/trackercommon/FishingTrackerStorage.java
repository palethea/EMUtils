package net.emutils.client.emskyblock.features.fishing.trackercommon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.features.fishing.profittracker.FishingProfitTrackerData;
import net.emutils.client.emskyblock.features.fishing.seacreaturetracker.SeaCreatureTrackerData;
import net.emutils.client.emhelpers.util.EMUtilsPaths;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class FishingTrackerStorage {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type ROOT_TYPE = new TypeToken<RootData>() {}.getType();
	private static final String PROFILE_PREFIX = "profile:";

	private static @Nullable RootData cache;
	private static @Nullable String activeScope;

	private FishingTrackerStorage() {
	}

	public static SeaCreatureTrackerData seaCreature(TrackerDisplayMode mode) {
		return scope().seaCreature.mode(mode);
	}

	public static FishingProfitTrackerData fishingProfit(TrackerDisplayMode mode) {
		return scope().profit.mode(mode);
	}

	public static void saveIfDirty() {
		if (cache == null) {
			return;
		}

		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(file)) {
				GSON.toJson(cache, ROOT_TYPE, writer);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to save fishing tracker data.", exception);
		}
	}

	public static void onProfileScopeChanged(@Nullable MinecraftClient client) {
		String scope = resolveScope(client);
		if (scope == null) {
			activeScope = null;
			return;
		}

		if (!scope.equals(activeScope)) {
			activeScope = scope;
			resetSessionSeaCreature();
			resetSessionProfit();
		}
	}

	public static void resetSessionSeaCreature() {
		scope().seaCreature.session().resetCounts();
		saveIfDirty();
	}

	public static void resetAllTimeSeaCreature() {
		scope().seaCreature.allTime().resetCounts();
		saveIfDirty();
	}

	public static void resetSessionProfit() {
		scope().profit.session().reset();
		saveIfDirty();
	}

	public static void resetAllTimeProfit() {
		scope().profit.allTime().reset();
		saveIfDirty();
	}

	private static ScopeData scope() {
		RootData root = ensureLoaded();
		String scope = activeScope;
		if (scope == null) {
			scope = "default";
		}

		return root.scopes.computeIfAbsent(scope, ignored -> new ScopeData());
	}

	private static RootData ensureLoaded() {
		if (cache != null) {
			return cache;
		}

		Path file = file();
		if (!Files.exists(file)) {
			cache = new RootData();
			return cache;
		}

		try (BufferedReader reader = Files.newBufferedReader(file)) {
			RootData loaded = GSON.fromJson(reader, ROOT_TYPE);
			cache = loaded == null ? new RootData() : loaded;
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to load fishing tracker data.", exception);
			cache = new RootData();
		}

		return cache;
	}

	@Nullable
	private static String resolveScope(@Nullable MinecraftClient client) {
		String profile = SkyblockContext.detectProfile(client);
		if (profile == null || profile.isBlank()) {
			return null;
		}

		return PROFILE_PREFIX + profile.toLowerCase();
	}

	private static Path file() {
		return EMUtilsPaths.configDir().resolve("fishing-trackers.json");
	}

	public static final class RootData {
		public Map<String, ScopeData> scopes = new HashMap<>();
	}

	public static final class ScopeData {
		public SeaCreatureDualData seaCreature = new SeaCreatureDualData();
		public ProfitDualData profit = new ProfitDualData();
	}

	public static final class SeaCreatureDualData {
		public SeaCreatureTrackerData session = new SeaCreatureTrackerData();
		public SeaCreatureTrackerData allTime = new SeaCreatureTrackerData();

		public SeaCreatureTrackerData mode(TrackerDisplayMode mode) {
			return mode == TrackerDisplayMode.SESSION ? session() : allTime();
		}

		public SeaCreatureTrackerData session() {
			if (session == null) {
				session = new SeaCreatureTrackerData();
			}

			return session;
		}

		public SeaCreatureTrackerData allTime() {
			if (allTime == null) {
				allTime = new SeaCreatureTrackerData();
			}

			return allTime;
		}
	}

	public static final class ProfitDualData {
		public FishingProfitTrackerData session = new FishingProfitTrackerData();
		public FishingProfitTrackerData allTime = new FishingProfitTrackerData();

		public FishingProfitTrackerData mode(TrackerDisplayMode mode) {
			return mode == TrackerDisplayMode.SESSION ? session() : allTime();
		}

		public FishingProfitTrackerData session() {
			if (session == null) {
				session = new FishingProfitTrackerData();
			}

			return session;
		}

		public FishingProfitTrackerData allTime() {
			if (allTime == null) {
				allTime = new FishingProfitTrackerData();
			}

			return allTime;
		}
	}
}
