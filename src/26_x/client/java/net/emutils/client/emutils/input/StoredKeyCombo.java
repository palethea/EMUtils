package net.emutils.client.emutils.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public record StoredKeyCombo(String keyType, int keyCode, boolean ctrl, boolean alt, boolean shift) {
	public static StoredKeyCombo from(KeyEvent input) {
		if (isModifierKey(input.key())) {
			return null;
		}

		InputConstants.Key key = InputConstants.getKey(input);
		return new StoredKeyCombo(
			key.getType().name(),
			key.getValue(),
			input.hasControlDownWithQuirk(),
			input.hasAltDown(),
			input.hasShiftDown()
		);
	}

	public static boolean isModifierKey(int key) {
		return key == InputConstants.KEY_LCONTROL
			|| key == InputConstants.KEY_RCONTROL
			|| key == InputConstants.KEY_LSHIFT
			|| key == InputConstants.KEY_RSHIFT
			|| key == InputConstants.KEY_LALT
			|| key == InputConstants.KEY_RALT
			|| key == InputConstants.KEY_LSUPER
			|| key == InputConstants.KEY_RSUPER;
	}

	public boolean isDown(long window) {
		InputConstants.Key key = key();
		return key != null
			&& key.getType() == InputConstants.Type.KEYSYM
			&& GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS
			&& matchesModifiers(window);
	}

	public boolean matchesModifiers(long window) {
		return ctrl == isCtrlOrCmdDown(window)
			&& alt == isAltDown(window)
			&& shift == isShiftDown(window);
	}

	public String displayName() {
		InputConstants.Key key = key();
		if (key == null) {
			return "Unknown";
		}

		StringBuilder builder = new StringBuilder();
		if (ctrl) {
			builder.append("Ctrl+");
		}
		if (alt) {
			builder.append("Alt+");
		}
		if (shift) {
			builder.append("Shift+");
		}
		builder.append(key.getDisplayName().getString());
		return builder.toString();
	}

	public InputConstants.Key key() {
		try {
			return InputConstants.Type.valueOf(keyType).getOrCreate(keyCode);
		} catch (IllegalArgumentException | NullPointerException exception) {
			return null;
		}
	}

	public boolean sameKeys(StoredKeyCombo other) {
		return other != null
			&& keyCode == other.keyCode
			&& java.util.Objects.equals(keyType, other.keyType)
			&& ctrl == other.ctrl
			&& alt == other.alt
			&& shift == other.shift;
	}

	private static boolean isCtrlOrCmdDown(long window) {
		return GLFW.glfwGetKey(window, InputConstants.KEY_LCONTROL) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputConstants.KEY_RCONTROL) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputConstants.KEY_LSUPER) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputConstants.KEY_RSUPER) == GLFW.GLFW_PRESS;
	}

	private static boolean isAltDown(long window) {
		return GLFW.glfwGetKey(window, InputConstants.KEY_LALT) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputConstants.KEY_RALT) == GLFW.GLFW_PRESS;
	}

	private static boolean isShiftDown(long window) {
		return GLFW.glfwGetKey(window, InputConstants.KEY_LSHIFT) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputConstants.KEY_RSHIFT) == GLFW.GLFW_PRESS;
	}
}
