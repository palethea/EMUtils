package net.emutils.client.emutils.tweaks;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class AntiDurabilityBreak {
	/** The same held item in the same slot only warns again after this many ticks. */
	private static final int WARNING_COOLDOWN_TICKS = 100;
	private static long ticks;
	private static int warnings;

	private static final HandWarning MAIN_HAND = new HandWarning();
	private static final HandWarning OFF_HAND = new HandWarning();

	private AntiDurabilityBreak() {
	}

	public static boolean protects(ItemStack stack) {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null
			&& config.tweakAntiDurabilityBreak()
			&& isAtOrBelow(stack, config.antiDurabilityUnit(), config.antiDurabilityProtectAt());
	}

	/** Whether a damageable {@code stack} has {@code threshold} (in {@code unit}) or less durability left. */
	public static boolean isAtOrBelow(ItemStack stack, AntiDurabilityUnit unit, int threshold) {
		return !stack.isEmpty()
			&& stack.isDamageableItem()
			&& unit.atOrBelow(stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage(), threshold);
	}

	/**
	 * Low durability warning: when a held item drops to Warn At, or a held item that's already that low
	 * is picked up or switched to, shows how much is left above the hotbar and plays a sound.
	 */
	public static void tick(Minecraft client) {
		ticks++;
		EMUtilsConfig config = EMUtilsClient.config();
		Player player = client.player;
		if (config == null || player == null || !config.tweakAntiDurabilityBreak() || !config.antiDurabilityWarning()) {
			MAIN_HAND.clear();
			OFF_HAND.clear();
			return;
		}

		ItemStack warned = MAIN_HAND.update(player, InteractionHand.MAIN_HAND, player.getInventory().getSelectedSlot(), config);
		if (warned == null) {
			warned = OFF_HAND.update(player, InteractionHand.OFF_HAND, -1, config);
		} else {
			OFF_HAND.update(player, InteractionHand.OFF_HAND, -1, config);
		}
		if (warned != null) {
			warn(client, warned);
		}
	}

	/** How many low durability warnings have been shown, for UI snapshot checks. */
	public static int warningsForSnapshot() {
		return warnings;
	}

	private static void warn(Minecraft client, ItemStack stack) {
		warnings++;
		int remaining = stack.getMaxDamage() - stack.getDamageValue();
		Component message = Component.translatable(EMUtilsTexts.ANTI_DURABILITY_WARNING, stack.getHoverName(), remaining)
			.withStyle(ChatFormatting.GOLD);
		client.gui.hud.setOverlayMessage(message, false);
		client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.6F));
	}

	/** What one hand held last tick, so a warning only fires when something changes. */
	private static final class HandWarning {
		private @Nullable String lastKey;
		private boolean lastLow;
		private @Nullable String lastWarnedKey;
		private long lastWarnTick;

		/** Returns the held stack if it should warn now, otherwise null. */
		@Nullable
		ItemStack update(Player player, InteractionHand hand, int slot, EMUtilsConfig config) {
			ItemStack stack = player.getItemInHand(hand);
			boolean low = isAtOrBelow(stack, config.antiDurabilityUnit(), config.antiDurabilityWarnAt());
			String key = stack.isEmpty() ? null : slot + ":" + stack.getItem();
			boolean becameLow = low && (!lastLow || !key.equals(lastKey));
			// Switching back and forth to the same low item, or mending flicking it above and below
			// Warn At, only warns again after a cooldown.
			boolean warn = becameLow && (!key.equals(lastWarnedKey) || ticks - lastWarnTick >= WARNING_COOLDOWN_TICKS);
			lastKey = key;
			lastLow = low;
			if (!warn) {
				return null;
			}
			lastWarnedKey = key;
			lastWarnTick = ticks;
			return stack;
		}

		void clear() {
			lastKey = null;
			lastLow = false;
			lastWarnedKey = null;
		}
	}
}
