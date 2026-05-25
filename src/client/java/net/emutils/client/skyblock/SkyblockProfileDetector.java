package net.emutils.client.skyblock;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class SkyblockProfileDetector {
	private static final Pattern PROFILE = Pattern.compile("Profile:\\s*([A-Za-z]+)");

	private SkyblockProfileDetector() {
	}

	public static boolean isHypixel(MinecraftClient client) {
		ServerInfo serverInfo = client.getCurrentServerEntry();
		if (serverInfo == null || serverInfo.address == null) {
			return false;
		}

		return serverInfo.address.toLowerCase(Locale.ROOT).contains("hypixel.net");
	}

	@Nullable
	public static String detect(MinecraftClient client) {
		if (client.getNetworkHandler() == null) {
			return null;
		}

		Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
		for (PlayerListEntry entry : entries) {
			@Nullable String profile = parseProfile(entry.getDisplayName());
			if (profile != null) {
				return profile;
			}
		}

		for (PlayerListEntry entry : client.getNetworkHandler().getListedPlayerListEntries()) {
			@Nullable String profile = parseProfile(entry.getDisplayName());
			if (profile != null) {
				return profile;
			}
		}

		return null;
	}

	@Nullable
	private static String parseProfile(@Nullable Text displayName) {
		if (displayName == null) {
			return null;
		}

		Matcher matcher = PROFILE.matcher(displayName.getString());
		if (matcher.find()) {
			return StoragePreviewKeys.normalize(matcher.group(1));
		}

		return null;
	}
}
