package net.emutils.client.emskyblock.features.inventory.bazaar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.util.EMUtilsPaths;
import net.emutils.client.emskyblock.context.SkyblockContext;

public final class BazaarLimitStorage {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String DEFAULT_SCOPE = "default";
	private static RootData cache;

	private BazaarLimitStorage() {
	}

	public static double coinsTowardsLimit() {
		ScopeData scope = scope();
		resetIfNewDay(scope);
		return scope.coinsTowardsLimit;
	}

	public static void addCoins(double coins) {
		if (coins <= 0.0D) {
			return;
		}

		ScopeData scope = scope();
		resetIfNewDay(scope);
		scope.coinsTowardsLimit += coins;
		save();
	}

	private static void resetIfNewDay(ScopeData scope) {
		String today = LocalDate.now(ZoneOffset.UTC).toString();
		if (!today.equals(scope.lastAccessedDay)) {
			scope.lastAccessedDay = today;
			scope.coinsTowardsLimit = 0.0D;
			save();
		}
	}

	private static ScopeData scope() {
		RootData root = ensureLoaded();
		String profile = SkyblockContext.detectProfile(net.minecraft.client.MinecraftClient.getInstance());
		String scope = profile == null || profile.isBlank() ? DEFAULT_SCOPE : "profile:" + profile.toLowerCase();
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
			RootData loaded = GSON.fromJson(reader, RootData.class);
			cache = loaded == null ? new RootData() : loaded;
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to load Bazaar tracker data.", exception);
			cache = new RootData();
		}
		return cache;
	}

	private static void save() {
		if (cache == null) {
			return;
		}

		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(file)) {
				GSON.toJson(cache, writer);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to save Bazaar tracker data.", exception);
		}
	}

	private static Path file() {
		return EMUtilsPaths.configDir().resolve("skyblock-bazaar.json");
	}

	public static final class RootData {
		public Map<String, ScopeData> scopes = new HashMap<>();
	}

	public static final class ScopeData {
		public double coinsTowardsLimit = 0.0D;
		public String lastAccessedDay = "";
	}
}
