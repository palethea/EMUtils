package net.emutils.client.emskyblock.features.inventory.storagepreview;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;

public final class StoragePreviewEntryData {
	private String id;
	private String title;
	private List<String> aliases = new ArrayList<>();
	private int rows;
	private List<JsonElement> stacks = new ArrayList<>();

	public String id() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String title() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> aliases() {
		return aliases;
	}

	public void setAliases(List<String> aliases) {
		this.aliases = aliases == null ? new ArrayList<>() : aliases;
	}

	public int rows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public List<JsonElement> stacks() {
		return stacks;
	}

	public void setStacks(List<JsonElement> stacks) {
		this.stacks = stacks == null ? new ArrayList<>() : stacks;
	}
}
