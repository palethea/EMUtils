package net.emutils.client.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FogRenderer.class, priority = 1100)
public abstract class FogRendererMixin {
	private static final float REMOVED_FOG_DISTANCE = 3.4028235E38F;

	@Shadow
	private GpuBuffer emptyBuffer;

	@Inject(
		method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
		at = @At("RETURN")
	)
	private void emutils$setupFogTweak(
		Camera camera,
		int viewDistance,
		DeltaTracker renderTickCounter,
		float tickProgress,
		ClientLevel clientWorld,
		CallbackInfoReturnable<FogData> cir
	) {
		if (EMUtilsClient.tweaks() != null) {
			EMUtilsClient.tweaks().updateFogState(camera, clientWorld);
			if (EMUtilsClient.tweaks().removeWorldFog()) {
				FogData fogData = cir.getReturnValue();
				fogData.environmentalStart = REMOVED_FOG_DISTANCE;
				fogData.environmentalEnd = REMOVED_FOG_DISTANCE;
				fogData.renderDistanceStart = REMOVED_FOG_DISTANCE;
				fogData.renderDistanceEnd = REMOVED_FOG_DISTANCE;
				fogData.skyEnd = REMOVED_FOG_DISTANCE;
				fogData.cloudEnd = REMOVED_FOG_DISTANCE;
			}
		}
	}

	@Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true)
	private void emutils$useEmptyWorldFog(FogRenderer.FogMode fogType, CallbackInfoReturnable<GpuBufferSlice> cir) {
		if (EMUtilsClient.tweaks() != null
			&& EMUtilsClient.tweaks().removeWorldFog()
			&& fogType == FogRenderer.FogMode.WORLD) {
			cir.setReturnValue(emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE));
		}
	}
}
