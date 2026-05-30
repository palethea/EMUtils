package net.emutils.client.emskyblock.features.inventory.estimateditemvalue;

import java.util.List;
import java.util.Locale;
import net.emutils.client.emskyblock.context.SkyblockTextUtils;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public enum SkyblockItemRarity {
	COMMON,
	UNCOMMON,
	RARE,
	EPIC,
	LEGENDARY,
	MYTHIC,
	DIVINE,
	SPECIAL,
	VERY_SPECIAL;

	public SkyblockItemRarity oneBelow() {
		return switch (this) {
			case VERY_SPECIAL -> SPECIAL;
			case SPECIAL -> MYTHIC;
			case MYTHIC -> LEGENDARY;
			case LEGENDARY -> EPIC;
			case EPIC -> RARE;
			case RARE -> UNCOMMON;
			case UNCOMMON -> COMMON;
			case COMMON, DIVINE -> COMMON;
		};
	}

	public String colorCode() {
		return switch (this) {
			case COMMON -> "§f";
			case UNCOMMON -> "§a";
			case RARE -> "§9";
			case EPIC -> "§5";
			case LEGENDARY -> "§6";
			case MYTHIC -> "§d";
			case DIVINE -> "§b";
			case SPECIAL -> "§c";
			case VERY_SPECIAL -> "§4";
		};
	}

	@Nullable
	public static SkyblockItemRarity fromTooltip(List<Text> tooltip, boolean recombobulated) {
		SkyblockItemRarity rarity = fromTooltip(tooltip);
		if (rarity == null) {
			return null;
		}

		if (recombobulated && rarity.ordinal() > COMMON.ordinal() && rarity != DIVINE) {
			if (rarity == SPECIAL || rarity == VERY_SPECIAL) {
				return LEGENDARY;
			}

			return rarity.oneBelow();
		}

		return rarity;
	}

	@Nullable
	public static SkyblockItemRarity fromTooltip(List<Text> tooltip) {
		for (Text line : tooltip) {
			String stripped = SkyblockTextUtils.normalizeKey(line.getString());
			for (SkyblockItemRarity rarity : values()) {
				if (stripped.contains(rarity.name().toLowerCase(Locale.ROOT))) {
					return rarity;
				}
			}
		}

		return null;
	}
}
