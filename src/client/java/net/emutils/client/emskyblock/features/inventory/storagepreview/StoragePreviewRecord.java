package net.emutils.client.emskyblock.features.inventory.storagepreview;

import com.google.gson.JsonElement;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.emutils.client.emskyblock.util.ItemStackSerializer;

public record StoragePreviewRecord(
	String id,
	String title,
	List<String> aliases,
	int rows,
	List<JsonElement> stacks
) {
	public DefaultedList<ItemStack> resolveContents() {
		int size = rows * StoragePreviewCapture.COLUMNS;
		DefaultedList<ItemStack> contents = DefaultedList.ofSize(size, ItemStack.EMPTY);
		for (int index = 0; index < Math.min(size, stacks.size()); index++) {
			contents.set(index, ItemStackSerializer.fromJson(stacks.get(index)));
		}

		return contents;
	}
}
