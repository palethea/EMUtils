package net.emutils.client.emhelpers.input;

import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public record StoredKeyCombo(String keyType, int keyCode, boolean ctrl, boolean alt, boolean shift) {
	public static StoredKeyCombo from(KeyInput input) {
		if (isModifierKey(input.key())) {
			return null;
		}

		InputUtil.Key key = InputUtil.fromKeyCode(input);
		return new StoredKeyCombo(
			key.getCategory().name(),
			key.getCode(),
			input.hasCtrlOrCmd(),
			input.hasAlt(),
			input.hasShift()
		);
	}

	public static boolean isModifierKey(int key) {
		return key == InputUtil.GLFW_KEY_LEFT_CONTROL
			|| key == InputUtil.GLFW_KEY_RIGHT_CONTROL
			|| key == InputUtil.GLFW_KEY_LEFT_SHIFT
			|| key == InputUtil.GLFW_KEY_RIGHT_SHIFT
			|| key == InputUtil.GLFW_KEY_LEFT_ALT
			|| key == InputUtil.GLFW_KEY_RIGHT_ALT
			|| key == InputUtil.GLFW_KEY_LEFT_SUPER
			|| key == InputUtil.GLFW_KEY_RIGHT_SUPER;
	}

	public boolean isDown(long window) {
		InputUtil.Key key = key();
		return key != null
			&& key.getCategory() == InputUtil.Type.KEYSYM
			&& GLFW.glfwGetKey(window, key.getCode()) == GLFW.GLFW_PRESS
			&& matchesModifiers(window);
	}

	public boolean matchesModifiers(long window) {
		return ctrl == isCtrlOrCmdDown(window)
			&& alt == isAltDown(window)
			&& shift == isShiftDown(window);
	}

	public String displayName() {
		InputUtil.Key key = key();
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
		builder.append(key.getLocalizedText().getString());
		return builder.toString();
	}

	public InputUtil.Key key() {
		try {
			return InputUtil.Type.valueOf(keyType).createFromCode(keyCode);
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
		return GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_LEFT_SUPER) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_RIGHT_SUPER) == GLFW.GLFW_PRESS;
	}

	private static boolean isAltDown(long window) {
		return GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
	}

	private static boolean isShiftDown(long window) {
		return GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
	}
}
