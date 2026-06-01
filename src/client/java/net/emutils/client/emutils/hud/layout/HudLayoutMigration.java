package net.emutils.client.emutils.hud.layout;

import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emhelpers.client.hud.layout.HudCustomLayoutEntry;
import net.emhelpers.client.hud.layout.HudElementId;
import net.emhelpers.client.hud.layout.HudLayoutManager;

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
		changed |= migrateScale(config, net.emutils.client.EMUtilsHudElements.INFO_OVERLAY, config.legacyHudScale());
		changed |= migrateScale(config, net.emutils.client.EMUtilsHudElements.SPOTIFY, config.legacySpotifyHudScale());
		changed |= migrateScale(config, net.emutils.client.EMUtilsHudElements.INVENTORY_PREVIEW, 100);

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
