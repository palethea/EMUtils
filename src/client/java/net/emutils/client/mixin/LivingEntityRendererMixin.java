package net.emutils.client.mixin;

import net.emutils.client.emskyblock.features.fishing.hookdisplay.FishingHookDisplayManager;
import net.emutils.client.emutils.tweaks.OwnNametagHelper;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
	@Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
	private void emutils$hideFishingHookStandLabel(T livingEntity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
		if (FishingHookDisplayManager.shouldHideHookStand(livingEntity)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "hasLabel", at = @At("RETURN"), cancellable = true)
	private void emutils$showOwnNametagInThirdPerson(T livingEntity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && OwnNametagHelper.shouldShow(livingEntity, squaredDistanceToCamera)) {
			cir.setReturnValue(true);
		}
	}
}
