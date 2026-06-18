package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelWeatherEffectsMixin {
	@Inject(method = "tickWeatherEffects", at = @At("HEAD"), cancellable = true)
	private void emutils$hideWeatherParticlesAndSound(CallbackInfo ci) {
		if (EMUtilsClient.config().shouldHideClearWeatherRainEffects()) {
			ci.cancel();
		}
	}
}
