package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererFreeCamera26_1Mixin {
	@Redirect(
		method = "extractVisibleEntities",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/Camera;entity()Lnet/minecraft/world/entity/Entity;",
			ordinal = 3
		)
	)
	private Entity emutils$renderRealPlayerDuringFreeCamera(Camera camera) {
		Minecraft client = Minecraft.getInstance();
		if (EMUtilsClient.tweaks() != null
			&& EMUtilsClient.tweaks().freeCamera().isActive()
			&& client.player != null) {
			return client.player;
		}
		return camera.entity();
	}
}
