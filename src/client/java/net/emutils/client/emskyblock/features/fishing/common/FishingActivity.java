package net.emutils.client.emskyblock.features.fishing.common;

import java.util.Set;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.context.SkyblockIsland;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.SkyblockItemAttributes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public final class FishingActivity {
	private static final long PICKUP_VISIBLE_MS = 3_000L;
	private static final long CAST_GRACE_MS = 120_000L;
	private static final long SEA_CREATURE_GRACE_MS = 120_000L;
	private static final double SEA_CREATURE_AREA_DISTANCE_SQ = 12.0D * 12.0D;

	private static final Set<SkyblockIsland> FISHING_ISLANDS = Set.of(
		SkyblockIsland.BACKWATER_BAYOU,
		SkyblockIsland.GALATEA,
		SkyblockIsland.LOTUS_ATOLL,
		SkyblockIsland.WINTER,
		SkyblockIsland.CRIMSON_ISLE,
		SkyblockIsland.THE_PARK,
		SkyblockIsland.MINING_ISLAND
	);

	private static final Set<String> FISHING_ARMOR_IDS = Set.of(
		"ANGLER_HELMET", "ANGLER_CHESTPLATE", "ANGLER_LEGGINGS", "ANGLER_BOOTS",
		"SALMON_HELMET", "SALMON_CHESTPLATE", "SALMON_LEGGINGS", "SALMON_BOOTS",
		"DIVER_HELMET", "DIVER_CHESTPLATE", "DIVER_LEGGINGS", "DIVER_BOOTS",
		"WATER_HYDRA_HEAD",
		"SPONGE_HELMET", "SPONGE_CHESTPLATE", "SPONGE_LEGGINGS", "SPONGE_BOOTS",
		"SHARK_SCALE_HELMET", "SHARK_SCALE_CHESTPLATE", "SHARK_SCALE_LEGGINGS", "SHARK_SCALE_BOOTS",
		"THUNDER_HELMET", "THUNDER_CHESTPLATE", "THUNDER_LEGGINGS", "THUNDER_BOOTS",
		"MAGMA_LORD_HELMET", "MAGMA_LORD_CHESTPLATE", "MAGMA_LORD_LEGGINGS", "MAGMA_LORD_BOOTS",
		"TERROR_HELMET", "TERROR_CHESTPLATE", "TERROR_LEGGINGS", "TERROR_BOOTS",
		"FISHING_HELMET_1", "FISHING_CHESTPLATE_1", "FISHING_LEGGINGS_1", "FISHING_BOOTS_1",
		"LAVA_FISHING_HELMET_1", "LAVA_FISHING_CHESTPLATE_1", "LAVA_FISHING_LEGGINGS_1", "LAVA_FISHING_BOOTS_1",
		"ICE_ROD_HELMET_1", "ICE_ROD_CHESTPLATE_1", "ICE_ROD_LEGGINGS_1", "ICE_ROD_BOOTS_1",
		"FAIRY_HELMET", "FAIRY_CHESTPLATE", "FAIRY_LEGGINGS", "FAIRY_BOOTS",
		"SQUID_BOOTS", "SLUG_BOOTS",
		"MOOGMA_HELMET", "MOOGMA_CHESTPLATE", "MOOGMA_LEGGINGS", "MOOGMA_BOOTS",
		"FLAMING_HELMET", "FLAMING_CHESTPLATE", "FLAMING_LEGGINGS", "FLAMING_BOOTS",
		"TAURUS_HELMET", "TAURUS_CHESTPLATE", "TAURUS_LEGGINGS", "TAURUS_BOOTS"
	);

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

	public static boolean isWearingFishingArmor(MinecraftClient client) {
		PlayerEntity player = client.player;
		if (player == null) {
			return false;
		}

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
				continue;
			}

			ItemStack stack = player.getEquippedStack(slot);
			if (stack.isEmpty()) {
				continue;
			}

			String itemId = SkyblockItemAttributes.itemId(stack);
			if (itemId != null && FISHING_ARMOR_IDS.contains(itemId.toUpperCase())) {
				return true;
			}
		}

		return false;
	}

	public static boolean isOnFishingIsland() {
		SkyblockIsland island = SkyblockContext.island();
		return island != null && FISHING_ISLANDS.contains(island);
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
