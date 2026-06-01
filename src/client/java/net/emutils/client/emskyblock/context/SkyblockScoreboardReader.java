package net.emutils.client.emskyblock.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class SkyblockScoreboardReader {
	private static final Pattern SKYBLOCK_TITLE = Pattern.compile(
		"SK[YI]BLOCK(?: CO-OP| GUEST)?(?: [♲☀Ⓑ])?",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern HYPIXEL_FOOTER = Pattern.compile("(?:.+\\.)?hypixel\\.net", Pattern.CASE_INSENSITIVE);
	private static final Pattern AREA = Pattern.compile("(?:⏣|ф)\\s*(.+)");
	private static final Pattern PURSE = Pattern.compile("(Piggy|Purse):\\s*(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SERVER = Pattern.compile("\\d+/\\d+/\\d+\\s*[mM](\\S+)");
	private static final Pattern VISITORS = Pattern.compile("✌\\s*\\((\\d+)/(\\d+)\\)");
	private static final Pattern IRONMAN = Pattern.compile("Ironman", Pattern.CASE_INSENSITIVE);
	private static final Pattern STRANDED = Pattern.compile("Stranded", Pattern.CASE_INSENSITIVE);

	private SkyblockScoreboardReader() {
	}

	public record ParsedScoreboard(
		@Nullable String title,
		List<String> lines,
		boolean showsSkyBlock,
		SkyblockProfileModes profileModes,
		@Nullable String area,
		@Nullable String areaWithSymbol,
		@Nullable String serverId,
		double purse,
		double piggyBank,
		int islandVisitorsCurrent,
		int islandVisitorsMax
	) {
		public static ParsedScoreboard empty() {
			return new ParsedScoreboard(null, List.of(), false, SkyblockProfileModes.EMPTY, null, null, null, 0.0D, 0.0D, 0, 0);
		}
	}

	public static ParsedScoreboard read(MinecraftClient client) {
		if (client.world == null) {
			return ParsedScoreboard.empty();
		}

		Scoreboard scoreboard = client.world.getScoreboard();
		ScoreboardObjective objective = objectiveForClient(client, scoreboard);
		if (objective == null) {
			return ParsedScoreboard.empty();
		}

		String title = SkyblockTextUtils.strip(objective.getDisplayName());
		List<String> lines = readLines(scoreboard, objective);
		boolean showsSkyBlock = title != null && SKYBLOCK_TITLE.matcher(title).matches();
		SkyblockProfileModes modes = parseModes(title, lines);
		String area = null;
		String areaWithSymbol = null;
		String serverId = null;
		double purse = 0.0D;
		double piggy = 0.0D;
		int visitorsCurrent = 0;
		int visitorsMax = 0;

		for (String line : lines) {
			String stripped = SkyblockTextUtils.strip(line);
			if (stripped.isEmpty()) {
				continue;
			}

			Matcher areaMatcher = AREA.matcher(stripped);
			if (areaMatcher.find()) {
				areaWithSymbol = stripped;
				area = areaMatcher.group(1).trim();
			}

			Matcher purseMatcher = PURSE.matcher(stripped);
			if (purseMatcher.find()) {
				double amount = SkyblockTextUtils.parseCoins(purseMatcher.group(2));
				if (purseMatcher.group(1).equalsIgnoreCase("piggy")) {
					piggy = amount;
				} else {
					purse = amount;
				}
			}

			Matcher serverMatcher = SERVER.matcher(stripped);
			if (serverMatcher.find()) {
				serverId = serverMatcher.group(1);
			}

			Matcher visitorsMatcher = VISITORS.matcher(stripped);
			if (visitorsMatcher.find()) {
				visitorsCurrent = parseInt(visitorsMatcher.group(1));
				visitorsMax = parseInt(visitorsMatcher.group(2));
			}
		}

		return new ParsedScoreboard(
			title,
			lines,
			showsSkyBlock,
			modes,
			area,
			areaWithSymbol,
			serverId,
			purse,
			piggy,
			visitorsCurrent,
			visitorsMax
		);
	}

	public static boolean isHypixelFooter(@Nullable String line) {
		return line != null && HYPIXEL_FOOTER.matcher(line).find();
	}

	@Nullable
	private static ScoreboardObjective objectiveForClient(MinecraftClient client, Scoreboard scoreboard) {
		if (client.player != null) {
			Team team = scoreboard.getScoreHolderTeam(client.player.getNameForScoreboard());
			if (team != null) {
				ScoreboardDisplaySlot slot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
				if (slot != null) {
					ScoreboardObjective objective = scoreboard.getObjectiveForSlot(slot);
					if (objective != null) {
						return objective;
					}
				}
			}
		}

		return scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
	}

	private static List<String> readLines(Scoreboard scoreboard, ScoreboardObjective objective) {
		Collection<ScoreboardEntry> scores = scoreboard.getScoreboardEntries(objective);
		List<ScoreboardEntry> sorted = new ArrayList<>(scores);
		sorted.removeIf(ScoreboardEntry::hidden);
		sorted.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed());

		List<String> lines = new ArrayList<>(sorted.size());
		for (ScoreboardEntry entry : sorted) {
			Text name = entry.name();
			if (name != null) {
				Team team = scoreboard.getScoreHolderTeam(entry.owner());
				lines.add(SkyblockTextUtils.strip(Team.decorateName(team, name)));
			}
		}

		return lines;
	}

	private static SkyblockProfileModes parseModes(@Nullable String title, List<String> lines) {
		SkyblockProfileModes modes = SkyblockProfileModes.EMPTY;
		if (title != null) {
			String upper = title.toUpperCase();
			modes = modes.withGuest(upper.contains("GUEST"));
			modes = modes.withCoop(upper.contains("CO-OP"));
			modes = modes.withIronman(title.contains("♲"));
			modes = modes.withStranded(title.contains("☀"));
			modes = modes.withBingo(title.contains("Ⓑ"));
		}

		for (String line : lines) {
			if (IRONMAN.matcher(line).find()) {
				modes = modes.withIronman(true);
			}
			if (STRANDED.matcher(line).find()) {
				modes = modes.withStranded(true);
			}
		}

		return modes;
	}

	private static int parseInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}
}
