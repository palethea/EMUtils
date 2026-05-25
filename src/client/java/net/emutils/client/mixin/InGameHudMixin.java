package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.skyblock.SkyblockActionBarManager;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.emutils.client.hud.layout.HudLayoutEditorContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@ModifyVariable(method = "setOverlayMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Text emutils$processSkyblockActionBar(Text message) {
		SkyblockActionBarManager manager = EMUtilsClient.skyblockActionBar();
		return manager == null ? message : manager.processOverlayMessage(message);
	}

	@Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true)
	private static void emutils$hideSkyblockHealthBar(
		DrawContext context,
		PlayerEntity player,
		int x,
		int y,
		int lines,
		int regeneratingHeartIndex,
		float health,
		int maxHealth,
		int lastHealth,
		int absorption,
		boolean blinking,
		CallbackInfo ci
	) {
		if (SkyblockFeatures.hideVanillaStatusBars(MinecraftClient.getInstance())) {
			ci.cancel();
		}
	}

	@Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
	private static void emutils$hideSkyblockFood(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
		if (SkyblockFeatures.hideVanillaStatusBars(MinecraftClient.getInstance())) {
			ci.cancel();
		}
	}

	@Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
	private static void emutils$hideSkyblockArmor(
		DrawContext context,
		PlayerEntity player,
		int y,
		int heartRows,
		int healthBarLines,
		int x,
		CallbackInfo ci
	) {
		if (SkyblockFeatures.hideVanillaStatusBars(MinecraftClient.getInstance())) {
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
