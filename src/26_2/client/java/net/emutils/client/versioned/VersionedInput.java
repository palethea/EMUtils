package net.emutils.client.versioned;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Minecraft 26.2 keyboard and mouse access (GLFW). Every supported version provides this class
 * with the same method signatures; shared code calls it instead of the platform input API.
 */
public final class VersionedInput {
	private VersionedInput() {
	}

	/** Input type for keyboard key mappings. */
	public static InputConstants.Type keyboardType() {
		return InputConstants.Type.KEYSYM;
	}

	/** Whether a bound keyboard key or mouse button is currently held. */
	public static boolean isDown(Minecraft client, InputConstants.Key key) {
		if (key.getType() == InputConstants.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
		}
		return InputConstants.isKeyDown(client.getWindow(), key.getValue());
	}

	public static boolean isShiftDown(Minecraft client) {
		return InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	/** Shows the normal cursor at the given window position. */
	public static void showCursorAt(Window window, double x, double y) {
		InputConstants.grabOrReleaseMouse(window, InputConstants.CURSOR_NORMAL, x, y);
	}
}
