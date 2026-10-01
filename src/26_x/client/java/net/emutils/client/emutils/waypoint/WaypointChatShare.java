package net.emutils.client.emutils.waypoint;

import com.mojang.authlib.GameProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.chat.EMUtilsChatMessages;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.emutils.waypoint.gui.WaypointsScreen;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Coordinates other players send in chat (#197): reads them out of incoming messages and, as the settings
 * say, offers a waypoint for them under the message or adds one at once. It never sends anything itself.
 */
public final class WaypointChatShare {
	private static final int MAX_PENDING = 32;
	private static final int MAX_NAME = 32;
	/** Shared locations waiting for a click on their offer, by the key the button carries. Used on the client thread only. */
	private static final Map<String, SharedWaypoint> PENDING = new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, SharedWaypoint> eldest) {
			return size() > MAX_PENDING;
		}
	};

	private WaypointChatShare() {
	}

	public static void register() {
		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> handle(Minecraft.getInstance(), message, sender));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			// Plugin-formatted chat arrives as game messages, without a sender; the action bar is not chat.
			if (!overlay) {
				handle(Minecraft.getInstance(), message, null);
			}
		});
	}

	private static void handle(Minecraft client, Component message, @Nullable GameProfile sender) {
		EMUtilsConfig config = EMUtilsClient.config();
		WaypointManager manager = EMUtilsClient.waypoint();
		if (config == null || manager == null || !manager.enabled() || !(config.waypointChatPrompt() || config.waypointChatAutoCreate())) {
			return;
		}
		if (client.player == null || client.level == null || EMUtilsChatMessages.isInternal(message)) {
			return;
		}

		receive(client, message.getString(), sender != null ? sender.name() : null, true);
	}

	/** Reads {@code text}; {@code deferred} puts the offer after the message it is for, which the client does when the message is shown. */
	private static void receive(Minecraft client, String text, @Nullable String knownSender, boolean deferred) {
		SharedWaypoint found = WaypointShare.parse(text);
		if (found == null) {
			return;
		}
		String senderName = knownSender != null ? knownSender : guessSender(client, text);
		if (senderName == null) {
			// Nobody said it: a game message such as command feedback ("Changed the block at 1, 2, 3") counts only in the stricter forms.
			found = WaypointShare.parse(text, false);
			if (found == null) {
				return;
			}
		}
		if (senderName != null && senderName.equalsIgnoreCase(client.getUser().getName())) {
			return;
		}
		SharedWaypoint shared = found;
		if (deferred) {
			client.execute(() -> present(client, shared, senderName));
		} else {
			present(client, shared, senderName);
		}
	}

	/** Receives a chat message as if {@code senderName} had sent it, right away; for UI snapshots. */
	public static void receiveForSnapshot(Minecraft client, String text, @Nullable String senderName) {
		receive(client, text, senderName, false);
	}

	/** How many offers are waiting for a click; for UI snapshots. */
	public static int pendingCountForSnapshot() {
		return PENDING.size();
	}

	/** The key of the newest offer; for UI snapshots. */
	public static @Nullable String newestKeyForSnapshot() {
		String newest = null;
		for (String key : PENDING.keySet()) {
			newest = key;
		}
		return newest;
	}

	/** For a message without a sender: the first online player whose name is in it. */
	private static @Nullable String guessSender(Minecraft client, String text) {
		if (client.getConnection() == null) {
			return null;
		}
		String found = null;
		int foundAt = Integer.MAX_VALUE;
		for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
			String name = info.getProfile().name();
			if (name == null || name.isEmpty()) {
				continue;
			}
			java.util.regex.Matcher matcher = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE).matcher(text);
			if (matcher.find() && (matcher.start() < foundAt || (matcher.start() == foundAt && name.length() > found.length()))) {
				found = name;
				foundAt = matcher.start();
			}
		}
		return found;
	}

	private static void present(Minecraft client, SharedWaypoint shared, @Nullable String senderName) {
		EMUtilsConfig config = EMUtilsClient.config();
		WaypointManager manager = EMUtilsClient.waypoint();
		if (client.player == null || client.level == null || client.gui == null) {
			return;
		}

		int y = shared.y() != null ? shared.y() : client.player.getBlockY();
		String name = nameFor(shared, senderName);
		if (manager.hasWaypointAt(client, shared.dimension(), shared.x(), shared.y(), shared.z())) {
			return;
		}
		SharedWaypoint resolved = new SharedWaypoint(name, shared.x(), y, shared.z(), shared.dimension(), shared.color());
		String coordinates = shared.x() + ", " + y + ", " + shared.z();

		if (config.waypointChatAutoCreate()) {
			int color = shared.color() != null ? shared.color() : 0xFF000000 | config.waypointDefaultCustomColor();
			Waypoint added = manager.addCustom(client, shared.dimension(), name, shared.x(), y, shared.z(), color, false, "");
			if (added != null) {
				MinecraftClientCompat.chat(client).addClientSystemMessage(EmUtilsChatPrefix.chat(WaypointMessage.sharedAdded(name, coordinates, added.id())));
			}
			return;
		}

		String key = UUID.randomUUID().toString();
		PENDING.put(key, resolved);
		String who = senderName != null ? senderName : Component.translatable(EMUtilsTexts.WAYPOINT_SHARED_SOMEONE).getString();
		MinecraftClientCompat.chat(client).addClientSystemMessage(EmUtilsChatPrefix.chat(WaypointMessage.sharedPrompt(who, coordinates, key)));
	}

	/** The name a shared location gets: its own if it has one, else the player who sent it. */
	private static String nameFor(SharedWaypoint shared, @Nullable String senderName) {
		String name = shared.name() != null && !shared.name().isBlank() ? shared.name().trim() : senderName != null ? senderName : "Shared waypoint";
		return name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name;
	}

	/** Opens Add Waypoint with the location behind an offer's button, from the chat or wherever the click was. */
	public static void open(Minecraft client, String key) {
		SharedWaypoint shared = PENDING.get(key);
		if (shared == null) {
			if (client.gui != null) {
				MinecraftClientCompat.chat(client).addClientSystemMessage(EmUtilsChatPrefix.chat(WaypointMessage.sharedExpired()));
			}
			return;
		}
		client.gui.setScreen(WaypointsScreen.addShared(MinecraftClientCompat.screen(client), shared));
	}

}
