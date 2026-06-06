package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
	@Shadow
	private float blockLightFlicker;

	@Inject(method = "tick", at = @At("TAIL"))
	private void emutils$disableFullbrightFlicker(CallbackInfo ci) {
		float strength = emutils$fullbrightStrength();
		if (strength > 0.0F) {
			blockLightFlicker *= 1.0F - strength;
		}
	}

	@Inject(method = "extract", at = @At("RETURN"))
	private void emutils$applyFullbright(LightmapRenderState state, float tickProgress, CallbackInfo ci) {
		float strength = emutils$fullbrightStrength();
		if (strength > 0.0F) {
			state.brightness = emutils$blend(state.brightness, 55.0F, strength);
			state.darknessEffectScale = emutils$blend(state.darknessEffectScale, 0.0F, strength);

			if (state.ambientColor != null) {
				float r = emutils$blend(state.ambientColor.x(), 1.0F, strength);
				float g = emutils$blend(state.ambientColor.y(), 1.0F, strength);
				float b = emutils$blend(state.ambientColor.z(), 1.0F, strength);
				state.ambientColor = new org.joml.Vector3f(r, g, b);
			}

			if (state.skyLightColor != null) {
				float r = emutils$blend(state.skyLightColor.x(), 1.0F, strength);
				float g = emutils$blend(state.skyLightColor.y(), 1.0F, strength);
				float b = emutils$blend(state.skyLightColor.z(), 1.0F, strength);
				state.skyLightColor = new org.joml.Vector3f(r, g, b);
			}
		}
	}

	@Unique
	private static float emutils$fullbrightStrength() {
		return EMUtilsClient.config() == null ? 0.0F : EMUtilsClient.config().tweakFullbrightStrengthFactor();
	}

	@Unique
	private static float emutils$blend(float original, float target, float strength) {
		return original + (target - original) * strength;
	}
}
