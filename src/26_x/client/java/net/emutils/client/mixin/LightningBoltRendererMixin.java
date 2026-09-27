package net.emutils.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clear Weather's Hide Lightning Bolts: skips drawing the bolt; the entity, its fire and damage are untouched. */
@Mixin(LightningBoltRenderer.class)
public abstract class LightningBoltRendererMixin {
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LightningBoltRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
	private void emutils$hideLightningBolt(LightningBoltRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (EMUtilsClient.config().shouldHideLightningBolts()) {
			ci.cancel();
		}
	}
}
