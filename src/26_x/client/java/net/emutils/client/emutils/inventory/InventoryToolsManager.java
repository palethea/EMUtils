package net.emutils.client.emutils.inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emhelpers.client.accessor.KeyBindingAccess;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.mixin.CreativeSlotAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class InventoryToolsManager {
	private static final int SLOT_SIZE = 16;
	private static final int LINE_COLOR = 0xFF00E5FF;
	private static final int BORDER_ALPHA_MASK = 0xFF000000;

	private final Set<InventorySlotRef> lockedSlots = new HashSet<>();
	private final Map<InventorySlotRef, InventorySlotRef> boundSlots = new HashMap<>();
	@Nullable
	private String activeScopeKey;
	@Nullable
	private KeyMapping slotLockKey;
	@Nullable
	private KeyMapping slotBindKey;
	@Nullable
	private DragState dragState;
	private boolean slotLockKeyConsumed;
	private final InventoryCursorManager cursorManager = new InventoryCursorManager();

	public InventoryCursorManager cursor() {
		return cursorManager;
	}

	public void setKeyMappings(KeyMapping slotLockKey, KeyMapping slotBindKey) {
		this.slotLockKey = slotLockKey;
		this.slotBindKey = slotBindKey;
	}

	public void onWorldJoin(Minecraft client) {
		onWorldLeave(client);
		activeScopeKey = InventoryToolsStore.scopeKey(client);
		if (activeScopeKey == null) {
			return;
		}

		InventoryToolsStore.applyScope(InventoryToolsStore.readScope(activeScopeKey), lockedSlots, boundSlots);
	}

	public void onWorldLeave(Minecraft client) {
		persist();
		clearSessionState();
	}

	public void tick(Minecraft client) {
		cursorManager.tick(client);

		if (slotLockKey == null || isLockKeyDown(client)) {
			return;
		}

		slotLockKeyConsumed = false;
	}

	public boolean isPlayerSlotLocked(int inventoryIndex) {
		if (!lockingAvailable()) {
			return false;
		}

		return isLocked(InventorySlotRef.forPlayerIndex(inventoryIndex));
	}

	public boolean handleKeyPressed(AbstractContainerMenu handler, @Nullable Slot focusedSlot, KeyEvent input, Inventory playerInventory) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.inventoryToolsEnabled() || focusedSlot == null) {
			return false;
		}

		if (slotLockKey != null && slotLockKey.matches(input) && config.slotLockingEnabled()) {
			if (slotLockKeyConsumed) {
				return true;
			}

			slotLockKeyConsumed = true;
			toggleLock(InventorySlotRef.from(handler, focusedSlot, playerInventory));
			return true;
		}

		if (slotBindKey != null && slotBindKey.matches(input) && config.slotBindingEnabled()) {
			InventorySlotRef focusedRef = InventorySlotRef.from(handler, focusedSlot, playerInventory);
			if (isBound(focusedRef)) {
				unbind(focusedRef);
				dragState = null;
			} else if (dragState == null) {
				dragState = new DragState(focusedRef, focusedSlot.x + SLOT_SIZE / 2, focusedSlot.y + SLOT_SIZE / 2);
			}
			return true;
		}

		return false;
	}

	public boolean handleKeyReleased(AbstractContainerMenu handler, @Nullable Slot focusedSlot, KeyEvent input, Inventory playerInventory) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.inventoryToolsEnabled() || slotBindKey == null || !slotBindKey.matches(input) || dragState == null) {
			return false;
		}

		if (config.slotBindingEnabled() && focusedSlot != null) {
			InventorySlotRef end = InventorySlotRef.from(handler, focusedSlot, playerInventory);
			bind(dragState.start(), end);
		}
		dragState = null;
		return true;
	}

	public void clearDrag() {
		dragState = null;
	}

	public void finishDragIfBindKeyReleased(
		Minecraft client,
		AbstractContainerMenu handler,
		@Nullable Slot focusedSlot,
		Inventory playerInventory
	) {
		if (dragState == null || isBindKeyDown(client)) {
			return;
		}

		EMUtilsConfig config = EMUtilsClient.config();
		if (config != null && config.inventoryToolsEnabled() && config.slotBindingEnabled() && focusedSlot != null) {
			bind(dragState.start(), InventorySlotRef.from(handler, focusedSlot, playerInventory));
		}
		dragState = null;
	}

	public Optional<BoundSwap> boundSwap(
		Minecraft client,
		AbstractContainerMenu handler,
		@Nullable Slot clickedSlot,
		ContainerInput actionType,
		Inventory playerInventory
	) {
		if (!slotBindingAvailable() || actionType != ContainerInput.QUICK_MOVE || clickedSlot == null || client.player == null) {
			return Optional.empty();
		}

		InventorySlotRef clicked = InventorySlotRef.from(handler, clickedSlot, playerInventory);
		InventorySlotRef other = boundSlots.get(clicked);
		if (other == null) {
			return Optional.empty();
		}

		InventorySlotRef hotbar = clicked.isHotbar() ? clicked : other.isHotbar() ? other : null;
		InventorySlotRef target = clicked.isHotbar() ? other : clicked;
		if (hotbar == null || target.isHotbar()) {
			return Optional.empty();
		}

		Slot targetSlot = resolveBoundTargetSlot(client, handler, clicked, target, clickedSlot, playerInventory);
		if (targetSlot == null) {
			return Optional.empty();
		}

		return Optional.of(new BoundSwap(handler.slots.indexOf(targetSlot), hotbar.hotbarButton()));
	}

	public boolean guardSlotMouseButtonEvent(
		Minecraft client,
		AbstractContainerMenu handler,
		@Nullable Slot slot,
		int button,
		ContainerInput actionType,
		ItemStack cursorStack,
		Inventory playerInventory
	) {
		if (client == null || client.gameMode == null || client.player == null || playerInventory == null) {
			return false;
		}

		Optional<BoundSwap> boundSwap = boundSwap(client, handler, slot, actionType, playerInventory);
		if (boundSwap.isPresent()) {
			executeBoundSwap(client, handler, boundSwap.get());
			return true;
		}

		return shouldBlockMouseButtonEvent(handler, slot, button, actionType, cursorStack, playerInventory);
	}

	private void executeBoundSwap(Minecraft client, AbstractContainerMenu handler, BoundSwap swap) {
		Player player = client.player;
		if (player == null) {
			return;
		}

		if (client.screen instanceof CreativeModeInventoryScreen) {
			player.containerMenu.clicked(swap.slotId(), swap.hotbarButton(), ContainerInput.SWAP, player);
			player.containerMenu.broadcastChanges();
			return;
		}

		client.gameMode.handleContainerInput(handler.containerId, swap.slotId(), swap.hotbarButton(), ContainerInput.SWAP, player);
	}

	@Nullable
	private Slot resolveBoundTargetSlot(
		Minecraft client,
		AbstractContainerMenu handler,
		InventorySlotRef clicked,
		InventorySlotRef target,
		@Nullable Slot clickedSlot,
		Inventory playerInventory
	) {
		if (client.screen instanceof CreativeModeInventoryScreen) {
			if (clicked.isHotbar()) {
				return findPlayerHandlerSlot(client.player, target);
			}

			return unwrapPlayerHandlerSlot(clickedSlot, client.player);
		}

		if (clicked.isHotbar()) {
			return findSlot(handler, target, playerInventory);
		}

		return clickedSlot;
	}

	@Nullable
	private static Slot findPlayerHandlerSlot(Player player, InventorySlotRef ref) {
		for (Slot slot : player.containerMenu.slots) {
			if (slot.container == player.getInventory() && slot.getContainerSlot() == ref.inventoryIndex()) {
				return slot;
			}
		}

		return null;
	}

	@Nullable
	private static Slot unwrapPlayerHandlerSlot(@Nullable Slot slot, Player player) {
		if (slot instanceof CreativeSlotAccess creativeSlot) {
			slot = creativeSlot.emutils$backingSlot();
		}

		if (slot == null || slot.container != player.getInventory()) {
			return null;
		}

		return findPlayerHandlerSlot(player, InventorySlotRef.forPlayerIndex(slot.getContainerSlot()));
	}

	public boolean shouldBlockMouseButtonEvent(
		AbstractContainerMenu handler,
		@Nullable Slot slot,
		int button,
		ContainerInput actionType,
		ItemStack cursorStack,
		Inventory playerInventory
	) {
		if (!lockingAvailable()) {
			return false;
		}

		if (actionType == ContainerInput.SWAP) {
			if (button >= 0 && button < Inventory.SELECTION_SIZE && isLocked(InventorySlotRef.forPlayerIndex(button))) {
				return true;
			}
			if (button == 40 && isLocked(InventorySlotRef.forPlayerIndex(40))) {
				return true;
			}
		}

		if (slot == null) {
			return false;
		}

		InventorySlotRef ref = InventorySlotRef.from(handler, slot, playerInventory);
		if (!isLocked(ref)) {
			return actionType == ContainerInput.PICKUP_ALL && hasAnyLockedSlots();
		}

		return switch (actionType) {
			case QUICK_MOVE, THROW, SWAP, PICKUP_ALL, CLONE -> true;
			case PICKUP -> blocksPickup(slot, cursorStack);
			case QUICK_CRAFT -> slot.hasItem() && !cursorStack.isEmpty() && !ItemStack.isSameItemSameComponents(slot.getItem(), cursorStack);
		};
	}

	public boolean handleMouseReleased(AbstractContainerMenu handler, @Nullable Slot slot, Inventory playerInventory) {
		if (dragState == null) {
			return false;
		}

		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.inventoryToolsEnabled() || !config.slotBindingEnabled()) {
			dragState = null;
			return false;
		}

		if (slot != null) {
			bind(dragState.start(), InventorySlotRef.from(handler, slot, playerInventory));
		}
		dragState = null;
		return true;
	}

	public void drawSlotOverlay(GuiGraphicsExtractor context, AbstractContainerMenu handler, Slot slot, Inventory playerInventory) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.inventoryToolsEnabled()) {
			return;
		}

		InventorySlotRef ref = InventorySlotRef.from(handler, slot, playerInventory);
		boolean bound = isBound(ref) && config.slotBindingEnabled();
		boolean locked = isLocked(ref);
		if (!bound && !locked) {
			return;
		}

		int color = config.slotLockOverlayColor() | BORDER_ALPHA_MASK;
		if (bound) {
			drawBoundRing(context, slot.x, slot.y, config.boundSlotOverlayColor());
		} else if (locked) {
			drawLockIcon(context, slot.x + 10, slot.y + 1, color);
		}
	}

	public void drawDragLine(GuiGraphicsExtractor context, @Nullable Slot focusedSlot, int mouseX, int mouseY) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.inventoryToolsEnabled() || !config.slotBindingEnabled() || dragState == null) {
			return;
		}

		int endX = focusedSlot == null ? mouseX : focusedSlot.x + SLOT_SIZE / 2;
		int endY = focusedSlot == null ? mouseY : focusedSlot.y + SLOT_SIZE / 2;
		drawLine(context, dragState.startX(), dragState.startY(), endX, endY, LINE_COLOR);
	}

	private void toggleLock(InventorySlotRef ref) {
		if (isBound(ref)) {
			unbind(ref);
			return;
		}

		if (!lockedSlots.remove(ref)) {
			lockedSlots.add(ref);
		}
		persist();
	}

	private void bind(InventorySlotRef first, InventorySlotRef second) {
		if (first.equals(second)) {
			return;
		}
		if (first.isHotbar() == second.isHotbar()) {
			return;
		}

		unbind(first);
		unbind(second);
		boundSlots.put(first, second);
		boundSlots.put(second, first);
		persist();
	}

	private void unbind(InventorySlotRef ref) {
		InventorySlotRef other = boundSlots.remove(ref);
		if (other != null) {
			boundSlots.remove(other);
			persist();
		}
	}

	private void persist() {
		if (activeScopeKey == null) {
			return;
		}

		InventoryToolsStore.writeScope(activeScopeKey, lockedSlots, boundSlots);
	}

	private void clearSessionState() {
		lockedSlots.clear();
		boundSlots.clear();
		dragState = null;
		slotLockKeyConsumed = false;
		activeScopeKey = null;
	}

	private boolean isBound(InventorySlotRef ref) {
		return boundSlots.containsKey(ref);
	}

	private boolean isLocked(InventorySlotRef ref) {
		EMUtilsConfig config = EMUtilsClient.config();
		return lockedSlots.contains(ref) || config != null && config.slotBindingLockBoundSlots() && isBound(ref);
	}

	private boolean hasAnyLockedSlots() {
		EMUtilsConfig config = EMUtilsClient.config();
		return !lockedSlots.isEmpty() || config != null && config.slotBindingLockBoundSlots() && !boundSlots.isEmpty();
	}

	private boolean lockingAvailable() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.inventoryToolsEnabled() && (config.slotLockingEnabled() || config.slotBindingLockBoundSlots());
	}

	private boolean slotBindingAvailable() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.inventoryToolsEnabled() && config.slotBindingEnabled();
	}

	private boolean isBindKeyDown(Minecraft client) {
		if (slotBindKey == null) {
			return false;
		}

		InputConstants.Key key = ((KeyBindingAccess) slotBindKey).emhelpers$getBoundKey();
		if (key.getValue() == InputConstants.UNKNOWN.getValue()) {
			return false;
		}
		if (key.getType() == InputConstants.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
		}

		return InputConstants.isKeyDown(client.getWindow(), key.getValue());
	}

	private boolean isLockKeyDown(Minecraft client) {
		if (slotLockKey == null) {
			return false;
		}

		InputConstants.Key key = ((KeyBindingAccess) slotLockKey).emhelpers$getBoundKey();
		if (key.getValue() == InputConstants.UNKNOWN.getValue()) {
			return false;
		}
		if (key.getType() == InputConstants.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
		}

		return InputConstants.isKeyDown(client.getWindow(), key.getValue());
	}

	private boolean blocksPickup(Slot slot, ItemStack cursorStack) {
		if (cursorStack.isEmpty()) {
			return slot.hasItem();
		}
		if (!slot.hasItem()) {
			return false;
		}

		ItemStack stack = slot.getItem();
		return !ItemStack.isSameItemSameComponents(stack, cursorStack);
	}

	@Nullable
	private static Slot findSlot(AbstractContainerMenu handler, InventorySlotRef ref, Inventory playerInventory) {
		for (Slot slot : handler.slots) {
			if (InventorySlotRef.from(handler, slot, playerInventory).equals(ref)) {
				return slot;
			}
		}

		return null;
	}

	private static void drawBoundRing(GuiGraphicsExtractor context, int x, int y, int color) {
		context.fill(x, y, x + SLOT_SIZE, y + 1, color);
		context.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
		context.fill(x, y, x + 1, y + SLOT_SIZE, color);
		context.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
	}

	private static void drawLockIcon(GuiGraphicsExtractor context, int x, int y, int color) {
		context.fill(x + 2, y, x + 5, y + 1, color);
		context.fill(x + 1, y + 1, x + 2, y + 4, color);
		context.fill(x + 5, y + 1, x + 6, y + 4, color);
		context.fill(x, y + 4, x + 7, y + 9, color);
	}

	private static void drawLine(GuiGraphicsExtractor context, int startX, int startY, int endX, int endY, int color) {
		int steps = Math.max(Math.abs(endX - startX), Math.abs(endY - startY));
		if (steps == 0) {
			context.fill(startX - 1, startY - 1, startX + 2, startY + 2, color);
			return;
		}

		for (int step = 0; step <= steps; step++) {
			int x = startX + Math.round((endX - startX) * (step / (float) steps));
			int y = startY + Math.round((endY - startY) * (step / (float) steps));
			context.fill(x - 1, y - 1, x + 2, y + 2, color);
		}
	}

	public record BoundSwap(int slotId, int hotbarButton) {
	}

	private record DragState(InventorySlotRef start, int startX, int startY) {
	}
}
