package net.emutils.client.emutils.waypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Reads locations out of chat text and writes waypoints as chat text (#105, #197).
 *
 * <p>Read: Xaero's Minimap share ({@code xaero-waypoint:name:initials:x:y:z:color:rotate:yaw:destination}, as
 * Xaero's own {@code WaypointSharingHandler} writes and splits it), JourneyMap's bracket form
 * ({@code [name:Home, x:1, y:2, z:3, dim:0]}, from its documentation), a teleport command such as the one F3+C
 * copies, labeled coordinates ({@code x: 1, y: 2, z: 3}, {@code x1 y2 z3}), and three plain numbers when they
 * are bracketed, follow a word like "coords", or end the message.
 */
public final class WaypointShare {
	private static final String NUMBER = "-?\\d+(?:\\.\\d+)?";
	private static final int MAX_COORDINATE = 30_000_000;
	private static final int MIN_HEIGHT = -2048;
	private static final int MAX_HEIGHT = 4096;
	private static final String COLON_ESCAPE = "^col^";

	private static final Pattern XAERO = Pattern.compile("xaero[-_]waypoint:", Pattern.CASE_INSENSITIVE);
	private static final Pattern BRACKET_GROUP = Pattern.compile("\\[([^\\[\\]]+)]");
	private static final Pattern TELEPORT = Pattern.compile(
		"(?:execute\\s+in\\s+([a-z0-9_.:/-]+)\\s+run\\s+)?/?(?:tp|teleport)\\s+(?:@[sp]|\\w{1,16})\\s+(" + NUMBER + ")\\s+(" + NUMBER + ")\\s+(" + NUMBER + ")",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern LABELED = Pattern.compile(
		"(?<![A-Za-z0-9_])x\\s*[:=]?\\s*(" + NUMBER + ")(?![A-Za-z0-9])[\\s,;/|]*(?:y\\s*[:=]?\\s*(" + NUMBER + ")(?![A-Za-z0-9])[\\s,;/|]*)?z\\s*[:=]?\\s*(" + NUMBER + ")(?![A-Za-z0-9])(?:[\\s,;/|]*dim\\s*[:=]\\s*(-?\\d+|[A-Za-z0-9_.:/-]+))?",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern BRACKETED = Pattern.compile(
		"[(\\[{<]\\s*(" + NUMBER + ")\\s*[,;\\s]\\s*(" + NUMBER + ")\\s*[,;\\s]\\s*(" + NUMBER + ")\\s*[)\\]}>]"
	);
	private static final Pattern KEYWORD = Pattern.compile(
		"\\b(?:coords?|coordinates|cords?|xyz|pos|position|location|loc)\\b\\s*(?:is|are|=|:)?\\s*[(\\[{<]?\\s*(" + NUMBER + ")[\\s,;]+(" + NUMBER + ")[\\s,;]+(" + NUMBER + ")",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern TRAILING = Pattern.compile(
		"(?:^|[\\s:>»\\]])(?<![\\d.-])(" + NUMBER + ")[\\s,]+(" + NUMBER + ")[\\s,]+(" + NUMBER + ")\\s*[.!?]*\\s*$"
	);

	/** Xaero's colors by the number it shares them as: Minecraft's sixteen chat colors, in order (its WaypointColor enum). */
	private static final int[] XAERO_COLORS = {
		0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
		0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
	};

	private WaypointShare() {
	}

	/** The location in {@code text}, or null when there is none. The first form that matches wins. */
	public static @Nullable SharedWaypoint parse(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		Matcher xaero = XAERO.matcher(text);
		if (xaero.find()) {
			// A Xaero share is what it is even when it doesn't parse: its numbers must not be read as something else.
			return parseXaero(text.substring(xaero.start()));
		}
		Matcher bracket = BRACKET_GROUP.matcher(text);
		while (bracket.find()) {
			SharedWaypoint journeyMap = parseJourneyMap(bracket.group(1));
			if (journeyMap != null) {
				return journeyMap;
			}
		}
		Matcher teleport = TELEPORT.matcher(text);
		if (teleport.find()) {
			return plain(teleport.group(2), teleport.group(3), teleport.group(4), teleport.group(1) == null ? null : dimensionId(teleport.group(1)), true);
		}
		Matcher labeled = LABELED.matcher(text);
		if (labeled.find()) {
			return plain(labeled.group(1), labeled.group(2), labeled.group(3), labeled.group(4) == null ? null : journeyMapDimension(labeled.group(4)), true);
		}
		for (Pattern pattern : new Pattern[] {BRACKETED, KEYWORD, TRAILING}) {
			Matcher matcher = pattern.matcher(text);
			if (matcher.find()) {
				SharedWaypoint found = plain(matcher.group(1), matcher.group(2), matcher.group(3), null, false);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/** x, y, z as typed. With {@code labeled} false the numbers must also look like a position, so a list of counts isn't one. */
	private static @Nullable SharedWaypoint plain(String xText, @Nullable String yText, String zText, @Nullable String dimension, boolean labeled) {
		Integer x = block(xText);
		Integer z = block(zText);
		Integer y = yText == null ? null : block(yText);
		if (x == null || z == null || !plausible(x, y, z)) {
			return null;
		}
		if (!labeled && Math.max(Math.abs(x), Math.abs(z)) < 8) {
			return null;
		}
		return new SharedWaypoint(null, x, y, z, dimension, null);
	}

	private static boolean plausible(int x, @Nullable Integer y, int z) {
		return Math.abs(x) <= MAX_COORDINATE && Math.abs(z) <= MAX_COORDINATE && (y == null || (y >= MIN_HEIGHT && y <= MAX_HEIGHT));
	}

	private static @Nullable Integer block(String text) {
		try {
			double value = Double.parseDouble(text.trim());
			return Double.isFinite(value) && Math.abs(value) < Integer.MAX_VALUE ? (int) Math.floor(value) : null;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	// ---- Xaero ----------------------------------------------------------------------------------

	/** {@code rest} starts at the prefix. Xaero splits everything after it on colons, and needs at least nine parts. */
	private static @Nullable SharedWaypoint parseXaero(String rest) {
		String[] parts = rest.split(":", -1);
		if (parts.length < 9) {
			return null;
		}
		Integer x = block(parts[3]);
		Integer z = block(parts[5]);
		// "~" is a waypoint without a height.
		Integer y = parts[4].trim().equals("~") ? null : block(parts[4]);
		if (x == null || z == null || (y == null && !parts[4].trim().equals("~")) || !plausible(x, y, z)) {
			return null;
		}
		String name = parts[1].replace(COLON_ESCAPE, ":").trim();
		Integer color = null;
		try {
			int ordinal = Integer.parseInt(parts[6].trim());
			color = ordinal >= 0 && ordinal < XAERO_COLORS.length ? XAERO_COLORS[ordinal] : null;
		} catch (NumberFormatException exception) {
			// Not a color number; keep the default.
		}
		String dimension = parts.length > 9 ? xaeroDimension(parts[9].trim().split("\\s+")[0]) : null;
		return new SharedWaypoint(name.isEmpty() ? null : name, x, y, z, dimension, color);
	}

	/** "Internal-overworld-waypoints" is the Overworld's set called waypoints; "dim%-1" an old Nether. */
	private static @Nullable String xaeroDimension(String destination) {
		String text = destination;
		if (text.startsWith("Internal-")) {
			int last = text.lastIndexOf('-');
			if (last <= "Internal-".length()) {
				return null;
			}
			text = text.substring("Internal-".length(), last).replace(COLON_ESCAPE, ":");
		}
		if (text.startsWith("dim%")) {
			return switch (text.substring(4)) {
				case "0" -> WaypointDimensions.OVERWORLD;
				case "-1" -> WaypointDimensions.NETHER;
				case "1" -> "minecraft:the_end";
				default -> null;
			};
		}
		return text.isEmpty() || text.startsWith("External") ? null : dimensionId(text);
	}

	// ---- JourneyMap -----------------------------------------------------------------------------

	/** The inside of a [bracket] group: name:value pairs, in any order, of which x and z are required. */
	private static @Nullable SharedWaypoint parseJourneyMap(String inside) {
		Integer x = null;
		Integer y = null;
		Integer z = null;
		String name = null;
		String dimension = null;
		Integer color = null;
		int pairs = 0;
		for (String pair : splitPairs(inside)) {
			int colon = pair.indexOf(':');
			if (colon <= 0) {
				continue;
			}
			pairs++;
			String key = pair.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			String value = pair.substring(colon + 1).trim();
			switch (key) {
				case "x" -> x = block(value);
				case "y" -> y = block(value);
				case "z" -> z = block(value);
				case "name" -> name = unquote(value);
				case "dim" -> dimension = journeyMapDimension(unquote(value));
				case "color" -> color = hexColor(value);
				default -> {
				}
			}
		}
		if (pairs < 2 || x == null || z == null || !plausible(x, y, z)) {
			return null;
		}
		return new SharedWaypoint(name == null || name.isBlank() ? null : name, x, y, z, dimension, color);
	}

	/** Splits on commas that aren't inside quotes. */
	private static List<String> splitPairs(String text) {
		List<String> pairs = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '"') {
				quoted = !quoted;
			}
			if (c == ',' && !quoted) {
				pairs.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		pairs.add(current.toString());
		return pairs;
	}

	private static String unquote(String value) {
		String text = value.trim();
		return text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"") ? text.substring(1, text.length() - 1) : text;
	}

	/** JourneyMap writes dimensions as 0, -1 and 1; newer text may use the id. */
	private static @Nullable String journeyMapDimension(String value) {
		return switch (value) {
			case "0" -> WaypointDimensions.OVERWORLD;
			case "-1" -> WaypointDimensions.NETHER;
			case "1" -> "minecraft:the_end";
			default -> value.isBlank() ? null : dimensionId(value);
		};
	}

	private static @Nullable Integer hexColor(String value) {
		String text = value.trim().replace("#", "").replace("0x", "");
		if (text.length() != 6) {
			return null;
		}
		try {
			return 0xFF000000 | Integer.parseInt(text, 16);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	/** "the_nether" and "minecraft:the_nether" both mean the Nether; "nether" and "end" are short forms. */
	private static String dimensionId(String text) {
		String id = text.trim().toLowerCase(Locale.ROOT);
		return switch (id) {
			case "nether" -> WaypointDimensions.NETHER;
			case "end" -> "minecraft:the_end";
			default -> id.contains(":") ? id : "minecraft:" + id;
		};
	}

	// ---- writing --------------------------------------------------------------------------------

	/**
	 * The waypoint as chat text in {@code format}. {@code currentDimension} is where the sender is: a plain
	 * share names the dimension only when the waypoint is in another one.
	 */
	public static String format(Waypoint waypoint, WaypointShareFormat format, String currentDimension) {
		String name = shareName(waypoint);
		String dimension = waypoint.dimension();
		return switch (format) {
			case XAERO -> "xaero-waypoint:" + name.replace(":", COLON_ESCAPE) + ":" + initial(name) + ":" + waypoint.x() + ":" + waypoint.y() + ":" + waypoint.z()
				+ ":" + nearestXaeroColor(waypoint.color()) + ":false:0:Internal-" + xaeroDestinationDimension(dimension) + "-waypoints";
			case JOURNEYMAP -> {
				String dim = journeyMapDimensionNumber(dimension);
				yield "[name:" + name.replace(",", " ").replace("\"", "").replace("[", "(").replace("]", ")") + ", x:" + waypoint.x() + ", y:" + waypoint.y() + ", z:" + waypoint.z()
					+ (dim == null ? "" : ", dim:" + dim) + "]";
			}
			case PLAIN -> name + " (x: " + waypoint.x() + ", y: " + waypoint.y() + ", z: " + waypoint.z()
				+ (dimension == null || dimension.equals(currentDimension) ? "" : ", dim: " + dimension) + ")";
		};
	}

	private static String shareName(Waypoint waypoint) {
		String label = waypoint.label() == null ? "" : waypoint.label().trim();
		return label.isEmpty() ? "Waypoint" : label;
	}

	private static String initial(String name) {
		return new String(Character.toChars(Character.toUpperCase(name.codePointAt(0))));
	}

	private static String xaeroDestinationDimension(@Nullable String dimension) {
		if (dimension == null) {
			return "overworld";
		}
		return dimension.startsWith("minecraft:") ? dimension.substring("minecraft:".length()) : dimension.replace(":", COLON_ESCAPE);
	}

	private static @Nullable String journeyMapDimensionNumber(@Nullable String dimension) {
		if (dimension == null) {
			return null;
		}
		return switch (dimension) {
			case WaypointDimensions.OVERWORLD -> "0";
			case WaypointDimensions.NETHER -> "-1";
			case "minecraft:the_end" -> "1";
			default -> null;
		};
	}

	/** The number of the Xaero color closest to {@code argb}. */
	private static int nearestXaeroColor(int argb) {
		int best = 0;
		long bestDistance = Long.MAX_VALUE;
		for (int i = 0; i < XAERO_COLORS.length; i++) {
			long dr = ((argb >> 16) & 0xFF) - ((XAERO_COLORS[i] >> 16) & 0xFF);
			long dg = ((argb >> 8) & 0xFF) - ((XAERO_COLORS[i] >> 8) & 0xFF);
			long db = (argb & 0xFF) - (XAERO_COLORS[i] & 0xFF);
			long distance = dr * dr + dg * dg + db * db;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = i;
			}
		}
		return best;
	}
}
