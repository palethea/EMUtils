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
		if (emutils$isFullbrightEnabled()) {
			flickerIntensity = 0.0F;
		}
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeInterpolator;get(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", ordinal = 0)
	)
	private Object emutils$fullbrightSkyLightColor(
		EnvironmentAttributeInterpolator interpolator,
		EnvironmentAttribute<?> attribute,
		float tickProgress
	) {
		return emutils$isFullbrightEnabled() ? Integer.valueOf(0xFFFFFF) : interpolator.get(attribute, tickProgress);
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/DimensionType;ambientLight()F")
	)
	private float emutils$fullbrightAmbientLight(DimensionType dimensionType) {
		return emutils$isFullbrightEnabled() ? 1.0F : dimensionType.ambientLight();
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeInterpolator;get(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", ordinal = 1)
	)
	private Object emutils$fullbrightSkyLightFactor(
		EnvironmentAttributeInterpolator interpolator,
		EnvironmentAttribute<?> attribute,
		float tickProgress
	) {
		return emutils$isFullbrightEnabled() ? Float.valueOf(0.0F) : interpolator.get(attribute, tickProgress);
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 1)
	)
	private Object emutils$fullbrightDarknessScale(SimpleOption<?> option) {
		return emutils$isFullbrightEnabled() ? Double.valueOf(0.0) : option.getValue();
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 2)
	)
	private Object emutils$fullbrightGamma(SimpleOption<?> option) {
		return emutils$isFullbrightEnabled() ? Double.valueOf(15.0) : option.getValue();
	}

	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getSkyDarkness(F)F")
	)
	private float emutils$fullbrightSkyDarkness(GameRenderer renderer, float tickProgress) {
		return emutils$isFullbrightEnabled() ? 0.0F : renderer.getSkyDarkness(tickProgress);
	}

	@Unique
	private static boolean emutils$isFullbrightEnabled() {
		return EMUtilsClient.config() != null && EMUtilsClient.config().tweakFullbright();
	}
}
