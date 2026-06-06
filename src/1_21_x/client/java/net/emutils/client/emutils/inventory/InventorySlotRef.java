package net.emutils.client.emutils.inventory;

import java.util.Objects;
import net.emutils.client.mixin.CreativeSlotAccess;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public record InventorySlotRef(Kind kind, int screenIdentity, String handlerType, int slotId, int inventoryIndex) {
	public enum Kind {
		PLAYER,
		SCREEN
	}

	public static InventorySlotRef from(ScreenHandler handler, Slot slot, PlayerInventory playerInventory) {
		Slot resolved = resolveSlot(slot);
		if (resolved.inventory == playerInventory) {
			int index = resolved.getIndex();
			return new InventorySlotRef(Kind.PLAYER, 0, "", index, index);
		}

		return new InventorySlotRef(Kind.SCREEN, System.identityHashCode(handler), handler.getClass().getName(), resolved.id, resolved.getIndex());
	}

	private static Slot resolveSlot(Slot slot) {
		if (slot instanceof CreativeSlotAccess creativeSlot) {
			return creativeSlot.emutils$backingSlot();
		}

		return slot;
	}

	public static InventorySlotRef forPlayerIndex(int inventoryIndex) {
		return new InventorySlotRef(Kind.PLAYER, 0, "", inventoryIndex, inventoryIndex);
	}

	public boolean isHotbar() {
		return kind == Kind.PLAYER && inventoryIndex >= 0 && inventoryIndex < PlayerInventory.HOTBAR_SIZE;
	}

	public int hotbarButton() {
		return isHotbar() ? inventoryIndex : -1;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof InventorySlotRef other)) {
			return false;
		}
		if (kind == Kind.PLAYER && other.kind == Kind.PLAYER) {
			return inventoryIndex == other.inventoryIndex;
		}

		return kind == other.kind
			&& screenIdentity == other.screenIdentity
			&& handlerType.equals(other.handlerType)
			&& slotId == other.slotId
			&& inventoryIndex == other.inventoryIndex;
	}

	@Override
	public int hashCode() {
		if (kind == Kind.PLAYER) {
			return Objects.hash(kind, inventoryIndex);
		}

		return Objects.hash(kind, screenIdentity, handlerType, slotId, inventoryIndex);
	}
}
