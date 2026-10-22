package net.emutils.client.emutils.waypoint;

import java.util.Optional;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.ChatActions;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.Identifier;

public final class WaypointClickHandler {
	private WaypointClickHandler() {
	}

	public static boolean tryHandle(Identifier actionId, Optional<Tag> payload, Minecraft client) {
		return tryHandle(actionId, new ClickEvent.Custom(actionId, payload), client);
	}

	/**
	 * Handles the waypoint buttons EMUtils puts in chat. Only ones it made this session do anything (#254):
	 * the same click event in text from a server is taken, so it isn't sent on, but ignored.
	 */
	public static boolean tryHandle(Identifier actionId, ClickEvent.Custom custom, Minecraft client) {
		WaypointManager manager = EMUtilsClient.waypoint();
		Optional<String> id = ChatActions.value(custom.payload());

		if (WaypointMessage.CLEAR_WAYPOINT_ACTION.equals(actionId)) {
			id.ifPresent(value -> manager.clear(client, value));
			return true;
		}

		if (WaypointMessage.KEEP_WAYPOINT_ACTION.equals(actionId)) {
			id.ifPresent(value -> manager.keep(client, value));
			return true;
		}

		if (WaypointMessage.ADD_SHARED_WAYPOINT_ACTION.equals(actionId)) {
			id.ifPresent(value -> WaypointChatShare.open(client, value));
			return true;
		}

		return false;
	}
}
