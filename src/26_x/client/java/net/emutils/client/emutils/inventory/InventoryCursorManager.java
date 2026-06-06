package net.emutils.client.emutils.inventory;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.mixin.MouseAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.jspecify.annotations.Nullable;

public final class InventoryCursorManager {
	private static final int CLOSE_GRACE_TICKS = 40;

	@Nullable
	private Double savedX;
	@Nullable
	private Double savedY;
	private boolean savedLeavingHandled;
	private int closeGraceTicks;

	public void saveBeforeScreenChange(Minecraft client) {
		if (!enabled() || client.screen == null) {
			return;
		}

		MouseHandler mouse = client.mouseHandler;
		savedX = mouse.xpos();
		savedY = mouse.ypos();
		savedLeavingHandled = true;
		closeGraceTicks = 0;
	}

	public void markCloseGracePeriod() {
		if (savedLeavingHandled) {
			closeGraceTicks = CLOSE_GRACE_TICKS;
		}
	}

	public void tick(Minecraft client) {
		if (!savedLeavingHandled || closeGraceTicks <= 0) {
			return;
		}

		closeGraceTicks--;
		if (closeGraceTicks == 0 && !(client.screen instanceof AbstractContainerScreen<?>)) {
			clearSaved();
		}
	}

	public void tryRestoreAfterInit(Minecraft client) {
		tryRestore(client);
	}

	@Nullable
	public double[] peekRestoreCoords() {
		if (!enabled() || !savedLeavingHandled || savedX == null || savedY == null) {
			return null;
		}

		return new double[] { savedX, savedY };
	}

	public void clearSaved() {
		savedX = null;
		savedY = null;
		savedLeavingHandled = false;
		closeGraceTicks = 0;
	}

	private void tryRestore(Minecraft client) {
		if (!enabled() || !savedLeavingHandled || savedX == null || savedY == null) {
			return;
		}

		if (!(client.screen instanceof AbstractContainerScreen<?>)) {
			return;
		}

		double x = savedX;
		double y = savedY;
		closeGraceTicks = 0;
		restoreCursor(client, x, y);
		client.execute(() -> {
			restoreCursor(client, x, y);
			if (client.screen instanceof AbstractContainerScreen<?>) {
				clearSaved();
			}
		});
	}

	private static void restoreCursor(Minecraft client, double x, double y) {
		Window window = client.getWindow();
		MouseAccess mouse = (MouseAccess) client.mouseHandler;
		mouse.emutils$setX(x);
		mouse.emutils$setY(y);
		if (!client.mouseHandler.isMouseGrabbed()) {
			InputConstants.grabOrReleaseMouse(window, InputConstants.CURSOR_NORMAL, x, y);
		}
	}

	private boolean enabled() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.inventoryToolsEnabled() && config.preserveContainerCursor();
	}
}
