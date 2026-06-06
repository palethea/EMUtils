package net.emutils.client.mixin;

import net.emutils.client.emutils.capes.CustomCapeManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "updateRenderState", at = @At("TAIL"))
	private void emutils$showCustomCape(
		PlayerLikeEntity playerLikeEntity,
		PlayerEntityRenderState playerEntityRenderState,
		float partialTick,
		CallbackInfo ci
	) {
		if (!(playerLikeEntity instanceof PlayerEntity player)) {
			return;
		}

		if (CustomCapeManager.hasCustomCape(player.getGameProfile())) {
			playerEntityRenderState.capeVisible = true;
		}
	}
}
