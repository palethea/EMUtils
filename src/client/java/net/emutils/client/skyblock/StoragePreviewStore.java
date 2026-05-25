package net.emutils.client.skyblock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.inventory.InventoryToolsStore;
import net.emutils.client.util.EMUtilsPaths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.jspecify.annotations.Nullable;

public final class StoragePreviewStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private StoragePreviewStore() {
	}

	public static LoadedScope readScope(@Nullable String scopeKey) {
		Map<String, StoragePreviewRecord> records = new LinkedHashMap<>();
		Map<String, String> aliasIndex = new LinkedHashMap<>();
		if (scopeKey == null || scopeKey.isBlank()) {
			return new LoadedScope(records, aliasIndex);
		}

		StoragePreviewScopeData scope = loadAll().scopes().get(scopeKey);
		if (scope == null) {
			return new LoadedScope(records, aliasIndex);
		}

		for (Map.Entry<String, StoragePreviewEntryData> entry : scope.storages().entrySet()) {
			StoragePreviewRecord record = decodeEntry(entry.getKey(), entry.getValue());
			if (record == null) {
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

		StoragePreviewSaveData saveData = loadAll();
		if (records.isEmpty()) {
			saveData.scopes().remove(scopeKey);
		} else {
			StoragePreviewScopeData scope = new StoragePreviewScopeData();
			Map<String, StoragePreviewEntryData> serialized = new LinkedHashMap<>();
			for (StoragePreviewRecord record : records.values()) {
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
		String base = InventoryToolsStore.scopeKey(client);
		if (base == null) {
			return null;
		}

		if (!SkyblockProfileDetector.isHypixel(client)) {
			return base;
		}

		String profile = SkyblockProfileDetector.detect(client);
		if (profile == null || profile.isBlank()) {
			return null;
		}

		return base + ":profile:" + profile;
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

	private static StoragePreviewSaveData loadAll() {
		if (!Files.exists(EMUtilsPaths.storagePreviewFile())) {
			return new StoragePreviewSaveData();
		}

		try {
			String json = Files.readString(EMUtilsPaths.storagePreviewFile());
			StoragePreviewSaveData saveData = GSON.fromJson(json, StoragePreviewSaveData.class);
			return saveData == null ? new StoragePreviewSaveData() : saveData;
		} catch (IOException | JsonParseException | IllegalStateException exception) {
			EMUtilsClient.LOGGER.warn("Failed to load Skyblock storage previews.", exception);
			return new StoragePreviewSaveData();
		}
	}

	private static void saveAll(StoragePreviewSaveData saveData) {
		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			if (saveData.scopes().isEmpty()) {
				Files.deleteIfExists(EMUtilsPaths.storagePreviewFile());
				return;
			}

			try (Writer writer = Files.newBufferedWriter(EMUtilsPaths.storagePreviewFile())) {
				GSON.toJson(saveData, writer);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to save Skyblock storage previews.", exception);
		}
	}

	public record LoadedScope(Map<String, StoragePreviewRecord> records, Map<String, String> aliasIndex) {
	}
}
