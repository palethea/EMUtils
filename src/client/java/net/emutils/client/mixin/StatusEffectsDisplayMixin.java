package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatusEffectsDisplay.class)
public abstract class StatusEffectsDisplayMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void emutils$hideInventoryStatusEffects(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
		EMUtilsConfig config = EMUtilsClient.config();
		MinecraftClient client = MinecraftClient.getInstance();
		if (config == null || client == null) {
			return;
		}

		if (config.skyblockEnabled()
			&& config.skyblockHideInventoryStatusEffects()
			&& SkyblockFeatures.inSkyBlock(client)) {
			ci.cancel();
		}
	}
}
