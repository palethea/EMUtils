package net.emutils.client.emutils.packs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.emutils.client.emhelpers.util.EMUtilsPaths;

public final class InstalledPackIndex {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final List<PackInstallRecord> records = new ArrayList<>();

	public static InstalledPackIndex load() {
		InstalledPackIndex index = null;
		if (Files.exists(EMUtilsPaths.packInstallIndexFile())) {
			try (Reader reader = Files.newBufferedReader(EMUtilsPaths.packInstallIndexFile())) {
				index = GSON.fromJson(reader, InstalledPackIndex.class);
			} catch (IOException | JsonParseException | IllegalStateException ignored) {
			}
		}

		return index == null ? new InstalledPackIndex() : index;
	}

	public synchronized List<PackInstallRecord> records() {
		return List.copyOf(records);
	}

	public synchronized Optional<PackInstallRecord> find(PackType type, String projectId) {
		return records.stream().filter(record -> record.matches(type, projectId)).findFirst();
	}

	public synchronized Optional<PackInstallRecord> findByFilename(PackType type, String filename) {
		return records.stream().filter(record -> record.type() == type && record.filename().equals(filename)).findFirst();
	}

	public synchronized void put(PackInstallRecord record) {
		remove(record.type(), record.projectId());
		records.add(record);
		save();
	}

	public synchronized void remove(PackType type, String projectId) {
		boolean changed = records.removeIf(record -> record.matches(type, projectId));
		if (changed) {
			save();
		}
	}

	public synchronized void removeMissing(PackType type, List<String> existingFilenames) {
		boolean changed = false;
		Iterator<PackInstallRecord> iterator = records.iterator();
		while (iterator.hasNext()) {
			PackInstallRecord record = iterator.next();
			if (record.type() == type && !existingFilenames.contains(record.filename())) {
				iterator.remove();
				changed = true;
			}
		}
		if (changed) {
			save();
		}
	}

	private void save() {
		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			try (Writer writer = Files.newBufferedWriter(EMUtilsPaths.packInstallIndexFile())) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}
}
