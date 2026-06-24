package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.access.MinecraftFastPlaceTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftFastPlaceMixin implements MinecraftFastPlaceTracker {
	@Shadow
	private int rightClickDelay;

	@Unique
	private boolean emutils$fastPlaceAttempted;

	@Inject(method = "startUseItem", at = @At("HEAD"))
	private void emutils$resetFastPlaceAttempt(CallbackInfo ci) {
		emutils$fastPlaceAttempted = false;
	}

	@Inject(method = "startUseItem", at = @At("RETURN"))
	private void emutils$clearFastPlaceOrUseCooldown(CallbackInfo ci) {
		if (emutils$fastPlaceAttempted
			|| (EMUtilsClient.config() != null && EMUtilsClient.config().tweakFastUse())) {
			rightClickDelay = 0;
		}
	}

	@Override
	public void emutils$markFastPlaceAttempt() {
		emutils$fastPlaceAttempted = true;
	}
}
