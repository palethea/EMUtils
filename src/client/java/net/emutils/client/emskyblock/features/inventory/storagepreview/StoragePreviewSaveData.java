package net.emutils.client.emskyblock.features.inventory.storagepreview;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StoragePreviewSaveData {
	private Map<String, StoragePreviewScopeData> scopes = new LinkedHashMap<>();

	public Map<String, StoragePreviewScopeData> scopes() {
		return scopes;
	}

	public void setScopes(Map<String, StoragePreviewScopeData> scopes) {
		this.scopes = scopes == null ? new LinkedHashMap<>() : scopes;
	}
}
