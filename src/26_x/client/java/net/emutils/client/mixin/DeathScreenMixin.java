package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {
	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$captureDeath(CallbackInfo ci) {
		EMUtilsClient.waypoint().captureDeath(Minecraft.getInstance());
	}
}
