package net.emutils.client.emskyblock.features.inventory.estimateditemvalue;

import java.util.List;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.context.SkyblockFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class EstimatedItemValueManager {
	private static final long STALE_MS = 500L;

	private static EstimatedItemValueManager instance;

	private EstimatedItemValueResult current = EstimatedItemValueResult.empty();
	private long lastHoverMs;

	private EstimatedItemValueManager() {
	}

	public static EstimatedItemValueManager get() {
		if (instance == null) {
			instance = new EstimatedItemValueManager();
		}

		return instance;
	}

	public EstimatedItemValueResult updateHoveredItem(ItemStack stack, List<Text> tooltip) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.estimatedItemValueHudEnabled()) {
			clear();
			return EstimatedItemValueResult.empty();
		}
		if (!SkyblockFeatures.inSkyBlock(client)) {
			clear();
			return EstimatedItemValueResult.empty();
		}

		if (stack.isEmpty()) {
			clear();
			return EstimatedItemValueResult.empty();
		}

		current = EstimatedItemValueCalculator.calculate(stack, tooltip);
		lastHoverMs = current.isEmpty() ? 0L : System.currentTimeMillis();
		return current;
	}

	public void tick() {
		if (lastHoverMs <= 0L) {
			return;
		}

		if (System.currentTimeMillis() - lastHoverMs > STALE_MS) {
			clear();
		}
	}

	public EstimatedItemValueResult current() {
		if (lastHoverMs <= 0L || System.currentTimeMillis() - lastHoverMs > STALE_MS) {
			return EstimatedItemValueResult.empty();
		}

		return current;
	}

	public void clear() {
		current = EstimatedItemValueResult.empty();
		lastHoverMs = 0L;
	}
}
