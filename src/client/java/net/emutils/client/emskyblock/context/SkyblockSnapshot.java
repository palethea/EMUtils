package net.emutils.client.emskyblock.context;

import org.jspecify.annotations.Nullable;

public record SkyblockSnapshot(
	boolean onHypixel,
	boolean onAlpha,
	boolean inSkyBlock,
	boolean inLobby,
	boolean inLimbo,
	@Nullable String profileName,
	SkyblockProfileModes profileModes,
	SkyblockIsland island,
	@Nullable String scoreboardTitle,
	@Nullable String area,
	@Nullable String areaWithSymbol,
	@Nullable String serverId,
	double purse,
	double piggyBank,
	@Nullable String bankBalance,
	int playerCount,
	int islandVisitorsCurrent,
	int islandVisitorsMax,
	SkyblockLocrawData locraw,
	long updatedAtMillis
) {
	public static SkyblockSnapshot empty() {
		return new SkyblockSnapshot(
			false,
			false,
			false,
			false,
			false,
			null,
			SkyblockProfileModes.EMPTY,
			SkyblockIsland.NONE,
			null,
			null,
			null,
			null,
			0.0D,
			0.0D,
			null,
			0,
			0,
			0,
			SkyblockLocrawData.EMPTY,
			0L
		);
	}

	public boolean active() {
		return onHypixel && inSkyBlock;
	}

	public boolean hasProfile() {
		return profileName != null && !profileName.isBlank();
	}

	public boolean guestIsland() {
		return island.guestVariant();
	}
}
