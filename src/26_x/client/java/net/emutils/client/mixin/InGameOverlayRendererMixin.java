package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public abstract class InGameOverlayRendererMixin {
	@Redirect(
		method = "renderScreenEffect",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
		)
	)
	private static void emutils$skipUnderwaterOverlay(
		Minecraft client,
		PoseStack matrices,
		MultiBufferSource vertexConsumers
	) {
		if (EMUtilsClient.config().tweakClearUnderwater()) {
			return;
		}

		InGameOverlayRendererAccessor.emutils$renderWater(client, matrices, vertexConsumers);
	}

	@Redirect(
		method = "renderScreenEffect",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
		)
	)
	private static void emutils$skipFireOverlay(
		PoseStack matrices,
		MultiBufferSource vertexConsumers,
		TextureAtlasSprite sprite
	) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config != null && config.tweakNoFireOverlay()) {
			return;
		}

		if (config != null && config.tweakLowFireOverlay()) {
			matrices.pushPose();
			matrices.translate(0.0F, -0.38F, 0.0F);
			matrices.scale(1.0F, 0.65F, 1.0F);
			InGameOverlayRendererAccessor.emutils$renderFire(matrices, vertexConsumers, sprite);
			matrices.popPose();
			return;
		}

		InGameOverlayRendererAccessor.emutils$renderFire(matrices, vertexConsumers, sprite);
	}
}
