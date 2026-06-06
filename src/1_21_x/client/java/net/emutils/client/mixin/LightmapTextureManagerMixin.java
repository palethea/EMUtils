package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeInterpolator;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
	@Shadow
	private float flickerIntensity;

	@Inject(method = "tick", at = @At("TAIL"))
	private void emutils$disableFullbrightFlicker(CallbackInfo ci) {
		float strength = emutils$fullbrightStrength();
		if (strength > 0.0F) {
			flickerIntensity *= 1.0F - strength;
		}
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeInterpolator;get(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", ordinal = 0),
		require = 0
	)
	private Object emutils$fullbrightSkyLightColor(
		EnvironmentAttributeInterpolator interpolator,
		EnvironmentAttribute<?> attribute,
		float tickProgress
	) {
		Object original = interpolator.get(attribute, tickProgress);
		float strength = emutils$fullbrightStrength();
		return strength > 0.0F && original instanceof Integer color
			? Integer.valueOf(emutils$blendColor(color, 0xFFFFFF, strength))
			: original;
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/DimensionType;ambientLight()F"),
		require = 0
	)
	private float emutils$fullbrightAmbientLight(DimensionType dimensionType) {
		float original = dimensionType.ambientLight();
		return emutils$blend(original, 1.0F, emutils$fullbrightStrength());
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeInterpolator;get(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", ordinal = 1),
		require = 0
	)
	private Object emutils$fullbrightSkyLightFactor(
		EnvironmentAttributeInterpolator interpolator,
		EnvironmentAttribute<?> attribute,
		float tickProgress
	) {
		Object original = interpolator.get(attribute, tickProgress);
		float strength = emutils$fullbrightStrength();
		return strength > 0.0F && original instanceof Float value
			? Float.valueOf(emutils$blend(value, 0.0F, strength))
			: original;
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 1),
		require = 0
	)
	private Object emutils$fullbrightDarknessScale(SimpleOption<?> option) {
		Object original = option.getValue();
		float strength = emutils$fullbrightStrength();
		return strength > 0.0F && original instanceof Double value
			? Double.valueOf(emutils$blend(value, 0.0, strength))
			: original;
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 2),
		require = 0
	)
	private Object emutils$fullbrightGamma(SimpleOption<?> option) {
		Object original = option.getValue();
		float strength = emutils$fullbrightStrength();
		return strength > 0.0F && original instanceof Double value
			? Double.valueOf(emutils$blend(value, 15.0, strength))
			: original;
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getSkyDarkness(F)F"),
		require = 0
	)
	private float emutils$fullbrightSkyDarkness(GameRenderer renderer, float tickProgress) {
		return emutils$blend(renderer.getSkyDarkness(tickProgress), 0.0F, emutils$fullbrightStrength());
	}

	@Unique
	private static float emutils$fullbrightStrength() {
		return EMUtilsClient.config() == null ? 0.0F : EMUtilsClient.config().tweakFullbrightStrengthFactor();
	}

	@Unique
	private static float emutils$blend(float original, float target, float strength) {
		return original + (target - original) * strength;
	}

	@Unique
	private static double emutils$blend(double original, double target, float strength) {
		return original + (target - original) * strength;
	}

	@Unique
	private static int emutils$blendColor(int original, int target, float strength) {
		int red = emutils$blendChannel(original >> 16, target >> 16, strength);
		int green = emutils$blendChannel(original >> 8, target >> 8, strength);
		int blue = emutils$blendChannel(original, target, strength);
		return red << 16 | green << 8 | blue;
	}

	@Unique
	private static int emutils$blendChannel(int original, int target, float strength) {
		return Math.max(0, Math.min(255, Math.round((original & 0xFF) + ((target & 0xFF) - (original & 0xFF)) * strength)));
	}
}
