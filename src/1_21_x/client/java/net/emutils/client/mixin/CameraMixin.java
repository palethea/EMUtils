package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1)
	)
	private void emutils$freelookRotation(Camera instance, float yaw, float pitch) {
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freelook().isActive()) {
			((CameraMixin) (Object) instance).setRotation(
				EMUtilsClient.tweaks().freelook().cameraYaw(),
				EMUtilsClient.tweaks().freelook().cameraPitch()
			);
			return;
		}

		((CameraMixin) (Object) instance).setRotation(yaw, pitch);
	}
}
