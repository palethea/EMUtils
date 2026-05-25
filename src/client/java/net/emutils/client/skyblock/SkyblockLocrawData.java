package net.emutils.client.skyblock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;

public record SkyblockLocrawData(
	String server,
	String gameType,
	String lobbyName,
	String lobbyType,
	String mode,
	String map
) {
	public static final SkyblockLocrawData EMPTY = new SkyblockLocrawData("", "", "", "", "", "");

	public boolean inLimbo() {
		return "limbo".equalsIgnoreCase(server);
	}

	public boolean inLobby() {
		return !lobbyName.isBlank();
	}

	public boolean inSkyBlock() {
		return "SKYBLOCK".equalsIgnoreCase(gameType);
	}

	public static SkyblockLocrawData fromJson(@Nullable JsonObject object) {
		if (object == null) {
			return EMPTY;
		}

		return new SkyblockLocrawData(
			readString(object, "server"),
			readString(object, "gametype"),
			readString(object, "lobbyname"),
			readString(object, "lobbytype"),
			readString(object, "mode"),
			readString(object, "map")
		);
	}

	@Nullable
	public static SkyblockLocrawData tryParseMessage(@Nullable String text) {
		if (text == null || text.isBlank()) {
			return null;
		}

		String trimmed = text.trim();
		if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
			return null;
		}

		try {
			JsonObject object = JsonParser.parseString(trimmed).getAsJsonObject();
			if (!object.has("server") || !object.has("gametype")) {
				return null;
			}

			return fromJson(object);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	public static boolean isLocrawMessage(@Nullable String text) {
		return tryParseMessage(text) != null;
	}

	private static String readString(JsonObject object, String key) {
		if (!object.has(key) || object.get(key).isJsonNull()) {
			return "";
		}

		return object.get(key).getAsString();
	}
}
