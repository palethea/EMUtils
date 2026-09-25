package net.emutils.client.versioned;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLMouse;

/**
 * Minecraft 26.3 keyboard and mouse access (SDL3). Every supported version provides this class
 * with the same method signatures; shared code calls it instead of the platform input API.
 */
public final class VersionedInput {
	public static final int KEY_LEFT_SUPER = InputConstants.KEY_LGUI;
	public static final int KEY_RIGHT_SUPER = InputConstants.KEY_RGUI;
	private static final String LEGACY_KEYBOARD_TYPE = "KEYSYM";

	private VersionedInput() {
	}

	/** Input type for keyboard key mappings. */
	public static InputConstants.Type keyboardType() {
		return InputConstants.Type.KEYBOARD;
	}

	/** Whether a keyboard key, by this version's key code, is currently held. */
	public static boolean isKeyCodeDown(long window, int keyCode) {
		return InputConstants.isKeyDown(keyCode);
	}

	/**
	 * Resolves a key saved as its input type name and key code, or null if it is not a valid key.
	 * Keys saved on 26.2 or earlier are GLFW {@code KEYSYM} codes; they are converted through the
	 * key name so existing shortcuts keep working.
	 */
	public static InputConstants.@Nullable Key storedKey(String keyType, int keyCode) {
		if (LEGACY_KEYBOARD_TYPE.equals(keyType)) {
			String name = LegacyGlfwKeys.name(keyCode);
			return name == null ? null : InputConstants.getKey(name);
		}
		try {
			return InputConstants.Type.valueOf(keyType).getOrCreate(keyCode);
		} catch (IllegalArgumentException | NullPointerException exception) {
			return null;
		}
	}

	/** Whether a bound keyboard key or mouse button is currently held. */
	public static boolean isDown(Minecraft client, InputConstants.Key key) {
		if (key.getType() == InputConstants.Type.MOUSE) {
			// Mouse keys hold SDL button numbers (left = 1); SDL_BUTTON_MASK(button) is 1 << (button - 1).
			int buttons = SDLMouse.SDL_GetMouseState((FloatBuffer) null, (FloatBuffer) null);
			return key.getValue() >= 1 && (buttons & (1 << (key.getValue() - 1))) != 0;
		}
		return InputConstants.isKeyDown(key.getValue());
	}

	public static boolean isShiftDown(Minecraft client) {
		return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
	}

	/**
	 * Starts or stops text input for a custom text field. SDL3 only delivers typed characters while
	 * text input is active, which vanilla EditBoxes request when they gain focus; widgets that are not
	 * EditBoxes must do the same.
	 */
	public static void setTextInputFocus(Object owner, boolean focused) {
		Minecraft.getInstance().textInputManager().onTextInputFocusChange(owner, focused);
	}

	/** Shows the normal cursor at the given window position. */
	public static void showCursorAt(Window window, double x, double y) {
		InputConstants.releaseMouse(window, x, y);
	}

	/**
	 * The key as the keyboard layout names it, for shortcuts that follow the printed character, such as
	 * Ctrl+/. 26.3 reports it separately from the key; 26.2 only has the key.
	 */
	public static int shortcutKey(KeyEvent input) {
		return input.shortcutKey();
	}
}
