package net.emutils.client.mixin;

import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.context.SkyblockFeatures;
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
		if (EMSkyblockSettings.skyblockEnabled()
			&& EMSkyblockSettings.skyblockHideInventoryStatusEffects()
			&& SkyblockFeatures.inSkyBlock(MinecraftClient.getInstance())) {
			ci.cancel();
		}
	}
}
