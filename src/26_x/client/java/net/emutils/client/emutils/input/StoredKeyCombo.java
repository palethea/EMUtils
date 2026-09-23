package net.emutils.client.emutils.input;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Objects;
import net.emutils.client.versioned.VersionedInput;
import net.minecraft.client.input.KeyEvent;

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
			|| key == VersionedInput.KEY_LEFT_SUPER
			|| key == VersionedInput.KEY_RIGHT_SUPER;
	}

	public boolean isDown(long window) {
		InputConstants.Key key = key();
		return key != null
			&& key.getType() == VersionedInput.keyboardType()
			&& VersionedInput.isKeyCodeDown(window, key.getValue())
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
		return VersionedInput.storedKey(keyType, keyCode);
	}

	public boolean sameKeys(StoredKeyCombo other) {
		// Compare resolved keys so a combo saved on an older Minecraft version matches the same key captured now.
		InputConstants.Key key = key();
		return other != null
			&& (key != null ? key == other.key() : keyCode == other.keyCode && Objects.equals(keyType, other.keyType))
			&& ctrl == other.ctrl
			&& alt == other.alt
			&& shift == other.shift;
	}

	private static boolean isCtrlOrCmdDown(long window) {
		return VersionedInput.isKeyCodeDown(window, InputConstants.KEY_LCONTROL)
			|| VersionedInput.isKeyCodeDown(window, InputConstants.KEY_RCONTROL)
			|| VersionedInput.isKeyCodeDown(window, VersionedInput.KEY_LEFT_SUPER)
			|| VersionedInput.isKeyCodeDown(window, VersionedInput.KEY_RIGHT_SUPER);
	}

	private static boolean isAltDown(long window) {
		return VersionedInput.isKeyCodeDown(window, InputConstants.KEY_LALT)
			|| VersionedInput.isKeyCodeDown(window, InputConstants.KEY_RALT);
	}

	private static boolean isShiftDown(long window) {
		return VersionedInput.isKeyCodeDown(window, InputConstants.KEY_LSHIFT)
			|| VersionedInput.isKeyCodeDown(window, InputConstants.KEY_RSHIFT);
	}
}
