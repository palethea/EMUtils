package net.emutils.client.hud.layout;

import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.skyblock.config.EMSkyblockSettings;

/**
 * One-time migration of legacy per-feature scale sliders into {@link HudCustomLayoutEntry#scale}.
 */
public final class HudLayoutMigration {
	private static boolean migrated;

	private HudLayoutMigration() {
	}

	public static void migrateIfNeeded(EMUtilsConfig config) {
		if (migrated || config == null) {
			return;
		}

		boolean changed = false;
		changed |= migrateScale(config, HudElementId.INFO_OVERLAY, config.legacyHudScale());
		changed |= migrateScale(config, HudElementId.SPOTIFY, config.legacySpotifyHudScale());
		changed |= migrateScale(config, HudElementId.SKYBLOCK_STATS, EMSkyblockSettings.legacySkyblockStatsHudScale(config));
		changed |= migrateScale(config, HudElementId.ESTIMATED_ITEM_VALUE, EMSkyblockSettings.legacyEstimatedItemValueHudScale(config));
		changed |= migrateScale(config, HudElementId.FISHING_HOOK, EMSkyblockSettings.legacyFishingHookHudScale(config));
		changed |= migrateScale(config, HudElementId.INVENTORY_PREVIEW, 100);

		migrated = true;
		if (changed) {
			config.save();
		}
	}

	private static boolean migrateScale(EMUtilsConfig config, HudElementId id, int legacyScale) {
		HudCustomLayoutEntry entry = config.hudCustomLayoutEntry(id);
		if (entry == null || entry.hasStoredScale()) {
			return false;
		}

		entry.setScale(HudLayoutManager.clampLayoutScale(legacyScale));
		return true;
	}
}
