package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fullbright with a shader pack on (#263). Shader packs light the world themselves instead of using the vanilla
 * lightmap that Fullbright brightens, but they brighten it for Night Vision through Iris's {@code nightVision}
 * uniform, which packs use for fullbright mods too. So Fullbright raises that uniform to its strength. Only
 * applied with Iris installed (see EMUtilsMixinPlugin); the target is named, since Iris isn't on the classpath.
 */
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public abstract class IrisCommonUniformsMixin {
	@Inject(method = "getNightVision", at = @At("RETURN"), cancellable = true)
	private static void emutils$fullbrightNightVision(CallbackInfoReturnable<Float> cir) {
		float strength = EMUtilsClient.config() == null ? 0.0F : EMUtilsClient.config().tweakFullbrightStrengthFactor();
		if (strength > cir.getReturnValueF()) {
			cir.setReturnValue(strength);
		}
	}
}
