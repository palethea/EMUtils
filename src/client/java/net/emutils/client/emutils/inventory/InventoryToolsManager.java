package net.emutils.client.emutils.inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.accessor.KeyBindingAccess;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.mixin.CreativeSlotAccess;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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
	private KeyBinding slotLockKey;
	@Nullable
	private KeyBinding slotBindKey;
	@Nullable
	private DragState dragState;
	private boolean slotLockKeyConsumed;
	private final InventoryCursorManager cursorManager = new InventoryCursorManager();

	public InventoryCursorManager cursor() {
		return cursorManager;
	}

	public void setKeyBindings(KeyBinding slotLockKey, KeyBinding slotBindKey) {
		this.slotLockKey = slotLockKey;
		this.slotBindKey = slotBindKey;
	}

	public void onWorldJoin(MinecraftClient client) {
		onWorldLeave(client);
		activeScopeKey = InventoryToolsStore.scopeKey(client);
		if (activeScopeKey == null) {
			return;
		}

		InventoryToolsStore.applyScope(InventoryToolsStore.readScope(activeScopeKey), lockedSlots, boundSlots);
	}

	public void onWorldLeave(MinecraftClient client) {
		persist();
		clearSessionState();
	}

	public void tick(MinecraftClient client) {
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

	public boolean handleKeyPressed(ScreenHandler handler, @Nullable Slot focusedSlot, KeyInput input, PlayerInventory playerInventory) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.inventoryToolsEnabled() || focusedSlot == null) {
			return false;
		}

		if (slotLockKey != null && slotLockKey.matchesKey(input) && config.slotLockingEnabled()) {
			if (slotLockKeyConsumed) {
				return true;
			}

			slotLockKeyConsumed = true;
			toggleLock(InventorySlotRef.from(handler, focusedSlot, playerInventory));
			return true;
		}

		if (slotBindKey != null && slotBindKey.matchesKey(input) && config.slotBindingEnabled()) {
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

	public boolean handleKeyReleased(ScreenHandler handler, @Nullable Slot focusedSlot, KeyInput input, PlayerInventory playerInventory) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.inventoryToolsEnabled() || slotBindKey == null || !slotBindKey.matchesKey(input) || dragState == null) {
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
		MinecraftClient client,
		ScreenHandler handler,
		@Nullable Slot focusedSlot,
		PlayerInventory playerInventory
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
		MinecraftClient client,
		ScreenHandler handler,
		@Nullable Slot clickedSlot,
		SlotActionType actionType,
		PlayerInventory playerInventory
	) {
		if (!slotBindingAvailable() || actionType != SlotActionType.QUICK_MOVE || clickedSlot == null || client.player == null) {
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

		return Optional.of(new BoundSwap(targetSlot.id, hotbar.hotbarButton()));
	}

	public boolean guardSlotClick(
		MinecraftClient client,
		ScreenHandler handler,
		@Nullable Slot slot,
		int button,
		SlotActionType actionType,
		ItemStack cursorStack,
		PlayerInventory playerInventory
	) {
		if (client == null || client.interactionManager == null || client.player == null || playerInventory == null) {
			return false;
		}

		Optional<BoundSwap> boundSwap = boundSwap(client, handler, slot, actionType, playerInventory);
		if (boundSwap.isPresent()) {
			executeBoundSwap(client, handler, boundSwap.get());
			return true;
		}

		return shouldBlockClick(handler, slot, button, actionType, cursorStack, playerInventory);
	}

	private void executeBoundSwap(MinecraftClient client, ScreenHandler handler, BoundSwap swap) {
		PlayerEntity player = client.player;
		if (player == null) {
			return;
		}

		if (client.currentScreen instanceof CreativeInventoryScreen) {
			player.playerScreenHandler.onSlotClick(swap.slotId(), swap.hotbarButton(), SlotActionType.SWAP, player);
			player.playerScreenHandler.sendContentUpdates();
			return;
		}

		client.interactionManager.clickSlot(handler.syncId, swap.slotId(), swap.hotbarButton(), SlotActionType.SWAP, player);
	}

	@Nullable
	private Slot resolveBoundTargetSlot(
		MinecraftClient client,
		ScreenHandler handler,
		InventorySlotRef clicked,
		InventorySlotRef target,
		@Nullable Slot clickedSlot,
		PlayerInventory playerInventory
	) {
		if (client.currentScreen instanceof CreativeInventoryScreen) {
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
	private static Slot findPlayerHandlerSlot(PlayerEntity player, InventorySlotRef ref) {
		for (Slot slot : player.playerScreenHandler.slots) {
			if (slot.inventory == player.getInventory() && slot.getIndex() == ref.inventoryIndex()) {
				return slot;
			}
		}

		return null;
	}

	@Nullable
	private static Slot unwrapPlayerHandlerSlot(@Nullable Slot slot, PlayerEntity player) {
		if (slot instanceof CreativeSlotAccess creativeSlot) {
			slot = creativeSlot.emutils$backingSlot();
		}

		if (slot == null || slot.inventory != player.getInventory()) {
			return null;
		}

		return findPlayerHandlerSlot(player, InventorySlotRef.forPlayerIndex(slot.getIndex()));
	}

	public boolean shouldBlockClick(
		ScreenHandler handler,
		@Nullable Slot slot,
		int button,
		SlotActionType actionType,
		ItemStack cursorStack,
		PlayerInventory playerInventory
	) {
		if (!lockingAvailable()) {
			return false;
		}

		if (actionType == SlotActionType.SWAP) {
			if (button >= 0 && button < PlayerInventory.HOTBAR_SIZE && isLocked(InventorySlotRef.forPlayerIndex(button))) {
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
			return actionType == SlotActionType.PICKUP_ALL && hasAnyLockedSlots();
		}

		return switch (actionType) {
			case QUICK_MOVE, THROW, SWAP, PICKUP_ALL, CLONE -> true;
			case PICKUP -> blocksPickup(slot, cursorStack);
			case QUICK_CRAFT -> slot.hasStack() && !cursorStack.isEmpty() && !ItemStack.areItemsAndComponentsEqual(slot.getStack(), cursorStack);
		};
	}

	public boolean handleMouseReleased(ScreenHandler handler, @Nullable Slot slot, PlayerInventory playerInventory) {
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

	public void drawSlotOverlay(DrawContext context, ScreenHandler handler, Slot slot, PlayerInventory playerInventory) {
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

	public void drawDragLine(DrawContext context, @Nullable Slot focusedSlot, int mouseX, int mouseY) {
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

	private boolean isBindKeyDown(MinecraftClient client) {
		if (slotBindKey == null) {
			return false;
		}

		InputUtil.Key key = ((KeyBindingAccess) slotBindKey).emutils$getBoundKey();
		if (key.getCode() == InputUtil.UNKNOWN_KEY.getCode()) {
			return false;
		}
		if (key.getCategory() == InputUtil.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), key.getCode()) == GLFW.GLFW_PRESS;
		}

		return InputUtil.isKeyPressed(client.getWindow(), key.getCode());
	}

	private boolean isLockKeyDown(MinecraftClient client) {
		if (slotLockKey == null) {
			return false;
		}

		InputUtil.Key key = ((KeyBindingAccess) slotLockKey).emutils$getBoundKey();
		if (key.getCode() == InputUtil.UNKNOWN_KEY.getCode()) {
			return false;
		}
		if (key.getCategory() == InputUtil.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), key.getCode()) == GLFW.GLFW_PRESS;
		}

		return InputUtil.isKeyPressed(client.getWindow(), key.getCode());
	}

	private boolean blocksPickup(Slot slot, ItemStack cursorStack) {
		if (cursorStack.isEmpty()) {
			return slot.hasStack();
		}
		if (!slot.hasStack()) {
			return false;
		}

		ItemStack stack = slot.getStack();
		return !ItemStack.areItemsAndComponentsEqual(stack, cursorStack);
	}

	@Nullable
	private static Slot findSlot(ScreenHandler handler, InventorySlotRef ref, PlayerInventory playerInventory) {
		for (Slot slot : handler.slots) {
			if (InventorySlotRef.from(handler, slot, playerInventory).equals(ref)) {
				return slot;
			}
		}

		return null;
	}

	private static void drawBoundRing(DrawContext context, int x, int y, int color) {
		context.fill(x, y, x + SLOT_SIZE, y + 1, color);
		context.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
		context.fill(x, y, x + 1, y + SLOT_SIZE, color);
		context.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
	}

	private static void drawLockIcon(DrawContext context, int x, int y, int color) {
		context.fill(x + 2, y, x + 5, y + 1, color);
		context.fill(x + 1, y + 1, x + 2, y + 4, color);
		context.fill(x + 5, y + 1, x + 6, y + 4, color);
		context.fill(x, y + 4, x + 7, y + 9, color);
	}

	private static void drawLine(DrawContext context, int startX, int startY, int endX, int endY, int color) {
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
