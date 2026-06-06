package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.food.FoodHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.emhelpers.client.hud.layout.HudLayoutEditorContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "renderFood", at = @At("HEAD"))
	private void emutils$renderFoodExhaustion(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
		FoodHudRenderer.renderExhaustion(context, player, top, right);
	}

	@Inject(method = "renderFood", at = @At("TAIL"))
	private void emutils$renderFoodOverlays(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
		FoodHudRenderer.renderOverlays(context, player, top, right, ((InGameHud) (Object) this).getTicks());
	}

	@Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
	private void emutils$hideSpyglassOverlay(DrawContext context, float scale, CallbackInfo ci) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().tweakNoSpyglassOverlay()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
	private void emutils$hidePortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().tweakNoNausea()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
	private void emutils$hideNauseaOverlay(DrawContext context, float strength, CallbackInfo ci) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().tweakNoNausea()) {
			ci.cancel();
		}
	}

	@Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"))
	private void emutils$beginVanillaHudDimForLayoutEditor(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (HudLayoutEditorContext.isActive(MinecraftClient.getInstance())) {
			HudLayoutEditorContext.beginVanillaHudDim();
		}
	}

	@Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("RETURN"))
	private void emutils$endVanillaHudDimForLayoutEditor(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		HudLayoutEditorContext.endVanillaHudDim();
	}

	@Redirect(
		method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;hudHidden:Z", ordinal = 0)
	)
	private boolean emutils$hideHudWhileZoomingFirst(GameOptions options) {
		return EMUtilsClient.zoom() == null
			? options.hudHidden
			: EMUtilsClient.zoom().shouldHideHudWhileZooming(options.hudHidden);
	}

	@Redirect(
		method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;hudHidden:Z", ordinal = 1)
	)
	private boolean emutils$hideHudWhileZoomingSecond(GameOptions options) {
		return EMUtilsClient.zoom() == null
			? options.hudHidden
			: EMUtilsClient.zoom().shouldHideHudWhileZooming(options.hudHidden);
	}
}
