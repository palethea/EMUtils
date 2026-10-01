package net.emutils.client.emutils.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/** The spot a waypoint is added at with the crosshair (#105). */
public final class WaypointTarget {
	/** Farther than the game's reach: the crosshair can aim at a far hill, as long as its chunks are loaded. */
	private static final double MAX_DISTANCE = 512.0D;

	private WaypointTarget() {
	}

	/**
	 * The block space in front of the face the crosshair is on, which is where you would stand on the ground
	 * you look at; null when it points at nothing in range.
	 */
	public static @Nullable SharedWaypoint lookedAt(Minecraft client) {
		if (client.player == null || client.level == null) {
			return null;
		}
		HitResult hit = client.player.pick(MAX_DISTANCE, 1.0F, false);
		if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		BlockPos pos = block.getBlockPos().relative(block.getDirection());
		return SharedWaypoint.at(pos.getX(), pos.getY(), pos.getZ());
	}
}
