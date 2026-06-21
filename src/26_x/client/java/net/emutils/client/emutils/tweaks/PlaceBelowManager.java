package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class PlaceBelowManager {
	@Nullable
	private KeyMapping keyMapping;

	public void setKeyMapping(KeyMapping keyMapping) {
		this.keyMapping = keyMapping;
	}

	public BlockHitResult redirect(BlockHitResult original) {
		if (EMUtilsClient.config() == null
			|| !EMUtilsClient.config().tweakPlaceBelow()
			|| keyMapping == null
			|| !keyMapping.isDown()) {
			return original;
		}

		BlockPos target = original.getBlockPos();
		Vec3 underside = new Vec3(
			target.getX() + 0.5D,
			target.getY(),
			target.getZ() + 0.5D
		);
		return new BlockHitResult(underside, Direction.DOWN, target, false);
	}
}
