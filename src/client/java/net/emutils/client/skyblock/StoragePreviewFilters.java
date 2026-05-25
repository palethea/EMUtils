package net.emutils.client.skyblock;

import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class StoragePreviewFilters {
	private static final Set<String> BLOCKED_HOVER_NAMES = Set.of(
		"back",
		"next",
		"previous",
		"close",
		"go back",
		"menu"
	);

	private StoragePreviewFilters() {
	}

	public static boolean isBlockedHover(ItemStack stack, String normalizedName) {
		if (normalizedName.isBlank()) {
			return true;
		}

		if (BLOCKED_HOVER_NAMES.contains(normalizedName)) {
			return true;
		}

		if (normalizedName.startsWith("empty backpack slot") || normalizedName.startsWith("locked backpack slot")) {
			return true;
		}

		if (stack.isOf(Items.ARROW) || stack.isOf(Items.BARRIER)) {
			return true;
		}

		return normalizedName.length() <= 4 && !normalizedName.chars().anyMatch(Character::isDigit);
	}

	public static boolean isStorableTitle(String title) {
		String normalized = StoragePreviewKeys.normalize(title);
		if (normalized.isBlank() || normalized.equals("storage")) {
			return false;
		}

		return normalized.contains("ender chest")
			|| normalized.contains("backpack")
			|| normalized.contains("rucksack")
			|| normalized.contains("sack");
	}
}
