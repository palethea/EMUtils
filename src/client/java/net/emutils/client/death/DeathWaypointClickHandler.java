package net.emutils.client.death;

import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Identifier;

public final class DeathWaypointClickHandler {
	private DeathWaypointClickHandler() {
	}

	public static boolean tryHandle(Identifier actionId, Optional<NbtElement> payload, MinecraftClient client) {
		return tryHandle(actionId, new ClickEvent.Custom(actionId, payload), client);
	}

	public static boolean tryHandle(Identifier actionId, ClickEvent.Custom custom, MinecraftClient client) {
		DeathWaypointManager manager = EMUtilsClient.deathWaypoint();
		Optional<Long> deathTimestamp = parseDeathTimestamp(custom);

		if (DeathWaypointMessage.CLEAR_DEATH_WAYPOINT_ACTION.equals(actionId)) {
			if (deathTimestamp.isPresent()) {
				manager.clear(client, deathTimestamp.get());
			} else if (manager.hasWaypoint()) {
				manager.clearForCurrentWorld(client);
			}

			return true;
		}

		if (DeathWaypointMessage.KEEP_DEATH_WAYPOINT_ACTION.equals(actionId)) {
			deathTimestamp.ifPresent(timestamp -> manager.keep(client, timestamp));
			return true;
		}

		return false;
	}

	private static Optional<Long> parseDeathTimestamp(ClickEvent.Custom custom) {
		return custom.payload()
			.flatMap(element -> element instanceof NbtString string ? string.asString() : Optional.empty())
			.flatMap(value -> {
				try {
					return Optional.of(Long.parseLong(value));
				} catch (NumberFormatException ignored) {
					return Optional.empty();
				}
			});
	}
}
