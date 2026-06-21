package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputFreeCameraMixin {
	@Inject(method = "tick", at = @At("RETURN"))
	private void emutils$freezePlayerInput(CallbackInfo ci) {
		if (EMUtilsClient.tweaks() == null || !EMUtilsClient.tweaks().freeCamera().isActive()) {
			return;
		}
		ClientInput input = (ClientInput) (Object) this;
		input.keyPresses = Input.EMPTY;
		((ClientInputAccessor) input).emutils$setMoveVector(Vec2.ZERO);
	}
}
