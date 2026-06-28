package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiFreeCamera26_1Mixin {
	@Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
	private void emutils$useSpectatorHudDuringFreeCamera(
		GuiGraphicsExtractor context,
		DeltaTracker deltaTracker,
		CallbackInfo ci
	) {
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freeCamera().isActive()) {
			ci.cancel();
		}
	}
}
