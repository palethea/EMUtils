package net.emutils.client.emutils.waypoint;

import org.jspecify.annotations.Nullable;

/**
 * A location someone shared (#105, #197), or one picked with the crosshair: where it is, and what else the
 * share said about it. Everything but the position is optional.
 *
 * @param name      the waypoint's name when the share has one (Xaero's and JourneyMap's do)
 * @param y         null when the share has no height, so the player's own is used
 * @param dimension a dimension id such as {@code minecraft:the_nether}, null when the share doesn't say
 * @param color     an ARGB color, null for the default one
 */
public record SharedWaypoint(@Nullable String name, int x, @Nullable Integer y, int z, @Nullable String dimension, @Nullable Integer color) {
	public static SharedWaypoint at(int x, int y, int z) {
		return new SharedWaypoint(null, x, y, z, null, null);
	}
}
