package net.emutils.client.skyblock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Formatting;

public final class StoragePreviewKeys {
	private static final Pattern ENDER_CHEST = Pattern.compile("Ender Chest \\((\\d+)/(\\d+)\\)", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENDER_CHEST_PAGE = Pattern.compile("Ender Chest Page (\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern BACKPACK = Pattern.compile("(?:(\\w+(?: \\w+)*) )?Backpack \\(Slot #?(\\d+)\\)", Pattern.CASE_INSENSITIVE);
	private static final Pattern BACKPACK_SLOT = Pattern.compile("Backpack Slot #?(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SLOT_NUMBER = Pattern.compile("Slot #?(\\d+)", Pattern.CASE_INSENSITIVE);

	private StoragePreviewKeys() {
	}

	@org.jspecify.annotations.Nullable
	public static String idFromTitle(String title) {
		Matcher enderChest = ENDER_CHEST.matcher(title);
		if (enderChest.find()) {
			return "ender_chest:" + enderChest.group(1);
		}

		Matcher enderChestPage = ENDER_CHEST_PAGE.matcher(title);
		if (enderChestPage.find()) {
			return "ender_chest:" + enderChestPage.group(1);
		}

		Matcher backpack = BACKPACK.matcher(title);
		if (backpack.find()) {
			return backpackId(backpack.group(1), backpack.group(2));
		}

		return null;
	}

	public static List<String> aliasesFromTitle(String title) {
		Set<String> aliases = new LinkedHashSet<>();
		addNormalized(aliases, title);

		Matcher enderChest = ENDER_CHEST.matcher(title);
		if (enderChest.find()) {
			int page = Integer.parseInt(enderChest.group(1));
			addNormalized(aliases, "Ender Chest Page " + page);
		}

		Matcher enderChestPage = ENDER_CHEST_PAGE.matcher(title);
		if (enderChestPage.find()) {
			int page = Integer.parseInt(enderChestPage.group(1));
			addNormalized(aliases, "Ender Chest Page " + page);
		}

		Matcher backpack = BACKPACK.matcher(title);
		if (backpack.find()) {
			addBackpackAliases(aliases, backpack.group(1), backpack.group(2));
		}

		return new ArrayList<>(aliases);
	}

	public static List<String> derivedLookupKeys(String normalizedName, List<String> tooltipLines) {
		Set<String> keys = new LinkedHashSet<>();
		if (!normalizedName.isBlank()) {
			keys.add(normalizedName);
		}

		for (String line : tooltipLines) {
			if (!line.isBlank()) {
				keys.add(line);
			}
		}

		for (String source : combinedSources(normalizedName, tooltipLines)) {
			Matcher enderChestPage = ENDER_CHEST_PAGE.matcher(source);
			if (enderChestPage.find()) {
				addNormalized(keys, enderChestPage.group(0));
			}

			Matcher backpack = BACKPACK.matcher(source);
			if (backpack.find()) {
				addBackpackAliases(keys, backpack.group(1), backpack.group(2));
			}

			Matcher backpackSlot = BACKPACK_SLOT.matcher(source);
			if (backpackSlot.find()) {
				addNormalized(keys, "Backpack Slot #" + backpackSlot.group(1));
				addNormalized(keys, "Backpack Slot " + backpackSlot.group(1));
				addNormalized(keys, "Backpack (Slot #" + backpackSlot.group(1) + ")");
			}
		}

		if (normalizedName.contains("backpack")) {
			Integer slot = findSlotNumber(normalizedName, tooltipLines);
			String type = extractBackpackType(normalizedName);
			if (slot != null) {
				if (type != null) {
					addBackpackAliases(keys, type, Integer.toString(slot));
				} else {
					addNormalized(keys, "Backpack (Slot #" + slot + ")");
					addNormalized(keys, "Backpack Slot #" + slot);
				}
			}
		}

		return new ArrayList<>(keys);
	}

	private static void addBackpackAliases(Set<String> aliases, @org.jspecify.annotations.Nullable String type, String slot) {
		addNormalized(aliases, "Backpack (Slot #" + slot + ")");
		addNormalized(aliases, "Backpack Slot #" + slot);
		addNormalized(aliases, "Backpack Slot " + slot);
		if (type != null && !type.isBlank()) {
			addNormalized(aliases, type.trim() + " Backpack (Slot #" + slot + ")");
		}
	}

	private static String backpackId(@org.jspecify.annotations.Nullable String type, String slot) {
		if (type == null || type.isBlank()) {
			return "backpack:" + slot;
		}

		return "backpack:" + slug(type) + ":" + slot;
	}

	@org.jspecify.annotations.Nullable
	private static Integer findSlotNumber(String normalizedName, List<String> tooltipLines) {
		Matcher nameMatcher = BACKPACK.matcher(normalizedName);
		if (nameMatcher.find()) {
			return Integer.parseInt(nameMatcher.group(2));
		}

		for (String source : combinedSources(normalizedName, tooltipLines)) {
			Matcher backpack = BACKPACK.matcher(source);
			if (backpack.find()) {
				return Integer.parseInt(backpack.group(2));
			}

			Matcher backpackSlot = BACKPACK_SLOT.matcher(source);
			if (backpackSlot.find()) {
				return Integer.parseInt(backpackSlot.group(1));
			}

			Matcher slot = SLOT_NUMBER.matcher(source);
			if (slot.find()) {
				return Integer.parseInt(slot.group(1));
			}
		}

		return null;
	}

	@org.jspecify.annotations.Nullable
	private static String extractBackpackType(String normalizedName) {
		if (!normalizedName.contains("backpack")) {
			return null;
		}

		int index = normalizedName.indexOf("backpack");
		if (index <= 0) {
			return null;
		}

		String prefix = normalizedName.substring(0, index).trim();
		return prefix.isBlank() ? null : titleCase(prefix);
	}

	private static List<String> combinedSources(String normalizedName, List<String> tooltipLines) {
		List<String> sources = new ArrayList<>(tooltipLines.size() + 2);
		if (!normalizedName.isBlank()) {
			sources.add(normalizedName);
		}

		sources.addAll(tooltipLines);
		if (!normalizedName.isBlank() && !tooltipLines.isEmpty()) {
			sources.add(normalizedName + " " + String.join(" ", tooltipLines));
		}

		return sources;
	}

	private static String titleCase(String text) {
		String[] parts = text.split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < parts.length; index++) {
			if (parts[index].isBlank()) {
				continue;
			}

			if (!builder.isEmpty()) {
				builder.append(' ');
			}

			builder.append(Character.toUpperCase(parts[index].charAt(0)));
			if (parts[index].length() > 1) {
				builder.append(parts[index].substring(1));
			}
		}

		return builder.toString();
	}

	private static void addNormalized(Set<String> aliases, String text) {
		String normalized = normalize(text);
		if (!normalized.isBlank()) {
			aliases.add(normalized);
		}
	}

	public static String normalize(String text) {
		return Formatting.strip(text).trim().toLowerCase(Locale.ROOT);
	}

	public static String displayTitle(String title) {
		return Formatting.strip(title).trim();
	}

	private static String slug(String text) {
		return normalize(text).replace(' ', '_');
	}
}
