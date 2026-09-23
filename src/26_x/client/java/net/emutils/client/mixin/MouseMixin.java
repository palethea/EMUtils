package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
	@Redirect(
		method = "turnPlayer(D)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
	)
	private void emutils$turnFreeCamera(LocalPlayer player, double yawDelta, double pitchDelta) {
		if (EMUtilsClient.tweaks() == null
			|| !EMUtilsClient.tweaks().freeCamera().handleMouseTurn(yawDelta, pitchDelta)) {
			player.turn(yawDelta, pitchDelta);
		}
	}

	@Redirect(
		method = "turnPlayer(D)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z")
	)
	private boolean emutils$enableCinematicCameraWhileZooming(Options options) {
		return options.smoothCamera
			|| EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldUseCinematicCamera();
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void emutils$handleZoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().handleScroll(vertical)) {
			ci.cancel();
		}
	}
}
