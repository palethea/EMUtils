package net.emutils.client.emutils.inventory;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public final class MassDropManager {
	private static final int INVENTORY_SIZE = 36;
	private final MassDropStore store = MassDropStore.load();

	public MassDropStore store() {
		return store;
	}

	public void dropSelected(Minecraft client) {
		if (client.player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		Set<String> selected = store.itemIds();
		if (selected.isEmpty()) {
			return;
		}

		Player player = client.player;
		Inventory inventory = player.getInventory();
		for (int inventorySlot = 0; inventorySlot < INVENTORY_SIZE; inventorySlot++) {
			ItemStack stack = inventory.getItem(inventorySlot);
			if (stack.isEmpty() || !selected.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
				continue;
			}
			int menuSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
			client.gameMode.handleContainerInput(player.inventoryMenu.containerId, menuSlot, 1, ContainerInput.THROW, player);
			if (store.mode() == MassDropMode.LEGIT) {
				return;
			}
		}
	}
}
