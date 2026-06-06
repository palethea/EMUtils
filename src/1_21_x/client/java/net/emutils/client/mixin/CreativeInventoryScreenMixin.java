package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {
	@Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
	private void emutils$guardCreativeInventoryToolSlotClick(@Nullable Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		PlayerInventory inventory = emutils$playerInventory();
		MinecraftClient client = MinecraftClient.getInstance();
		if (inventory == null || client == null) {
			return;
		}

		ScreenHandler handler = ((CreativeInventoryScreen) (Object) this).getScreenHandler();
		if (EMUtilsClient.inventoryTools().guardSlotClick(client, handler, slot, button, actionType, handler.getCursorStack(), inventory)) {
			ci.cancel();
		}
	}

	@Nullable
	private PlayerInventory emutils$playerInventory() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client == null || client.player == null ? null : client.player.getInventory();
	}
}
