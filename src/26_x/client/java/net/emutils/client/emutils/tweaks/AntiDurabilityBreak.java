package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.minecraft.world.item.ItemStack;

public final class AntiDurabilityBreak {
	public static final int MINIMUM_DURABILITY = 5;

	private AntiDurabilityBreak() {
	}

	public static boolean protects(ItemStack stack) {
		return EMUtilsClient.config() != null
			&& EMUtilsClient.config().tweakAntiDurabilityBreak()
			&& !stack.isEmpty()
			&& stack.isDamageableItem()
			&& stack.getMaxDamage() - stack.getDamageValue() <= MINIMUM_DURABILITY;
	}
}
