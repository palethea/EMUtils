package net.emutils.client.skyblock.tracker;

import net.emutils.client.hud.layout.HudElementId;
import net.emutils.client.skyblock.fishing.tracker.profit.FishingProfitTrackerManager;
import net.emutils.client.skyblock.fishing.tracker.seacreature.SeaCreatureTrackerManager;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class TrackerHudClickHandler {
	private TrackerHudClickHandler() {
	}

	public static boolean handleClick(MinecraftClient client, double mouseX, double mouseY) {
		if (client == null || client.player == null || client.world == null) {
			return false;
		}

		TrackerHudHitbox hitbox = TrackerHudHitbox.atModeClick(mouseX, mouseY);
		if (hitbox == null) {
			return false;
		}

		if (hitbox.elementId() == HudElementId.SEA_CREATURE_TRACKER) {
			SeaCreatureTrackerManager.cycleDisplayMode();
			return true;
		} else if (hitbox.elementId() == HudElementId.FISHING_PROFIT_TRACKER) {
			FishingProfitTrackerManager.cycleDisplayMode();
			return true;
		}

		return false;
	}

	public static double @Nullable [] scaledMouse(MinecraftClient client) {
		if (client.mouse == null || client.getWindow() == null) {
			return null;
		}

		double scale = client.getWindow().getScaleFactor();
		return new double[] {
			client.mouse.getX() / scale,
			client.mouse.getY() / scale
		};
	}
}
