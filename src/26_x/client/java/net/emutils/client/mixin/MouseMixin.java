package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.mixin.MouseAccess;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Options;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
	@Redirect(
		method = "turnPlayer(D)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z")
	)
	private boolean emutils$enableCinematicCameraWhileZooming(Options options) {
		return options.smoothCamera
			|| EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldUseCinematicCamera();
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void emutils$handleZoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().handleScroll(vertical)) {
			ci.cancel();
		}
	}

	@Redirect(
		method = "releaseMouse",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(Lcom/mojang/blaze3d/platform/Window;IDD)V"
		)
	)
	private void emutils$unlockCursorWithPreservedPosition(Window window, int mode, double x, double y) {
		@Nullable double[] coords = EMUtilsClient.inventoryTools().cursor().peekRestoreCoords();
		if (coords != null) {
			MouseAccess mouse = (MouseAccess) this;
			mouse.emutils$setX(coords[0]);
			mouse.emutils$setY(coords[1]);
			InputConstants.grabOrReleaseMouse(window, mode, coords[0], coords[1]);
			return;
		}

		InputConstants.grabOrReleaseMouse(window, mode, x, y);
	}

	@Redirect(
		method = "turnPlayer(D)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
	)
	private void emutils$redirectFreelookMouse(LocalPlayer player, double cursorDeltaX, double cursorDeltaY) {
		if (EMUtilsClient.tweaks() != null) {
			EMUtilsClient.tweaks().freelook().changeLookDirection(player, cursorDeltaX, cursorDeltaY);
		} else {
			player.turn(cursorDeltaX, cursorDeltaY);
		}
	}
}
