package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class OwnNametagHelper {
	private OwnNametagHelper() {
	}

	public static boolean shouldShow(LivingEntity entity, double squaredDistanceToCamera) {
		if (!EMUtilsClient.config().tweakOwnNametag()) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		Player player = client.player;
		if (player == null || entity != player) {
			return false;
		}

		if (client.options.getCameraType().isFirstPerson()) {
			return false;
		}

		if (net.emutils.client.emutils.compat.MinecraftClientCompat.isHudHidden(client)) {
			return false;
		}

		if (entity.isVehicle()) {
			return false;
		}

		if (entity.isCrouching() && squaredDistanceToCamera >= 1024.0D) {
			return false;
		}

		return !entity.isInvisibleTo(player);
	}
}
