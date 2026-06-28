package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AutoToolManager {
	private static final int HOTBAR_SIZE = 9;
	private static final int MAIN_INVENTORY_END = 36;
	private static final float EPSILON = 0.0001F;

	private int restoreHotbarSlot = -1;
	private int restoreInventorySlot = -1;

	public void tick(Minecraft client) {
		if (EMUtilsClient.config() == null
			|| client.player == null
			|| client.level == null
			|| client.gameMode == null) {
			clearRestore();
			return;
		}

		if (!EMUtilsClient.config().autoToolEnabled()
			|| net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) != null
			|| !client.options.keyAttack.isDown()
			|| !(client.hitResult instanceof BlockHitResult blockHit)) {
			restorePreviousItem(client);
			return;
		}

		BlockState state = client.level.getBlockState(blockHit.getBlockPos());
		if (state.isAir()) {
			restorePreviousItem(client);
			return;
		}

		Player player = client.player;
		Inventory inventory = player.getInventory();
		int selected = inventory.getSelectedSlot();
		int searchEnd = EMUtilsClient.config().autoToolMode() == AutoToolMode.UNFAIR
			? MAIN_INVENTORY_END
			: HOTBAR_SIZE;
		int bestSlot = findBestSlot(inventory, state, selected, searchEnd);
		if (bestSlot < 0 || bestSlot == selected) {
			return;
		}

		rememberPreviousItem(selected, bestSlot);
		if (bestSlot < HOTBAR_SIZE) {
			inventory.setSelectedSlot(bestSlot);
			return;
		}

		// Player inventory menu slots 9-35 map directly to inventory indexes 9-35.
		client.gameMode.handleContainerInput(
			player.inventoryMenu.containerId,
			bestSlot,
			selected,
			ContainerInput.SWAP,
			player
		);
	}

	private void rememberPreviousItem(int selected, int bestSlot) {
		if (!EMUtilsClient.config().autoToolReturnToPreviousItem() || restoreHotbarSlot >= 0) {
			return;
		}

		restoreHotbarSlot = selected;
		if (bestSlot >= HOTBAR_SIZE) {
			restoreInventorySlot = bestSlot;
		}
	}

	private void restorePreviousItem(Minecraft client) {
		if (restoreHotbarSlot < 0 || client.player == null || client.gameMode == null) {
			clearRestore();
			return;
		}

		Player player = client.player;
		Inventory inventory = player.getInventory();
		if (restoreInventorySlot >= HOTBAR_SIZE && restoreInventorySlot < MAIN_INVENTORY_END) {
			client.gameMode.handleContainerInput(
				player.inventoryMenu.containerId,
				restoreInventorySlot,
				restoreHotbarSlot,
				ContainerInput.SWAP,
				player
			);
		}
		inventory.setSelectedSlot(restoreHotbarSlot);
		clearRestore();
	}

	private void clearRestore() {
		restoreHotbarSlot = -1;
		restoreInventorySlot = -1;
	}

	private static int findBestSlot(Inventory inventory, BlockState state, int selected, int searchEnd) {
		ItemStack current = inventory.getItem(selected);
		float bestScore = miningScore(current, state);
		float bestSpeed = current.getDestroySpeed(state);
		int bestSlot = selected;

		for (int slot = 0; slot < searchEnd; slot++) {
			ItemStack candidate = inventory.getItem(slot);
			if (candidate.isEmpty()) {
				continue;
			}

			float score = miningScore(candidate, state);
			float speed = candidate.getDestroySpeed(state);
			if (score > bestScore + EPSILON
				|| (Math.abs(score - bestScore) <= EPSILON && speed > bestSpeed + EPSILON)) {
				bestScore = score;
				bestSpeed = speed;
				bestSlot = slot;
			}
		}

		return bestSlot;
	}

	private static float miningScore(ItemStack stack, BlockState state) {
		float speed = stack.getDestroySpeed(state);
		// Vanilla divides block damage by 30 with the correct tool and by 100 otherwise.
		return speed * (stack.isCorrectToolForDrops(state) ? 100.0F : 30.0F);
	}
}
