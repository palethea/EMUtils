package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Every leaves block with falling leaf particles is one of these two classes: tinted leaves (oak, birch,
// mangrove, ...) and leaves with their own particle (cherry, pale oak, and the 26.3 poplar leaves).
@Mixin({TintedParticleLeavesBlock.class, UntintedParticleLeavesBlock.class})
public abstract class FallingLeavesParticleMixin {
	@Inject(method = "spawnFallingLeavesParticle", at = @At("HEAD"), cancellable = true)
	private void emutils$hideFallingLeaves(Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (EMUtilsClient.config() != null && EMUtilsClient.config().tweakNoFallingLeafParticles()) {
			ci.cancel();
		}
	}
}
