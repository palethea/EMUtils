package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
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
