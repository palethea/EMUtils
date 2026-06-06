package net.emutils.client.emutils.waypoint;

import java.util.ArrayList;
import java.util.List;

public final class WaypointSaveData {
	private List<Waypoint> waypoints = new ArrayList<>();

	public WaypointSaveData() {
	}

	public List<Waypoint> waypoints() {
		return waypoints;
	}

	public void setWaypoints(List<Waypoint> waypoints) {
		this.waypoints = waypoints == null ? new ArrayList<>() : waypoints;
	}
}
