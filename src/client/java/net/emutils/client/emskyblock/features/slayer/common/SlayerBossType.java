package net.emutils.client.emskyblock.features.slayer.common;

import java.util.Locale;
import net.emutils.client.emskyblock.context.SkyblockIsland;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import org.jspecify.annotations.Nullable;

public enum SlayerBossType {
	REVENANT("Revenant Horror", "Zombie", "revenant"),
	TARANTULA("Tarantula Broodfather", "Spider", "tarantula"),
	SVEN("Sven Packmaster", "Wolf", "sven"),
	VOID("Voidgloom Seraph", "Enderman", "voidgloom"),
	INFERNO("Inferno Demonlord", "Blaze", "inferno"),
	VAMPIRE("Bloodfiend", "Vampire", "vampire");

	private final String displayName;
	private final String mobClass;
	private final String configKey;

	SlayerBossType(String displayName, String mobClass, String configKey) {
		this.displayName = displayName;
		this.mobClass = mobClass;
		this.configKey = configKey;
	}

	public String displayName() {
		return displayName;
	}

	public String mobClass() {
		return mobClass;
	}

	public String configKey() {
		return configKey;
	}

	public String langKey() {
		return EMUtilsTexts.SLAYER_BOSS_PREFIX + configKey;
	}

	public String titleLabel() {
		return displayName;
	}

	@Nullable
	public static SlayerBossType fromMobClass(String raw) {
		if (raw == null) {
			return null;
		}

		String normalized = raw.trim().toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "zombie" -> REVENANT;
			case "spider" -> TARANTULA;
			case "wolf" -> SVEN;
			case "enderman" -> VOID;
			case "blaze" -> INFERNO;
			case "vampire" -> VAMPIRE;
			default -> null;
		};
	}

	@Nullable
	public static SlayerBossType forIsland(SkyblockIsland island, @Nullable String area) {
		if (island == null) {
			return null;
		}

		String areaLower = area == null ? "" : area.toLowerCase(Locale.ROOT);
		return switch (island) {
			case SPIDER_DEN -> {
				if (areaLower.contains("arachne") || areaLower.contains("mound") || areaLower.contains("nest")) {
					yield TARANTULA;
				}
				yield TARANTULA;
			}
			case THE_END -> VOID;
			case CRIMSON_ISLE -> {
				if (areaLower.contains("smoldering") || areaLower.contains("wasteland") || areaLower.contains("stronghold")) {
					yield INFERNO;
				}
				yield null;
			}
			case THE_RIFT -> VAMPIRE;
			case HUB -> {
				if (areaLower.contains("graveyard")) {
					yield REVENANT;
				}
				yield null;
			}
			case THE_PARK -> {
				if (
					areaLower.contains("howling") ||
					areaLower.contains("soul") ||
					areaLower.contains("spirit") ||
					areaLower.contains("ruins")
				) {
					yield SVEN;
				}
				yield null;
			}
			default -> null;
		};
	}
}
