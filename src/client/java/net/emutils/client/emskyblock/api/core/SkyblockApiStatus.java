package net.emutils.client.emskyblock.api.core;

import org.jspecify.annotations.Nullable;

public record SkyblockApiStatus(
	String name,
	boolean fetching,
	long lastAttemptMillis,
	long lastSuccessMillis,
	int failureCount,
	@Nullable String lastError
) {
	public static SkyblockApiStatus idle(String name) {
		return new SkyblockApiStatus(name, false, 0L, 0L, 0, null);
	}

	public boolean hasData() {
		return lastSuccessMillis > 0L;
	}
}
