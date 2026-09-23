package net.emutils.client.versioned.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks dropping the selected hotbar item while its slot is locked. 26.3 drops through the game mode. */
@Mixin(MultiPlayerGameMode.class)
public abstract class HotbarDropMixin {
	@Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
	private void emutils$blockLockedHotbarDrop(LocalPlayer player, boolean all, CallbackInfo ci) {
		if (EMUtilsClient.inventoryTools().isPlayerSlotLocked(player.getInventory().getSelectedSlot())) {
			ci.cancel();
		}
	}
}
