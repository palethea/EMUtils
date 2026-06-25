package net.emutils.client.emutils.inventory;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public final class AutoRefillManager {
	private static final int HOTBAR_SIZE = 9;
	private static final int PLAYER_INVENTORY_SIZE = 36;

	private ItemStack trackedStack = ItemStack.EMPTY;
	private int trackedSlot = -1;

	public void tick(Minecraft client) {
		if (!canRefill(client)) {
			clear();
			return;
		}

		Player player = client.player;
		Inventory inventory = player.getInventory();
		int selected = inventory.getSelectedSlot();
		ItemStack selectedStack = inventory.getItem(selected);

		if (selected != trackedSlot) {
			clear();
		}

		if (isPlaceIntent(client) && isRefillableBlockStack(selectedStack)) {
			trackedStack = selectedStack.copy();
			trackedSlot = selected;
			return;
		}

		if (trackedSlot == selected && selectedStack.isEmpty() && isRefillableBlockStack(trackedStack)) {
			int sourceSlot = findMatchingStack(inventory, trackedStack, selected);
			if (sourceSlot >= 0) {
				client.gameMode.handleContainerInput(
					player.inventoryMenu.containerId,
					inventoryToMenuSlot(sourceSlot),
					selected,
					ContainerInput.SWAP,
					player
				);
			}
			clear();
		}
	}

	public void reset() {
		clear();
	}

	private static boolean canRefill(Minecraft client) {
		return EMUtilsClient.config() != null
			&& EMUtilsClient.config().inventoryToolsEnabled()
			&& EMUtilsClient.config().autoRefillEnabled()
			&& client.player != null
			&& client.level != null
			&& client.gameMode != null
			&& client.gui.screen() == null
			&& client.player.containerMenu == client.player.inventoryMenu
			&& client.player.inventoryMenu.getCarried().isEmpty();
	}

	private static boolean isPlaceIntent(Minecraft client) {
		return client.options.keyUse.isDown() && client.hitResult instanceof BlockHitResult;
	}

	private static boolean isRefillableBlockStack(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
	}

	private static int findMatchingStack(Inventory inventory, ItemStack targetStack, int selectedSlot) {
		for (int slot = 0; slot < PLAYER_INVENTORY_SIZE; slot++) {
			if (slot == selectedSlot || EMUtilsClient.inventoryTools().isPlayerSlotLocked(slot)) {
				continue;
			}

			ItemStack candidate = inventory.getItem(slot);
			if (isRefillableBlockStack(candidate) && ItemStack.isSameItemSameComponents(candidate, targetStack)) {
				return slot;
			}
		}
		return -1;
	}

	private static int inventoryToMenuSlot(int inventorySlot) {
		return inventorySlot < HOTBAR_SIZE ? 36 + inventorySlot : inventorySlot;
	}

	private void clear() {
		trackedStack = ItemStack.EMPTY;
		trackedSlot = -1;
	}
}
