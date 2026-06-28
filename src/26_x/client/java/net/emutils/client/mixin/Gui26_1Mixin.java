package net.emutils.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.lang.reflect.InvocationTargetException;
import net.emhelpers.client.hud.layout.HudLayoutEditorContext;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.food.FoodHudRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class Gui26_1Mixin {
	@Inject(method = "extractFood", at = @At("HEAD"))
	private void emutils$renderFoodExhaustion(GuiGraphicsExtractor context, Player player, int top, int right, CallbackInfo ci) {
		FoodHudRenderer.renderExhaustion(context, player, top, right);
	}

	@Inject(method = "extractFood", at = @At("TAIL"))
	private void emutils$renderFoodOverlays(GuiGraphicsExtractor context, Player player, int top, int right, CallbackInfo ci) {
		FoodHudRenderer.renderOverlays(context, player, top, right, emutils$getGuiTicks());
	}

	private int emutils$getGuiTicks() {
		try {
			return (Integer) Gui.class.getMethod("getGuiTicks").invoke(this);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException("Missing Gui.getGuiTicks on Minecraft 26.1.x", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException("Failed to call Gui.getGuiTicks on Minecraft 26.1.x", cause);
		}
	}

	@Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
	private void emutils$hideSpyglassOverlay(GuiGraphicsExtractor context, float scale, CallbackInfo ci) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().tweakNoSpyglassOverlay()) {
			ci.cancel();
		}
	}

	@Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
	private void emutils$hidePortalOverlay(GuiGraphicsExtractor context, float nauseaStrength, CallbackInfo ci) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().tweakNoNausea()) {
			ci.cancel();
		}
	}

	@Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
	private void emutils$hideNauseaOverlay(GuiGraphicsExtractor context, float strength, CallbackInfo ci) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().tweakNoNausea()) {
			ci.cancel();
		}
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"))
	private void emutils$beginVanillaHudDimForLayoutEditor(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
		if (HudLayoutEditorContext.isActive(Minecraft.getInstance())) {
			HudLayoutEditorContext.beginVanillaHudDim();
		}
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("RETURN"))
	private void emutils$endVanillaHudDimForLayoutEditor(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
		HudLayoutEditorContext.endVanillaHudDim();
	}

	@ModifyExpressionValue(
		method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;isHidden:Z", ordinal = 0)
	)
	private boolean emutils$hideHudWhileZoomingRenderState(boolean hidden) {
		return EMUtilsClient.zoom() == null
			? hidden
			: EMUtilsClient.zoom().shouldHideHudWhileZooming(hidden);
	}

	@ModifyExpressionValue(
		method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;isHidden:Z", ordinal = 1)
	)
	private boolean emutils$hideHudWhileZoomingFirstGate(boolean hidden) {
		return EMUtilsClient.zoom() == null
			? hidden
			: EMUtilsClient.zoom().shouldHideHudWhileZooming(hidden);
	}

	@ModifyExpressionValue(
		method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;isHidden:Z", ordinal = 2)
	)
	private boolean emutils$hideHudWhileZoomingSecondGate(boolean hidden) {
		return EMUtilsClient.zoom() == null
			? hidden
			: EMUtilsClient.zoom().shouldHideHudWhileZooming(hidden);
	}
}
