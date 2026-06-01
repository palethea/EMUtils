package net.emutils.client.emutils.minescript;

import net.emutils.client.emhelpers.input.StoredKeyCombo;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;

public record MinescriptKeyBinding(String command, String keyType, int keyCode, boolean ctrl, boolean alt, boolean shift) {
	public static MinescriptKeyBinding from(String command, KeyInput input) {
		StoredKeyCombo combo = StoredKeyCombo.from(input);
		if (combo == null) {
			return null;
		}
		return new MinescriptKeyBinding(
			command,
			combo.keyType(),
			combo.keyCode(),
			combo.ctrl(),
			combo.alt(),
			combo.shift()
		);
	}

	public static boolean isModifierKey(int key) {
		return StoredKeyCombo.isModifierKey(key);
	}

	public boolean isDown(long window) {
		return combo().isDown(window);
	}

	public String displayName() {
		return combo().displayName();
	}

	public InputUtil.Key key() {
		return combo().key();
	}

	public boolean sameKeys(MinescriptKeyBinding other) {
		return other != null && combo().sameKeys(other.combo());
	}

	private StoredKeyCombo combo() {
		return new StoredKeyCombo(keyType, keyCode, ctrl, alt, shift);
	}
}
