package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.mixin.MouseAccess;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerHudClickHandler;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
	@Redirect(
		method = "updateMouse(D)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;smoothCameraEnabled:Z")
	)
	private boolean emutils$enableCinematicCameraWhileZooming(GameOptions options) {
		return options.smoothCameraEnabled
			|| EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldUseCinematicCamera();
	}

	@Inject(method = "onMouseButton", at = @At("HEAD"))
	private void emutils$trackerHudClick(long window, MouseInput input, int action, CallbackInfo ci) {
		if (action != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
			return;
		}

		net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		if (client == null || client.currentScreen != null) {
			return;
		}

		int button = input.button();
		boolean isLeftClick = button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
		boolean isRightClick = button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

		if (!isLeftClick && !isRightClick) {
			return;
		}

		double[] scaledMouse = TrackerHudClickHandler.scaledMouse(client);
		if (scaledMouse != null) {
			TrackerHudClickHandler.handleClick(client, scaledMouse[0], scaledMouse[1], isRightClick);
		}
	}

	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
	private void emutils$handleZoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().handleScroll(vertical)) {
			ci.cancel();
		}
	}

	@Redirect(
		method = "unlockCursor",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(Lnet/minecraft/client/util/Window;IDD)V"
		)
	)
	private void emutils$unlockCursorWithPreservedPosition(Window window, int mode, double x, double y) {
		@Nullable double[] coords = EMUtilsClient.inventoryTools().cursor().peekRestoreCoords();
		if (coords != null) {
			MouseAccess mouse = (MouseAccess) this;
			mouse.emutils$setX(coords[0]);
			mouse.emutils$setY(coords[1]);
			InputUtil.setCursorParameters(window, mode, coords[0], coords[1]);
			return;
		}

		InputUtil.setCursorParameters(window, mode, x, y);
	}

	@Redirect(
		method = "updateMouse(D)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V")
	)
	private void emutils$redirectFreelookMouse(ClientPlayerEntity player, double cursorDeltaX, double cursorDeltaY) {
		if (EMUtilsClient.tweaks() != null) {
			EMUtilsClient.tweaks().freelook().changeLookDirection(player, cursorDeltaX, cursorDeltaY);
		} else {
			player.changeLookDirection(cursorDeltaX, cursorDeltaY);
		}
	}
}
