package net.emutils.client.mixin;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.tweaks.TweaksManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererMixin {
	@ModifyVariable(
		method = "setupTerrain",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 0
	)
	private FogParameters emutils$removeSodiumTerrainFog(FogParameters fogParameters) {
		TweaksManager tweaks = EMUtilsClient.tweaks();
		Minecraft client = Minecraft.getInstance();
		if (tweaks == null || client.level == null || client.gameRenderer == null) {
			return fogParameters;
		}

		Camera camera = client.gameRenderer.getMainCamera();
		if (camera == null) {
			return fogParameters;
		}

		tweaks.updateFogState(camera, client.level);
		return tweaks.removeWorldFog() ? FogParameters.NONE : fogParameters;
	}
}
