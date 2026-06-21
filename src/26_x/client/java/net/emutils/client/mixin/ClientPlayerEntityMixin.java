package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void emutils$freezePlayerDuringFreeCamera(MoverType type, Vec3 movement, CallbackInfo ci) {
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freeCamera().isActive()) {
			ci.cancel();
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
