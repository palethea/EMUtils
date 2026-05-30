package net.emutils.client.emskyblock.features.inventory.storagepreview;

import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jspecify.annotations.Nullable;

public final class StoragePreviewFilters {
	private static final Set<String> BLOCKED_HOVER_NAMES = Set.of(
		"back",
		"next",
		"previous",
		"close",
		"go back",
		"menu",
		"ender chest",
		"backpacks",
		"locked page"
	);

	private static final Pattern PREVIEW_HOVER_ENDER = Pattern.compile("^ender chest page \\d+$");
	private static final Pattern PREVIEW_HOVER_BACKPACK = Pattern.compile("^backpack slot \\d+$");

	private static final Pattern CAPTURE_ENDER = Pattern.compile("^ender chest \\(\\d+/\\d+\\)$");
	private static final Pattern CAPTURE_BACKPACK = Pattern.compile("^(?:\\w+(?: \\w+)* )?backpack \\(slot #\\d+\\)$");
	private static final Pattern CAPTURE_RUCKSACK = Pattern.compile("^(?:\\w+(?: \\w+)* )?rucksack \\(slot #\\d+\\)$");

	private StoragePreviewFilters() {
	}

	public static boolean isStorageMenuScreen(@Nullable Screen screen) {
		if (!(screen instanceof HandledScreen<?> handledScreen)) {
			return false;
		}

		return "storage".equals(StoragePreviewKeys.normalize(handledScreen.getTitle().getString()));
	}

	public static boolean isPreviewableHover(String normalizedName) {
		return PREVIEW_HOVER_ENDER.matcher(normalizedName).matches()
			|| PREVIEW_HOVER_BACKPACK.matcher(normalizedName).matches();
	}

	public static boolean isSackName(String normalizedName) {
		return normalizedName.contains("sack")
			&& !normalizedName.contains("backpack")
			&& !normalizedName.contains("rucksack");
	}

	public static boolean isSackRecord(StoragePreviewRecord record) {
		return isSackName(StoragePreviewKeys.normalize(record.title()));
	}

	public static boolean isValidRecord(StoragePreviewRecord record) {
		if (isSackRecord(record)) {
			return false;
		}

		String id = record.id();
		return id != null
			&& (id.startsWith("ender_chest:") || id.startsWith("backpack:"))
			&& isStorableTitle(record.title());
	}

	public static boolean isBlockedHover(ItemStack stack, String normalizedName) {
		if (normalizedName.isBlank()) {
			return true;
		}

		if (BLOCKED_HOVER_NAMES.contains(normalizedName)) {
			return true;
		}

		if (isSackName(normalizedName)) {
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
		if (normalized.isBlank() || normalized.equals("storage") || isSackName(normalized)) {
			return false;
		}

		return CAPTURE_ENDER.matcher(normalized).matches()
			|| CAPTURE_BACKPACK.matcher(normalized).matches()
			|| CAPTURE_RUCKSACK.matcher(normalized).matches();
	}
}
