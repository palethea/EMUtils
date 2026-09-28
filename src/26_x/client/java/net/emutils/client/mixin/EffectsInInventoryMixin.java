package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide Effects (#176): no effect list, and no effect tooltips, beside the survival and creative inventory. */
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void emutils$hideInventoryEffects(GuiGraphicsExtractor context, int mouseX, int mouseY, CallbackInfo ci) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config != null && config.hideInventoryEffects()) {
			ci.cancel();
		}
	}
}
