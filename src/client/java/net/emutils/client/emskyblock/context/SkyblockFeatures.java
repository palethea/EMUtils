package net.emutils.client.emskyblock.context;

import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class SkyblockFeatures {
	private SkyblockFeatures() {
	}

	public static boolean inSkyBlock() {
		return inSkyBlock(MinecraftClient.getInstance());
	}

	public static boolean inSkyBlock(@Nullable MinecraftClient client) {
		if (client != null && !SkyblockManager.isHypixel(client)) {
			return false;
		}

		return SkyblockContext.active();
	}

	public static boolean hideVanillaStatusBars(@Nullable MinecraftClient client) {
		return EMSkyblockSettings.skyblockEnabled()
			&& EMSkyblockSettings.skyblockHideVanillaStatusBars()
			&& inSkyBlock(client);
	}
}
