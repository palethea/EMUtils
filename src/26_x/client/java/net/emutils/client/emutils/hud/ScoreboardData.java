package net.emutils.client.emutils.hud;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import org.jspecify.annotations.Nullable;

/**
 * What the sidebar shows (#169): the objective's title and its lines, picked and ordered the way vanilla's
 * {@code Hud#displayScoreboardSidebar} does. Each line is a name, with its team prefix and suffix, and the
 * score as the objective's number format writes it, which is empty for a blank format.
 */
record ScoreboardData(Component title, List<Line> lines) {
	/** Vanilla shows at most this many lines. */
	static final int MAX_LINES = 15;
	/** Highest score first, ties by name without regard to case, as vanilla sorts them. */
	private static final Comparator<PlayerScoreEntry> ORDER = Comparator.comparing(PlayerScoreEntry::value).reversed()
		.thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

	record Line(Component name, Component score) {
	}

	/** The sidebar the game would show this player, or {@code null} while there is none. */
	static @Nullable ScoreboardData current(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null || client.player == null) {
			return null;
		}
		Scoreboard scoreboard = level.getScoreboard();
		// The sidebar for the player's team color takes the place of the plain one.
		Objective objective = null;
		PlayerTeam team = scoreboard.getPlayersTeam(client.player.getScoreboardName());
		if (team != null) {
			Optional<TeamColor> color = team.getColor();
			if (color.isPresent()) {
				objective = scoreboard.getDisplayObjective(color.get().displaySlot());
			}
		}
		if (objective == null) {
			objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		}
		return objective == null ? null : of(scoreboard, objective);
	}

	private static ScoreboardData of(Scoreboard scoreboard, Objective objective) {
		var numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
		List<Line> lines = scoreboard.listPlayerScores(objective).stream()
			.filter(entry -> !entry.isHidden())
			.sorted(ORDER)
			.limit(MAX_LINES)
			.map(entry -> new Line(
				PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()),
				entry.formatValue(numberFormat)
			))
			.toList();
		return new ScoreboardData(objective.getDisplayName(), lines);
	}

	/** A made-up sidebar for the layout editor, while the server sends none. */
	static ScoreboardData sample() {
		return new ScoreboardData(
			Component.translatable("emutils.scoreboard.sample.title").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
			List.of(
				sampleLine("emutils.scoreboard.sample.line1", ChatFormatting.WHITE, 12),
				sampleLine("emutils.scoreboard.sample.line2", ChatFormatting.AQUA, 9),
				sampleLine("emutils.scoreboard.sample.line3", ChatFormatting.GREEN, 7),
				sampleLine("emutils.scoreboard.sample.line4", ChatFormatting.GRAY, 3)
			)
		);
	}

	private static Line sampleLine(String key, ChatFormatting color, int score) {
		return new Line(
			Component.translatable(key).withStyle(color),
			Component.literal(Integer.toString(score)).withStyle(ChatFormatting.RED)
		);
	}
}
