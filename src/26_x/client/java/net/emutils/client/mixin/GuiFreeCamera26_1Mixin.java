package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Gui.class)
public abstract class GuiFreeCamera26_1Mixin {
	@Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
	private void emutils$useSpectatorHudDuringFreeCamera(
		GuiGraphicsExtractor context,
		DeltaTracker deltaTracker,
		CallbackInfo ci
	) {
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freeCamera().shouldUseSpectatorHud()) {
			ci.cancel();
		}
	}

	@Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
	private void emutils$useRealPlayerForRegularFreeCameraHud(CallbackInfoReturnable<Player> cir) {
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freeCamera().shouldUseRegularHud()) {
			cir.setReturnValue(Minecraft.getInstance().player);
		}
	}
}
