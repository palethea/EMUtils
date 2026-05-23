package net.emutils.client.death;

import java.util.ArrayList;
import java.util.List;

public final class DeathWaypointSaveData {
	private List<DeathLocation> deaths = new ArrayList<>();

	public DeathWaypointSaveData() {
	}

	public List<DeathLocation> deaths() {
		return deaths;
	}

	public void setDeaths(List<DeathLocation> deaths) {
		this.deaths = deaths == null ? new ArrayList<>() : deaths;
	}
}
