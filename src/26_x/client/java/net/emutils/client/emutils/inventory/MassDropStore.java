package net.emutils.client.emutils.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import net.emutils.client.emutils.util.EMUtilsPaths;

public final class MassDropStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final Set<String> itemIds = new LinkedHashSet<>();
	private MassDropMode mode = MassDropMode.LEGIT;

	public static MassDropStore load() {
		if (!Files.isRegularFile(EMUtilsPaths.massDropFile())) {
			return new MassDropStore();
		}
		try (Reader reader = Files.newBufferedReader(EMUtilsPaths.massDropFile())) {
			MassDropStore store = GSON.fromJson(reader, MassDropStore.class);
			return store == null ? new MassDropStore() : store;
		} catch (IOException | RuntimeException ignored) {
			return new MassDropStore();
		}
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

	public synchronized void cycleMode() {
		mode = mode().next();
		save();
	}

	private void save() {
		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			try (Writer writer = Files.newBufferedWriter(EMUtilsPaths.massDropFile())) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}
}
