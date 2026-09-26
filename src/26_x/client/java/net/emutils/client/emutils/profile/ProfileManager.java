package net.emutils.client.emutils.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.AtomicFiles;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Config profiles (#89): named sets of EMUtils settings to switch between, which can load by themselves
 * when you join a server or a singleplayer world.
 *
 * <p>The Default profile keeps its settings in {@code config.json}, as before profiles existed, so
 * nothing moves for players who never use them. Every other profile has its own file in
 * {@code profiles/}, and {@code profiles.json} lists them all. Profiles hold every setting in the
 * config, including the HUD layout. Keybinds are Minecraft's own and stay the same for every profile,
 * like the lists that have their own files (waypoints, shortcuts, mass drop, slot locks), and so does
 * the settings UI's dark or light look.
 *
 * <p>Changes save to the active profile as they're made, so switching never asks about unsaved changes.
 */
public final class ProfileManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Pattern ID = Pattern.compile("[a-z0-9]{1,16}");
	private static final int EXPORT_VERSION = 1;

	private final List<Profile> profiles = new ArrayList<>();
	private String active = Profile.DEFAULT_ID;
	/**
	 * The profile last picked by hand. Joining a place no profile is linked to goes back to it, so an
	 * automatic switch lasts only as long as you're on that server.
	 */
	private String picked = Profile.DEFAULT_ID;

	private ProfileManager() {
	}

	/** Reads the profile list; a missing or unreadable one starts with just the Default profile. */
	public static ProfileManager load() {
		ProfileManager manager = new ProfileManager();
		Path file = EMUtilsPaths.profilesFile();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				SaveData data = GSON.fromJson(reader, SaveData.class);
				if (data != null) {
					manager.read(data);
				}
			} catch (IOException | JsonParseException | IllegalStateException exception) {
				EMUtilsClient.LOGGER.warn("Could not read the EMUtils profile list; only the Default profile is listed.", exception);
			}
		}
		manager.ensureDefault();
		return manager;
	}

	private void read(SaveData data) {
		if (data.profiles != null) {
			for (Profile profile : data.profiles) {
				// Ids name files, so anything unexpected in a hand-edited list is skipped.
				if (profile != null && profile.id() != null && ID.matcher(profile.id()).matches() && byId(profile.id()) == null) {
					profile.repair();
					profiles.add(profile);
				}
			}
		}
		active = data.active == null ? Profile.DEFAULT_ID : data.active;
		picked = data.picked == null ? active : data.picked;
	}

	private void ensureDefault() {
		if (byId(Profile.DEFAULT_ID) == null) {
			profiles.addFirst(new Profile(Profile.DEFAULT_ID, null, ProfileIcon.GLOBE, ProfileColor.GRAY));
		}
		if (byId(active) == null) {
			active = Profile.DEFAULT_ID;
		}
		if (byId(picked) == null) {
			picked = active;
		}
	}

	/** Loads the active profile's settings, at startup. */
	public EMUtilsConfig loadActive() {
		return EMUtilsConfig.load(file(active()));
	}

	public List<Profile> profiles() {
		return List.copyOf(profiles);
	}

	public Profile active() {
		Profile profile = byId(active);
		return profile == null ? profiles.getFirst() : profile;
	}

	public @Nullable Profile byId(String id) {
		for (Profile profile : profiles) {
			if (profile.id().equals(id)) {
				return profile;
			}
		}
		return null;
	}

	/** The profile with this name, ignoring case, for the command. */
	public @Nullable Profile byName(String name) {
		for (Profile profile : profiles) {
			if (profile.name().getString().equalsIgnoreCase(name.strip())) {
				return profile;
			}
		}
		return null;
	}

	private static Path file(Profile profile) {
		return profile.isDefault() ? EMUtilsPaths.configFile() : EMUtilsPaths.profilesDir().resolve(profile.id() + ".json");
	}

	// ---- switching ------------------------------------------------------------------------------

	/**
	 * Switches to {@code profile} because the player picked it. Returns false if the current settings
	 * couldn't be saved first, and then stays on the current profile so nothing is lost.
	 */
	public boolean pick(Profile profile) {
		if (!switchTo(profile)) {
			return false;
		}
		picked = profile.id();
		save();
		return true;
	}

	/** Picks the profile after the active one, going around; for the keybind. */
	public @Nullable Profile pickNext() {
		if (profiles.size() < 2) {
			return null;
		}
		Profile next = profiles.get((profiles.indexOf(active()) + 1) % profiles.size());
		return pick(next) ? next : null;
	}

	private boolean switchTo(Profile profile) {
		EMUtilsConfig current = EMUtilsClient.config();
		if (profile.id().equals(active) && current != null) {
			return true;
		}
		if (current != null && !current.flush()) {
			return false;
		}
		EMUtilsConfig next = EMUtilsConfig.load(file(profile));
		if (current != null) {
			// The settings UI's look belongs to the player, not the profile.
			next.setSettingsUiDark(current.settingsUiDark());
		}
		active = profile.id();
		EMUtilsClient.replaceConfig(next);
		save();
		return true;
	}

	/**
	 * Loads the profile linked to the server or world just joined, or, when none is, the one picked by
	 * hand. Says so in chat when the profile changes.
	 */
	public void onJoin(Minecraft client) {
		Profile linked = linkedProfile(client);
		Profile target = linked != null ? linked : byId(picked);
		if (target == null || target.id().equals(active)) {
			return;
		}
		if (!switchTo(target)) {
			return;
		}
		if (client.player != null) {
			Component message = Component.translatable(linked != null ? EMUtilsTexts.PROFILE_AUTO_SWITCHED : EMUtilsTexts.PROFILE_SWITCHED_BACK, target.name());
			client.player.sendSystemMessage(EmUtilsChatPrefix.chat(message));
		}
	}

	private @Nullable Profile linkedProfile(Minecraft client) {
		boolean singleplayer = client.hasSingleplayerServer();
		ServerData server = client.getCurrentServer();
		String address = singleplayer || server == null ? null : server.ip;
		for (Profile profile : profiles) {
			if (singleplayer ? profile.singleplayer() : address != null && profile.matchesServer(address)) {
				return profile;
			}
		}
		return null;
	}

	/** The address of the server you're on, or null in singleplayer or outside a world. */
	public static @Nullable String currentServerAddress(Minecraft client) {
		if (client.hasSingleplayerServer() || client.getConnection() == null) {
			return null;
		}
		ServerData server = client.getCurrentServer();
		return server == null || server.ip == null || server.ip.isBlank() ? null : Profile.normalizeAddress(server.ip);
	}

	// ---- editing --------------------------------------------------------------------------------

	/** Adds a profile, starting from the current settings or from the defaults. */
	public Profile create(String name, ProfileIcon icon, ProfileColor color, List<String> servers, boolean singleplayer, boolean copyCurrent) {
		Profile profile = new Profile(newId(), name, icon, color);
		profile.set(name, icon, color, servers, singleplayer);
		claim(profile);
		EMUtilsConfig settings = copyCurrent && EMUtilsClient.config() != null
			? EMUtilsClient.config().copyTo(file(profile))
			: EMUtilsConfig.defaults(file(profile));
		settings.flush();
		profiles.add(profile);
		save();
		return profile;
	}

	/** Adds a copy of {@code source}, settings included, right after it. */
	public Profile duplicate(Profile source) {
		String name = Component.translatable(EMUtilsTexts.UI_PROFILE_COPY_NAME, source.name()).getString();
		Profile copy = new Profile(newId(), name, source.icon(), source.color());
		// Auto-switching stays with the original; two profiles for one server would fight over it.
		copy.set(name, source.icon(), source.color(), List.of(), false);
		settingsOf(source).copyTo(file(copy)).flush();
		profiles.add(profiles.indexOf(source) + 1, copy);
		save();
		return copy;
	}

	public void update(Profile profile, String name, ProfileIcon icon, ProfileColor color, List<String> servers, boolean singleplayer) {
		profile.set(name, icon, color, servers, singleplayer);
		claim(profile);
		save();
	}

	/**
	 * Each server, and singleplayer, loads one profile: the one it was last given to. Takes what
	 * {@code profile} now loads on away from every other profile.
	 */
	private void claim(Profile profile) {
		for (Profile other : profiles) {
			if (other == profile) {
				continue;
			}
			List<String> servers = new ArrayList<>(other.servers());
			boolean changed = servers.removeAll(profile.servers());
			boolean singleplayer = other.singleplayer() && !profile.singleplayer();
			if (changed || singleplayer != other.singleplayer()) {
				other.set(other.rawName(), other.icon(), other.color(), servers, singleplayer);
			}
		}
	}

	/** The other profile that already loads on {@code server} (normalized), if any. */
	public @Nullable Profile ownerOfServer(String server, @Nullable Profile except) {
		String normalized = Profile.normalizeAddress(server);
		for (Profile profile : profiles) {
			if (profile != except && profile.servers().contains(normalized)) {
				return profile;
			}
		}
		return null;
	}

	/** The other profile that already loads in singleplayer, if any. */
	public @Nullable Profile ownerOfSingleplayer(@Nullable Profile except) {
		for (Profile profile : profiles) {
			if (profile != except && profile.singleplayer()) {
				return profile;
			}
		}
		return null;
	}

	/** Splits typed server addresses at commas and spaces. */
	public static List<String> parseServers(String text) {
		List<String> servers = new ArrayList<>();
		for (String part : text.split("[,\\s]+")) {
			String normalized = Profile.normalizeAddress(part);
			if (!normalized.isEmpty() && !servers.contains(normalized)) {
				servers.add(normalized);
			}
		}
		return servers;
	}

	/**
	 * Deletes a profile and its settings. The Default profile can't be deleted; deleting the active one
	 * switches to Default first. Returns false if nothing was deleted.
	 */
	public boolean delete(Profile profile) {
		if (profile.isDefault() || !profiles.contains(profile)) {
			return false;
		}
		if (profile.id().equals(active) && !switchTo(profiles.getFirst())) {
			return false;
		}
		if (profile.id().equals(picked)) {
			picked = Profile.DEFAULT_ID;
		}
		profiles.remove(profile);
		save();
		try {
			Files.deleteIfExists(file(profile));
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not delete the settings of profile {}.", profile.id(), exception);
		}
		return true;
	}

	/** Moves a profile one place up or down the list; Default stays first. */
	public void move(Profile profile, int direction) {
		int from = profiles.indexOf(profile);
		int to = from + direction;
		if (from <= 0 || to <= 0 || to >= profiles.size()) {
			return;
		}
		profiles.remove(from);
		profiles.add(to, profile);
		save();
	}

	/** The live settings for the active profile, or the saved ones for any other. */
	private EMUtilsConfig settingsOf(Profile profile) {
		EMUtilsConfig current = EMUtilsClient.config();
		if (profile.id().equals(active) && current != null) {
			return current;
		}
		return EMUtilsConfig.load(file(profile));
	}

	private String newId() {
		String id;
		do {
			id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		} while (byId(id) != null);
		return id;
	}

	// ---- import and export ----------------------------------------------------------------------

	/**
	 * The profile as text to share: its name, icon, color and settings. Which servers it loads on is left
	 * out, since that's about where you play rather than how.
	 */
	public String export(Profile profile) {
		JsonObject root = new JsonObject();
		root.addProperty("emutilsProfile", EXPORT_VERSION);
		root.addProperty("name", profile.name().getString());
		root.addProperty("icon", profile.icon().name());
		root.addProperty("color", profile.color().name());
		root.add("settings", JsonParser.parseString(settingsOf(profile).toJson()));
		return GSON.toJson(root);
	}

	/**
	 * Adds a profile from exported text. Also takes a plain settings export ({@code /emutils export}).
	 * Returns null if the text is neither.
	 */
	public @Nullable Profile importProfile(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		String name = Component.translatable(EMUtilsTexts.UI_PROFILE_IMPORTED_NAME).getString();
		ProfileIcon icon = ProfileIcon.GLOBE;
		ProfileColor color = ProfileColor.BLUE;
		String settingsJson = text;
		try {
			JsonElement parsed = JsonParser.parseString(text);
			if (!parsed.isJsonObject()) {
				return null;
			}
			JsonObject root = parsed.getAsJsonObject();
			if (root.has("emutilsProfile")) {
				if (!root.has("settings") || !root.get("settings").isJsonObject()) {
					return null;
				}
				if (root.has("name") && root.get("name").isJsonPrimitive() && !root.get("name").getAsString().isBlank()) {
					name = root.get("name").getAsString();
				}
				if (root.has("icon") && root.get("icon").isJsonPrimitive()) {
					icon = ProfileIcon.byName(root.get("icon").getAsString());
				}
				if (root.has("color") && root.get("color").isJsonPrimitive()) {
					color = ProfileColor.byName(root.get("color").getAsString());
				}
				settingsJson = root.get("settings").toString();
			}
		} catch (JsonParseException | IllegalStateException | UnsupportedOperationException exception) {
			return null;
		}
		Profile profile = new Profile(newId(), name, icon, color);
		EMUtilsConfig settings = EMUtilsConfig.fromJson(settingsJson, file(profile));
		if (settings == null) {
			return null;
		}
		profile.set(name, icon, color, List.of(), false);
		settings.flush();
		profiles.add(profile);
		save();
		return profile;
	}

	// ---- saving ---------------------------------------------------------------------------------

	private void save() {
		SaveData data = new SaveData();
		data.active = active;
		data.picked = picked;
		data.profiles = new ArrayList<>(profiles);
		try {
			AtomicFiles.writeString(EMUtilsPaths.profilesFile(), GSON.toJson(data));
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Failed to save the EMUtils profile list.", exception);
		}
	}

	private static final class SaveData {
		private String active;
		private String picked;
		private List<Profile> profiles;
	}
}
