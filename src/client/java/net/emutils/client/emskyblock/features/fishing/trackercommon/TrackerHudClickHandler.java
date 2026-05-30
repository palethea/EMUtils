package net.emutils.client.emskyblock.features.fishing.trackercommon;

import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emskyblock.features.fishing.profittracker.FishingProfitTrackerManager;
import net.emutils.client.emskyblock.features.fishing.seacreaturetracker.SeaCreatureTrackerManager;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class TrackerHudClickHandler {
	private TrackerHudClickHandler() {
	}

	public static boolean handleClick(MinecraftClient client, double mouseX, double mouseY) {
		return handleClick(client, mouseX, mouseY, false);
	}

	public static boolean handleClick(MinecraftClient client, double mouseX, double mouseY, boolean rightClick) {
		if (client == null || client.player == null || client.world == null) {
			return false;
		}

		TrackerHudHitbox hitbox = TrackerHudHitbox.atModeClick(mouseX, mouseY);
		if (hitbox == null) {
			return false;
		}

		if (hitbox.elementId() == HudElementId.SEA_CREATURE_TRACKER) {
			if (rightClick) {
				SeaCreatureTrackerManager.resetCurrentMode();
			} else {
				SeaCreatureTrackerManager.cycleDisplayMode();
			}
			return true;
		} else if (hitbox.elementId() == HudElementId.FISHING_PROFIT_TRACKER) {
			if (rightClick) {
				FishingProfitTrackerManager.resetCurrentMode();
			} else {
				FishingProfitTrackerManager.cycleDisplayMode();
			}
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
