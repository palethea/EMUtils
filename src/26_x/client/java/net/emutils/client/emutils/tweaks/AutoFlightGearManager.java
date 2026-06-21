package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

/** Handles the temporary inventory swaps used by the automatic flight gear tweaks. */
public final class AutoFlightGearManager {
	private static final int HOTBAR_SIZE = 9;
	private static final int PLAYER_INVENTORY_SIZE = 36;
	private static final int CHEST_EQUIPMENT_MENU_SLOT = 6;
	private static final double FALLING_VELOCITY = -0.08D;

	private int elytraSourceMenuSlot = -1;
	private int rocketSourceMenuSlot = -1;
	private int rocketTargetMenuSlot = -1;
	private boolean fallCycleActive;

	public void tick(Minecraft client) {
		if (client.player == null || client.level == null || client.gameMode == null) {
			reset();
			return;
		}

		Player player = client.player;
		boolean canSwap = client.gui.screen() == null
			&& player.containerMenu == player.inventoryMenu
			&& player.inventoryMenu.getCarried().isEmpty();
		boolean grounded = player.onGround();
		boolean falling = !grounded
			&& !player.isPassenger()
			&& player.getDeltaMovement().y < FALLING_VELOCITY;

		if (fallCycleActive && canSwap) {
			if (!EMUtilsClient.config().tweakAutoSwitchRockets()) {
				restoreRockets(client, player);
			}
			if (!EMUtilsClient.config().tweakAutoSwitchElytra()) {
				restoreElytra(client, player);
			}
		}

		if (fallCycleActive && (grounded || !anyFeatureEnabled())) {
			if (canSwap) {
				restoreRockets(client, player);
				restoreElytra(client, player);
				fallCycleActive = false;
			}
			return;
		}

		if (!falling || !canSwap || !anyFeatureEnabled()) {
			return;
		}

		fallCycleActive = true;
		if (EMUtilsClient.config().tweakAutoSwitchElytra()) {
			equipElytra(client, player);
		}
		if (EMUtilsClient.config().tweakAutoSwitchRockets()) {
			equipRockets(client, player);
		}
	}

	public void reset() {
		elytraSourceMenuSlot = -1;
		rocketSourceMenuSlot = -1;
		rocketTargetMenuSlot = -1;
		fallCycleActive = false;
	}

	private static boolean anyFeatureEnabled() {
		return EMUtilsClient.config() != null
			&& (EMUtilsClient.config().tweakAutoSwitchElytra()
				|| EMUtilsClient.config().tweakAutoSwitchRockets());
	}

	private void equipElytra(Minecraft client, Player player) {
		if (elytraSourceMenuSlot >= 0 || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
			return;
		}

		int inventorySlot = findItem(player.getInventory(), Items.ELYTRA, -1);
		if (inventorySlot < 0) {
			return;
		}

		int sourceMenuSlot = inventoryToMenuSlot(inventorySlot);
		swapMenuSlots(client, player, sourceMenuSlot, CHEST_EQUIPMENT_MENU_SLOT);
		elytraSourceMenuSlot = sourceMenuSlot;
	}

	private void equipRockets(Minecraft client, Player player) {
		if (rocketSourceMenuSlot >= 0) {
			return;
		}

		int targetInventorySlot = EMUtilsClient.config().autoSwitchRocketsHotbarSlot() - 1;
		if (player.getInventory().getItem(targetInventorySlot).is(Items.FIREWORK_ROCKET)) {
			return;
		}

		int sourceInventorySlot = findItem(player.getInventory(), Items.FIREWORK_ROCKET, targetInventorySlot);
		if (sourceInventorySlot < 0) {
			return;
		}

		int sourceMenuSlot = inventoryToMenuSlot(sourceInventorySlot);
		int targetMenuSlot = inventoryToMenuSlot(targetInventorySlot);
		swapMenuSlots(client, player, sourceMenuSlot, targetMenuSlot);
		rocketSourceMenuSlot = sourceMenuSlot;
		rocketTargetMenuSlot = targetMenuSlot;
	}

	private void restoreElytra(Minecraft client, Player player) {
		if (elytraSourceMenuSlot < 0) {
			return;
		}

		if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
			swapMenuSlots(client, player, elytraSourceMenuSlot, CHEST_EQUIPMENT_MENU_SLOT);
		}
		elytraSourceMenuSlot = -1;
	}

	private void restoreRockets(Minecraft client, Player player) {
		if (rocketSourceMenuSlot < 0) {
			return;
		}

		if (rocketTargetMenuSlot >= 36
			&& player.getInventory().getItem(rocketTargetMenuSlot - 36).is(Items.FIREWORK_ROCKET)) {
			swapMenuSlots(client, player, rocketSourceMenuSlot, rocketTargetMenuSlot);
		}
		rocketSourceMenuSlot = -1;
		rocketTargetMenuSlot = -1;
	}

	private static int findItem(Inventory inventory, net.minecraft.world.item.Item item, int excludedSlot) {
		for (int slot = 0; slot < PLAYER_INVENTORY_SIZE; slot++) {
			if (slot != excludedSlot && inventory.getItem(slot).is(item)) {
				return slot;
			}
		}
		return -1;
	}

	private static int inventoryToMenuSlot(int inventorySlot) {
		return inventorySlot < HOTBAR_SIZE ? 36 + inventorySlot : inventorySlot;
	}

	private static void swapMenuSlots(Minecraft client, Player player, int firstSlot, int secondSlot) {
		int containerId = player.inventoryMenu.containerId;
		client.gameMode.handleContainerInput(containerId, firstSlot, 0, ContainerInput.PICKUP, player);
		client.gameMode.handleContainerInput(containerId, secondSlot, 0, ContainerInput.PICKUP, player);
		client.gameMode.handleContainerInput(containerId, firstSlot, 0, ContainerInput.PICKUP, player);
	}
}
