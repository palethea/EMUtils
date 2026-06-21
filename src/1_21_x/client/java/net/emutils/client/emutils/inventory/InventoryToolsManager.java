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
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
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
	private static final int SORT_BUTTON_BORDER = 0xFF5A5A5A;
	private static final int SORT_BUTTON_FILL = 0xFFBEBEBE;
	private static final int SORT_BUTTON_FILL_HOVERED = 0xFFD0D0D0;
	private static final int SORT_BUTTON_FILL_DISABLED = 0xFF9B9B9B;
	private static final int SORT_BUTTON_HIGHLIGHT = 0xFFF2F2F2;
	private static final int SORT_BUTTON_SHADOW = 0xFF7A7A7A;
	private static final Identifier SORT_NAME_ICON = Identifier.of(EMUtilsClient.MOD_ID, "textures/gui/inventory/sort_name.png");
	private static final Identifier SORT_CATEGORY_ICON = Identifier.of(EMUtilsClient.MOD_ID, "textures/gui/inventory/sort_category.png");
	private static final Identifier SORT_QUANTITY_ICON = Identifier.of(EMUtilsClient.MOD_ID, "textures/gui/inventory/sort_quantity.png");

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
	private final Set<Integer> hoverTransferSlots = new HashSet<>();
	private boolean hoverTransferActive;
	private boolean slotLockKeyConsumed;
	@Nullable
	private SortTask sortTask;
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
		runQueuedSort(client);

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
		clearHoverTransfer();
		sortTask = null;
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
			return actionType == SlotActionType.PICKUP_ALL && hasLockedMatchingStack(handler, playerInventory, cursorStack);
		}

		return switch (actionType) {
			case QUICK_MOVE, THROW, SWAP, PICKUP_ALL, CLONE -> true;
			case PICKUP -> blocksPickup(slot, cursorStack);
			case QUICK_CRAFT -> slot.hasStack() && !cursorStack.isEmpty() && !ItemStack.areItemsAndComponentsEqual(slot.getStack(), cursorStack);
		};
	}

	public boolean handleMouseReleased(ScreenHandler handler, @Nullable Slot slot, PlayerInventory playerInventory) {
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
		MinecraftClient client,
		ScreenHandler handler,
		@Nullable Slot slot,
		int button,
		boolean shiftDown,
		PlayerInventory playerInventory
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
		MinecraftClient client,
		ScreenHandler handler,
		@Nullable Slot slot,
		int button,
		boolean shiftDown,
		PlayerInventory playerInventory
	) {
		if (!hoverTransferActive) {
			return false;
		}
		if (!hoverTransferAvailable() || button != 0 || !shiftDown || client.player == null || client.interactionManager == null) {
			clearHoverTransfer();
			return false;
		}

		tryHoverTransferSlot(client, handler, slot, playerInventory);
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

	public void drawSortButtons(
		MinecraftClient client,
		DrawContext context,
		ScreenHandler handler,
		PlayerInventory playerInventory,
		int screenX,
		int screenY,
		int mouseX,
		int mouseY
	) {
		if (!sortButtonsAvailable(client, handler, playerInventory)) {
			return;
		}

		boolean enabled = handler.getCursorStack().isEmpty() && sortTask == null;
		for (SortButton button : sortButtons(handler, playerInventory)) {
			drawSortButton(context, button, screenX, screenY, enabled, button.contains(mouseX, mouseY));
		}
	}

	public boolean handleSortButtonMouseClicked(
		MinecraftClient client,
		ScreenHandler handler,
		int button,
		double mouseX,
		double mouseY,
		PlayerInventory playerInventory
	) {
		if (button != 0 || !sortButtonsAvailable(client, handler, playerInventory) || !handler.getCursorStack().isEmpty()) {
			return false;
		}

		for (SortButton sortButton : sortButtons(handler, playerInventory)) {
			if (sortButton.contains(mouseX, mouseY)) {
				startSort(client, handler, playerInventory, sortButton.target(), sortButton.mode());
				return true;
			}
		}

		return false;
	}

	private void startSort(
		MinecraftClient client,
		ScreenHandler handler,
		PlayerInventory playerInventory,
		SortTarget target,
		InventorySortMode mode
	) {
		if (client.player == null || client.interactionManager == null) {
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

	private void runQueuedSort(MinecraftClient client) {
		if (sortTask == null) {
			return;
		}
		if (client.player == null || client.interactionManager == null || !isStorageContainerScreen(client) || !sortTask.handler().getCursorStack().isEmpty()) {
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

	private void executeSortOperation(MinecraftClient client, ScreenHandler handler, SortOperation operation) {
		PlayerEntity player = client.player;
		if (player == null || client.interactionManager == null) {
			return;
		}

		for (int slotId : operation.slotIds()) {
			client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, player);
		}
	}

	private List<SortOperation> buildSortOperations(
		ScreenHandler handler,
		PlayerInventory playerInventory,
		SortTarget target,
		InventorySortMode mode,
		PlayerEntity player
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

			int max = targetStack.getMaxCount();
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
			.comparing((ItemStack stack) -> stack.getName().getString(), String.CASE_INSENSITIVE_ORDER)
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
		if (stack.get(DataComponentTypes.FOOD) != null) return 7;
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
		return Registries.ITEM.getId(stack.getItem()).toString();
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
		ScreenHandler handler,
		PlayerInventory playerInventory,
		SortTarget target,
		PlayerEntity player
	) {
		List<SortSlot> slots = new ArrayList<>();
		for (Slot slot : handler.slots) {
			if (!slot.isEnabled() || !belongsToTarget(slot, playerInventory, target)) {
				continue;
			}
			if (slot.hasStack() && slot.getStack().get(DataComponentTypes.BUNDLE_CONTENTS) != null) {
				continue;
			}
			if (isLocked(InventorySlotRef.from(handler, slot, playerInventory))) {
				continue;
			}
			if (slot.hasStack() && !slot.canTakeItems(player)) {
				continue;
			}

			slots.add(new SortSlot(slot.id, slot.getStack().isEmpty() ? ItemStack.EMPTY : slot.getStack().copy()));
		}
		return slots;
	}

	private List<SortButton> sortButtons(ScreenHandler handler, PlayerInventory playerInventory) {
		List<SortButton> buttons = new ArrayList<>();
		addSortButtons(buttons, handler, playerInventory, SortTarget.CONTAINER);
		addSortButtons(buttons, handler, playerInventory, SortTarget.PLAYER);
		return buttons;
	}

	private void addSortButtons(List<SortButton> buttons, ScreenHandler handler, PlayerInventory playerInventory, SortTarget target) {
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
	private Bounds boundsFor(ScreenHandler handler, PlayerInventory playerInventory, SortTarget target) {
		Bounds bounds = null;
		for (Slot slot : handler.slots) {
			if (!slot.isEnabled() || !belongsToTarget(slot, playerInventory, target)) {
				continue;
			}
			bounds = bounds == null
				? new Bounds(slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE)
				: bounds.include(slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE);
		}
		return bounds;
	}

	private boolean sortButtonsAvailable(MinecraftClient client, ScreenHandler handler, PlayerInventory playerInventory) {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null
			&& config.inventoryToolsEnabled()
			&& config.sortButtonsEnabled()
			&& client.player != null
			&& client.interactionManager != null
			&& isStorageContainerScreen(client)
			&& hasContainerSlots(handler, playerInventory);
	}

	private static boolean belongsToTarget(Slot slot, PlayerInventory playerInventory, SortTarget target) {
		boolean playerSlot = slot.inventory == playerInventory;
		return target == SortTarget.PLAYER ? playerSlot : !playerSlot;
	}

	private static boolean canMerge(ItemStack left, ItemStack right) {
		return !left.isEmpty() && !right.isEmpty() && ItemStack.areItemsAndComponentsEqual(left, right);
	}

	private static boolean sameStack(ItemStack left, ItemStack right) {
		if (left.isEmpty() || right.isEmpty()) {
			return left.isEmpty() && right.isEmpty();
		}
		return left.getCount() == right.getCount() && ItemStack.areItemsAndComponentsEqual(left, right);
	}

	private static void drawSortButton(DrawContext context, SortButton button, int screenX, int screenY, boolean enabled, boolean hovered) {
		int x = screenX + button.x();
		int y = screenY + button.y();
		int fill = enabled ? hovered ? SORT_BUTTON_FILL_HOVERED : SORT_BUTTON_FILL : SORT_BUTTON_FILL_DISABLED;
		context.fill(x, y, x + SORT_BUTTON_SIZE, y + SORT_BUTTON_SIZE, SORT_BUTTON_BORDER);
		context.fill(x + 1, y + 1, x + SORT_BUTTON_SIZE - 1, y + SORT_BUTTON_SIZE - 1, fill);
		context.fill(x + 1, y + 1, x + SORT_BUTTON_SIZE - 1, y + 2, SORT_BUTTON_HIGHLIGHT);
		context.fill(x + 1, y + 1, x + 2, y + SORT_BUTTON_SIZE - 1, SORT_BUTTON_HIGHLIGHT);
		context.fill(x + 1, y + SORT_BUTTON_SIZE - 2, x + SORT_BUTTON_SIZE - 1, y + SORT_BUTTON_SIZE - 1, SORT_BUTTON_SHADOW);
		context.fill(x + SORT_BUTTON_SIZE - 2, y + 1, x + SORT_BUTTON_SIZE - 1, y + SORT_BUTTON_SIZE - 1, SORT_BUTTON_SHADOW);
		drawSortIcon(context, button.mode(), x, y);
	}

	private static void drawSortIcon(DrawContext context, InventorySortMode mode, int x, int y) {
		int iconX = x + (SORT_BUTTON_SIZE - SORT_ICON_SIZE) / 2;
		int iconY = y + (SORT_BUTTON_SIZE - SORT_ICON_SIZE) / 2;
		context.drawTexture(
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

	private boolean hasLockedMatchingStack(ScreenHandler handler, PlayerInventory playerInventory, ItemStack targetStack) {
		if (targetStack.isEmpty()) {
			return false;
		}

		for (Slot slot : handler.slots) {
			if (slot.hasStack()
				&& isLocked(InventorySlotRef.from(handler, slot, playerInventory))
				&& ItemStack.areItemsAndComponentsEqual(slot.getStack(), targetStack)) {
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
		MinecraftClient client,
		ScreenHandler handler,
		int button,
		boolean shiftDown,
		PlayerInventory playerInventory
	) {
		return hoverTransferAvailable()
			&& button == 0
			&& shiftDown
			&& dragState == null
			&& client.player != null
			&& client.interactionManager != null
			&& handler.getCursorStack().isEmpty()
			&& isStorageContainerScreen(client)
			&& hasContainerSlots(handler, playerInventory);
	}

	private void tryHoverTransferSlot(
		MinecraftClient client,
		ScreenHandler handler,
		@Nullable Slot slot,
		PlayerInventory playerInventory
	) {
		PlayerEntity player = client.player;
		if (player == null || client.interactionManager == null || slot == null || !slot.hasStack() || !slot.canTakeItems(player)) {
			return;
		}

		int slotId = slot.id;
		if (slotId < 0 || !hoverTransferSlots.add(slotId)) {
			return;
		}

		if (isLocked(InventorySlotRef.from(handler, slot, playerInventory))) {
			return;
		}

		client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, player);
	}

	private void clearHoverTransfer() {
		hoverTransferActive = false;
		hoverTransferSlots.clear();
	}

	private static boolean isStorageContainerScreen(MinecraftClient client) {
		return client.currentScreen instanceof GenericContainerScreen
			|| client.currentScreen instanceof ShulkerBoxScreen
			|| client.currentScreen instanceof HopperScreen
			|| client.currentScreen instanceof Generic3x3ContainerScreen;
	}

	private static boolean hasContainerSlots(ScreenHandler handler, PlayerInventory playerInventory) {
		for (Slot slot : handler.slots) {
			if (slot.inventory != playerInventory) {
				return true;
			}
		}

		return false;
	}

	private boolean isBindKeyDown(MinecraftClient client) {
		if (slotBindKey == null) {
			return false;
		}

		InputUtil.Key key = ((KeyBindingAccess) slotBindKey).emhelpers$getBoundKey();
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

		InputUtil.Key key = ((KeyBindingAccess) slotLockKey).emhelpers$getBoundKey();
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

	private record SortSlot(int slotId, ItemStack stack) {
	}

	private record SortOperation(int[] slotIds) {
	}

	private static final class SortTask {
		private final ScreenHandler handler;
		private final List<SortOperation> operations;
		private int index;

		private SortTask(ScreenHandler handler, List<SortOperation> operations) {
			this.handler = handler;
			this.operations = operations;
		}

		private ScreenHandler handler() {
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
