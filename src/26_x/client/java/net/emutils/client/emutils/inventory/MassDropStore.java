package net.emutils.client.emutils.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import net.emutils.client.emutils.util.AtomicFiles;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.emutils.util.DataFiles;

public final class MassDropStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final Set<String> itemIds = new LinkedHashSet<>();
	private MassDropMode mode = MassDropMode.LEGIT;

	public static MassDropStore load() {
		MassDropStore store = DataFiles.readJsonOrNull(EMUtilsPaths.massDropFile(), GSON, MassDropStore.class);
		return store == null ? new MassDropStore() : store;
	}

	public synchronized Set<String> itemIds() {
		return Set.copyOf(itemIds);
	}

	public synchronized boolean contains(String itemId) {
		return itemIds.contains(itemId);
	}

	public synchronized void toggle(String itemId) {
		if (!itemIds.remove(itemId)) {
			itemIds.add(itemId);
		}
		save();
	}

	public synchronized MassDropMode mode() {
		return mode == null ? MassDropMode.LEGIT : mode;
	}

	public synchronized void setMode(MassDropMode mode) {
		this.mode = mode;
		save();
	}

	public synchronized void cycleMode() {
		mode = mode().next();
		save();
	}

	private void save() {
		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			AtomicFiles.writeString(EMUtilsPaths.massDropFile(), GSON.toJson(this));
		} catch (IOException ignored) {
		}
	}
}
