package net.emutils.client.emutils.commandshortcuts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.emutils.client.emhelpers.input.StoredKeyCombo;
import net.emutils.client.emhelpers.util.EMUtilsPaths;

public final class CommandShortcutStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private List<CommandShortcut> shortcuts = new ArrayList<>();

	public static CommandShortcutStore load() {
		CommandShortcutStore store = null;
		if (Files.exists(EMUtilsPaths.commandShortcutsFile())) {
			try (Reader reader = Files.newBufferedReader(EMUtilsPaths.commandShortcutsFile())) {
				store = GSON.fromJson(reader, CommandShortcutStore.class);
			} catch (IOException | JsonParseException | IllegalStateException ignored) {
			}
		}

		if (store == null) {
			store = new CommandShortcutStore();
		}
		store.normalize();
		return store;
	}

	public synchronized List<CommandShortcut> shortcuts() {
		normalize();
		return List.copyOf(shortcuts);
	}

	public synchronized Optional<CommandShortcut> get(String id) {
		normalize();
		return shortcuts.stream().filter(shortcut -> shortcut.id().equals(id)).findFirst();
	}

	public synchronized Optional<CommandShortcut> duplicateOf(StoredKeyCombo candidate, String currentId) {
		if (candidate == null) {
			return Optional.empty();
		}

		normalize();
		return shortcuts.stream()
			.filter(shortcut -> !Objects.equals(shortcut.id(), currentId))
			.filter(shortcut -> candidate.sameKeys(shortcut.keyCombo()))
			.findFirst();
	}

	public synchronized void put(CommandShortcut shortcut) {
		if (shortcut == null || !shortcut.valid()) {
			return;
		}

		normalize();
		shortcuts.removeIf(existing -> existing.id().equals(shortcut.id()));
		shortcuts.add(shortcut);
		save();
	}

	public synchronized void remove(String id) {
		normalize();
		if (shortcuts.removeIf(shortcut -> shortcut.id().equals(id))) {
			save();
		}
	}

	public synchronized void clear() {
		normalize();
		if (!shortcuts.isEmpty()) {
			shortcuts.clear();
			save();
		}
	}

	private void normalize() {
		if (shortcuts == null) {
			shortcuts = new ArrayList<>();
		}
		shortcuts.removeIf(shortcut -> shortcut == null || !shortcut.valid());
	}

	private void save() {
		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			try (Writer writer = Files.newBufferedWriter(EMUtilsPaths.commandShortcutsFile())) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}
}
