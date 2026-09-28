package net.emutils.client.emutils.tweaks;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

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
		int bestSlot = findBestSlot(inventory, state, selected, searchEnd, EMUtilsClient.config(), enchantments(client));
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

	/**
	 * The slot with the best tool for {@code state}, or {@code selected} when none beats what's held.
	 * Hotbar slots left out of Hotbar Slots are skipped.
	 */
	static int findBestSlot(Inventory inventory, BlockState state, int selected, int searchEnd, EMUtilsConfig config, HolderLookup.RegistryLookup<Enchantment> enchantments) {
		List<AutoToolEnchantment> order = config.autoToolEnchantmentOrder().stream()
			.filter(enchantment -> config.autoToolEnchantmentEnabled(enchantment) && enchantment.matters(state))
			.toList();
		boolean efficiency = order.contains(AutoToolEnchantment.EFFICIENCY);
		Tool best = Tool.of(inventory.getItem(selected), state, enchantments, efficiency);
		int bestSlot = selected;

		for (int slot = 0; slot < searchEnd; slot++) {
			ItemStack candidate = inventory.getItem(slot);
			if (candidate.isEmpty() || slot == selected || (slot < HOTBAR_SIZE && !config.autoToolHotbarSlot(slot))) {
				continue;
			}

			Tool tool = Tool.of(candidate, state, enchantments, efficiency);
			if (compare(tool, best, state, order, enchantments) > 0) {
				best = tool;
				bestSlot = slot;
			}
		}

		return bestSlot;
	}

	/**
	 * Whether {@code a} is a better tool than {@code b} (positive), a worse one (negative) or as good.
	 * A tool that gets drops beats one that doesn't; then each preferred enchantment in priority order
	 * decides, Efficiency by how fast the tools mine; then the faster tool wins.
	 */
	private static int compare(Tool a, Tool b, BlockState state, List<AutoToolEnchantment> order, HolderLookup.RegistryLookup<Enchantment> enchantments) {
		if (a.harvests() != b.harvests()) {
			return a.harvests() ? 1 : -1;
		}
		for (AutoToolEnchantment enchantment : order) {
			int result = enchantment == AutoToolEnchantment.EFFICIENCY
				? compareSpeed(a.speed(), b.speed())
				: Integer.compare(level(enchantments, enchantment, a.stack()), level(enchantments, enchantment, b.stack()));
			if (result != 0) {
				return result;
			}
		}
		return compareSpeed(a.speed(), b.speed());
	}

	/** The slot Auto Tool would pick for mining {@code state} right now; used by UI snapshots. */
	public static int bestSlotForSnapshot(Minecraft client, BlockState state) {
		Inventory inventory = client.player.getInventory();
		int searchEnd = EMUtilsClient.config().autoToolMode() == AutoToolMode.UNFAIR ? MAIN_INVENTORY_END : HOTBAR_SIZE;
		return findBestSlot(inventory, state, inventory.getSelectedSlot(), searchEnd, EMUtilsClient.config(), enchantments(client));
	}

	private static int compareSpeed(float a, float b) {
		return Math.abs(a - b) <= EPSILON ? 0 : Float.compare(a, b);
	}

	/** An item and how well it mines a block: whether it gets drops, and its mining speed. */
	private record Tool(ItemStack stack, boolean harvests, float speed) {
		static Tool of(ItemStack stack, BlockState state, HolderLookup.RegistryLookup<Enchantment> enchantments, boolean efficiency) {
			boolean harvests = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state);
			float speed = stack.getDestroySpeed(state);
			// Like vanilla, Efficiency only speeds up a tool that's already faster than a bare hand.
			int efficiencyLevel = efficiency ? level(enchantments, AutoToolEnchantment.EFFICIENCY, stack) : 0;
			if (speed > 1.0F && efficiencyLevel > 0) {
				speed += efficiencyLevel * efficiencyLevel + 1;
			}
			return new Tool(stack, harvests, speed);
		}
	}

	private static int level(HolderLookup.RegistryLookup<Enchantment> enchantments, AutoToolEnchantment enchantment, ItemStack stack) {
		@Nullable Holder<Enchantment> holder = enchantments.get(enchantment.key()).orElse(null);
		return holder == null || stack.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
	}

	private static HolderLookup.RegistryLookup<Enchantment> enchantments(Minecraft client) {
		return client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
	}
}
