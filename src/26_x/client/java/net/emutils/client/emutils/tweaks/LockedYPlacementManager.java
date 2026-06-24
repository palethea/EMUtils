package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public final class LockedYPlacementManager {
	@Nullable
	private KeyMapping keyMapping;
	@Nullable
	private Integer lockedY;
	private boolean suppressNextUseItemOnPacket;

	public void setKeyMapping(KeyMapping keyMapping) {
		this.keyMapping = keyMapping;
	}

	public void tick(Minecraft client) {
		while (keyMapping != null && keyMapping.consumeClick()) {
			handleKeybind(client);
		}
	}

	public void reset() {
		lockedY = null;
		suppressNextUseItemOnPacket = false;
	}

	public boolean active() {
		return enabled() && lockedY != null;
	}

	@Nullable
	public Integer lockedY() {
		return active() ? lockedY : null;
	}

	public boolean shouldBlockPlacement(ItemStack stack, BlockHitResult hit) {
		return shouldBlockPlacement(stack, hit.getBlockPos(), hit.getDirection());
	}

	public boolean shouldBlockPlacement(ItemStack stack, BlockPos clickedPos, Direction clickedFace) {
		if (!active() || !(stack.getItem() instanceof BlockItem)) {
			return false;
		}

		BlockPos placementPos = clickedPos.relative(clickedFace);
		return placementPos.getY() != lockedY;
	}

	public void markBlockedPlacementPacket() {
		suppressNextUseItemOnPacket = true;
	}

	public boolean consumeBlockedPlacementPacket() {
		if (!suppressNextUseItemOnPacket) {
			return false;
		}

		suppressNextUseItemOnPacket = false;
		return true;
	}

	private void handleKeybind(Minecraft client) {
		if (!enabled() || client == null || client.player == null || client.level == null) {
			reset();
			return;
		}

		HitResult hit = client.hitResult;
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			reset();
			return;
		}

		lockedY = blockHit.getBlockPos().getY();
	}

	private static boolean enabled() {
		return EMUtilsClient.config() != null && EMUtilsClient.config().tweakLockedYPlacement();
	}

	public static InteractionResult blockedResult() {
		return InteractionResult.FAIL;
	}
}
