package net.emutils.client.emutils.debug;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.capes.CapeSource;
import net.emutils.client.emutils.commandshortcuts.CommandShortcutStore;
import net.emutils.client.emutils.inventory.InventoryToolsScopeData;
import net.emutils.client.emutils.inventory.InventoryToolsStore;
import net.emutils.client.emutils.inventory.MassDropStore;
import net.emutils.client.emutils.minescript.MinescriptKeybindStore;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.emutils.waypoint.Waypoint;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Checks that settings, keybinds and data files from an older release carry over (#267). The test run starts
 * with that release's files (src/test/carry-over/&lt;version&gt;, put in place by the build), in which every
 * setting differs from its default. Once the smoke launch is in its world, this checks that the current build
 * read every value and kept it, that no file was set aside as unreadable, and that the data files still load.
 */
public final class CarryOverCheck {
	private static final String PROPERTY = "emutils.carryOver";
	private static final Gson GSON = new Gson();

	private CarryOverCheck() {
	}

	public static boolean enabled() {
		return System.getProperty(PROPERTY) != null;
	}

	/** Runs the checks and logs each problem; the test script looks for the result line. */
	public static void run(Minecraft client) {
		Path fixture = Path.of(System.getProperty(PROPERTY));
		List<String> problems = new ArrayList<>();
		try {
			checkConfig(fixture, problems);
			checkKeybinds(client, fixture, problems);
			checkDataFiles(client, fixture, problems);
			checkNoBrokenFiles(problems);
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.error("EMUtils carry-over check couldn't finish.", exception);
			problems.add("the check couldn't finish: " + exception);
		}
		for (String problem : problems) {
			EMUtilsClient.LOGGER.error("EMUtils carry-over check FAILED: {}", problem);
		}
		if (problems.isEmpty()) {
			EMUtilsClient.LOGGER.info("EMUtils carry-over check passed for {}.", fixture.getFileName());
		} else {
			EMUtilsClient.LOGGER.error("EMUtils carry-over check found {} problem(s) for {}.", problems.size(), fixture.getFileName());
		}
	}

	private static void checkConfig(Path fixture, List<String> problems) throws IOException {
		JsonElement expected = read(fixture.resolve("config/emutils/config.json"));
		// The settings as loaded, and as the load saved them again.
		contains("settings", GSON.toJsonTree(EMUtilsClient.config()), expected, "", problems);
		contains("config.json", read(EMUtilsPaths.configFile()), expected, "", problems);

		// Settings an update replaced, which only count as kept if the new ones say the same. Only for files
		// from before the new setting; a newer file has it saved, and the check above covers it.
		JsonObject old = expected.getAsJsonObject();
		if (!old.has("capeOrder") && old.has("capePreferredProvider")) {
			CapeSource preferred = CapeSource.fromName(old.get("capePreferredProvider").getAsString());
			if (preferred != null && EMUtilsClient.config().capeOrder().getFirst() != preferred) {
				problems.add("the preferred cape provider " + preferred + " isn't first in the cape order " + EMUtilsClient.config().capeOrder());
			}
		}
		if (!old.has("spotifyEnabled")) {
			boolean spotify = flag(old, "spotifyPlayerEnabled") || flag(old, "spotifyHudOverlay");
			if (EMUtilsClient.config().spotifyEnabled() != spotify) {
				problems.add("Spotify is " + (spotify ? "off" : "on") + " after the update");
			}
		}
		if (!old.has("chatFeaturesEnabled")) {
			// Copy Chat is on by default, so a file without it had it on.
			boolean chat = !old.has("copyChat") || flag(old, "copyChat");
			chat |= Stream.of("chatTimestamps", "smartChatFilters", "chatMentionAlerts", "chatMentionHighlight").anyMatch(name -> flag(old, name));
			if (EMUtilsClient.config().chatFeaturesEnabled() != chat) {
				problems.add("the chat features are " + (chat ? "off" : "on") + " after the update");
			}
		}
	}

	private static void checkKeybinds(Minecraft client, Path fixture, List<String> problems) throws IOException {
		Map<String, String> expected = new LinkedHashMap<>();
		for (String line : Files.readAllLines(fixture.resolve("options.txt"))) {
			int colon = line.indexOf(':');
			if (line.startsWith("key_") && colon > 0) {
				expected.put(line.substring("key_".length(), colon), line.substring(colon + 1));
			}
		}
		for (Map.Entry<String, String> entry : expected.entrySet()) {
			KeyMapping mapping = null;
			for (KeyMapping candidate : client.options.keyMappings) {
				if (candidate.getName().equals(entry.getKey())) {
					mapping = candidate;
				}
			}
			if (mapping == null) {
				problems.add("the keybind " + entry.getKey() + " is gone");
			} else if (!mapping.saveString().equals(entry.getValue())) {
				problems.add("the keybind " + entry.getKey() + " is " + mapping.saveString() + " instead of " + entry.getValue());
			}
		}
	}

	private static void checkDataFiles(Minecraft client, Path fixture, List<String> problems) throws IOException {
		Path data = fixture.resolve("config/emutils");
		// Read again by the stores, so what they make of the files is checked, not just that the files are there.
		contains("command shortcuts", GSON.toJsonTree(CommandShortcutStore.load()), read(data.resolve("command-shortcuts.json")), "", problems);
		contains("mass drop", GSON.toJsonTree(MassDropStore.load()), read(data.resolve("mass-drop.json")), "", problems);
		contains("Minescript keybinds", GSON.toJsonTree(MinescriptKeybindStore.load()), read(data.resolve("minescript-keybinds.json")), "", problems);
		JsonObject scopes = read(data.resolve("inventory-tools.json")).getAsJsonObject().getAsJsonObject("scopes");
		for (String scope : scopes.keySet()) {
			InventoryToolsScopeData loaded = InventoryToolsStore.readScope(scope);
			contains("slot locks for " + scope, GSON.toJsonTree(loaded), scopes.get(scope), "", problems);
		}
		String scope = InventoryToolsStore.scopeKey(client);
		if (!scopes.has(scope)) {
			problems.add("the test world's slot locks are under " + scope + ", which the fixture doesn't have");
		}

		JsonElement waypoints = read(data.resolve("waypoints.json"));
		contains("waypoints.json", read(EMUtilsPaths.waypointFile()), waypoints, "", problems);
		// The waypoints made in this world before the update still show in it.
		List<String> shown = EMUtilsClient.waypoint().waypointsForCurrentWorld(client).stream().map(Waypoint::label).toList();
		for (JsonElement waypoint : waypoints.getAsJsonObject().getAsJsonArray("waypoints")) {
			JsonObject object = waypoint.getAsJsonObject();
			boolean here = object.get("serverAddress").getAsString().equals(scope)
				&& object.get("dimension").getAsString().equals(client.level.dimension().identifier().toString());
			String label = object.get("label").getAsString();
			if (here && !shown.contains(label)) {
				problems.add("the waypoint " + label + " doesn't show in its world; shown: " + shown);
			}
		}
	}

	private static void checkNoBrokenFiles(List<String> problems) throws IOException {
		try (Stream<Path> files = Files.walk(EMUtilsPaths.configDir())) {
			files.filter(file -> file.getFileName().toString().endsWith(".broken"))
				.forEach(file -> problems.add(EMUtilsPaths.configDir().relativize(file) + " was set aside as unreadable"));
		}
	}

	/** Adds a problem for everything in {@code expected} that {@code actual} doesn't have; more in {@code actual} is fine. */
	private static void contains(String what, JsonElement actual, JsonElement expected, String path, List<String> problems) {
		if (expected.isJsonObject()) {
			if (actual == null || !actual.isJsonObject()) {
				problems.add(what + " " + path + " is " + actual + " instead of " + expected);
				return;
			}
			JsonObject object = actual.getAsJsonObject();
			for (String key : expected.getAsJsonObject().keySet()) {
				String child = path.isEmpty() ? key : path + "." + key;
				if (!object.has(key)) {
					problems.add(what + " lost " + child + " (was " + expected.getAsJsonObject().get(key) + ")");
				} else {
					contains(what, object.get(key), expected.getAsJsonObject().get(key), child, problems);
				}
			}
		} else if (expected.isJsonArray()) {
			JsonArray list = expected.getAsJsonArray();
			if (actual == null || !actual.isJsonArray() || actual.getAsJsonArray().size() != list.size()) {
				problems.add(what + " " + path + " is " + actual + " instead of " + expected);
				return;
			}
			for (int i = 0; i < list.size(); i++) {
				contains(what, actual.getAsJsonArray().get(i), list.get(i), path + "[" + i + "]", problems);
			}
		} else if (!expected.equals(actual)) {
			problems.add(what + " " + path + " is " + actual + " instead of " + expected);
		}
	}

	private static boolean flag(JsonObject object, String name) {
		return object.has(name) && object.get(name).getAsBoolean();
	}

	private static JsonElement read(Path file) throws IOException {
		return JsonParser.parseString(Files.readString(file));
	}
}
