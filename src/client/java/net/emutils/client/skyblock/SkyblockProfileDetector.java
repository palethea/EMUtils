package net.emutils.client.skyblock;

import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

/**
 * Legacy entry points retained for older call sites. Prefer {@link SkyblockContext}.
 */
public final class SkyblockProfileDetector {
	private SkyblockProfileDetector() {
	}

	public static boolean isHypixel(MinecraftClient client) {
		return SkyblockContext.onHypixel(client);
	}

	@Nullable
	public static String detect(MinecraftClient client) {
		String profile = SkyblockContext.detectProfile(client);
		return profile == null ? null : StoragePreviewKeys.normalize(profile);
	}
}
