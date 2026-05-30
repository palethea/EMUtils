package net.emutils.client.emskyblock.features.fishing.common;

import java.util.HashMap;
import java.util.Map;
import net.emutils.client.emskyblock.context.SkyblockFeatures;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.SkyblockItemAttributes;
import net.emutils.client.emskyblock.features.fishing.profittracker.FishingProfitTrackerManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public final class FishingInventoryPickupTracker {
	private static final Map<String, Long> LAST_COUNTS = new HashMap<>();
	private static int tickCooldown;

	private FishingInventoryPickupTracker() {
	}

	public static void tick(MinecraftClient client) {
		if (!EMSkyblockSettings.fishingProfitTrackerEnabled() || !SkyblockFeatures.inSkyBlock(client)) {
			LAST_COUNTS.clear();
			return;
		}

		if (!FishingActivity.isFishing(client) || client.player == null) {
			if (++tickCooldown > 40) {
				LAST_COUNTS.clear();
			}

			return;
		}

		tickCooldown = 0;
		if (client.player.age % 5 != 0) {
			return;
		}

		Map<String, Long> current = snapshot(client.player.getInventory());
		if (LAST_COUNTS.isEmpty()) {
			LAST_COUNTS.putAll(current);
			return;
		}

		Map<String, Long> gained = new HashMap<>();
		for (Map.Entry<String, Long> entry : current.entrySet()) {
			long previous = LAST_COUNTS.getOrDefault(entry.getKey(), 0L);
			long delta = entry.getValue() - previous;
			if (delta > 0L) {
				gained.put(entry.getKey(), delta);
			}
		}

		if (!gained.isEmpty()) {
			FishingProfitTrackerManager.onItemPickups(gained);
		}

		LAST_COUNTS.clear();
		LAST_COUNTS.putAll(current);
	}

	private static Map<String, Long> snapshot(PlayerInventory inventory) {
		Map<String, Long> counts = new HashMap<>();
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isEmpty()) {
				continue;
			}

			String itemId = SkyblockItemAttributes.itemId(stack);
			if (itemId == null || itemId.isBlank()) {
				continue;
			}

			counts.merge(itemId, (long) stack.getCount(), Long::sum);
		}

		return counts;
	}
}
