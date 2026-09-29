package net.emutils.client.emutils.hud;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.emutils.client.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jspecify.annotations.Nullable;

/**
 * What the tab list shows (#170): the players vanilla lists with their display names, skins and pings, the
 * server's header and footer, and the score of the list objective when the server sets one. Read the way
 * vanilla's {@code PlayerTabOverlay} reads it, so the custom list has the same players and text.
 */
record TabListData(List<Entry> entries, @Nullable Component header, @Nullable Component footer, boolean scores, boolean hearts, boolean online) {
	/** Vanilla lists at most this many players. */
	static final int MAX_PLAYERS = 80;
	/** The tab list order the server sets, spectators after the others, then team and name, as vanilla sorts. */
	private static final Comparator<Entry> VANILLA_ORDER = Comparator.<Entry>comparingInt(entry -> -entry.order())
		.thenComparingInt(entry -> entry.spectator() ? 1 : 0)
		.thenComparing(Entry::team)
		.thenComparing(Entry::profileName, String.CASE_INSENSITIVE_ORDER);
	private static final Comparator<Entry> NAME_ORDER = Comparator.comparing(Entry::profileName, String.CASE_INSENSITIVE_ORDER);
	private static final Comparator<Entry> PING_ORDER = Comparator.<Entry>comparingInt(entry -> entry.latency() < 0 ? 1 : 0)
		.thenComparingInt(Entry::latency)
		.thenComparing(Entry::profileName, String.CASE_INSENSITIVE_ORDER);

	/** One player: what a row needs, and what the order is made of. */
	record Entry(
		UUID id,
		String profileName,
		Component name,
		boolean spectator,
		int latency,
		int order,
		String team,
		@Nullable Identifier face,
		boolean hat,
		boolean flipped,
		int score,
		@Nullable Component scoreText,
		boolean self
	) {
	}

	/** The list the server sends this player, or {@code null} outside a world. */
	static @Nullable TabListData current(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null || client.player == null || client.getConnection() == null) {
			return null;
		}
		Scoreboard scoreboard = level.getScoreboard();
		Objective objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
		boolean hearts = objective != null && objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS;
		PlayerTabOverlay overlay = client.gui.hud.getTabList();
		Collection<PlayerInfo> listed = client.player.connection.getListedOnlinePlayers();
		List<Entry> entries = new ArrayList<>(listed.size());
		for (PlayerInfo info : listed) {
			entries.add(entry(client, level, scoreboard, objective, hearts, overlay, info));
		}
		PlayerTabOverlayAccessor accessor = (PlayerTabOverlayAccessor) overlay;
		return new TabListData(entries, accessor.emutils$getHeader(), accessor.emutils$getFooter(), objective != null, hearts, client.getConnection().onlineMode());
	}

	private static Entry entry(Minecraft client, ClientLevel level, Scoreboard scoreboard, @Nullable Objective objective, boolean hearts, PlayerTabOverlay overlay, PlayerInfo info) {
		UUID id = info.getProfile().id();
		Player player = level.getPlayerByUUID(id);
		int score = 0;
		Component scoreText = null;
		if (objective != null) {
			ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(ScoreHolder.fromGameProfile(info.getProfile()), objective);
			if (scoreInfo != null) {
				score = scoreInfo.value();
				if (!hearts) {
					scoreText = scoreInfo.formatValue(objective.numberFormatOrDefault(StyledFormat.PLAYER_LIST_DEFAULT));
				}
			}
		}
		String team = info.getTeam() == null ? "" : info.getTeam().getName();
		return new Entry(
			id,
			info.getProfile().name(),
			overlay.getNameForDisplay(info),
			info.getGameMode() == GameType.SPECTATOR,
			info.getLatency(),
			info.getTabListOrder(),
			team,
			info.getSkin().body().texturePath(),
			info.showHat(),
			player != null && AvatarRenderer.isPlayerUpsideDown(player),
			score,
			scoreText,
			id.equals(client.player.getUUID())
		);
	}

	/** The players in the order picked, at most {@link #MAX_PLAYERS} of them. */
	TabListData sorted(TabListSort sort) {
		Comparator<Entry> order = switch (sort) {
			case VANILLA -> VANILLA_ORDER;
			case NAME -> NAME_ORDER;
			case PING -> PING_ORDER;
		};
		List<Entry> sorted = entries.stream().sorted(order).limit(MAX_PLAYERS).toList();
		return new TabListData(sorted, header, footer, scores, hearts, online);
	}
}
