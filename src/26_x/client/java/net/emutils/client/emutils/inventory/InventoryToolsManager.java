package net.emutils.client.emutils.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emhelpers.client.accessor.KeyBindingAccess;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.mixin.CreativeSlotAccess;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class InventoryToolsManager {
	private static final int SLOT_SIZE = 16;
	private static final int LINE_COLOR = 0xFF00E5FF;
	private static final int BORDER_ALPHA_MASK = 0xFF000000;
	private static final int SORT_BUTTON_SIZE = 16;
	private static final int SORT_BUTTON_GAP = 1;
	private static final int SORT_BUTTON_OFFSET = 10;
	private static final int SORT_TITLE_BAR_HEIGHT = 14;
	private static final int SORT_NORMAL_OPERATION_LIMIT = 512;
	private static final int SORT_ICON_SIZE = 12;
	private static final int SORT_ICON_TEXTURE_SIZE = 64;
	private static final int PLAYER_STORAGE_SLOT_COUNT = 36;
	private static final int SORT_BUTTON_BORDER = 0xFF5A5A5A;
	private static final int SORT_BUTTON_FILL = 0xFFBEBEBE;
	private static final int SORT_BUTTON_FILL_HOVERED = 0xFFD0D0D0;
	private static final int SORT_BUTTON_FILL_DISABLED = 0xFF9B9B9B;
	private static final int SORT_BUTTON_HIGHLIGHT = 0xFFF2F2F2;
	private static final int SORT_BUTTON_SHADOW = 0xFF7A7A7A;
	private static final Identifier SORT_NAME_ICON = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "textures/gui/inventory/sort_name.png");
	private static final Identifier SORT_CATEGORY_ICON = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "textures/gui/inventory/sort_category.png");
	private static final Identifier SORT_QUANTITY_ICON = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "textures/gui/inventory/sort_quantity.png");

	private final Set<InventorySlotRef> lockedSlots = new HashSet<>();
	private final Map<InventorySlotRef, InventorySlotRef> boundSlots = new HashMap<>();
	@Nullable
	private String activeScopeKey;
	@Nullable
	private KeyMapping slotLockKey;
	@Nullable
	private KeyMapping slotBindKey;
	@Nullable
	private KeyMapping quickStackKey;
	@Nullable
	private DragState dragState;
	private final Set<Integer> hoverTransferSlots = new HashSet<>();
	private boolean hoverTransferActive;
	private boolean slotLockKeyConsumed;
	@Nullable
	private SortTask sortTask;
	@Nullable
	private QuickStackTask quickStackTask;
	private final InventoryCursorManager cursorManager = new InventoryCursorManager();

	public InventoryCursorManager cursor() {
		return cursorManager;
	}

	public void setKeyMappings(KeyMapping slotLockKey, KeyMapping slotBindKey, KeyMapping quickStackKey) {
		this.slotLockKey = slotLockKey;
		this.slotBindKey = slotBindKey;
		this.quickStackKey = quickStackKey;
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
		runQueuedSort(client);
		runQueuedQuickStack(client);

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
		if (config == null || !config.inventoryToolsEnabled()) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		if (quickStackKey != null
			&& quickStackKey.matches(input)
			&& quickStackAvailable(client, handler, playerInventory)
			&& handler.getCarried().isEmpty()
			&& !inventoryActionRunning()) {
			startQuickStack(client, handler, playerInventory);
			return true;
		}

		if (focusedSlot == null) {
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
		clearHoverTransfer();
		sortTask = null;
		quickStackTask = null;
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

		if (client.gui.screen() instanceof CreativeModeInventoryScreen) {
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
		if (client.gui.screen() instanceof CreativeModeInventoryScreen) {
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
			return actionType == ContainerInput.PICKUP_ALL && hasLockedMatchingStack(handler, playerInventory, cursorStack);
		}

		return switch (actionType) {
			case QUICK_MOVE, THROW, SWAP, PICKUP_ALL, CLONE -> true;
			case PICKUP -> blocksPickup(slot, cursorStack);
			case QUICK_CRAFT -> slot.hasItem() && !cursorStack.isEmpty() && !ItemStack.isSameItemSameComponents(slot.getItem(), cursorStack);
		};
	}

	public boolean handleMouseReleased(AbstractContainerMenu handler, @Nullable Slot slot, Inventory playerInventory) {
		boolean handledHoverTransfer = hoverTransferActive;
		clearHoverTransfer();

		if (dragState == null) {
			return handledHoverTransfer;
		}

		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.inventoryToolsEnabled() || !config.slotBindingEnabled()) {
			dragState = null;
			return handledHoverTransfer;
		}

		if (slot != null) {
			bind(dragState.start(), InventorySlotRef.from(handler, slot, playerInventory));
		}
		dragState = null;
		return true;
	}

	public boolean handleHoverTransferMouseClicked(
		Minecraft client,
		AbstractContainerMenu handler,
		@Nullable Slot slot,
		int button,
		boolean shiftDown,
		Inventory playerInventory
	) {
		if (!canStartHoverTransfer(client, handler, button, shiftDown, playerInventory)) {
			clearHoverTransfer();
			return false;
		}

		hoverTransferActive = true;
		hoverTransferSlots.clear();
		tryHoverTransferSlot(client, handler, slot, playerInventory);
		return true;
	}

	public boolean handleHoverTransferMouseDragged(
		Minecraft client,
		AbstractContainerMenu handler,
		@Nullable Slot slot,
		int button,
		boolean shiftDown,
		Inventory playerInventory
	) {
		if (!hoverTransferActive) {
			return false;
		}
		if (!hoverTransferAvailable() || button != 0 || !shiftDown || client.player == null || client.gameMode == null) {
			clearHoverTransfer();
			return false;
		}

		tryHoverTransferSlot(client, handler, slot, playerInventory);
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

	public void drawDragLine(
		GuiGraphicsExtractor context,
		@Nullable Slot focusedSlot,
		int screenX,
		int screenY,
		int mouseX,
		int mouseY
	) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.inventoryToolsEnabled() || !config.slotBindingEnabled() || dragState == null) {
			return;
		}

		int endX = focusedSlot == null ? mouseX : screenX + focusedSlot.x + SLOT_SIZE / 2;
		int endY = focusedSlot == null ? mouseY : screenY + focusedSlot.y + SLOT_SIZE / 2;
		drawLine(
			context,
			screenX + dragState.startX(),
			screenY + dragState.startY(),
			endX,
			endY,
			LINE_COLOR
		);
	}

	public void drawSortButtons(
		Minecraft client,
		GuiGraphicsExtractor context,
		AbstractContainerMenu handler,
		Inventory playerInventory,
		int screenX,
		int screenY,
		int mouseX,
		int mouseY
	) {
		boolean sortButtonsAvailable = sortButtonsAvailable(client, handler, playerInventory);
		boolean quickStackAvailable = quickStackAvailable(client, handler, playerInventory);
		if (!sortButtonsAvailable && !quickStackAvailable) {
			return;
		}

		boolean enabled = handler.getCarried().isEmpty() && !inventoryActionRunning();
		Component tooltip = null;
		if (sortButtonsAvailable) {
			for (SortButton sortButton : sortButtons(handler, playerInventory)) {
				boolean hovered = sortButton.contains(mouseX, mouseY);
				drawSortButton(context, sortButton, screenX, screenY, enabled, hovered);
				if (hovered) {
					tooltip = Component.translatable(sortButton.mode().labelKey());
				}
			}
		}

		QuickStackButton quickStackButton = quickStackButton(handler, playerInventory, sortButtonsAvailable);
		if (quickStackButton != null && quickStackAvailable) {
			boolean hovered = quickStackButton.contains(mouseX, mouseY);
			drawQuickStackButton(context, quickStackButton, screenX, screenY, enabled, hovered);
			if (hovered) {
				tooltip = Component.translatable("emutils.inventory.quick_stack");
			}
		}

		if (tooltip != null) {
			context.setTooltipForNextFrame(
				client.font,
				List.of(tooltip),
				Optional.empty(),
				screenX + mouseX,
				screenY + mouseY,
				null
			);
		}
	}

	public boolean handleSortButtonMouseClicked(
		Minecraft client,
		AbstractContainerMenu handler,
		int button,
		double mouseX,
		double mouseY,
		Inventory playerInventory
	) {
		if (button != 0 || !handler.getCarried().isEmpty() || inventoryActionRunning()) {
			return false;
		}

		boolean sortButtonsAvailable = sortButtonsAvailable(client, handler, playerInventory);
		if (sortButtonsAvailable) {
			for (SortButton sortButton : sortButtons(handler, playerInventory)) {
				if (sortButton.contains(mouseX, mouseY)) {
					startSort(client, handler, playerInventory, sortButton.target(), sortButton.mode());
					return true;
				}
			}
		}

		QuickStackButton quickStackButton = quickStackButton(handler, playerInventory, sortButtonsAvailable);
		if (quickStackButton != null
			&& quickStackAvailable(client, handler, playerInventory)
			&& quickStackButton.contains(mouseX, mouseY)) {
			startQuickStack(client, handler, playerInventory);
			return true;
		}

		return false;
	}

	private void startQuickStack(Minecraft client, AbstractContainerMenu handler, Inventory playerInventory) {
		if (client.player == null || client.gameMode == null) {
			return;
		}

		List<Integer> slotIds = quickStackSlotIds(handler, playerInventory, client.player);
		if (slotIds.isEmpty()) {
			return;
		}

		if (EMUtilsClient.config().quickStackSpeed() == InventorySortSpeed.ANTI_CHEAT) {
			quickStackTask = new QuickStackTask(handler, slotIds);
			runQueuedQuickStack(client);
			return;
		}

		for (int index = 0; index < Math.min(slotIds.size(), SORT_NORMAL_OPERATION_LIMIT); index++) {
			executeQuickStackOperation(client, handler, slotIds.get(index));
		}
	}

	private void runQueuedQuickStack(Minecraft client) {
		if (quickStackTask == null) {
			return;
		}
		if (client.player == null
			|| client.gameMode == null
			|| client.player.containerMenu != quickStackTask.handler()
			|| !isStorageContainerScreen(client)
			|| !quickStackTask.handler().getCarried().isEmpty()) {
			quickStackTask = null;
			return;
		}

		Integer slotId = quickStackTask.next();
		if (slotId == null) {
			quickStackTask = null;
			return;
		}

		executeQuickStackOperation(client, quickStackTask.handler(), slotId);
		if (quickStackTask.done()) {
			quickStackTask = null;
		}
	}

	private static void executeQuickStackOperation(Minecraft client, AbstractContainerMenu handler, int slotId) {
		if (client.player != null && client.gameMode != null) {
			client.gameMode.handleContainerInput(handler.containerId, slotId, 0, ContainerInput.QUICK_MOVE, client.player);
		}
	}

	private List<Integer> quickStackSlotIds(AbstractContainerMenu handler, Inventory playerInventory, Player player) {
		List<ItemStack> containerStacks = new ArrayList<>();
		for (Slot slot : handler.slots) {
			if (slot.isActive() && slot.container != playerInventory && slot.hasItem()) {
				containerStacks.add(slot.getItem().copy());
			}
		}

		List<Integer> slotIds = new ArrayList<>();
		for (int slotId = 0; slotId < handler.slots.size(); slotId++) {
			Slot slot = handler.slots.get(slotId);
			int inventoryIndex = slot.getContainerSlot();
			if (!slot.isActive()
				|| slot.container != playerInventory
				|| inventoryIndex < 0
				|| inventoryIndex >= PLAYER_STORAGE_SLOT_COUNT
				|| !slot.hasItem()
				|| !slot.mayPickup(player)
				|| isLocked(InventorySlotRef.from(handler, slot, playerInventory))) {
				continue;
			}

			ItemStack playerStack = slot.getItem();
			if (containerStacks.stream().anyMatch(containerStack -> ItemStack.isSameItemSameComponents(containerStack, playerStack))) {
				slotIds.add(slotId);
			}
		}
		return slotIds;
	}

	private void startSort(
		Minecraft client,
		AbstractContainerMenu handler,
		Inventory playerInventory,
		SortTarget target,
		InventorySortMode mode
	) {
		if (client.player == null || client.gameMode == null) {
			return;
		}

		List<SortOperation> operations = buildSortOperations(handler, playerInventory, target, mode, client.player);
		if (operations.isEmpty()) {
			return;
		}

		if (EMUtilsClient.config().sortSpeed() == InventorySortSpeed.ANTI_CHEAT) {
			sortTask = new SortTask(handler, operations);
			runQueuedSort(client);
			return;
		}

		int count = 0;
		for (SortOperation operation : operations) {
			executeSortOperation(client, handler, operation);
			count++;
			if (count >= SORT_NORMAL_OPERATION_LIMIT) {
				break;
			}
		}
	}

	private void runQueuedSort(Minecraft client) {
		if (sortTask == null) {
			return;
		}
		if (client.player == null || client.gameMode == null || !isSortableContainerScreen(client) || !sortTask.handler().getCarried().isEmpty()) {
			sortTask = null;
			return;
		}

		SortOperation operation = sortTask.next();
		if (operation == null) {
			sortTask = null;
			return;
		}

		executeSortOperation(client, sortTask.handler(), operation);
		if (sortTask.done()) {
			sortTask = null;
		}
	}

	private void executeSortOperation(Minecraft client, AbstractContainerMenu handler, SortOperation operation) {
		Player player = client.player;
		if (player == null || client.gameMode == null) {
			return;
		}

		for (int slotId : operation.slotIds()) {
			client.gameMode.handleContainerInput(handler.containerId, slotId, 0, ContainerInput.PICKUP, player);
		}
	}

	private List<SortOperation> buildSortOperations(
		AbstractContainerMenu handler,
		Inventory playerInventory,
		SortTarget target,
		InventorySortMode mode,
		Player player
	) {
		List<SortSlot> slots = sortableSlots(handler, playerInventory, target, player);
		List<ItemStack> state = new ArrayList<>();
		for (SortSlot slot : slots) {
			state.add(slot.stack());
		}

		List<SortOperation> operations = new ArrayList<>();
		compactStacks(slots, state, operations);

		List<ItemStack> sorted = new ArrayList<>();
		for (ItemStack stack : state) {
			if (!stack.isEmpty()) {
				sorted.add(stack.copy());
			}
		}
		sorted.sort(sortComparator(mode));

		List<ItemStack> desired = new ArrayList<>(state.size());
		for (int index = 0; index < state.size(); index++) {
			desired.add(index < sorted.size() ? sorted.get(index).copy() : ItemStack.EMPTY);
		}

		sortIntoDesiredOrder(slots, state, desired, operations);
		return operations;
	}

	private void compactStacks(List<SortSlot> slots, List<ItemStack> state, List<SortOperation> operations) {
		for (int target = 0; target < state.size(); target++) {
			ItemStack targetStack = state.get(target);
			if (targetStack.isEmpty()) {
				continue;
			}

			int max = targetStack.getMaxStackSize();
			for (int source = target + 1; source < state.size() && targetStack.getCount() < max; source++) {
				ItemStack sourceStack = state.get(source);
				if (!canMerge(targetStack, sourceStack)) {
					continue;
				}

				int room = max - targetStack.getCount();
				int moved = Math.min(room, sourceStack.getCount());
				if (moved <= 0) {
					continue;
				}

				boolean sourceEmptied = moved == sourceStack.getCount();
				operations.add(sourceEmptied
					? new SortOperation(new int[] {slots.get(source).slotId(), slots.get(target).slotId()})
					: new SortOperation(new int[] {slots.get(source).slotId(), slots.get(target).slotId(), slots.get(source).slotId()}));
				targetStack.setCount(targetStack.getCount() + moved);
				sourceStack.setCount(sourceStack.getCount() - moved);
				if (sourceStack.getCount() <= 0) {
					state.set(source, ItemStack.EMPTY);
				}
			}
		}
	}

	private void sortIntoDesiredOrder(
		List<SortSlot> slots,
		List<ItemStack> state,
		List<ItemStack> desired,
		List<SortOperation> operations
	) {
		for (int target = 0; target < desired.size(); target++) {
			ItemStack desiredStack = desired.get(target);
			if (sameStack(state.get(target), desiredStack)) {
				continue;
			}
			if (desiredStack.isEmpty()) {
				continue;
			}

			int source = findMatchingStack(state, desiredStack, target + 1);
			if (source < 0) {
				continue;
			}

			operations.add(state.get(target).isEmpty()
				? new SortOperation(new int[] {slots.get(source).slotId(), slots.get(target).slotId()})
				: new SortOperation(new int[] {slots.get(source).slotId(), slots.get(target).slotId(), slots.get(source).slotId()}));

			ItemStack previousTarget = state.get(target);
			state.set(target, state.get(source));
			state.set(source, previousTarget);
		}
	}

	private int findMatchingStack(List<ItemStack> state, ItemStack desired, int start) {
		for (int index = start; index < state.size(); index++) {
			if (sameStack(state.get(index), desired)) {
				return index;
			}
		}
		return -1;
	}

	private Comparator<ItemStack> sortComparator(InventorySortMode mode) {
		Comparator<ItemStack> byName = Comparator
			.comparing((ItemStack stack) -> stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
			.thenComparing(this::itemId)
			.thenComparing((ItemStack stack) -> stack.getCount(), Comparator.reverseOrder());

		return switch (mode) {
			case NAME -> byName;
			case CATEGORY -> Comparator
				.comparingInt(this::categoryRank)
				.thenComparingInt(this::categorySubRank)
				.thenComparing(byName);
			case QUANTITY -> Comparator
				.comparingInt(ItemStack::getCount)
				.reversed()
				.thenComparing(byName);
		};
	}

	private int categoryRank(ItemStack stack) {
		String path = itemPath(stack);
		if (isBuildingBlock(stack, path)) return 0;
		if (isFunctionalBlock(path)) return 1;
		if (isTransport(path)) return 2;
		if (isRedstone(path)) return 3;
		if (isTool(path)) return 4;
		if (isWeapon(path)) return 5;
		if (isArmor(path)) return 6;
		if (stack.get(DataComponents.FOOD) != null) return 7;
		if (isBrewOrEnchant(path)) return 8;
		if (isResource(path)) return 9;
		if (isNature(path)) return 10;
		return 11;
	}

	private int categorySubRank(ItemStack stack) {
		String path = itemPath(stack);
		int materialRank = materialRank(path);
		if (isBuildingBlock(stack, path)) {
			return blockRank(path) * 100 + materialRank;
		}
		if (isTool(path)) {
			return materialRank * 100 + toolRank(path);
		}
		if (isWeapon(path) || isArmor(path)) {
			return materialRank * 100 + equipmentRank(path);
		}
		if (isResource(path)) {
			return resourceGroupRank(path) * 1000 + resourceRank(path) * 100 + materialRank;
		}
		return materialRank;
	}

	private String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private String itemPath(ItemStack stack) {
		String id = itemId(stack);
		int separator = id.indexOf(':');
		return (separator >= 0 ? id.substring(separator + 1) : id).toLowerCase(Locale.ROOT);
	}

	private static boolean isBuildingBlock(ItemStack stack, String path) {
		return stack.getItem() instanceof BlockItem
			&& !isFunctionalBlock(path)
			&& !isRedstone(path)
			&& !isNature(path);
	}

	private static boolean isFunctionalBlock(String path) {
		return containsAny(path, "chest", "barrel", "shulker_box", "furnace", "smoker", "blast_furnace", "crafting_table", "stonecutter", "cartography_table", "smithing_table", "fletching_table", "grindstone", "loom", "anvil", "enchanting_table", "ender_chest", "beacon", "conduit", "brewing_stand", "cauldron", "respawn_anchor", "lodestone", "lectern", "jukebox", "note_block", "bell", "campfire", "lantern", "torch", "candle", "bed", "door", "trapdoor");
	}

	private static boolean isRedstone(String path) {
		return containsAny(path, "redstone", "repeater", "comparator", "piston", "observer", "dispenser", "dropper", "hopper", "lever", "button", "pressure_plate", "tripwire", "daylight_detector", "target", "rail", "minecart", "sculk_sensor");
	}

	private static boolean isTool(String path) {
		return containsAny(path, "pickaxe", "shovel", "_axe", "hoe", "shears", "flint_and_steel", "brush", "fishing_rod", "compass", "clock", "spyglass", "map");
	}

	private static boolean isWeapon(String path) {
		return containsAny(path, "sword", "bow", "crossbow", "trident", "mace", "shield", "arrow", "firework_rocket");
	}

	private static boolean isArmor(String path) {
		return containsAny(path, "helmet", "chestplate", "leggings", "boots", "elytra", "horse_armor");
	}

	private static boolean isResource(String path) {
		return containsAny(path, "ingot", "nugget", "diamond", "emerald", "lapis", "coal", "charcoal", "quartz", "amethyst", "copper", "iron", "gold", "netherite", "redstone", "dust", "gem", "shard", "crystal", "stick", "string", "leather", "paper", "book", "bone", "feather", "wool", "dye", "clay_ball", "brick", "flint", "slime_ball", "gunpowder", "blaze_rod", "ender_pearl", "nether_star");
	}

	private static boolean isNature(String path) {
		return containsAny(path, "sapling", "leaves", "flower", "tulip", "rose", "orchid", "dandelion", "azalea", "mushroom", "fungus", "roots", "vines", "grass", "fern", "kelp", "coral", "seagrass", "cactus", "sugar_cane", "bamboo", "seeds", "wheat", "carrot", "potato", "beetroot", "melon", "pumpkin", "egg", "spawn_egg");
	}

	private static boolean isBrewOrEnchant(String path) {
		return containsAny(path, "potion", "bottle", "dragon_breath", "glistering_melon", "magma_cream", "fermented_spider_eye", "ghast_tear", "experience_bottle", "enchanted_book");
	}

	private static boolean isTransport(String path) {
		return containsAny(path, "boat", "raft", "minecart", "saddle", "lead", "bucket", "elytra");
	}

	private static int blockRank(String path) {
		if (containsAny(path, "ore", "raw_", "ancient_debris", "netherite_block", "diamond_block", "emerald_block", "gold_block", "iron_block", "copper_block", "coal_block", "lapis_block", "redstone_block", "quartz_block", "amethyst_block")) return 0;
		if (containsAny(path, "stone", "deepslate", "granite", "diorite", "andesite", "tuff", "calcite", "basalt", "blackstone", "netherrack", "end_stone")) return 1;
		if (containsAny(path, "dirt", "grass_block", "podzol", "mycelium", "mud", "clay", "sand", "gravel", "snow", "ice")) return 2;
		if (containsAny(path, "log", "wood", "stem", "hyphae", "planks", "bamboo", "mangrove", "oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak", "crimson", "warped")) return 3;
		if (containsAny(path, "glass", "wool", "concrete", "terracotta", "glazed_terracotta")) return 4;
		if (containsAny(path, "slab", "stairs", "wall", "fence", "pane")) return 5;
		return 6;
	}

	private static int toolRank(String path) {
		if (path.contains("pickaxe")) return 0;
		if (path.contains("shovel")) return 1;
		if (path.contains("_axe")) return 2;
		if (path.contains("hoe")) return 3;
		return 4;
	}

	private static int equipmentRank(String path) {
		if (path.contains("sword") || path.contains("mace") || path.contains("trident")) return 0;
		if (path.contains("bow") || path.contains("crossbow") || path.contains("arrow")) return 1;
		if (path.contains("shield")) return 2;
		if (path.contains("helmet")) return 3;
		if (path.contains("chestplate") || path.contains("elytra")) return 4;
		if (path.contains("leggings")) return 5;
		if (path.contains("boots")) return 6;
		return 7;
	}

	private static int resourceRank(String path) {
		if (path.contains("netherite")) return 0;
		if (path.contains("diamond")) return 1;
		if (path.contains("emerald")) return 2;
		if (path.contains("gold")) return 3;
		if (path.contains("iron")) return 4;
		if (path.contains("copper")) return 5;
		if (path.contains("coal") || path.contains("charcoal")) return 6;
		if (path.contains("redstone")) return 7;
		if (path.contains("lapis")) return 8;
		if (path.contains("quartz")) return 9;
		if (path.contains("amethyst")) return 10;
		if (path.contains("flint")) return 11;
		if (path.contains("clay_ball") || path.contains("brick")) return 12;
		if (path.contains("stick")) return 20;
		if (path.contains("string")) return 21;
		if (path.contains("paper") || path.contains("book")) return 22;
		if (path.contains("leather")) return 23;
		if (path.contains("wool") || path.contains("dye")) return 24;
		if (path.contains("bone")) return 30;
		if (path.contains("feather")) return 31;
		if (path.contains("slime_ball")) return 32;
		if (path.contains("gunpowder")) return 33;
		if (path.contains("ender_pearl")) return 40;
		if (path.contains("blaze_rod")) return 41;
		if (path.contains("nether_star")) return 42;
		return 99;
	}

	private static int resourceGroupRank(String path) {
		if (containsAny(path, "ingot", "nugget", "raw_", "netherite", "diamond", "emerald", "iron", "gold", "copper", "coal", "charcoal", "redstone", "lapis", "quartz", "amethyst", "gem", "shard", "crystal")) return 0;
		if (containsAny(path, "flint", "clay_ball", "brick")) return 1;
		if (containsAny(path, "stick", "string", "paper", "book", "leather", "wool", "dye")) return 2;
		if (containsAny(path, "bone", "feather", "slime_ball", "gunpowder")) return 3;
		if (containsAny(path, "ender_pearl", "blaze_rod", "nether_star")) return 4;
		return 5;
	}

	private static int materialRank(String path) {
		if (path.contains("netherite")) return 0;
		if (path.contains("diamond")) return 1;
		if (path.contains("emerald")) return 2;
		if (path.contains("gold")) return 3;
		if (path.contains("iron")) return 4;
		if (path.contains("copper")) return 5;
		if (path.contains("stone")) return 6;
		if (path.contains("wood") || path.contains("planks") || path.contains("log")) return 7;
		if (path.contains("leather")) return 8;
		return 9;
	}

	private static boolean containsAny(String text, String... needles) {
		for (String needle : needles) {
			if (text.contains(needle)) {
				return true;
			}
		}
		return false;
	}

	private List<SortSlot> sortableSlots(
		AbstractContainerMenu handler,
		Inventory playerInventory,
		SortTarget target,
		Player player
	) {
		List<SortSlot> slots = new ArrayList<>();
		for (Slot slot : handler.slots) {
			if (!slot.isActive() || !belongsToTarget(slot, playerInventory, target)) {
				continue;
			}
			if (slot.hasItem() && slot.getItem().get(DataComponents.BUNDLE_CONTENTS) != null) {
				continue;
			}
			if (isLocked(InventorySlotRef.from(handler, slot, playerInventory))) {
				continue;
			}
			if (slot.hasItem() && !slot.mayPickup(player)) {
				continue;
			}

			slots.add(new SortSlot(handler.slots.indexOf(slot), slot.getItem().isEmpty() ? ItemStack.EMPTY : slot.getItem().copy()));
		}
		return slots;
	}

	private List<SortButton> sortButtons(AbstractContainerMenu handler, Inventory playerInventory) {
		List<SortButton> buttons = new ArrayList<>();
		addSortButtons(buttons, handler, playerInventory, SortTarget.CONTAINER);
		addSortButtons(buttons, handler, playerInventory, SortTarget.PLAYER);
		return buttons;
	}

	private void addSortButtons(List<SortButton> buttons, AbstractContainerMenu handler, Inventory playerInventory, SortTarget target) {
		Bounds bounds = boundsFor(handler, playerInventory, target);
		if (bounds == null) {
			return;
		}

		int totalHeight = InventorySortMode.values().length * SORT_BUTTON_SIZE + (InventorySortMode.values().length - 1) * SORT_BUTTON_GAP;
		int x = bounds.right() + SORT_BUTTON_OFFSET;
		int y = bounds.top() - SORT_TITLE_BAR_HEIGHT;
		int index = 0;
		for (InventorySortMode mode : InventorySortMode.values()) {
			buttons.add(new SortButton(target, mode, x, y + index * (SORT_BUTTON_SIZE + SORT_BUTTON_GAP)));
			index++;
		}
	}

	@Nullable
	private Bounds boundsFor(AbstractContainerMenu handler, Inventory playerInventory, SortTarget target) {
		Bounds bounds = null;
		for (Slot slot : handler.slots) {
			if (!slot.isActive() || !belongsToTarget(slot, playerInventory, target)) {
				continue;
			}
			bounds = bounds == null
				? new Bounds(slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE)
				: bounds.include(slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE);
		}
		return bounds;
	}

	private boolean sortButtonsAvailable(Minecraft client, AbstractContainerMenu handler, Inventory playerInventory) {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null
			&& config.inventoryToolsEnabled()
			&& config.sortButtonsEnabled()
			&& client.player != null
			&& client.gameMode != null
			&& isSortableContainerScreen(client)
			&& hasContainerSlots(handler, playerInventory);
	}

	private boolean quickStackAvailable(Minecraft client, AbstractContainerMenu handler, Inventory playerInventory) {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null
			&& config.inventoryToolsEnabled()
			&& config.quickStackEnabled()
			&& client.player != null
			&& client.gameMode != null
			&& isStorageContainerScreen(client)
			&& hasContainerSlots(handler, playerInventory);
	}

	@Nullable
	private QuickStackButton quickStackButton(
		AbstractContainerMenu handler,
		Inventory playerInventory,
		boolean sortButtonsAvailable
	) {
		Bounds bounds = boundsFor(handler, playerInventory, SortTarget.CONTAINER);
		if (bounds == null) {
			return null;
		}

		int sortButtonCount = sortButtonsAvailable ? InventorySortMode.values().length : 0;
		return new QuickStackButton(
			bounds.right() + SORT_BUTTON_OFFSET,
			bounds.top() - SORT_TITLE_BAR_HEIGHT + sortButtonCount * (SORT_BUTTON_SIZE + SORT_BUTTON_GAP)
		);
	}

	private boolean inventoryActionRunning() {
		return sortTask != null || quickStackTask != null;
	}

	private static boolean belongsToTarget(Slot slot, Inventory playerInventory, SortTarget target) {
		boolean playerSlot = slot.container == playerInventory;
		return target == SortTarget.PLAYER ? playerSlot : !playerSlot;
	}

	private static boolean canMerge(ItemStack left, ItemStack right) {
		return !left.isEmpty() && !right.isEmpty() && ItemStack.isSameItemSameComponents(left, right);
	}

	private static boolean sameStack(ItemStack left, ItemStack right) {
		if (left.isEmpty() || right.isEmpty()) {
			return left.isEmpty() && right.isEmpty();
		}
		return left.getCount() == right.getCount() && ItemStack.isSameItemSameComponents(left, right);
	}

	private static void drawSortButton(GuiGraphicsExtractor context, SortButton button, int screenX, int screenY, boolean enabled, boolean hovered) {
		int x = screenX + button.x();
		int y = screenY + button.y();
		drawInventoryActionButton(context, x, y, enabled, hovered);
		drawSortIcon(context, button.mode(), x, y);
	}

	private static void drawQuickStackButton(GuiGraphicsExtractor context, QuickStackButton button, int screenX, int screenY, boolean enabled, boolean hovered) {
		int x = screenX + button.x();
		int y = screenY + button.y();
		drawInventoryActionButton(context, x, y, enabled, hovered);

		int iconColor = enabled ? 0xFF303030 : 0xFF666666;
		context.fill(x + 7, y + 3, x + 9, y + 8, iconColor);
		context.fill(x + 5, y + 6, x + 11, y + 8, iconColor);
		context.fill(x + 6, y + 8, x + 10, y + 9, iconColor);
		context.fill(x + 4, y + 10, x + 5, y + 13, iconColor);
		context.fill(x + 11, y + 10, x + 12, y + 13, iconColor);
		context.fill(x + 4, y + 12, x + 12, y + 13, iconColor);
	}

	private static void drawInventoryActionButton(GuiGraphicsExtractor context, int x, int y, boolean enabled, boolean hovered) {
		int fill = enabled ? hovered ? SORT_BUTTON_FILL_HOVERED : SORT_BUTTON_FILL : SORT_BUTTON_FILL_DISABLED;
		context.fill(x, y, x + SORT_BUTTON_SIZE, y + SORT_BUTTON_SIZE, SORT_BUTTON_BORDER);
		context.fill(x + 1, y + 1, x + SORT_BUTTON_SIZE - 1, y + SORT_BUTTON_SIZE - 1, fill);
		context.fill(x + 1, y + 1, x + SORT_BUTTON_SIZE - 1, y + 2, SORT_BUTTON_HIGHLIGHT);
		context.fill(x + 1, y + 1, x + 2, y + SORT_BUTTON_SIZE - 1, SORT_BUTTON_HIGHLIGHT);
		context.fill(x + 1, y + SORT_BUTTON_SIZE - 2, x + SORT_BUTTON_SIZE - 1, y + SORT_BUTTON_SIZE - 1, SORT_BUTTON_SHADOW);
		context.fill(x + SORT_BUTTON_SIZE - 2, y + 1, x + SORT_BUTTON_SIZE - 1, y + SORT_BUTTON_SIZE - 1, SORT_BUTTON_SHADOW);
	}

	private static void drawSortIcon(GuiGraphicsExtractor context, InventorySortMode mode, int x, int y) {
		int iconX = x + (SORT_BUTTON_SIZE - SORT_ICON_SIZE) / 2;
		int iconY = y + (SORT_BUTTON_SIZE - SORT_ICON_SIZE) / 2;
		context.blit(
			RenderPipelines.GUI_TEXTURED,
			iconFor(mode),
			iconX,
			iconY,
			0.0F,
			0.0F,
			SORT_ICON_SIZE,
			SORT_ICON_SIZE,
			SORT_ICON_TEXTURE_SIZE,
			SORT_ICON_TEXTURE_SIZE,
			SORT_ICON_TEXTURE_SIZE,
			SORT_ICON_TEXTURE_SIZE
		);
	}

	private static Identifier iconFor(InventorySortMode mode) {
		return switch (mode) {
			case NAME -> SORT_NAME_ICON;
			case CATEGORY -> SORT_CATEGORY_ICON;
			case QUANTITY -> SORT_QUANTITY_ICON;
		};
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

	private boolean hasLockedMatchingStack(AbstractContainerMenu handler, Inventory playerInventory, ItemStack targetStack) {
		if (targetStack.isEmpty()) {
			return false;
		}

		for (Slot slot : handler.slots) {
			if (slot.hasItem()
				&& isLocked(InventorySlotRef.from(handler, slot, playerInventory))
				&& ItemStack.isSameItemSameComponents(slot.getItem(), targetStack)) {
				return true;
			}
		}
		return false;
	}

	private boolean lockingAvailable() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.inventoryToolsEnabled() && (config.slotLockingEnabled() || config.slotBindingLockBoundSlots());
	}

	private boolean slotBindingAvailable() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.inventoryToolsEnabled() && config.slotBindingEnabled();
	}

	private boolean hoverTransferAvailable() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.inventoryToolsEnabled() && config.hoverTransferEnabled();
	}

	private boolean canStartHoverTransfer(
		Minecraft client,
		AbstractContainerMenu handler,
		int button,
		boolean shiftDown,
		Inventory playerInventory
	) {
		return hoverTransferAvailable()
			&& button == 0
			&& shiftDown
			&& dragState == null
			&& client.player != null
			&& client.gameMode != null
			&& handler.getCarried().isEmpty()
			&& isStorageContainerScreen(client)
			&& hasContainerSlots(handler, playerInventory);
	}

	private void tryHoverTransferSlot(
		Minecraft client,
		AbstractContainerMenu handler,
		@Nullable Slot slot,
		Inventory playerInventory
	) {
		Player player = client.player;
		if (player == null || client.gameMode == null || slot == null || !slot.hasItem() || !slot.mayPickup(player)) {
			return;
		}

		int slotId = handler.slots.indexOf(slot);
		if (slotId < 0 || !hoverTransferSlots.add(slotId)) {
			return;
		}

		if (isLocked(InventorySlotRef.from(handler, slot, playerInventory))) {
			return;
		}

		client.gameMode.handleContainerInput(handler.containerId, slotId, 0, ContainerInput.QUICK_MOVE, player);
	}

	private void clearHoverTransfer() {
		hoverTransferActive = false;
		hoverTransferSlots.clear();
	}

	private static boolean isStorageContainerScreen(Minecraft client) {
		return client.gui.screen() instanceof ContainerScreen
			|| client.gui.screen() instanceof ShulkerBoxScreen
			|| client.gui.screen() instanceof HopperScreen
			|| client.gui.screen() instanceof DispenserScreen;
	}

	private static boolean isSortableContainerScreen(Minecraft client) {
		return client.gui.screen() instanceof ContainerScreen
			|| client.gui.screen() instanceof ShulkerBoxScreen;
	}

	private static boolean hasContainerSlots(AbstractContainerMenu handler, Inventory playerInventory) {
		for (Slot slot : handler.slots) {
			if (slot.container != playerInventory) {
				return true;
			}
		}

		return false;
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

	private enum SortTarget {
		CONTAINER,
		PLAYER
	}

	private record SortButton(SortTarget target, InventorySortMode mode, int x, int y) {
		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= x
				&& mouseX < x + SORT_BUTTON_SIZE
				&& mouseY >= y
				&& mouseY < y + SORT_BUTTON_SIZE;
		}
	}

	private record QuickStackButton(int x, int y) {
		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= x
				&& mouseX < x + SORT_BUTTON_SIZE
				&& mouseY >= y
				&& mouseY < y + SORT_BUTTON_SIZE;
		}
	}

	private record SortSlot(int slotId, ItemStack stack) {
	}

	private record SortOperation(int[] slotIds) {
	}

	private static final class SortTask {
		private final AbstractContainerMenu handler;
		private final List<SortOperation> operations;
		private int index;

		private SortTask(AbstractContainerMenu handler, List<SortOperation> operations) {
			this.handler = handler;
			this.operations = operations;
		}

		private AbstractContainerMenu handler() {
			return handler;
		}

		@Nullable
		private SortOperation next() {
			if (index >= operations.size()) {
				return null;
			}
			return operations.get(index++);
		}

		private boolean done() {
			return index >= operations.size();
		}
	}

	private static final class QuickStackTask {
		private final AbstractContainerMenu handler;
		private final List<Integer> slotIds;
		private int index;

		private QuickStackTask(AbstractContainerMenu handler, List<Integer> slotIds) {
			this.handler = handler;
			this.slotIds = slotIds;
		}

		private AbstractContainerMenu handler() {
			return handler;
		}

		@Nullable
		private Integer next() {
			return index >= slotIds.size() ? null : slotIds.get(index++);
		}

		private boolean done() {
			return index >= slotIds.size();
		}
	}

	private record Bounds(int left, int top, int right, int bottom) {
		private Bounds include(int left, int top, int right, int bottom) {
			return new Bounds(
				Math.min(this.left, left),
				Math.min(this.top, top),
				Math.max(this.right, right),
				Math.max(this.bottom, bottom)
			);
		}
	}

	private record DragState(InventorySlotRef start, int startX, int startY) {
	}
}
