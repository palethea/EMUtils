package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
	@Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
	private void emutils$blockLockedHotbarDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
		ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
		if (EMUtilsClient.inventoryTools().isPlayerSlotLocked(player.getInventory().getSelectedSlot())) {
			cir.setReturnValue(false);
		}
	}
}
