package net.emutils.client.emutils.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A named set of EMUtils settings (#89). Its settings live in their own config file; this is what the
 * profile list keeps about it: how it's shown and where it loads by itself.
 */
public final class Profile {
	public static final String DEFAULT_ID = "default";
	public static final int MAX_NAME_LENGTH = 32;

	private String id;
	/** Null for the Default profile until it's renamed, so it shows in the game's language. */
	private @Nullable String name;
	private String icon = ProfileIcon.GLOBE.name();
	private String color = ProfileColor.GRAY.name();
	/** Server addresses that load this profile when you join them; a domain also covers its subdomains. */
	private List<String> servers = new ArrayList<>();
	/** Whether singleplayer worlds load this profile. */
	private boolean singleplayer;

	Profile(String id, @Nullable String name, ProfileIcon icon, ProfileColor color) {
		this.id = id;
		this.name = name;
		this.icon = icon.name();
		this.color = color.name();
	}

	public String id() {
		return id;
	}

	public boolean isDefault() {
		return DEFAULT_ID.equals(id);
	}

	public Component name() {
		return name == null || name.isBlank() ? Component.translatable(EMUtilsTexts.UI_PROFILE_DEFAULT_NAME) : Component.literal(name);
	}

	/** The name as typed, or empty for the Default profile's built-in name. */
	public String rawName() {
		return name == null ? "" : name;
	}

	public ProfileIcon icon() {
		return ProfileIcon.byName(icon);
	}

	public ProfileColor color() {
		return ProfileColor.byName(color);
	}

	public List<String> servers() {
		return servers == null ? List.of() : List.copyOf(servers);
	}

	public boolean singleplayer() {
		return singleplayer;
	}

	public boolean autoSwitches() {
		return singleplayer || !servers().isEmpty();
	}

	void set(@Nullable String name, ProfileIcon icon, ProfileColor color, List<String> servers, boolean singleplayer) {
		String trimmed = name == null ? "" : name.strip();
		if (trimmed.length() > MAX_NAME_LENGTH) {
			trimmed = trimmed.substring(0, MAX_NAME_LENGTH);
		}
		this.name = trimmed.isEmpty() && isDefault() ? null : trimmed;
		this.icon = icon.name();
		this.color = color.name();
		this.servers = new ArrayList<>();
		for (String server : servers) {
			String normalized = normalizeAddress(server);
			if (!normalized.isEmpty() && !this.servers.contains(normalized)) {
				this.servers.add(normalized);
			}
		}
		this.singleplayer = singleplayer;
	}

	/** Whether joining a server at {@code address} loads this profile. */
	boolean matchesServer(String address) {
		String joined = normalizeAddress(address);
		String joinedHost = host(joined);
		for (String server : servers()) {
			String rule = normalizeAddress(server);
			if (rule.isEmpty()) {
				continue;
			}
			// A rule with a port only matches that port; one without matches the host on any port.
			String candidate = rule.contains(":") ? joined : joinedHost;
			if (candidate.equals(rule) || candidate.endsWith("." + rule)) {
				return true;
			}
		}
		return false;
	}

	/** Makes addresses comparable: lower case, no spaces, and without the default port or a trailing dot. */
	public static String normalizeAddress(String address) {
		String normalized = address == null ? "" : address.strip().toLowerCase(Locale.ROOT).replace(" ", "");
		if (normalized.endsWith(":25565")) {
			normalized = normalized.substring(0, normalized.length() - ":25565".length());
		}
		while (normalized.endsWith(".")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	private static String host(String address) {
		int colon = address.lastIndexOf(':');
		// IPv6 addresses have several colons and are only matched whole.
		return colon > 0 && address.indexOf(':') == colon ? address.substring(0, colon) : address;
	}

	/** Fixes a profile read from a hand-edited file. */
	void repair() {
		if (servers == null) {
			servers = new ArrayList<>();
		}
		if (icon == null) {
			icon = ProfileIcon.GLOBE.name();
		}
		if (color == null) {
			color = ProfileColor.GRAY.name();
		}
	}
}
