package net.emutils.client.emskyblock.api.modapi;

import org.jspecify.annotations.Nullable;

public record SkyblockModApiLocationData(
	boolean onHypixel,
	boolean onAlpha,
	@Nullable String serverName,
	@Nullable String serverType,
	@Nullable String lobbyName,
	@Nullable String mode,
	@Nullable String map,
	long updatedAtMillis
) {
	public static final SkyblockModApiLocationData EMPTY = new SkyblockModApiLocationData(false, false, null, null, null, null, null, 0L);
	private static final long STALE_AFTER_MS = 120_000L;

	public boolean fresh() {
		return updatedAtMillis > 0L && System.currentTimeMillis() - updatedAtMillis < STALE_AFTER_MS;
	}

	public boolean inSkyBlock() {
		return onHypixel && fresh() && "SKYBLOCK".equalsIgnoreCase(serverType);
	}
}
