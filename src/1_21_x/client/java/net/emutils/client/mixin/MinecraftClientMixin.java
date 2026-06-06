package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow
	@Nullable
	public Screen currentScreen;

	@Shadow
	private int itemUseCooldown;

	@Inject(method = "setScreen", at = @At("HEAD"))
	private void emutils$saveContainerCursorBeforeScreenChange(@Nullable Screen screen, CallbackInfo ci) {
		if (this.currentScreen instanceof HandledScreen<?>) {
			EMUtilsClient.inventoryTools().cursor().saveBeforeScreenChange((MinecraftClient) (Object) this);
		}
	}

	@Inject(method = "setScreen", at = @At("TAIL"))
	private void emutils$clearContainerCursorAfterScreenChange(@Nullable Screen screen, CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		if (screen == null) {
			EMUtilsClient.inventoryTools().cursor().markCloseGracePeriod();
			return;
		}

		if (screen instanceof HandledScreen<?>) {
			EMUtilsClient.inventoryTools().cursor().tryRestoreAfterInit(client);
			return;
		}

		EMUtilsClient.inventoryTools().cursor().clearSaved();
	}

	@Inject(method = "doItemUse", at = @At("RETURN"))
	private void emutils$clearFastPlaceCooldown(CallbackInfo ci) {
		if (emutils$shouldClearFastPlaceCooldown()) {
			itemUseCooldown = 0;
		}
	}

	private boolean emutils$shouldClearFastPlaceCooldown() {
		if (EMUtilsClient.config() == null || !EMUtilsClient.config().tweakFastPlace()) {
			return false;
		}

		MinecraftClient client = (MinecraftClient) (Object) this;
		ClientPlayerEntity player = client.player;
		return player != null
			&& (emutils$isBlockItem(player.getMainHandStack()) || emutils$isBlockItem(player.getOffHandStack()));
	}

	private static boolean emutils$isBlockItem(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
	}
}
