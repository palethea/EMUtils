package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftFreeCameraInputMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void emutils$preventFreeCameraAttack(CallbackInfoReturnable<Boolean> cir) {
		if (active()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void emutils$preventFreeCameraAttackHold(boolean leftClick, CallbackInfo ci) {
		if (active()) {
			ci.cancel();
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void emutils$preventFreeCameraUse(CallbackInfo ci) {
		if (active()) {
			ci.cancel();
		}
	}

	@Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
	private void emutils$preventFreeCameraPick(CallbackInfo ci) {
		if (active()) {
			ci.cancel();
		}
	}

	private static boolean active() {
		return EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freeCamera().isActive();
	}
}
