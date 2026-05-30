package net.emutils.client.mixin;

import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EstimatedItemValueHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void emutils$renderEstimatedItemValueOverlay(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		context.createNewRootLayer();
		EstimatedItemValueHudRenderer.renderOverlay(context, MinecraftClient.getInstance());
	}
}
