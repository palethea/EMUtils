package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherRenderingMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void emutils$hideWeather(ClientLevel world, float tickProgress, Vec3 cameraPos, WeatherRenderState state, CallbackInfo ci) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config.shouldHideClearWeatherRain() && config.shouldHideClearWeatherSnow()) {
			state.intensity = 0.0F;
			state.rainColumns.clear();
			state.snowColumns.clear();
			ci.cancel();
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void emutils$filterWeatherPieces(ClientLevel world, float tickProgress, Vec3 cameraPos, WeatherRenderState state, CallbackInfo ci) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (!config.tweakClearWeather()) {
			return;
		}
		if (config.tweakClearWeatherHideRain()) {
			state.rainColumns.clear();
		}
		if (config.tweakClearWeatherHideSnow()) {
			state.snowColumns.clear();
		}
		if (state.rainColumns.isEmpty() && state.snowColumns.isEmpty()) {
			state.intensity = 0.0F;
		}
	}
}
