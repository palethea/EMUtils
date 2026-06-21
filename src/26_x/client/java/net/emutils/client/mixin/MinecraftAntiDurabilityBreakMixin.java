package net.emutils.client.mixin;

import net.emutils.client.emutils.tweaks.AntiDurabilityBreak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftAntiDurabilityBreakMixin {
	@Shadow
	public LocalPlayer player;

	@Shadow
	public MultiPlayerGameMode gameMode;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void emutils$blockProtectedAttack(CallbackInfoReturnable<Boolean> cir) {
		if (emutils$protectsMainHand()) {
			gameMode.stopDestroyBlock();
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void emutils$blockProtectedBreaking(boolean leftClick, CallbackInfo ci) {
		if (emutils$protectsMainHand()) {
			gameMode.stopDestroyBlock();
			ci.cancel();
		}
	}

	private boolean emutils$protectsMainHand() {
		return player != null && gameMode != null && AntiDurabilityBreak.protects(player.getMainHandItem());
	}
}
