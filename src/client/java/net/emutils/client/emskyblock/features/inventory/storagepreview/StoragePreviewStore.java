package net.emutils.client.emskyblock.features.inventory.storagepreview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.util.EMUtilsPaths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.jspecify.annotations.Nullable;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.context.SkyblockFeatures;

public final class StoragePreviewStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final String SCOPE_PREFIX = "hypixel_skyblock";
	private static final String PROFILE_SUFFIX = ":profile:";
	private static @Nullable StoragePreviewSaveData cache;

	private StoragePreviewStore() {
	}

	public static void invalidateCache() {
		cache = null;
	}

	public static LoadedScope readScope(@Nullable String scopeKey) {
		Map<String, StoragePreviewRecord> records = new LinkedHashMap<>();
		Map<String, String> aliasIndex = new LinkedHashMap<>();
		if (scopeKey == null || scopeKey.isBlank()) {
			return new LoadedScope(records, aliasIndex);
		}

		StoragePreviewScopeData scope = ensureLoaded().scopes().get(scopeKey);
		if (scope == null) {
			return new LoadedScope(records, aliasIndex);
		}

		for (Map.Entry<String, StoragePreviewEntryData> entry : scope.storages().entrySet()) {
			StoragePreviewRecord record = decodeEntry(entry.getKey(), entry.getValue());
			if (record == null || !StoragePreviewFilters.isValidRecord(record)) {
				continue;
			}

			records.put(record.id(), record);
			indexAliases(aliasIndex, record);
		}

		return new LoadedScope(records, aliasIndex);
	}

	public static void writeScope(@Nullable String scopeKey, Map<String, StoragePreviewRecord> records) {
		if (scopeKey == null || scopeKey.isBlank()) {
			return;
		}

		StoragePreviewSaveData saveData = ensureLoaded();
		if (records.isEmpty()) {
			saveData.scopes().remove(scopeKey);
		} else {
			StoragePreviewScopeData scope = new StoragePreviewScopeData();
			Map<String, StoragePreviewEntryData> serialized = new LinkedHashMap<>();
			for (StoragePreviewRecord record : records.values()) {
				if (!StoragePreviewFilters.isValidRecord(record)) {
					continue;
				}

				StoragePreviewEntryData data = encodeEntry(record);
				if (data != null) {
					serialized.put(record.id(), data);
				}
			}
			scope.setStorages(serialized);
			saveData.scopes().put(scopeKey, scope);
		}

		saveAll(saveData);
	}

	public static void indexAliases(Map<String, String> aliasIndex, StoragePreviewRecord record) {
		for (String key : StoragePreviewKeys.derivedLookupKeys(
			StoragePreviewKeys.normalize(record.title()),
			record.aliases()
		)) {
			aliasIndex.put(key, record.id());
		}
	}

	@Nullable
	public static String scopeKey(MinecraftClient client) {
		if (!SkyblockFeatures.inSkyBlock(client)) {
			return null;
		}

		String profile = SkyblockContext.detectProfile(client);
		if (profile == null || profile.isBlank()) {
			return null;
		}

		return scopeKeyForProfile(profile);
	}

	public static String scopeKeyForProfile(String profile) {
		return SCOPE_PREFIX + PROFILE_SUFFIX + StoragePreviewKeys.normalize(profile);
	}

	@Nullable
	private static StoragePreviewRecord decodeEntry(String mapKey, @Nullable StoragePreviewEntryData data) {
		if (data == null || data.rows() <= 0) {
			return null;
		}

		String title = data.title();
		if (title == null || title.isBlank()) {
			title = mapKey;
		}

		String id = data.id();
		if (id == null || id.isBlank()) {
			id = StoragePreviewKeys.idFromTitle(title);
		}
		if (id == null || id.isBlank()) {
			return null;
		}

		List<String> aliases = data.aliases();
		if (aliases.isEmpty()) {
			aliases = StoragePreviewKeys.aliasesFromTitle(title);
		}

		int size = data.rows() * StoragePreviewCapture.COLUMNS;
		List<JsonElement> stacks = data.stacks();
		if (stacks.size() < size) {
			List<JsonElement> padded = new ArrayList<>(stacks);
			while (padded.size() < size) {
				padded.add(JsonNull.INSTANCE);
			}
			stacks = padded;
		}

		return new StoragePreviewRecord(id, title, aliases, data.rows(), stacks.subList(0, size));
	}

	@Nullable
	private static StoragePreviewEntryData encodeEntry(StoragePreviewRecord record) {
		if (record.rows() <= 0) {
			return null;
		}

		Set<String> aliases = new LinkedHashSet<>();
		for (String alias : record.aliases()) {
			aliases.add(StoragePreviewKeys.normalize(alias));
		}
		aliases.add(StoragePreviewKeys.normalize(record.title()));

		List<JsonElement> stacks = new ArrayList<>(record.stacks());
		while (stacks.size() < record.rows() * StoragePreviewCapture.COLUMNS) {
			stacks.add(JsonNull.INSTANCE);
		}

		StoragePreviewEntryData data = new StoragePreviewEntryData();
		data.setId(record.id());
		data.setTitle(record.title());
		data.setAliases(new ArrayList<>(aliases));
		data.setRows(record.rows());
		data.setStacks(stacks);
		return data;
	}

	private static StoragePreviewSaveData ensureLoaded() {
		if (cache != null) {
			return cache;
		}

		cache = loadFromDisk();
		return cache;
	}

	private static StoragePreviewSaveData loadFromDisk() {
		Path file = EMUtilsPaths.storagePreviewFile();
		if (!Files.exists(file)) {
			return new StoragePreviewSaveData();
		}

		try {
			String json = Files.readString(file);
			StoragePreviewSaveData saveData = GSON.fromJson(json, StoragePreviewSaveData.class);
			if (saveData == null) {
				return new StoragePreviewSaveData();
			}

			if (migrateLegacyScopes(saveData)) {
				saveAll(saveData);
			}

			return saveData;
		} catch (IOException | JsonParseException | IllegalStateException exception) {
			EMUtilsClient.LOGGER.warn("Failed to load Skyblock storage previews.", exception);
			backupCorruptFile(file);
			return new StoragePreviewSaveData();
		}
	}

	private static void backupCorruptFile(Path file) {
		try {
			Path backup = file.resolveSibling(
				file.getFileName().toString() + ".corrupt-" + Instant.now().toEpochMilli()
			);
			Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
			EMUtilsClient.LOGGER.warn("Backed up corrupt Skyblock storage previews to {}.", backup.getFileName());
		} catch (IOException moveException) {
			EMUtilsClient.LOGGER.warn("Failed to back up corrupt Skyblock storage previews.", moveException);
		}
	}

	private static boolean migrateLegacyScopes(StoragePreviewSaveData saveData) {
		Map<String, StoragePreviewScopeData> scopes = saveData.scopes();
		List<String> legacyKeys = new ArrayList<>();

		for (String key : scopes.keySet()) {
			if (key.startsWith(SCOPE_PREFIX + PROFILE_SUFFIX)) {
				continue;
			}

			int profileIndex = key.indexOf(PROFILE_SUFFIX);
			if (profileIndex < 0) {
				continue;
			}

			legacyKeys.add(key);
		}

		if (legacyKeys.isEmpty()) {
			return false;
		}

		for (String legacyKey : legacyKeys) {
			StoragePreviewScopeData legacyScope = scopes.remove(legacyKey);
			if (legacyScope == null) {
				continue;
			}

			String profile = legacyKey.substring(legacyKey.indexOf(PROFILE_SUFFIX) + PROFILE_SUFFIX.length());
			String newKey = scopeKeyForProfile(profile);
			StoragePreviewScopeData target = scopes.computeIfAbsent(newKey, ignored -> new StoragePreviewScopeData());
			for (Map.Entry<String, StoragePreviewEntryData> entry : legacyScope.storages().entrySet()) {
				target.storages().putIfAbsent(entry.getKey(), entry.getValue());
			}
		}

		EMUtilsClient.LOGGER.info("Migrated {} legacy Skyblock storage preview scope(s) to {}.", legacyKeys.size(), SCOPE_PREFIX);
		return true;
	}

	private static void saveAll(StoragePreviewSaveData saveData) {
		cache = saveData;

		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			Path file = EMUtilsPaths.storagePreviewFile();
			if (saveData.scopes().isEmpty()) {
				Files.deleteIfExists(file);
				return;
			}

			Path temp = file.resolveSibling(file.getFileName().toString() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temp)) {
				GSON.toJson(saveData, writer);
			}

			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to save Skyblock storage previews.", exception);
		}
	}

	public record LoadedScope(Map<String, StoragePreviewRecord> records, Map<String, String> aliasIndex) {
	}
}
