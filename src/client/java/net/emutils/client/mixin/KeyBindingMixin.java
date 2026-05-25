package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.accessor.KeyBindingAccess;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
	@Inject(method = "wasPressed", at = @At("RETURN"), cancellable = true)
	private void emutils$suppressWhenScriptBindingMatches(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) {
			return;
		}
		if (EMUtilsClient.minescriptKeybinds().shouldSuppressKeyBinding((KeyBinding) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
	private void emutils$suppressHeldWhenScriptBindingMatches(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) {
			return;
		}
		if (EMUtilsClient.minescriptKeybinds().shouldSuppressKeyBinding((KeyBinding) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
