package net.emutils.client.emutils.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import org.jspecify.annotations.Nullable;

/**
 * The worlds kept on one server, or in one save, for one dimension (#219). Servers like Hypixel run many
 * worlds that all call themselves the Overworld (the hub, private islands, dungeons, lobbies), so each gets a
 * map of its own: a folder per world in the dimension's folder, listed in {@code worlds.json} with its name,
 * spawn point and height range, which help recognise it next time. Maps saved before worlds were told apart
 * move into the first world. Client thread only.
 */
final class MapWorlds {
	private static final String FILE = "worlds.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	/** One catalog per dimension folder, shared by the minimap and the world map. */
	private static final Map<Path, MapWorlds> OPEN = new ConcurrentHashMap<>();

	/** A world: its folder's name, the name shown, and what it looked like when last seen. */
	static final class Entry {
		String id;
		String name;
		int @Nullable [] spawn;
		@Nullable Integer minY;
		@Nullable Integer height;
		long lastSeen;

		String id() {
			return id;
		}

		String name() {
			return name;
		}
	}

	private static final class Data {
		List<Entry> worlds = new ArrayList<>();
	}

	private final Path folder;
	private final String dimension;
	private final Data data;

	private MapWorlds(Path folder, String dimension, Data data) {
		this.folder = folder;
		this.dimension = dimension;
		this.data = data;
	}

	/** The worlds kept in a dimension's folder, read from disk the first time. */
	static MapWorlds of(Path dimensionFolder, String dimension) {
		return OPEN.computeIfAbsent(dimensionFolder, path -> load(path, dimension));
	}

	private static MapWorlds load(Path folder, String dimension) {
		Data data = null;
		Path file = folder.resolve(FILE);
		if (Files.isRegularFile(file)) {
			try {
				data = GSON.fromJson(Files.readString(file), Data.class);
			} catch (IOException | RuntimeException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils map couldn't read {}", file, exception);
			}
		}
		MapWorlds worlds = new MapWorlds(folder, dimension, data == null || data.worlds == null ? new Data() : data);
		worlds.data.worlds.removeIf(entry -> entry == null || entry.id == null || entry.id.isBlank());
		worlds.migrate();
		return worlds;
	}

	/** Maps saved before worlds were told apart lie loose in the dimension's folder: they become the first world. */
	private void migrate() {
		List<Path> loose = new ArrayList<>();
		if (Files.isDirectory(folder)) {
			try (Stream<Path> files = Files.list(folder)) {
				files.filter(path -> path.getFileName().toString().endsWith(".emap")).forEach(loose::add);
			} catch (IOException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils map couldn't list {}", folder, exception);
			}
		}
		if (loose.isEmpty()) {
			return;
		}
		Entry first = data.worlds.isEmpty() ? newEntry() : data.worlds.getFirst();
		Path target = folder(first.id);
		try {
			Files.createDirectories(target);
			for (Path file : loose) {
				Files.move(file, target.resolve(file.getFileName()));
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't move the map in {} to {}", folder, target, exception);
		}
		save();
	}

	List<Entry> worlds() {
		return List.copyOf(data.worlds);
	}

	@Nullable Entry get(@Nullable String id) {
		for (Entry entry : data.worlds) {
			if (entry.id.equals(id)) {
				return entry;
			}
		}
		return null;
	}

	/** The world seen last, or null when there's none yet. */
	@Nullable Entry latest() {
		return data.worlds.stream().max(Comparator.comparingLong(entry -> entry.lastSeen)).orElse(null);
	}

	/** A new world, named after how many there are. */
	Entry create() {
		Entry entry = newEntry();
		save();
		return entry;
	}

	private Entry newEntry() {
		int number = 1;
		while (get("w" + number) != null) {
			number++;
		}
		Entry entry = new Entry();
		entry.id = "w" + number;
		entry.name = "World " + number;
		data.worlds.add(entry);
		return entry;
	}

	/** Remembers what a world looked like now, so it's recognised next time. */
	void seen(Entry entry, int @Nullable [] spawn, int minY, int height) {
		if (spawn != null) {
			entry.spawn = spawn;
		}
		entry.minY = minY;
		entry.height = height;
		entry.lastSeen = System.currentTimeMillis();
		save();
	}

	void rename(String id, String name) {
		Entry entry = get(id);
		if (entry != null && !name.isBlank()) {
			entry.name = name.strip();
			save();
		}
	}

	/** Forgets a world and deletes its map, in the background. */
	void delete(String id) {
		Entry entry = get(id);
		if (entry == null) {
			return;
		}
		data.worlds.remove(entry);
		save();
		Path target = folder(id);
		MapWorld.runIo(() -> deleteTree(target));
	}

	/** The folder a world's surface map is saved in. */
	Path folder(String id) {
		return folder.resolve(id);
	}

	/** The folder one of a world's cave layers is saved in (#222). */
	Path caveFolder(String id, int layer) {
		return folder(id).resolve("caves").resolve(Integer.toString(layer));
	}

	String dimension() {
		return dimension;
	}

	private void save() {
		try {
			Files.createDirectories(folder);
			Files.writeString(folder.resolve(FILE), GSON.toJson(data));
			// The dimension's id, beside its worlds, so the world map can name the folder.
			Path dimensionFile = folder.resolve(MapWorld.DIMENSION_FILE);
			if (!Files.exists(dimensionFile)) {
				Files.writeString(dimensionFile, dimension);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't save {}", folder.resolve(FILE), exception);
		}
	}

	private static void deleteTree(Path root) {
		if (!Files.exists(root)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(root)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils map couldn't delete {}", root, exception);
		}
	}
}
