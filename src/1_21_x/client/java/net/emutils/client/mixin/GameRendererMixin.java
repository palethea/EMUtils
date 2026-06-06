package net.emutils.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void emutils$applyZoom(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		if (!changingFov || EMUtilsClient.zoom() == null || !EMUtilsClient.zoom().isZoomEffectActive()) {
			return;
		}

		cir.setReturnValue(cir.getReturnValue() / EMUtilsClient.zoom().zoomDivisor());
	}

	@Redirect(
		method = "renderHand",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;hudHidden:Z")
	)
	private boolean emutils$hideHandWhileZooming(GameOptions options) {
		return EMUtilsClient.zoom() == null
			? options.hudHidden
			: EMUtilsClient.zoom().shouldHideHandWhileZooming(options.hudHidden);
	}

	@Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
	private void emutils$disableHurtCamera(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
		if (EMUtilsClient.config().tweakNoHurtCam()) {
			ci.cancel();
		}
	}

	@ModifyExpressionValue(
		method = "renderWorld",
		at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F")
	)
	private float emutils$disableNauseaPortalProjection(float strength) {
		return EMUtilsClient.config() != null && EMUtilsClient.config().tweakNoNausea() ? 0.0F : strength;
	}
}
