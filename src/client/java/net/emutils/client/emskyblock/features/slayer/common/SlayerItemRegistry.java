package net.emutils.client.emskyblock.features.slayer.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.emutils.client.EMUtilsClient;

public final class SlayerItemRegistry {
	private static final Map<SlayerBossType, Set<String>> ALLOWED_BY_BOSS =
		new EnumMap<>(SlayerBossType.class);
	private static final Set<String> ALL_ALLOWED = new HashSet<>();
	private static final Map<String, String> DISPLAY_NAME_OVERRIDES = buildDisplayNameOverrides();
	private static boolean loaded;

	private SlayerItemRegistry() {}

	public static void load() {
		if (loaded) {
			return;
		}

		loaded = true;
		try (
			InputStream stream = SlayerItemRegistry.class.getResourceAsStream(
				"/data/emutils/skyblock/slayer_profit_items.json"
			)
		) {
			if (stream == null) {
				EMUtilsClient.LOGGER.warn("Missing slayer_profit_items.json");
				return;
			}

			JsonObject root = JsonParser.parseReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8)
			).getAsJsonObject();
			JsonObject bosses = root.getAsJsonObject("bosses");
			for (SlayerBossType type : SlayerBossType.values()) {
				JsonElement bossElement = bosses.get(type.name());
				Set<String> set = new HashSet<>();
				if (bossElement != null && bossElement.isJsonObject()) {
					JsonArray items = bossElement.getAsJsonObject().getAsJsonArray("items");
					if (items != null) {
						for (JsonElement item : items) {
							String id = item.getAsString();
							set.add(id);
							ALL_ALLOWED.add(id);
						}
					}
				}
				ALLOWED_BY_BOSS.put(type, set);
			}

