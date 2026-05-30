package net.emutils.client.emskyblock.features.inventory.storagepreview;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StoragePreviewScopeData {
	private Map<String, StoragePreviewEntryData> storages = new LinkedHashMap<>();

	public Map<String, StoragePreviewEntryData> storages() {
		return storages;
	}

	public void setStorages(Map<String, StoragePreviewEntryData> storages) {
		this.storages = storages == null ? new LinkedHashMap<>() : storages;
	}
}
