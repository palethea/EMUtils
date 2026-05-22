package net.emutils.client.death;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public final class DeathWaypointClickHandler {
	private DeathWaypointClickHandler() {
	}

	public static boolean tryHandle(Identifier actionId, MinecraftClient client) {
		DeathWaypointManager manager = EMUtilsClient.deathWaypoint();

		if (DeathWaypointMessage.CLEAR_DEATH_WAYPOINT_ACTION.equals(actionId)) {
			if (!manager.hasWaypoint()) {
				return true;
			}

			manager.clear(client);
			return true;
		}

		if (DeathWaypointMessage.KEEP_DEATH_WAYPOINT_ACTION.equals(actionId)) {
			if (!manager.hasWaypoint()) {
				return true;
			}

			manager.keep(client);
			return true;
		}

		return false;
	}
}
