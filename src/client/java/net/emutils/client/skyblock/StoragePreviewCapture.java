package net.emutils.client.skyblock;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import org.jspecify.annotations.Nullable;

public final class StoragePreviewCapture {
	public static final int COLUMNS = 9;
	public static final int MAX_ROWS = 5;

	private StoragePreviewCapture() {
	}

	@Nullable
	public static StoragePreviewRecord capture(ScreenHandler handler, PlayerInventory playerInventory, String title) {
		if (!StoragePreviewFilters.isStorableTitle(title)) {
			return null;
		}

		List<Slot> containerSlots = new ArrayList<>();
		for (Slot slot : handler.slots) {
			if (slot.inventory instanceof PlayerInventory) {
				continue;
			}

			containerSlots.add(slot);
		}

		containerSlots.sort(Comparator.<Slot>comparingInt(slot -> slot.y).thenComparingInt(slot -> slot.x));
		if (containerSlots.size() < COLUMNS * 2) {
			return null;
		}

		TreeMap<Integer, List<Slot>> rowsByY = new TreeMap<>();
		for (Slot slot : containerSlots) {
			rowsByY.computeIfAbsent(slot.y, ignored -> new ArrayList<>()).add(slot);
		}

		if (rowsByY.size() < 2) {
			return null;
		}

		List<Integer> rowYs = new ArrayList<>(rowsByY.keySet());
		int contentRows = Math.min(MAX_ROWS, rowYs.size() - 1);
		if (contentRows <= 0) {
			return null;
		}

		DefaultedList<ItemStack> contents = DefaultedList.ofSize(contentRows * COLUMNS, ItemStack.EMPTY);
		for (int rowIndex = 0; rowIndex < contentRows; rowIndex++) {
			List<Slot> rowSlots = new ArrayList<>(rowsByY.get(rowYs.get(rowIndex + 1)));
			rowSlots.sort(Comparator.comparingInt((Slot slot) -> slot.x));
			for (int column = 0; column < Math.min(COLUMNS, rowSlots.size()); column++) {
				ItemStack stack = rowSlots.get(column).getStack();
				if (!stack.isEmpty()) {
					contents.set(rowIndex * COLUMNS + column, stack.copy());
				}
			}
		}

		String displayTitle = StoragePreviewKeys.displayTitle(title);
		String id = StoragePreviewKeys.idFromTitle(displayTitle);
		if (id == null) {
			return null;
		}

		List<String> aliases = StoragePreviewKeys.aliasesFromTitle(displayTitle);
		List<JsonElement> stacks = new ArrayList<>(contents.size());
		for (ItemStack stack : contents) {
			JsonElement json = ItemStackSerializer.toJson(stack);
			stacks.add(json != null ? json : JsonNull.INSTANCE);
		}

		return new StoragePreviewRecord(id, displayTitle, aliases, contentRows, stacks);
	}
}
