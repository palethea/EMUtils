package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Low Totem (#193): draws a Totem of Undying you hold lower in first person, by moving the hand render down like {@link LowShield}. */
public final class LowTotem {
	/** How far down the totem goes at 100%, in the first-person view's units (a block is 1). */
	private static final float MAX_OFFSET = 0.35F;

	private LowTotem() {
	}

	/** How far to move the held item down: 0 unless it is a Totem of Undying and the tweak is on. */
	public static float offset(ItemStack stack) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || !config.tweakLowTotem() || !stack.is(Items.TOTEM_OF_UNDYING)) {
			return 0.0F;
		}
		return config.lowTotemAmount() / 100.0F * MAX_OFFSET;
	}
}
