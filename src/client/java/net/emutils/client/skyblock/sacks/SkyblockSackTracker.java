package net.emutils.client.skyblock.sacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.skyblock.SkyblockTextUtils;
import net.emutils.client.skyblock.bazaar.SkyblockItemIds;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class SkyblockSackTracker {
	private static final Pattern SACK_LINE = Pattern.compile("([+-][\\d,.]+[kKmMbB]?x?)\\s+(.+?)\\s+\\(([^)]+)\\)");
	private static final Pattern SACK_INVENTORY_NAME = Pattern.compile("^(?:.* Sack|Enchanted .* Sack)$");
	private static final long SACK_INVENTORY_SUPPRESS_MS = 10_000L;
	private static final List<Consumer<SkyblockSackChangeBatch>> LISTENERS = new CopyOnWriteArrayList<>();

	private static boolean inSackInventory;
	private static long lastSackInventoryCloseMs;

	private SkyblockSackTracker() {
	}

	public static void addListener(Consumer<SkyblockSackChangeBatch> listener) {
		if (!LISTENERS.contains(listener)) {
			LISTENERS.add(listener);
		}
	}

	public static void onInventoryOpen(@Nullable String inventoryName) {
		if (isSackInventoryName(inventoryName)) {
			inSackInventory = true;
			lastSackInventoryCloseMs = 0L;
		}
	}

	public static void onInventoryClose(@Nullable String inventoryName) {
		if (inSackInventory || isSackInventoryName(inventoryName)) {
			inSackInventory = false;
			lastSackInventoryCloseMs = System.currentTimeMillis();
		}
	}

	public static boolean isManualSackInteractionRecent() {
		return inSackInventory
			|| lastSackInventoryCloseMs > 0L
			&& System.currentTimeMillis() - lastSackInventoryCloseMs < SACK_INVENTORY_SUPPRESS_MS;
	}

	public static boolean onChat(Text message) {
		String stripped = SkyblockTextUtils.strip(message);
		if (!stripped.startsWith("[Sacks]")) {
			return false;
		}

		SkyblockSackChangeBatch batch = parse(message);
		if (batch.isEmpty()) {
			return false;
		}

		for (Consumer<SkyblockSackChangeBatch> listener : LISTENERS) {
			listener.accept(batch);
		}
		return true;
	}

	public static SkyblockSackChangeBatch parse(Text message) {
		List<String> addedHoverText = new ArrayList<>();
		List<String> removedHoverText = new ArrayList<>();
		collectMatchingHoverText(message, addedHoverText, removedHoverText);

		String directText = SkyblockTextUtils.strip(message);
		List<String> parseSources = new ArrayList<>();
		parseSources.addAll(addedHoverText);
		parseSources.addAll(removedHoverText);
		if (parseSources.isEmpty() && directText.contains("(") && directText.contains(")")) {
			parseSources.add(directText);
		}

		List<SkyblockSackChange> changes = new ArrayList<>();
		for (String source : parseSources) {
			for (Matcher matcher = SACK_LINE.matcher(source); matcher.find();) {
				int delta = parseAmount(matcher.group(1));
				if (delta == 0) {
					continue;
				}

				String itemName = SkyblockTextUtils.strip(matcher.group(2));
				String itemId = SkyblockItemIds.guessFromDisplayName(itemName);
				if (itemId == null || itemName.isBlank()) {
					continue;
				}

				changes.add(new SkyblockSackChange(
					delta,
					itemId,
					itemName,
					parseSackNames(matcher.group(3))
				));
			}
		}

		return new SkyblockSackChangeBatch(
			List.copyOf(changes),
			containsOtherItems(addedHoverText),
			containsOtherItems(removedHoverText),
			System.currentTimeMillis()
		);
	}

	private static void collectMatchingHoverText(
		Text text,
		List<String> addedHoverText,
		List<String> removedHoverText
	) {
		HoverEvent hover = text.getStyle().getHoverEvent();
		if (hover instanceof HoverEvent.ShowText showText) {
			String value = SkyblockTextUtils.strip(showText.value());
			if (value.startsWith("Added")) {
				addedHoverText.add(value);
			} else if (value.startsWith("Removed")) {
				removedHoverText.add(value);
			}
		}

		for (Text sibling : text.getSiblings()) {
			collectMatchingHoverText(sibling, addedHoverText, removedHoverText);
		}
	}

	private static boolean containsOtherItems(List<String> hoverText) {
		for (String text : hoverText) {
			if (text.toLowerCase(Locale.ROOT).contains("other items")) {
				return true;
			}
		}

		return false;
	}

	private static List<String> parseSackNames(String raw) {
		List<String> names = new ArrayList<>();
		for (String part : raw.split(",")) {
			String name = SkyblockTextUtils.strip(part);
			if (!name.isBlank()) {
				names.add(name);
			}
		}

		return List.copyOf(names);
	}

	private static int parseAmount(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0;
		}

		boolean negative = raw.startsWith("-");
		String digits = raw.replace("+", "")
			.replace("-", "")
			.replace(",", "")
			.replace("x", "")
			.replace("X", "");
		double value;
		try {
			if (digits.endsWith("k") || digits.endsWith("K")) {
				value = Double.parseDouble(digits.substring(0, digits.length() - 1)) * 1_000D;
			} else if (digits.endsWith("m") || digits.endsWith("M")) {
				value = Double.parseDouble(digits.substring(0, digits.length() - 1)) * 1_000_000D;
			} else if (digits.endsWith("b") || digits.endsWith("B")) {
				value = Double.parseDouble(digits.substring(0, digits.length() - 1)) * 1_000_000_000D;
			} else {
				value = Double.parseDouble(digits);
			}
		} catch (NumberFormatException ignored) {
			return 0;
		}

		int amount = (int) Math.min(Integer.MAX_VALUE, Math.round(value));
		return negative ? -amount : amount;
	}

	private static boolean isSackInventoryName(@Nullable String inventoryName) {
		String name = SkyblockTextUtils.strip(inventoryName);
		if (name.isBlank()) {
			return false;
		}

		String lowerName = name.toLowerCase(Locale.ROOT);
		return (lowerName.contains("sack") && !lowerName.contains("rucksack"))
			|| SACK_INVENTORY_NAME.matcher(name).matches();
	}
}
