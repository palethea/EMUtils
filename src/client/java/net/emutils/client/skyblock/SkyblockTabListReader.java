package net.emutils.client.skyblock;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class SkyblockTabListReader {
	private static final Pattern PROFILE = Pattern.compile("Profile:\\s*(.+?)(?:\\s*[♲☀Ⓑ]+)?$", Pattern.CASE_INSENSITIVE);
	private static final Pattern AREA = Pattern.compile("(?:Area|Dungeon):\\s*(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SERVER = Pattern.compile("Server:\\s*(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern BANK = Pattern.compile("Bank:\\s*(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern PLAYERS = Pattern.compile("Players\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

	private SkyblockTabListReader() {
	}

	public record ParsedTabList(
		@Nullable String profileName,
		@Nullable String islandName,
		@Nullable String serverId,
		@Nullable String bankBalance,
		int playerCount,
		List<String> lines
	) {
		public static ParsedTabList empty() {
			return new ParsedTabList(null, null, null, null, 0, List.of());
		}
	}

	public static ParsedTabList read(MinecraftClient client, @Nullable Text header, @Nullable Text footer) {
		List<String> lines = new ArrayList<>();
		lines.addAll(SkyblockTextUtils.splitLines(header));
		lines.addAll(collectPlayerListLines(client));
		lines.addAll(SkyblockTextUtils.splitLines(footer));

		String profile = null;
		String island = null;
		String serverId = null;
		String bank = null;
		int playerCount = 0;

		for (String line : lines) {
			Matcher profileMatcher = PROFILE.matcher(line);
			if (profileMatcher.find()) {
				profile = SkyblockTextUtils.normalizeProfile(profileMatcher.group(1));
			}

			Matcher areaMatcher = AREA.matcher(line);
			if (areaMatcher.find()) {
				island = SkyblockTextUtils.strip(areaMatcher.group(1));
			}

			Matcher serverMatcher = SERVER.matcher(line);
			if (serverMatcher.find()) {
				serverId = SkyblockTextUtils.strip(serverMatcher.group(1));
			}

			Matcher bankMatcher = BANK.matcher(line);
			if (bankMatcher.find()) {
				bank = SkyblockTextUtils.strip(bankMatcher.group(1));
			}

			Matcher playersMatcher = PLAYERS.matcher(line);
			if (playersMatcher.find()) {
				playerCount = Math.max(playerCount, parseInt(playersMatcher.group(1)));
			}
		}

		return new ParsedTabList(profile, island, serverId, bank, playerCount, List.copyOf(lines));
	}

	private static List<String> collectPlayerListLines(MinecraftClient client) {
		if (client.getNetworkHandler() == null) {
			return List.of();
		}

		List<String> lines = new ArrayList<>();
		for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
			Text displayName = entry.getDisplayName();
			if (displayName != null) {
				String stripped = SkyblockTextUtils.strip(displayName);
				if (!stripped.isEmpty()) {
					lines.add(stripped);
				}
			}
		}

		for (PlayerListEntry entry : client.getNetworkHandler().getListedPlayerListEntries()) {
			Text displayName = entry.getDisplayName();
			if (displayName != null) {
				String stripped = SkyblockTextUtils.strip(displayName);
				if (!stripped.isEmpty() && !lines.contains(stripped)) {
					lines.add(stripped);
				}
			}
		}

		return lines;
	}

	private static int parseInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}
}
