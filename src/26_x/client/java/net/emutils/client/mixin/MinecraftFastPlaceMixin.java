package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftFastPlaceMixin {
	@Shadow
	private int rightClickDelay;

	@Inject(method = "startUseItem", at = @At("RETURN"))
	private void emutils$clearFastPlaceCooldown(CallbackInfo ci) {
		if (emutils$shouldClearFastPlaceCooldown()) {
			rightClickDelay = 0;
		}
	}

	private boolean emutils$shouldClearFastPlaceCooldown() {
		if (EMUtilsClient.config() == null || !EMUtilsClient.config().tweakFastPlace()) {
			return false;
		}

		LocalPlayer player = ((Minecraft) (Object) this).player;
		return player != null
			&& (emutils$isBlockItem(player.getMainHandItem()) || emutils$isBlockItem(player.getOffhandItem()));
	}

	private static boolean emutils$isBlockItem(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
	}
}
