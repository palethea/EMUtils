package net.emutils.client.versioned.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blocks dropping the selected hotbar item while its slot is locked. 26.2 drops through LocalPlayer. */
@Mixin(LocalPlayer.class)
public abstract class HotbarDropMixin {
	@Inject(method = "drop", at = @At("HEAD"), cancellable = true)
	private void emutils$blockLockedHotbarDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (EMUtilsClient.inventoryTools().isPlayerSlotLocked(player.getInventory().getSelectedSlot())) {
			cir.setReturnValue(false);
		}
	}
}
