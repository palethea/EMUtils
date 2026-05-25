package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
	@Redirect(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 2)
	)
	private Object emutils$fullbrightGamma(SimpleOption<?> option) {
		return EMUtilsClient.config().tweakFullbright() ? Double.valueOf(15.0) : option.getValue();
	}
}
