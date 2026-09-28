package net.emutils.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.emutils.client.emutils.hud.ClickCounter;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts attack and use presses for the Keystrokes overlay's clicks per second (#43). */
@Mixin(KeyMapping.class)
public abstract class KeyMappingClickMixin {
	@Inject(method = "click", at = @At("HEAD"))
	private static void emutils$countClick(InputConstants.Key key, CallbackInfo ci) {
		ClickCounter.onPress(key);
	}
}
