package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void emutils$applyZoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().isZoomEffectActive()) {
			cir.setReturnValue(cir.getReturnValue() / EMUtilsClient.zoom().zoomDivisor());
		}
	}

	@Redirect(
		method = "alignWithEntity",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1),
		require = 0
	)
	private void emutils$freelookRotation(Camera camera, float yaw, float pitch) {
		emutils$setFreelookOrVanillaRotation(camera, yaw, pitch);
	}

	private void emutils$setFreelookOrVanillaRotation(Camera camera, float yaw, float pitch) {
		if (EMUtilsClient.tweaks() != null) {
			EMUtilsClient.tweaks().freelook().updateCamera(Minecraft.getInstance());
		}

		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freelook().isActive()) {
			((CameraMixin) (Object) camera).setRotation(
				EMUtilsClient.tweaks().freelook().cameraYaw(),
				EMUtilsClient.tweaks().freelook().cameraPitch()
			);
			return;
		}

		((CameraMixin) (Object) camera).setRotation(yaw, pitch);
	}
}
