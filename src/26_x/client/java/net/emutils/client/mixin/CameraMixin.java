package net.emutils.client.mixin;

import java.lang.reflect.Field;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void emutils$disableFreeCameraOcclusionInSolidBlocks(
		CameraRenderState renderState,
		float partialTick,
		CallbackInfo ci
	) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null
			&& EMUtilsClient.tweaks() != null
			&& EMUtilsClient.tweaks().freeCamera().isActive()
			&& client.level.getBlockState(renderState.blockPos).isSolidRender()) {
			emutils$setSmartCull(renderState, false);
		}
	}

	private static void emutils$setSmartCull(CameraRenderState renderState, boolean enabled) {
		try {
			Field field = CameraRenderState.class.getField("smartCull");
			field.setBoolean(renderState, enabled);
		} catch (NoSuchFieldException ignored) {
			// 26.1.x has no smart-cull flag; its camera render state uses the older culling path.
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Could not update camera smart culling", e);
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
