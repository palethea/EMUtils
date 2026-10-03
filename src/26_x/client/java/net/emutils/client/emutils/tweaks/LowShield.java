package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Low Shield (#193): draws the shield you are blocking with lower in first person. The raised pose is part of
 * the shield's own item model, so the whole hand render is moved down instead; blocking is unaffected.
 */
public final class LowShield {
	/** How far down the shield goes at 100%, in the first-person view's units (a block is 1). */
	private static final float MAX_OFFSET = 0.5F;

	private LowShield() {
	}

	/** How far to move the item held in {@code hand} down: 0 unless it is a shield being held up with the tweak on. */
	public static float offset(InteractionHand hand, ItemStack stack) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || !config.tweakLowShield() || client.player == null || !(stack.getItem() instanceof ShieldItem)) {
			return 0.0F;
		}
		if (!client.player.isUsingItem() || client.player.getUsedItemHand() != hand) {
			return 0.0F;
		}
		return config.lowShieldAmount() / 100.0F * MAX_OFFSET;
	}
}
