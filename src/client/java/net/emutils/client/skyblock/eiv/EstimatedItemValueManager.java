package net.emutils.client.skyblock.eiv;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.skyblock.SkyblockFeatures;
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
		EMUtilsConfig config = EMUtilsClient.config();
		MinecraftClient client = MinecraftClient.getInstance();
		if (config == null || !config.skyblockEnabled() || !config.estimatedItemValueHudEnabled()) {
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

		current = EstimatedItemValueCalculator.calculate(stack, tooltip, config);
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
