package net.emutils.client.emutils.debug;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;

public final class SmokeLaunchVerifier {
	private static final String ENABLED_PROPERTY = "emutils.smokeLaunch";
	private static final int MIN_TICKS = 80;
	private static final int MAX_TICKS = 1200;

	private static boolean enabled;
	private static boolean registered;
	private static int ticks;

	private SmokeLaunchVerifier() {
	}

	public static void registerIfEnabled() {
		enabled = Boolean.getBoolean(ENABLED_PROPERTY);
		registered = enabled;
		ticks = 0;
		if (enabled) {
			EMUtilsClient.LOGGER.info("EMUtils smoke launch verifier enabled.");
		}
	}

	public static void tick(Minecraft client) {
		if (!registered) {
			return;
		}

		ticks++;
		if (ticks == MIN_TICKS) {
			EMUtilsClient.LOGGER.info("EMUtils smoke launch verifier reached client tick loop; stopping Minecraft.");
			client.stop();
			registered = false;
			return;
		}

		if (ticks > MAX_TICKS) {
			registered = false;
			throw new IllegalStateException("EMUtils smoke launch verifier timed out before clean shutdown");
		}
	}
}
