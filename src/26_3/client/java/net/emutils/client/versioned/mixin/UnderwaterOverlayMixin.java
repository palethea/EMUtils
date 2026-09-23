package net.emutils.client.versioned.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Clear Underwater: skips the first-person underwater overlay. */
@Mixin(ScreenEffectRenderer.class)
public abstract class UnderwaterOverlayMixin {
	@Shadow
	private static void submitWater(
		PlayerRenderState.WaterOverlay waterOverlay,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector
	) {
	}

	@Redirect(
		method = "submit",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;submitWater(Lnet/minecraft/client/renderer/state/level/PlayerRenderState$WaterOverlay;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
		)
	)
	private static void emutils$skipUnderwaterOverlay(
		PlayerRenderState.WaterOverlay waterOverlay,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector
	) {
		if (EMUtilsClient.config().tweakClearUnderwater()) {
			return;
		}

		submitWater(waterOverlay, poseStack, submitNodeCollector);
	}
}
