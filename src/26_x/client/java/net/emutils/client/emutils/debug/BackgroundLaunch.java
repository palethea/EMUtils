package net.emutils.client.emutils.debug;

import net.minecraft.client.Minecraft;

/**
 * Automated test launches (smoke tests and UI snapshots) run in the background, so the developer can
 * keep using the computer: the game never grabs the mouse, which would lock the cursor to the middle of
 * the screen, and it doesn't pause when its window loses focus. Gradle also asks SDL not to focus the
 * window when it opens. Manual test worlds keep the normal behavior.
 */
public final class BackgroundLaunch {
	private static final boolean ACTIVE = Boolean.getBoolean("emutils.backgroundLaunch");

	private BackgroundLaunch() {
	}

	public static boolean active() {
		return ACTIVE;
	}

	/** Call every client tick. The option is only changed in memory, not saved. */
	public static void tick(Minecraft client) {
		if (ACTIVE && client.options.pauseOnLostFocus) {
			client.options.pauseOnLostFocus = false;
		}
	}
}
