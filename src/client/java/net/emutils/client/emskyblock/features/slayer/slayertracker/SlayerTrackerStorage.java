package net.emutils.client.emskyblock.features.slayer.slayertracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerDisplayMode;
import net.emutils.client.emskyblock.features.slayer.common.SlayerBossType;
import net.emutils.client.emhelpers.util.EMUtilsPaths;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class SlayerTrackerStorage {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type ROOT_TYPE = new TypeToken<RootData>() {}.getType();
	private static final String PROFILE_PREFIX = "profile:";

	private static @Nullable RootData cache;
	private static @Nullable String activeScope;

	private SlayerTrackerStorage() {}

	public static SlayerTrackerData bucket(
		SlayerBossType boss,
		TrackerDisplayMode mode
	) {
		return scope().buckets.computeIfAbsent(
			boss.name().toLowerCase(Locale.ROOT),
			ignored -> new BucketData()
		).mode(mode);
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
			EMUtilsClient.LOGGER.warn("Failed to save slayer tracker data.", exception);
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
			resetSessionAll();
		}
	}

	public static void resetSessionAll() {
		for (BucketData bucket : scope().buckets.values()) {
			bucket.session().reset();
		}
		saveIfDirty();
	}

	public static void resetAllTimeAll() {
		for (BucketData bucket : scope().buckets.values()) {
			bucket.allTime().reset();
		}
		saveIfDirty();
	}

	public static void resetSession(SlayerBossType boss) {
		bucket(boss, TrackerDisplayMode.SESSION).reset();
		saveIfDirty();
	}

	public static void resetAllTime(SlayerBossType boss) {
		bucket(boss, TrackerDisplayMode.ALL_TIME).reset();
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
			EMUtilsClient.LOGGER.warn("Failed to load slayer tracker data.", exception);
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
		return EMUtilsPaths.configDir().resolve("slayer-trackers.json");
	}

	public static final class RootData {
		public Map<String, ScopeData> scopes = new HashMap<>();
	}

	public static final class ScopeData {
		public Map<String, BucketData> buckets = new HashMap<>();
	}

	public static final class BucketData {
		public SlayerTrackerData session = new SlayerTrackerData();
		public SlayerTrackerData allTime = new SlayerTrackerData();

		public SlayerTrackerData mode(TrackerDisplayMode mode) {
			return mode == TrackerDisplayMode.SESSION ? session() : allTime();
		}

		public SlayerTrackerData session() {
			if (session == null) {
				session = new SlayerTrackerData();
			}
			return session;
		}

		public SlayerTrackerData allTime() {
			if (allTime == null) {
				allTime = new SlayerTrackerData();
			}
			return allTime;
		}
	}
}
