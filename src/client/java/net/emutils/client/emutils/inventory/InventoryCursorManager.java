package net.emutils.client.emutils.inventory;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.mixin.MouseAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.jspecify.annotations.Nullable;

public final class InventoryCursorManager {
	private static final int CLOSE_GRACE_TICKS = 40;

	@Nullable
	private Double savedX;
	@Nullable
	private Double savedY;
	private boolean savedLeavingHandled;
	private int closeGraceTicks;

	public void saveBeforeScreenChange(MinecraftClient client) {
		if (!enabled() || client.currentScreen == null) {
			return;
		}

		Mouse mouse = client.mouse;
		savedX = mouse.getX();
		savedY = mouse.getY();
		savedLeavingHandled = true;
		closeGraceTicks = 0;
	}

	public void markCloseGracePeriod() {
		if (savedLeavingHandled) {
			closeGraceTicks = CLOSE_GRACE_TICKS;
		}
	}

	public void tick(MinecraftClient client) {
		if (!savedLeavingHandled || closeGraceTicks <= 0) {
			return;
		}

		closeGraceTicks--;
		if (closeGraceTicks == 0 && !(client.currentScreen instanceof HandledScreen<?>)) {
			clearSaved();
		}
	}

	public void tryRestoreAfterInit(MinecraftClient client) {
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

	private void tryRestore(MinecraftClient client) {
		if (!enabled() || !savedLeavingHandled || savedX == null || savedY == null) {
			return;
		}

		if (!(client.currentScreen instanceof HandledScreen<?>)) {
			return;
		}

		double x = savedX;
		double y = savedY;
		closeGraceTicks = 0;
		restoreCursor(client, x, y);
		client.execute(() -> {
			restoreCursor(client, x, y);
			if (client.currentScreen instanceof HandledScreen<?>) {
				clearSaved();
			}
		});
	}

	private static void restoreCursor(MinecraftClient client, double x, double y) {
		Window window = client.getWindow();
		MouseAccess mouse = (MouseAccess) client.mouse;
		mouse.emutils$setX(x);
		mouse.emutils$setY(y);
		if (!client.mouse.isCursorLocked()) {
			InputUtil.setCursorParameters(window, InputUtil.GLFW_CURSOR_NORMAL, x, y);
		}
	}

	private boolean enabled() {
		EMUtilsConfig config = EMUtilsClient.config();
		return config != null && config.inventoryToolsEnabled() && config.preserveContainerCursor();
	}
}
