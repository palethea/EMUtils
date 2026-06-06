package net.emutils.client.emutils.inventory;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryToolsSaveData {
	private Map<String, InventoryToolsScopeData> scopes = new LinkedHashMap<>();

	public InventoryToolsSaveData() {
	}

	public Map<String, InventoryToolsScopeData> scopes() {
		return scopes;
	}

	public void setScopes(Map<String, InventoryToolsScopeData> scopes) {
		this.scopes = scopes == null ? new LinkedHashMap<>() : scopes;
	}
}
