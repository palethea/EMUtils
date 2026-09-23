package net.emutils.client.versioned.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Free Camera: turns off smart culling while the free camera is inside a solid block. */
@Mixin(Camera.class)
public abstract class CameraRenderStateMixin {
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
			renderState.smartCull = false;
		}
	}
}
