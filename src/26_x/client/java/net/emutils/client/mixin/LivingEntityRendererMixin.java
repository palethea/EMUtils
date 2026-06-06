package net.emutils.client.mixin;

import net.emutils.client.emutils.tweaks.OwnNametagHelper;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
	@Inject(method = "shouldShowName", at = @At("RETURN"), cancellable = true)
	private void emutils$showOwnNametagInThirdPerson(T livingEntity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && OwnNametagHelper.shouldShow(livingEntity, squaredDistanceToCamera)) {
			cir.setReturnValue(true);
		}
	}
}
