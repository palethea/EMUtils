package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClient26_1Mixin {
	@Shadow
	@Nullable
	public Screen screen;

	@Inject(method = "setScreen", at = @At("HEAD"))
	private void emutils$saveContainerCursorBeforeScreenChange(@Nullable Screen screen, CallbackInfo ci) {
		if (this.screen instanceof AbstractContainerScreen<?>) {
			EMUtilsClient.inventoryTools().cursor().saveBeforeScreenChange((Minecraft) (Object) this);
		}
	}

	@Inject(method = "setScreen", at = @At("TAIL"))
	private void emutils$clearContainerCursorAfterScreenChange(@Nullable Screen screen, CallbackInfo ci) {
		Minecraft client = (Minecraft) (Object) this;
		if (screen == null) {
			EMUtilsClient.inventoryTools().cursor().markCloseGracePeriod();
			return;
		}

		if (screen instanceof AbstractContainerScreen<?>) {
			EMUtilsClient.inventoryTools().cursor().tryRestoreAfterInit(client);
			return;
		}

		EMUtilsClient.inventoryTools().cursor().clearSaved();
	}
}
