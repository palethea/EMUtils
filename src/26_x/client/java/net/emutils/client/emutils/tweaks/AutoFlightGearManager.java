package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

/** Handles the temporary inventory swaps used by the automatic flight gear tweaks. */
public final class AutoFlightGearManager {
	private static final int HOTBAR_SIZE = 9;
	private static final int PLAYER_INVENTORY_SIZE = 36;
	private static final int CHEST_EQUIPMENT_MENU_SLOT = 6;
	private static final double FALLING_VELOCITY = -0.08D;
	private static final int DOUBLE_JUMP_WINDOW_TICKS = 7;
	private static final int WATER_DOUBLE_JUMP_LAUNCH_TICKS = 20;

	private int elytraSourceMenuSlot = -1;
	private int rocketSourceMenuSlot = -1;
	private int rocketTargetMenuSlot = -1;
	private int delayedDoubleJumpLaunchTicks;
	private boolean fallCycleActive;
	private boolean jumpWasDown;
	private int jumpTapTicks;

	public void tick(Minecraft client) {
		if (client.player == null || client.level == null || client.gameMode == null) {
			reset();
			return;
		}

		Player player = client.player;
		boolean canSwap = net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) == null
			&& player.containerMenu == player.inventoryMenu
			&& player.inventoryMenu.getCarried().isEmpty();
		boolean grounded = player.onGround();
		boolean doubleJump = detectDoubleJump(client, player);
		boolean falling = !grounded
			&& !player.isPassenger()
			&& player.getDeltaMovement().y < FALLING_VELOCITY
			&& !EMUtilsClient.config().autoFlightDoubleJump()
			&& (!EMUtilsClient.config().autoFlightIgnoreShortFalls() || !hasSelectableGroundBelow(player));

		if (fallCycleActive && delayedDoubleJumpLaunchTicks > 0) {
			if (!canSwap || !anyFeatureEnabled()) {
				return;
			}
			equipElytra(client, player);
			if (EMUtilsClient.config().tweakAutoSwitchRockets()) {
				equipRockets(client, player);
			}
			if (tryStartFallFlying(client, player)) {
				delayedDoubleJumpLaunchTicks = 0;
			} else if (!player.isInWater() || --delayedDoubleJumpLaunchTicks <= 0) {
				restoreRockets(client, player);
				restoreElytra(client, player);
				fallCycleActive = false;
				delayedDoubleJumpLaunchTicks = 0;
			}
			return;
		}

		if (fallCycleActive && canSwap) {
			if (!EMUtilsClient.config().tweakAutoSwitchRockets()) {
				restoreRockets(client, player);
			}
			if (!EMUtilsClient.config().tweakAutoSwitchElytra()
				&& !EMUtilsClient.config().autoFlightDoubleJump()) {
				restoreElytra(client, player);
			}
		}

		if (fallCycleActive && (grounded || !anyFeatureEnabled())) {
			if (canSwap) {
				restoreRockets(client, player);
				restoreElytra(client, player);
				fallCycleActive = false;
				delayedDoubleJumpLaunchTicks = 0;
			}
			return;
		}

		if ((!falling && !doubleJump) || !canSwap || !anyFeatureEnabled()) {
			return;
		}

		fallCycleActive = true;
		if (EMUtilsClient.config().tweakAutoSwitchElytra() || doubleJump) {
			equipElytra(client, player);
		}
		if (EMUtilsClient.config().tweakAutoSwitchRockets()) {
			equipRockets(client, player);
		}
		if (doubleJump) {
			if (tryStartFallFlying(client, player)) {
				delayedDoubleJumpLaunchTicks = 0;
			} else if (player.isInWater()) {
				delayedDoubleJumpLaunchTicks = WATER_DOUBLE_JUMP_LAUNCH_TICKS;
			} else {
				restoreRockets(client, player);
				restoreElytra(client, player);
				fallCycleActive = false;
				delayedDoubleJumpLaunchTicks = 0;
			}
		}
	}

	public void reset() {
		elytraSourceMenuSlot = -1;
		rocketSourceMenuSlot = -1;
		rocketTargetMenuSlot = -1;
		delayedDoubleJumpLaunchTicks = 0;
		fallCycleActive = false;
		jumpWasDown = false;
		jumpTapTicks = 0;
	}

	private static boolean anyFeatureEnabled() {
		return EMUtilsClient.config() != null
			&& EMUtilsClient.config().autoFlightGearEnabled()
			&& (EMUtilsClient.config().tweakAutoSwitchElytra()
				|| EMUtilsClient.config().tweakAutoSwitchRockets()
				|| EMUtilsClient.config().autoFlightDoubleJump());
	}

	private static boolean hasSelectableGroundBelow(Player player) {
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(0.0D, -player.blockInteractionRange(), 0.0D);
		return player.level().clip(new ClipContext(
			start,
			end,
			ClipContext.Block.OUTLINE,
			ClipContext.Fluid.NONE,
			player
		)).getType() != HitResult.Type.MISS;
	}

	private boolean detectDoubleJump(Minecraft client, Player player) {
		boolean jumpDown = client.options.keyJump.isDown();
		boolean pressed = jumpDown && !jumpWasDown;
		jumpWasDown = jumpDown;

		if (!EMUtilsClient.config().autoFlightGearEnabled()
			|| !EMUtilsClient.config().autoFlightDoubleJump()
			|| net.emutils.client.emutils.compat.MinecraftClientCompat.screen(client) != null) {
			jumpTapTicks = 0;
			return false;
		}

		if (pressed) {
			if (jumpTapTicks > 0
				&& !player.onGround()
				&& !player.isPassenger()
				&& !player.getAbilities().mayfly) {
				jumpTapTicks = 0;
				return true;
			}
			jumpTapTicks = DOUBLE_JUMP_WINDOW_TICKS;
		} else if (jumpTapTicks > 0) {
			jumpTapTicks--;
		}
		return false;
	}

	private static boolean tryStartFallFlying(Minecraft client, Player player) {
		if (!player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
			|| !player.tryToStartFallFlying()) {
			return false;
		}

		client.getConnection().send(new ServerboundPlayerCommandPacket(
			player,
			ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
		));
		return true;
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
