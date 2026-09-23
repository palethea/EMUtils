package net.emutils.client.versioned.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.mixin.MouseAccess;
import net.minecraft.client.MouseHandler;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseCursorMixin {
	@Redirect(
		method = "releaseMouse",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/platform/InputConstants;releaseMouse(Lcom/mojang/blaze3d/platform/Window;DD)V"
		)
	)
	private void emutils$unlockCursorWithPreservedPosition(Window window, double x, double y) {
		@Nullable double[] coords = EMUtilsClient.inventoryTools().cursor().peekRestoreCoords();
		if (coords != null) {
			MouseAccess mouse = (MouseAccess) this;
			mouse.emutils$setX(coords[0]);
			mouse.emutils$setY(coords[1]);
			InputConstants.releaseMouse(window, coords[0], coords[1]);
			return;
		}

		InputConstants.releaseMouse(window, x, y);
	}
}
