package net.emutils.client.emutils.gui.settings;

import com.mojang.blaze3d.platform.InputConstants;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Rebinding a key from the new settings UI, like the vanilla Controls screen: after clicking a keycap,
 * the next key or mouse side button is bound. Esc cancels (in this UI it always backs out), Backspace
 * unbinds, and a left or right click cancels.
 */
final class KeybindCapture {
	private @Nullable KeyMapping listening;
	/** Set when a key press was used for binding, so its character isn't typed into a field too. */
	private boolean swallowChar;

	static @Nullable KeyMapping mapping(String name) {
		return KeyMapping.get(name);
	}

	boolean active() {
		return listening != null;
	}

	boolean isListening(KeyMapping mapping) {
		return listening == mapping;
	}

	void start(KeyMapping mapping) {
		listening = mapping;
	}

	void cancel() {
		listening = null;
	}

	boolean keyPressed(KeyEvent input) {
		KeyMapping mapping = listening;
		if (mapping == null) {
			return false;
		}
		listening = null;
		swallowChar = true;
		if (input.isEscape()) {
			return true;
		}
		apply(mapping, input.key() == InputConstants.KEY_BACKSPACE ? InputConstants.UNKNOWN : InputConstants.getKey(input));
		return true;
	}

	/** Binds a middle or side mouse button; the left and right buttons cancel instead. */
	boolean mouseClicked(int button) {
		KeyMapping mapping = listening;
		if (mapping == null) {
			return false;
		}
		listening = null;
		if (button > 1) {
			apply(mapping, InputConstants.Type.MOUSE.getOrCreate(button));
		}
		return true;
	}

	/** Whether to drop a typed character because its key was just used for binding. */
	boolean swallowChar() {
		boolean swallow = swallowChar;
		swallowChar = false;
		return swallow;
	}

	/** Call once per frame; a character belongs to the key press of the same frame or none. */
	void frame() {
		swallowChar = false;
	}

	static void resetToDefault(KeyMapping mapping) {
		apply(mapping, mapping.getDefaultKey());
	}

	private static void apply(KeyMapping mapping, InputConstants.Key key) {
		mapping.setKey(key);
		KeyMapping.resetMapping();
		Minecraft.getInstance().options.save();
	}

	/** The keycap's text: the bound key, "Press a key" while listening, or "None". */
	Component label(KeyMapping mapping) {
		if (listening == mapping) {
			return Component.translatable(EMUtilsTexts.UI_PRESS_KEY);
		}
		return mapping.isUnbound() ? Component.translatable(EMUtilsTexts.UI_NOT_BOUND) : mapping.getTranslatedKeyMessage();
	}

	/** Whether another key mapping uses the same key. */
	static boolean clashes(KeyMapping mapping) {
		if (mapping.isUnbound()) {
			return false;
		}
		for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
			if (other != mapping && other.same(mapping)) {
				return true;
			}
		}
		return false;
	}
}
