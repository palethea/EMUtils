package net.emutils.client.emskyblock.api;

import net.emutils.client.emskyblock.api.modapi.SkyblockModApiLocationData;
import org.jspecify.annotations.Nullable;

public final class SkyblockApiContext {
	@Nullable
	private static SkyblockApiManager manager;

	private SkyblockApiContext() {
	}

	public static void bind(SkyblockApiManager apiManager) {
		manager = apiManager;
	}

	@Nullable
	public static SkyblockApiManager manager() {
		return manager;
	}

	public static SkyblockModApiLocationData location() {
		SkyblockApiManager apiManager = manager;
		return apiManager != null ? apiManager.modApiLocation() : SkyblockModApiLocationData.EMPTY;
	}
}
