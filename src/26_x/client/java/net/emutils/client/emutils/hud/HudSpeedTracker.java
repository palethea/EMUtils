package net.emutils.client.emutils.hud;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Horizontal speed of whatever the player is moving with (the player, or the root vehicle when riding),
 * averaged over the last few ticks so the value stays readable.
 */
final class HudSpeedTracker {
	private static final int SAMPLE_COUNT = 10;
	// Anything faster than this in one tick is a teleport, not movement.
	private static final double TELEPORT_DISTANCE = 10.0;

	private static final double[] samples = new double[SAMPLE_COUNT];
	private static int sampleIndex;
	private static int sampleCount;
	private static Entity trackedEntity;
	private static double lastX;
	private static double lastZ;

	private HudSpeedTracker() {
	}

	static String collect(Minecraft client) {
		Entity entity = client.player.getRootVehicle();
		if (entity != trackedEntity) {
			trackedEntity = entity;
			lastX = entity.getX();
			lastZ = entity.getZ();
			sampleIndex = 0;
			sampleCount = 0;
		} else if (!client.isPaused()) {
			double dx = entity.getX() - lastX;
			double dz = entity.getZ() - lastZ;
			lastX = entity.getX();
			lastZ = entity.getZ();
			double distance = Math.sqrt(dx * dx + dz * dz);
			if (distance > TELEPORT_DISTANCE) {
				sampleIndex = 0;
				sampleCount = 0;
			} else {
				samples[sampleIndex] = distance;
				sampleIndex = (sampleIndex + 1) % SAMPLE_COUNT;
				sampleCount = Math.min(SAMPLE_COUNT, sampleCount + 1);
			}
		}

		if (sampleCount == 0) {
			return format(0.0);
		}

		double total = 0.0;
		for (int index = 0; index < sampleCount; index++) {
			total += samples[index];
		}
		return format(total / sampleCount * client.level.tickRateManager().tickrate());
	}

	private static String format(double blocksPerSecond) {
		return String.format(Locale.ENGLISH, "%.2f b/s", blocksPerSecond);
	}
}
