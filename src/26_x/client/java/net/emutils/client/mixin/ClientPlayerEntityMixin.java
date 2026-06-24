package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
	@Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
	private void emutils$continuePlayerSimulationDuringFreeCamera(CallbackInfoReturnable<Boolean> cir) {
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freeCamera().isActive()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "drop", at = @At("HEAD"), cancellable = true)
	private void emutils$blockLockedHotbarDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (EMUtilsClient.inventoryTools().isPlayerSlotLocked(player.getInventory().getSelectedSlot())) {
			cir.setReturnValue(false);
		}
	}
}
