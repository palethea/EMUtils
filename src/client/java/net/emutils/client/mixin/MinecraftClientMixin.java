package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
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
}
