package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class OwnNametagHelper {
	private OwnNametagHelper() {
	}

	public static boolean shouldShow(LivingEntity entity, double squaredDistanceToCamera) {
		if (!EMUtilsClient.config().tweakOwnNametag()) {
			return false;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;
		if (player == null || entity != player) {
			return false;
		}

		if (client.options.getPerspective().isFirstPerson()) {
			return false;
		}

		if (!MinecraftClient.isHudEnabled()) {
			return false;
		}

		if (entity.hasPassengers()) {
			return false;
		}

		if (entity.isSneaky() && squaredDistanceToCamera >= 1024.0D) {
			return false;
		}

		return !entity.isInvisibleTo(player);
	}
}
