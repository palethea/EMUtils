package net.emutils.client.mixin;

import net.emutils.client.emutils.render.BeaconRadiusRenderer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Notes right-clicked beacons, so Only Active Beacons can pick up an effect chosen in the beacon screen. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeBeaconMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"))
	private void emutils$noteUsedBeacon(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		BeaconRadiusRenderer.onBlockUsed(hit.getBlockPos());
	}
}
