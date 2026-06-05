package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
	@Redirect(
		method = "renderOverlays",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/InGameOverlayRenderer;renderUnderwaterOverlay(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"
		)
	)
	private static void emutils$skipUnderwaterOverlay(
		MinecraftClient client,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers
	) {
		if (EMUtilsClient.config().tweakClearUnderwater()) {
			return;
		}

		InGameOverlayRendererAccessor.emutils$renderUnderwaterOverlay(client, matrices, vertexConsumers);
	}

	@Redirect(
		method = "renderOverlays",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/InGameOverlayRenderer;renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V"
		)
	)
	private static void emutils$skipFireOverlay(
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		Sprite sprite
	) {
		if (EMUtilsClient.config().tweakNoFireOverlay()) {
			return;
		}

		InGameOverlayRendererAccessor.emutils$renderFireOverlay(matrices, vertexConsumers, sprite);
	}
}
