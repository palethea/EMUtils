package net.emutils.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.lang.reflect.InvocationTargetException;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.food.FoodHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.emhelpers.client.hud.layout.HudLayoutEditorContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.Hud")
public abstract class InGameHudMixin {
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
			return (Integer) Class.forName("net.minecraft.client.gui.Hud").getMethod("getGuiTicks").invoke(this);
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException("Missing Hud.getGuiTicks on Minecraft 26.2", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException("Failed to call Hud.getGuiTicks on Minecraft 26.2", cause);
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
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Hud;isHidden:Z", ordinal = 0)
	)
	private boolean emutils$hideHudWhileZoomingRenderState(boolean hidden) {
		return EMUtilsClient.zoom() == null
			? hidden
			: EMUtilsClient.zoom().shouldHideHudWhileZooming(hidden);
	}

	@ModifyExpressionValue(
		method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Hud;isHidden:Z", ordinal = 1)
	)
	private boolean emutils$hideHudWhileZoomingFirstGate(boolean hidden) {
		return EMUtilsClient.zoom() == null
			? hidden
			: EMUtilsClient.zoom().shouldHideHudWhileZooming(hidden);
	}

	@ModifyExpressionValue(
		method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Hud;isHidden:Z", ordinal = 2)
	)
	private boolean emutils$hideHudWhileZoomingSecondGate(boolean hidden) {
		return EMUtilsClient.zoom() == null
			? hidden
			: EMUtilsClient.zoom().shouldHideHudWhileZooming(hidden);
	}
}
