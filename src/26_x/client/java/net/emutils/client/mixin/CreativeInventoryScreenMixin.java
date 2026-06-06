package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<AbstractContainerMenu> {
	protected CreativeInventoryScreenMixin(AbstractContainerMenu handler, Inventory inventory, net.minecraft.network.chat.Component title) {
		super(handler, inventory, title);
	}

	@Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
	private void emutils$guardCreativeInventoryToolSlotMouseButtonEvent(@Nullable Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
		Inventory inventory = emutils$playerInventory();
		Minecraft client = Minecraft.getInstance();
		if (inventory == null || client == null) {
			return;
		}

		AbstractContainerMenu handler = this.menu;
		if (EMUtilsClient.inventoryTools().guardSlotMouseButtonEvent(client, handler, slot, button, actionType, handler.getCarried(), inventory)) {
			ci.cancel();
		}
	}

	@Nullable
	private Inventory emutils$playerInventory() {
		Minecraft client = Minecraft.getInstance();
		return client == null || client.player == null ? null : client.player.getInventory();
	}
}
