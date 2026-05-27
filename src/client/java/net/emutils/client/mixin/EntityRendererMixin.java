package net.emutils.client.mixin;

import net.emutils.client.skyblock.fishing.FishingHookDisplayManager;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void emutils$hideFishingHookStand(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		if (FishingHookDisplayManager.shouldHideHookStand(entity)) {
			cir.setReturnValue(false);
		}
	}
}
