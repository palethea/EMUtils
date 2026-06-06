package net.emutils.client.emutils.waypoint;

import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.Identifier;

public final class WaypointClickHandler {
	private WaypointClickHandler() {
	}

	public static boolean tryHandle(Identifier actionId, Optional<Tag> payload, Minecraft client) {
		return tryHandle(actionId, new ClickEvent.Custom(actionId, payload), client);
	}

	public static boolean tryHandle(Identifier actionId, ClickEvent.Custom custom, Minecraft client) {
		WaypointManager manager = EMUtilsClient.waypoint();
		Optional<Long> timestamp = parseTimestamp(custom);

		if (WaypointMessage.CLEAR_WAYPOINT_ACTION.equals(actionId)) {
			if (timestamp.isPresent()) {
				manager.clear(client, timestamp.get());
			} else if (manager.hasWaypoint()) {
				manager.clearForCurrentWorld(client);
			}

			return true;
		}

		if (WaypointMessage.KEEP_WAYPOINT_ACTION.equals(actionId)) {
			timestamp.ifPresent(t -> manager.keep(client, t));
			return true;
		}

		return false;
	}

	private static Optional<Long> parseTimestamp(ClickEvent.Custom custom) {
		return custom.payload()
			.flatMap(element -> element instanceof StringTag string ? string.asString() : Optional.empty())
			.flatMap(value -> {
				try {
					return Optional.of(Long.parseLong(value));
				} catch (NumberFormatException ignored) {
					return Optional.empty();
				}
			});
	}
}
