package net.emutils.client.mixin;

import net.emutils.client.emutils.gui.ui.UiBlur;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the new settings screen fade the menu blur in and out with itself (see {@link UiBlur}). */
@Mixin(Options.class)
public abstract class OptionsMenuBlurMixin {
	@Inject(method = "getMenuBackgroundBlurriness", at = @At("RETURN"), cancellable = true)
	private void emutils$fadeMenuBlur(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(UiBlur.apply(cir.getReturnValueI()));
	}
}
