package net.emutils.client.emutils.waypoint;

/**
 * A waypoint as seen from the dimension you are in (#105): where it is here, in that dimension's coordinates.
 * A waypoint from the Overworld or the Nether is converted to the other one; one from anywhere else has no
 * place here, so it keeps its own coordinates and can be listed but not shown in the world.
 *
 * @param sameDimension the waypoint was made in the dimension you are in
 * @param placeable     it has a position in this dimension, so it can be measured and drawn in the world
 */
public record WaypointEntry(Waypoint waypoint, int x, int y, int z, boolean sameDimension, boolean placeable) {
	public boolean converted() {
		return placeable && !sameDimension;
	}

	public double renderX() {
		return x + 0.5D;
	}

	public double renderY() {
		return y + 1.25D;
	}

	public double renderZ() {
		return z + 0.5D;
	}
}
