package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.particle.ParticlesMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherRendering.class)
public abstract class WeatherRenderingMixin {
	@Inject(method = "buildPrecipitationPieces", at = @At("HEAD"), cancellable = true)
	private void emutils$hideWeather(World world, int ticks, float tickProgress, Vec3d cameraPos, WeatherRenderState state, CallbackInfo ci) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config.shouldHideClearWeatherRain() && config.shouldHideClearWeatherSnow()) {
			state.intensity = 0.0F;
			state.rainPieces.clear();
			state.snowPieces.clear();
			ci.cancel();
		}
	}

	@Inject(method = "buildPrecipitationPieces", at = @At("TAIL"))
	private void emutils$filterWeatherPieces(World world, int ticks, float tickProgress, Vec3d cameraPos, WeatherRenderState state, CallbackInfo ci) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (!config.tweakClearWeather()) {
			return;
		}
		if (config.tweakClearWeatherHideRain()) {
			state.rainPieces.clear();
		}
		if (config.tweakClearWeatherHideSnow()) {
			state.snowPieces.clear();
		}
		if (state.rainPieces.isEmpty() && state.snowPieces.isEmpty()) {
			state.intensity = 0.0F;
		}
	}

	@Inject(method = "addParticlesAndSound", at = @At("HEAD"), cancellable = true)
	private void emutils$hideWeatherParticles(ClientWorld world, Camera camera, int ticks, ParticlesMode particlesMode, int weatherRadius, CallbackInfo ci) {
		if (EMUtilsClient.config().shouldHideClearWeatherRainEffects()) {
			ci.cancel();
		}
	}
}
