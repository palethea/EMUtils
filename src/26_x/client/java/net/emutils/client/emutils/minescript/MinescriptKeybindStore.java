package net.emutils.client.emutils.minescript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.emutils.client.emutils.util.EMUtilsPaths;
import java.nio.file.Files;

public final class MinescriptKeybindStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final List<MinescriptKeyBinding> bindings = new ArrayList<>();

	public static MinescriptKeybindStore load() {
		MinescriptKeybindStore store = null;
		if (Files.exists(EMUtilsPaths.minescriptKeybindFile())) {
			try (Reader reader = Files.newBufferedReader(EMUtilsPaths.minescriptKeybindFile())) {
				store = GSON.fromJson(reader, MinescriptKeybindStore.class);
			} catch (IOException | JsonParseException | IllegalStateException ignored) {
			}
		}
		return store == null ? new MinescriptKeybindStore() : store;
	}

	public synchronized List<MinescriptKeyBinding> bindings() {
		return List.copyOf(bindings);
	}

	public synchronized Optional<MinescriptKeyBinding> get(String command) {
		return bindings.stream().filter(binding -> binding.command().equals(command)).findFirst();
	}

	public synchronized Optional<MinescriptKeyBinding> duplicateOf(MinescriptKeyBinding candidate) {
		return bindings.stream()
			.filter(binding -> !binding.command().equals(candidate.command()) && binding.sameKeys(candidate))
			.findFirst();
	}

	public synchronized void put(MinescriptKeyBinding binding) {
		remove(binding.command());
		bindings.add(binding);
		save();
	}

	public synchronized void remove(String command) {
		if (bindings.removeIf(binding -> binding.command().equals(command))) {
			save();
		}
	}

	public synchronized void removeBinding(MinescriptKeyBinding binding) {
		if (bindings.removeIf(existing -> existing.command().equals(binding.command()))) {
			save();
		}
	}

	private void save() {
		try {
			Files.createDirectories(EMUtilsPaths.configDir());
			try (Writer writer = Files.newBufferedWriter(EMUtilsPaths.minescriptKeybindFile())) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}
}
