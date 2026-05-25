package net.emutils.client.minescript;

import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;

public record MinescriptKeyBinding(String command, String keyType, int keyCode, boolean ctrl, boolean alt, boolean shift) {
	public static MinescriptKeyBinding from(String command, KeyInput input) {
		if (isModifierKey(input.key())) {
			return null;
		}
		InputUtil.Key key = InputUtil.fromKeyCode(input);
		return new MinescriptKeyBinding(
			command,
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

	public boolean matchesModifiers(boolean ctrlDown, boolean altDown, boolean shiftDown) {
		return ctrl == ctrlDown && alt == altDown && shift == shiftDown;
	}

	public String displayName() {
		InputUtil.Key key = key();
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
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public boolean sameKeys(MinescriptKeyBinding other) {
		return other != null
			&& keyCode == other.keyCode
			&& keyType.equals(other.keyType)
			&& ctrl == other.ctrl
			&& alt == other.alt
			&& shift == other.shift;
	}
}
