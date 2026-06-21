package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
public abstract class HudFreeCameraMixin {
	@Redirect(
		method = "getCameraPlayer",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;")
	)
	private Entity emutils$useRealPlayerForFreeCameraHud(Minecraft client) {
		if (EMUtilsClient.tweaks() != null
			&& EMUtilsClient.tweaks().freeCamera().isActive()
			&& client.player != null) {
			return client.player;
		}
		return client.getCameraEntity();
	}
}
