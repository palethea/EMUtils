package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.Hud")
public abstract class HudFreeCameraMixin {
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
