package net.emutils.client.emskyblock.features.gui.statshud;

import org.jspecify.annotations.Nullable;

public record SkyblockActionBarStats(
	@Nullable Integer healthCurrent,
	@Nullable Integer healthMax,
	@Nullable Integer defense,
	@Nullable Integer manaCurrent,
	@Nullable Integer manaMax,
	@Nullable Integer soulflow
) {
	public static final SkyblockActionBarStats EMPTY = new SkyblockActionBarStats(null, null, null, null, null, null);

	public boolean hasAny() {
		return healthCurrent != null
			|| healthMax != null
			|| defense != null
			|| manaCurrent != null
			|| manaMax != null
			|| soulflow != null;
	}

	public int absorption() {
		if (healthCurrent == null || healthMax == null || healthCurrent <= healthMax) {
			return 0;
		}

		return healthCurrent - healthMax;
	}

	public int baseHealthCurrent() {
		if (healthCurrent == null) {
			return 0;
		}

		if (healthMax == null) {
			return healthCurrent;
		}

		return Math.min(healthCurrent, healthMax);
	}

	public SkyblockActionBarStats merge(SkyblockActionBarStats update) {
		return new SkyblockActionBarStats(
			update.healthCurrent != null ? update.healthCurrent : healthCurrent,
			update.healthMax != null ? update.healthMax : healthMax,
			update.defense != null ? update.defense : defense,
			update.manaCurrent != null ? update.manaCurrent : manaCurrent,
			update.manaMax != null ? update.manaMax : manaMax,
			update.soulflow != null ? update.soulflow : soulflow
		);
	}
}