			EMUtilsClient.LOGGER.info(
				"Loaded {} slayer profit item ids across {} bosses.",
				ALL_ALLOWED.size(),
				ALLOWED_BY_BOSS.size()
			);
		} catch (Exception exception) {
			EMUtilsClient.LOGGER.warn(
				"Failed to load slayer profit item registry.",
				exception
			);
		}
	}

	public static boolean isAllowedFor(String itemId, SlayerBossType boss) {
		load();
		if (itemId == null) {
			return false;
		}

		return boss != null && ALLOWED_BY_BOSS.get(boss).contains(itemId);
	}

	public static Set<String> allowedIds(SlayerBossType boss) {
		load();
		return Collections.unmodifiableSet(ALLOWED_BY_BOSS.get(boss));
	}

	public static Map<SlayerBossType, Set<String>> all() {
		load();
		return Collections.unmodifiableMap(ALLOWED_BY_BOSS);
	}

	public static String displayName(String itemId) {
		String override = DISPLAY_NAME_OVERRIDES.get(itemId);
		if (override != null) {
			return override;
		}

		String[] parts = itemId.toLowerCase(Locale.ROOT).split("_");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}

			if (!builder.isEmpty()) {
				builder.append(' ');
			}

			builder.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				builder.append(part.substring(1));
			}
		}

		return builder.toString();
	}

	private static Map<String, String> buildDisplayNameOverrides() {
		Map<String, String> map = new java.util.HashMap<>();
		map.put("GHOUL;3", "Ghoul Pet");
		map.put("GHOUL;4", "Ghoul Pet");
		map.put("TARANTULA;3", "Tarantula Pet");
		map.put("TARANTULA;4", "Tarantula Pet");
		map.put("HOUND;3", "Hound Pet");
		map.put("HOUND;4", "Hound Pet");
		map.put("ENDERMAN;0", "Enderman Pet");
		map.put("ENDERMAN;1", "Enderman Pet");
		map.put("ENDERMAN;2", "Enderman Pet");
		map.put("ENDERMAN;3", "Enderman Pet");
		map.put("ENDERMAN;4", "Enderman Pet");
		map.put("BONE", "Bone");
		map.put("GOLD_INGOT", "Gold Ingot");
		map.put("ROTTEN_FLESH", "Rotten Flesh");
		map.put("SPIDER_EYE", "Spider Eye");
		map.put("STRING", "String");
		map.put("ENDER_PEARL", "Ender Pearl");
		map.put("ENCHANTED_ENDER_PEARL", "Enchanted Ender Pearl");
		map.put("BLAZE_ROD", "Blaze Rod");
		map.put("ENCHANTED_BLAZE_POWDER", "Enchanted Blaze Powder");
		map.put("BLAZE_ASHES", "Blaze Ashes");
		map.put("DYE_MATCHA", "Matcha Dye");
		map.put("DYE_BRICK_RED", "Brick Red Dye");
		map.put("DYE_CELESTE", "Celeste Dye");
		map.put("DYE_BYZANTIUM", "Byzantium Dye");
		map.put("DYE_FLAME", "Flame Dye");
		map.put("DYE_SANGRIA", "Sangria Dye");
		map.put("SHARD_OF_THE_SHREDDED", "Shard Of The Shredded");
		map.put("WARDEN_HEART", "Warden Heart");
		map.put("SCYTHE_BLADE", "Scythe Blade");
		map.put("SMITE;6", "Smite VI");
		map.put("SMITE;7", "Smite VII");
		map.put("FIRE_ASPECT;3", "Fire Aspect III");
		map.put("BANE_OF_ARTHROPODS;6", "Bane Of Arthropods VI");
		map.put("CRITICAL;6", "Critical VI");
		map.put("ENDER_SLAYER;7", "Ender Slayer VII");
		map.put("ULTIMATE_REITERATE;1", "Ultimate Reiterate I");
		map.put("FIERY_BURST_RUNE;1", "Fiery Burst Rune I");
		map.put("SPIRIT_RUNE;1", "Spirit Rune I");
		map.put("COUTURE_RUNE;1", "Couture Rune I");
		map.put("ENCHANT_RUNE;1", "Enchant Rune I");
		map.put("DARKNESS_WITHIN_RUNE;1", "Darkness Within Rune I");
		map.put("DRAGON_RUNE;1", "Dragon Rune I");
		map.put("SMARTY_PANTS;1", "Smarty Pants I");
		map.put("SNAKE_RUNE;1", "Snake Rune I");
		map.put("BITE_RUNE;1", "Bite Rune I");
		map.put("ENDERSNAKE_RUNE;1", "Endersnake Rune I");
		map.put("ZOMBIE_SLAYER_RUNE;1", "Zombie Slayer Rune I");
		map.put("LAVATEARS_RUNE;1", "Lavatears Rune I");
		map.put("SOULTWIST_RUNE;1", "Soultwist Rune I");
		map.put("MANA_STEAL;1", "Mana Steal I");
		map.put("SMOLDERING;1", "Smoldering I");
		map.put("FLAWED_OPAL_GEM", "Flawed Opal Gem");
		map.put("PET_SKIN_ENDERMAN_SLAYER", "Enderman Slayer Skin");
		map.put("PET_ITEM_FORAGING_SKILL_BOOST_EPIC", "Foraging Skill Boost (Epic)");
		map.put("ATTRIBUTE_SHARD_UNDEAD_ESSENCE;1", "Undead Essence Shard");
		map.put("ATTRIBUTE_SHARD_LIFE_RECOVERY;1", "Life Recovery Shard");
		map.put("ATTRIBUTE_SHARD_MIDAS_TOUCH;1", "Midas Touch Shard");
		map.put("ATTRIBUTE_SHARD_EXPERIENCE;1", "Experience Shard");
		map.put("ATTRIBUTE_SHARD_ROTTEN_PICKAXE;1", "Rotten Pickaxe Shard");
		map.put("ATTRIBUTE_SHARD_ARACHNO;1", "Arachno Shard");
		map.put("ATTRIBUTE_SHARD_ARACHNO_RESISTANCE;1", "Arachno Resistance Shard");
		map.put("ATTRIBUTE_SHARD_SPIDER_ESSENCE;1", "Spider Essence Shard");
		map.put("ATTRIBUTE_SHARD_COMBO;1", "Combo Shard");
		map.put("ATTRIBUTE_SHARD_ENDER;1", "Ender Shard");
		map.put("ATTRIBUTE_SHARD_ENDER_RESISTANCE;1", "Ender Resistance Shard");
		map.put("ATTRIBUTE_SHARD_ATTACK_SPEED;1", "Attack Speed Shard");
		return Collections.unmodifiableMap(map);
	}

	public static List<String> categoriesForDisplay() {
		List<String> names = new ArrayList<>();
		for (SlayerBossType type : SlayerBossType.values()) {
			names.add(type.displayName());
		}
		return names;
	}
}
