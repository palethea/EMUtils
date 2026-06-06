package net.emutils.client.mixin;

import net.emutils.client.emutils.capes.CustomCapeManager;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void emutils$showCustomCape(
		Avatar playerLikeEntity,
		AvatarRenderState playerEntityRenderState,
		float partialTick,
		CallbackInfo ci
	) {
		if (!(playerLikeEntity instanceof Player player)) {
			return;
		}

		if (CustomCapeManager.hasCustomCape(player.getGameProfile())) {
			playerEntityRenderState.showCape = true;
		}
	}
}
