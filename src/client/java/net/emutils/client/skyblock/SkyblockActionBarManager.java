package net.emutils.client.skyblock;

import net.emutils.client.skyblock.config.EMSkyblockSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class SkyblockActionBarManager {
	private SkyblockActionBarStats stats = SkyblockActionBarStats.EMPTY;

	public SkyblockActionBarManager() {
		SkyblockContext.events().addListener(event -> {
			if (event instanceof SkyblockEvent.HypixelLeave || event instanceof SkyblockEvent.IslandLeave) {
				clear();
			}
		});
	}

	public SkyblockActionBarStats stats() {
		return stats;
	}

	public void clear() {
		stats = SkyblockActionBarStats.EMPTY;
	}

	public boolean active(MinecraftClient client) {
		return EMSkyblockSettings.skyblockEnabled()
			&& EMSkyblockSettings.skyblockStatsHudEnabled()
			&& client != null
			&& SkyblockFeatures.inSkyBlock(client);
	}

	public boolean shouldHideActionBarStats(MinecraftClient client) {
		return EMSkyblockSettings.skyblockStatsHideActionBar()
			&& active(client)
			&& stats.hasAny();
	}

	public Text processOverlayMessage(Text message) {
		MinecraftClient client = MinecraftClient.getInstance();
		boolean inSkyBlock = SkyblockFeatures.inSkyBlock(client);

		if (active(client)) {
			String plain = Formatting.strip(message.getString());
			SkyblockActionBarStats parsed = SkyblockActionBarParser.parse(plain);
			if (parsed.hasAny()) {
				stats = stats.merge(parsed);
			}

			if (EMSkyblockSettings.skyblockHideActionBarMessages() && inSkyBlock) {
				return Text.empty();
			}

			if (!shouldHideActionBarStats(client)) {
				return message;
			}

			String stripped = SkyblockActionBarParser.stripStats(plain);
			if (stripped.isBlank()) {
				return Text.empty();
			}

			return Text.literal(stripped);
		}

		if (EMSkyblockSettings.skyblockHideActionBarMessages() && inSkyBlock) {
			return Text.empty();
		}

		return message;
	}
}
