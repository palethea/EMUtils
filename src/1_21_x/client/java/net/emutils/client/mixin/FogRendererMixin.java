package net.emutils.client.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.llamalad7.mixinextras.sugar.Local;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(value = FogRenderer.class, priority = 1100)
public abstract class FogRendererMixin {
	private static final float REMOVED_FOG_DISTANCE = 3.4028235E38F;
	private static final String APPLY_FOG_1_21_11 =
		"applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;";
	private static final String APPLY_FOG_1_21_9 =
		"applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;";

	@Shadow
	private GpuBuffer emptyBuffer;

	@Shadow
	private void applyFog(
		ByteBuffer buffer,
		int bufPos,
		Vector4f fogColor,
		float environmentalStart,
		float environmentalEnd,
		float renderDistanceStart,
		float renderDistanceEnd,
		float skyEnd,
		float cloudEnd
	) {
	}

	@Inject(
		method = { APPLY_FOG_1_21_11, APPLY_FOG_1_21_9 },
		at = @At("HEAD")
	)
	private void emutils$rememberFogState(CallbackInfoReturnable<Vector4f> cir) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (EMUtilsClient.tweaks() != null && client != null && client.world != null) {
			EMUtilsClient.tweaks().updateFogState(client.gameRenderer.getCamera(), client.world);
		}
	}

	@Inject(
		method = { APPLY_FOG_1_21_11, APPLY_FOG_1_21_9 },
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V",
			shift = At.Shift.BEFORE
		)
	)
	private void emutils$expandFogData(
		CallbackInfoReturnable<Vector4f> cir,
		@Local FogData fogData
	) {
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().removeWorldFog()) {
			fogData.environmentalStart = REMOVED_FOG_DISTANCE;
			fogData.environmentalEnd = REMOVED_FOG_DISTANCE;
			fogData.renderDistanceStart = REMOVED_FOG_DISTANCE;
			fogData.renderDistanceEnd = REMOVED_FOG_DISTANCE;
			fogData.skyEnd = REMOVED_FOG_DISTANCE;
			fogData.cloudEnd = REMOVED_FOG_DISTANCE;
		}
	}

	@Redirect(
		method = { APPLY_FOG_1_21_11, APPLY_FOG_1_21_9 },
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
		)
	)
	private void emutils$writeExpandedFog(
		FogRenderer instance,
		ByteBuffer buffer,
		int bufPos,
		Vector4f fogColor,
		float environmentalStart,
		float environmentalEnd,
		float renderDistanceStart,
		float renderDistanceEnd,
		float skyEnd,
		float cloudEnd
	) {
		if (EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().removeWorldFog()) {
			environmentalStart = REMOVED_FOG_DISTANCE;
			environmentalEnd = REMOVED_FOG_DISTANCE;
			renderDistanceStart = REMOVED_FOG_DISTANCE;
			renderDistanceEnd = REMOVED_FOG_DISTANCE;
			skyEnd = REMOVED_FOG_DISTANCE;
			cloudEnd = REMOVED_FOG_DISTANCE;
		}

		this.applyFog(
			buffer,
			bufPos,
			fogColor,
			environmentalStart,
			environmentalEnd,
			renderDistanceStart,
			renderDistanceEnd,
			skyEnd,
			cloudEnd
		);
	}

	@Inject(method = "getFogBuffer", at = @At("HEAD"), cancellable = true)
	private void emutils$useEmptyWorldFog(FogRenderer.FogType fogType, CallbackInfoReturnable<GpuBufferSlice> cir) {
		if (EMUtilsClient.tweaks() != null
			&& EMUtilsClient.tweaks().removeWorldFog()
			&& fogType == FogRenderer.FogType.WORLD) {
			cir.setReturnValue(emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE));
		}
	}
}
