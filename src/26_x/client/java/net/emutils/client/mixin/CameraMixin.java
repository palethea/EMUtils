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
		float fov = cir.getReturnValue();
		// Free Camera's own FOV replaces the base FOV, and zooming still works on top of it.
		if (EMUtilsClient.tweaks() != null) {
			fov = EMUtilsClient.tweaks().freeCamera().fov(fov);
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().isZoomEffectActive()) {
			fov /= EMUtilsClient.zoom().zoomDivisor();
		}
		cir.setReturnValue(fov);
	}

	// Required, so a game version that moves these calls fails at launch instead of Freelook quietly stopping
	// (#258). The first call is for riding a minecart with the new minecart behavior, the second for the rest;
	// the later ones flip the camera around for third person and are left alone.
	@Redirect(
		method = "alignWithEntity",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0)
	)
	private void emutils$freelookRotationInMinecart(Camera camera, float yaw, float pitch) {
		emutils$setFreelookOrVanillaRotation(camera, yaw, pitch);
	}

	@Redirect(
		method = "alignWithEntity",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1)
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
