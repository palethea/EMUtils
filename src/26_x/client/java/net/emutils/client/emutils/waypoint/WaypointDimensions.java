package net.emutils.client.emutils.waypoint;

import org.jspecify.annotations.Nullable;

/** Converts waypoint coordinates between dimensions that line up with each other. */
public final class WaypointDimensions {
	public static final String OVERWORLD = "minecraft:overworld";
	public static final String NETHER = "minecraft:the_nether";
	private static final int NETHER_SCALE = 8;

	private WaypointDimensions() {
	}

	/**
	 * Where a point at the given x and z in one dimension lies in another, as {x, z}. Y is not scaled, so the
	 * caller keeps it. Only the Overworld and the Nether line up (one Nether block is eight Overworld blocks);
	 * returns null for any other pair, including the End and dimensions from datapacks or mods.
	 */
	public static int @Nullable [] convert(String from, String to, int x, int z) {
		if (from == null || to == null) {
			return null;
		}
		if (from.equals(to)) {
			return new int[] {x, z};
		}
		if (OVERWORLD.equals(from) && NETHER.equals(to)) {
			return new int[] {Math.floorDiv(x, NETHER_SCALE), Math.floorDiv(z, NETHER_SCALE)};
		}
		if (NETHER.equals(from) && OVERWORLD.equals(to)) {
			return new int[] {x * NETHER_SCALE, z * NETHER_SCALE};
		}
		return null;
	}
}
