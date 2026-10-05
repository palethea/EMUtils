package net.emutils.client.emutils.waypoint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Brings the waypoints of Xaero's Minimap over (#233), for every world and server at once, from its files in
 * {@code .minecraft/xaero/minimap}: a folder per singleplayer world (named after the save's folder) or server
 * ({@code Multiplayer_} and its address, colons as underscores), in it a folder per dimension ({@code dim%0},
 * {@code dim%-1}, {@code dim%1}, or {@code dim%namespace$path}), and in that a text file per waypoint world,
 * a line each:
 * {@code waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination}.
 *
 * <p>Names, colors (Xaero's sixteen, as its shares number them), hidden ones, death waypoints and sets come
 * along. A spot that already has a waypoint is skipped, so importing again adds nothing.
 */
public final class XaeroWaypointImport {
	private static final String MULTIPLAYER = "Multiplayer_";
	private static final String DEFAULT_SET = "gui.xaero_default";
	/** Xaero's death waypoints are called after these keys of its own. */
	private static final String DEATH_NAME = "gui.xaero_deathpoint";
	/** A waypoint without a height, which Xaero puts on the ground; this is about where the ground usually is. */
	private static final int NO_HEIGHT_Y = 64;
	/** Xaero's colors by number: Minecraft's sixteen chat colors, in order. */
	private static final int[] COLORS = {
		0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
		0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
	};

	private XaeroWaypointImport() {
	}

	/** Where Xaero's Minimap keeps its waypoints. */
	public static Path folder(Minecraft client) {
		return client.gameDirectory.toPath().resolve("xaero").resolve("minimap");
	}

	/** Xaero's Minimap has waypoint files to import. */
	public static boolean available(Minecraft client) {
		return !files(folder(client)).isEmpty();
	}

	/** Imports them all and says how it went in a toast. */
	public static void run(Minecraft client) {
		List<Waypoint> read = read(client, folder(client));
		int added = EMUtilsClient.waypoint().importWaypoints(read);
		Component title = Component.translatable(EMUtilsTexts.WAYPOINT_XAERO_IMPORT_TITLE);
		Component description = added < 0
			? Component.translatable(EMUtilsTexts.WAYPOINT_XAERO_IMPORT_FAILED)
			: Component.translatable(EMUtilsTexts.WAYPOINT_XAERO_IMPORT_DONE, added, read.size() - added);
		SystemToast.addOrUpdate(MinecraftClientCompat.toastManager(client), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, description);
	}

	/** The waypoints in Xaero's files under {@code root}, as EMUtils waypoints of the worlds they were made in. */
	public static List<Waypoint> read(Minecraft client, Path root) {
		Map<String, String> servers = servers(client);
		List<Waypoint> waypoints = new ArrayList<>();
		long now = System.currentTimeMillis();
		for (Path file : files(root)) {
			Path dimensionFolder = file.getParent();
			String dimension = dimension(dimensionFolder.getFileName().toString());
			String worldKey = worldKey(client, dimensionFolder.getParent().getFileName().toString(), servers);
			if (dimension == null || worldKey == null) {
				continue;
			}
			List<String> lines;
			try {
				lines = Files.readAllLines(file);
			} catch (IOException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils couldn't read Xaero's waypoints in {}", file, exception);
				continue;
			}
			for (String line : lines) {
				Waypoint waypoint = parse(line, dimension, worldKey, now + waypoints.size());
				if (waypoint != null) {
					waypoints.add(waypoint);
				}
			}
		}
		return waypoints;
	}

	/** Xaero's waypoint files: the text files in each world's dimension folders, without its backups. */
	private static List<Path> files(Path root) {
		List<Path> files = new ArrayList<>();
		if (!Files.isDirectory(root)) {
			return files;
		}
		try (Stream<Path> walk = Files.walk(root, 3)) {
			walk.filter(path -> path.getNameCount() - root.getNameCount() == 3)
				.filter(path -> path.getFileName().toString().endsWith(".txt"))
				.filter(path -> path.getParent().getFileName().toString().startsWith("dim%"))
				.filter(path -> !path.getParent().getParent().getFileName().toString().equals("backup"))
				.filter(Files::isRegularFile)
				.sorted()
				.forEach(files::add);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("EMUtils couldn't look through Xaero's waypoints in {}", root, exception);
		}
		return files;
	}

	/** One line of a waypoint file, or null when it isn't a waypoint, or is Xaero's temporary destination. */
	static @Nullable Waypoint parse(String line, String dimension, String worldKey, long timestamp) {
		if (!line.startsWith("waypoint:")) {
			return null;
		}
		String[] parts = line.split(":", -1);
		if (parts.length < 9) {
			return null;
		}
		try {
			int x = Integer.parseInt(parts[3].trim());
			int y = parts[4].trim().equals("~") ? NO_HEIGHT_Y : Integer.parseInt(parts[4].trim());
			int z = Integer.parseInt(parts[5].trim());
			int colorIndex = Integer.parseInt(parts[6].trim());
			boolean hidden = Boolean.parseBoolean(parts[7].trim());
			int type = Integer.parseInt(parts[8].trim());
			if (parts.length > 13 && Boolean.parseBoolean(parts[13].trim())) {
				return null;
			}
			// Xaero writes a colon in a name as "§§", and newer versions as "^col^".
			String name = parts[1].replace("§§", ":").replace("^col^", ":").trim();
			boolean death = type == 1 || type == 2 || name.startsWith(DEATH_NAME);
			if (name.startsWith(DEATH_NAME) || name.isEmpty()) {
				name = death ? "Death" : parts[2].trim();
			}
			int color = colorIndex >= 0 && colorIndex < COLORS.length
				? COLORS[colorIndex]
				: death ? EMUtilsClient.config().waypointDefaultDeathColor() : EMUtilsClient.config().waypointDefaultCustomColor();
			Waypoint waypoint = new Waypoint(x, y, z, dimension, worldKey, timestamp, name, color, death ? WaypointType.DEATH : WaypointType.CUSTOM);
			waypoint.setHidden(hidden);
			String set = parts.length > 9 ? parts[9].replace("§§", ":").replace("^col^", ":").trim() : "";
			if (!set.equals(DEFAULT_SET) && !set.startsWith("gui.xaero_")) {
				waypoint.setSet(set);
			}
			if (death) {
				// Yours to delete, like a death waypoint you chose to keep, so Deaths to Keep doesn't drop it.
				waypoint.setNearPromptShown(true);
			}
			return waypoint;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	/** The dimension a Xaero dimension folder is for, or null for one it can't tell. */
	static @Nullable String dimension(String folder) {
		if (!folder.startsWith("dim%")) {
			return null;
		}
		String id = folder.substring(4);
		return switch (id) {
			case "0" -> WaypointDimensions.OVERWORLD;
			case "-1" -> WaypointDimensions.NETHER;
			case "1" -> "minecraft:the_end";
			default -> id.contains("$") ? id.replace('$', ':') : null;
		};
	}

	/**
	 * The world key EMUtils keeps a world's waypoints under, for a Xaero world folder: a server's address from
	 * the server list when one matches, else read from the folder's name; a singleplayer world's name from its
	 * save, else the folder's name. Null for Realms, which Xaero names in a way that can't be matched.
	 */
	static @Nullable String worldKey(Minecraft client, String folder, Map<String, String> servers) {
		if (folder.startsWith("Realms_")) {
			return null;
		}
		if (folder.startsWith(MULTIPLAYER)) {
			String known = servers.get(folder.toLowerCase(Locale.ROOT));
			if (known != null) {
				return "multiplayer:" + known;
			}
			// "host_25566" is "host:25566", as Xaero writes the colon of a port.
			String address = folder.substring(MULTIPLAYER.length()).replaceFirst("_(\\d{1,5})$", ":$1");
			return address.isBlank() ? null : "multiplayer:" + address;
		}
		return "singleplayer:" + levelName(client, folder);
	}

	/** The servers in the server list, by the folder name Xaero gives each. */
	private static Map<String, String> servers(Minecraft client) {
		Map<String, String> servers = new HashMap<>();
		ServerList list = new ServerList(client);
		list.load();
		for (int i = 0; i < list.size(); i++) {
			ServerData server = list.get(i);
			if (server.ip != null && !server.ip.isBlank()) {
				servers.put((MULTIPLAYER + server.ip.replace(':', '_')).toLowerCase(Locale.ROOT), server.ip);
			}
		}
		return servers;
	}

	/** A singleplayer world's name, which EMUtils keys its waypoints by, from its save; else the folder's name. */
	private static String levelName(Minecraft client, String saveFolder) {
		Path levelDat = client.gameDirectory.toPath().resolve("saves").resolve(saveFolder).resolve("level.dat");
		if (Files.isRegularFile(levelDat)) {
			try {
				CompoundTag root = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap());
				String name = root.getCompoundOrEmpty("Data").getStringOr("LevelName", "");
				if (!name.isBlank()) {
					return name;
				}
			} catch (IOException | RuntimeException exception) {
				EMUtilsClient.LOGGER.warn("EMUtils couldn't read the name of the world in {}", saveFolder, exception);
			}
		}
		return saveFolder;
	}
}
