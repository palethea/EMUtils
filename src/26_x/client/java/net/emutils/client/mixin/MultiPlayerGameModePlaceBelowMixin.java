package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModePlaceBelowMixin {
	@ModifyVariable(method = "useItemOn", at = @At("HEAD"), argsOnly = true)
	private BlockHitResult emutils$placeBelowTarget(BlockHitResult original) {
		return EMUtilsClient.tweaks() == null
			? original
			: EMUtilsClient.tweaks().placeBelow().redirect(original);
	}
}
