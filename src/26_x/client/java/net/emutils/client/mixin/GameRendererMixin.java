package net.emutils.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Redirect(
		method = "renderItemInHand",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;hideGui:Z"),
		require = 0
	)
	private boolean emutils$hideHandWhileZooming(OptionsRenderState options) {
		boolean hidden = Minecraft.getInstance().gui.hud.isHidden();
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freeCamera().isActive()) {
			return true;
		}
		return EMUtilsClient.zoom() == null
			? hidden
			: EMUtilsClient.zoom().shouldHideHandWhileZooming(hidden);
	}

	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void emutils$disableHurtCamera(CameraRenderState cameraRenderState, PoseStack matrices, CallbackInfo ci) {
		if (EMUtilsClient.config().tweakNoHurtCam()) {
			ci.cancel();
		}
	}

	@ModifyExpressionValue(
		method = "renderLevel",
		at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F")
	)
	private float emutils$disableNauseaPortalProjection(float strength) {
		return EMUtilsClient.config() != null && EMUtilsClient.config().tweakNoNausea() ? 0.0F : strength;
	}
}
