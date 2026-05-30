package net.emutils.client.emskyblock.context;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

public enum SkyblockIsland {
	PRIVATE_ISLAND("Private Island", "dynamic"),
	PRIVATE_ISLAND_GUEST("Private Island", "dynamic", true),
	THE_END("The End", "combat_3"),
	CRIMSON_ISLE("Crimson Isle", "crimson_isle"),
	CRYSTAL_HOLLOWS("Crystal Hollows", "crystal_hollows"),
	DARK_AUCTION("Dark Auction", "dark_auction"),
	DEEP_CAVERNS("Deep Caverns", "deep_caverns"),
	DUNGEON_HUB("Dungeon Hub", "dungeon_hub"),
	CATACOMBS("Catacombs", "dungeon"),
	DWARVEN_MINES("Dwarven Mines", "mining_3"),
	GOLD_MINES("Gold Mine", "mining_2"),
	GARDEN("Garden", "garden"),
	GARDEN_GUEST("Garden", "garden", true),
	HUB("Hub", "hub"),
	KUUDRA_ARENA("Kuudra Arena", "kuudra"),
	MINING_ISLAND("The Farming Islands", "farming_1"),
	THE_PARK("The Park", "foraging_1"),
	SPIDER_DEN("Spider's Den", "combat_1"),
	WINTER("Jerry's Workshop", "winter"),
	THE_RIFT("The Rift", "rift"),
	MINESHAFT("Mineshaft", "mineshaft"),
	BACKWATER_BAYOU("Backwater Bayou", "backwater_bayou"),
	GALATEA("Galatea", "galatea"),
	LOTUS_ATOLL("Lotus Atoll", "lotus_atoll"),
	NONE("None", ""),
	UNKNOWN("Unknown", "");

	private final String displayName;
	private final String apiMode;
	private final boolean guestVariant;

	SkyblockIsland(String displayName, String apiMode) {
		this(displayName, apiMode, false);
	}

	SkyblockIsland(String displayName, String apiMode, boolean guestVariant) {
		this.displayName = displayName;
		this.apiMode = apiMode;
		this.guestVariant = guestVariant;
	}

	public String displayName() {
		return displayName;
	}

	public String apiMode() {
		return apiMode;
	}

	public boolean guestVariant() {
		return guestVariant;
	}

	public boolean hasGuestVariant() {
		return this == PRIVATE_ISLAND || this == GARDEN;
	}

	public SkyblockIsland guestVariantOf() {
		return switch (this) {
			case PRIVATE_ISLAND -> PRIVATE_ISLAND_GUEST;
			case GARDEN -> GARDEN_GUEST;
			case PRIVATE_ISLAND_GUEST -> PRIVATE_ISLAND;
			case GARDEN_GUEST -> GARDEN;
			default -> this;
		};
	}

	public static SkyblockIsland fromTabName(@Nullable String rawName) {
		if (rawName == null || rawName.isBlank()) {
			return NONE;
		}

		String normalized = SkyblockTextUtils.strip(rawName);
		for (SkyblockIsland island : values()) {
			if (isMeta(island)) {
				continue;
			}

			if (island.displayName.equalsIgnoreCase(normalized)) {
				return island;
			}
		}

		return UNKNOWN;
	}

	public static SkyblockIsland fromLocrawMode(@Nullable String mode) {
		if (mode == null || mode.isBlank()) {
			return NONE;
		}

		String normalized = mode.toLowerCase(Locale.ROOT);
		for (SkyblockIsland island : values()) {
			if (isMeta(island) || island.apiMode.isBlank()) {
				continue;
			}

			if (island.apiMode.equalsIgnoreCase(normalized)) {
				return island;
			}
		}

		return UNKNOWN;
	}

	private static boolean isMeta(SkyblockIsland island) {
		return island == NONE || island == UNKNOWN;
	}
}
