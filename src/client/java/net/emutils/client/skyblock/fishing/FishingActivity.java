package net.emutils.client.skyblock.fishing;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public final class FishingActivity {
	private static final long PICKUP_VISIBLE_MS = 3_000L;
	private static final long CAST_GRACE_MS = 120_000L;
	private static final long SEA_CREATURE_GRACE_MS = 120_000L;
	private static final double SEA_CREATURE_AREA_DISTANCE_SQ = 12.0D * 12.0D;

	private static long lastCastMs;
	private static long lastCatchMs;
	private static long lastSeaCreatureHookMs;
	private static Vec3d lastSeaCreatureHookPos;

	private FishingActivity() {
	}

	public static void onBobberCast() {
		lastCastMs = System.currentTimeMillis();
	}

	public static void onCatch() {
		lastCatchMs = System.currentTimeMillis();
	}

	public static void onSeaCreatureHook(MinecraftClient client) {
		PlayerEntity player = client.player;
		if (player == null) {
			return;
		}

		lastSeaCreatureHookMs = System.currentTimeMillis();
		lastSeaCreatureHookPos = positionOf(player);
		onCatch();
	}

	public static void clear() {
		lastCastMs = 0L;
		lastCatchMs = 0L;
		lastSeaCreatureHookMs = 0L;
		lastSeaCreatureHookPos = null;
	}

	public static boolean isFishing(MinecraftClient client) {
		PlayerEntity player = client.player;
		if (player == null) {
			return false;
		}

		if (hasActiveBobber(player)) {
			return true;
		}

		long now = System.currentTimeMillis();
		if (lastCatchMs > 0L && now - lastCatchMs <= PICKUP_VISIBLE_MS) {
			return true;
		}

		if (isInRecentSeaCreatureArea(player, now)) {
			return true;
		}

		return lastCastMs > 0L && now - lastCastMs <= CAST_GRACE_MS && isHoldingRod(player);
	}

	public static boolean isFishingStrict(MinecraftClient client) {
		PlayerEntity player = client.player;
		return player != null && hasActiveBobber(player);
	}

	private static boolean hasActiveBobber(PlayerEntity player) {
		FishingBobberEntity bobber = player.fishHook;
		return bobber != null && bobber.isAlive();
	}

	public static boolean isHoldingRod(PlayerEntity player) {
		return player.getMainHandStack().isOf(Items.FISHING_ROD) || player.getOffHandStack().isOf(Items.FISHING_ROD);
	}

	private static boolean isInRecentSeaCreatureArea(PlayerEntity player, long now) {
		if (lastSeaCreatureHookMs <= 0L || now - lastSeaCreatureHookMs > SEA_CREATURE_GRACE_MS) {
			return false;
		}

		return lastSeaCreatureHookPos != null
			&& positionOf(player).squaredDistanceTo(lastSeaCreatureHookPos) <= SEA_CREATURE_AREA_DISTANCE_SQ;
	}

	private static Vec3d positionOf(PlayerEntity player) {
		return new Vec3d(player.getX(), player.getY(), player.getZ());
	}
}
