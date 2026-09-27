package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.tweaks.SkyFlashAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelWeatherEffectsMixin implements SkyFlashAccess {
	@Shadow
	private int getSkyFlashTime() {
		throw new AssertionError();
	}

	@Inject(method = "tickWeatherEffects", at = @At("HEAD"), cancellable = true)
	private void emutils$hideWeatherParticlesAndSound(CallbackInfo ci) {
		if (EMUtilsClient.config().shouldHideClearWeatherRainEffects()) {
			ci.cancel();
		}
	}

	/**
	 * Hide Thunder Flash: the same hook as vanilla's Hide Lightning Flashes accessibility option, which
	 * keeps the sky from brightening and the lightmap from flashing. Only the visuals change.
	 */
	@Inject(method = "getSkyFlashTime", at = @At("RETURN"), cancellable = true)
	private void emutils$hideThunderFlash(CallbackInfoReturnable<Integer> cir) {
		if (cir.getReturnValueI() > 0 && EMUtilsClient.config().shouldHideThunderFlash()) {
			cir.setReturnValue(0);
		}
	}

	@Override
	public int emutils$visibleSkyFlashTime() {
		return getSkyFlashTime();
	}
}
