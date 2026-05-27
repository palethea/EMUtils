package net.emutils.client.minescript;

import java.util.HashSet;
import java.util.Set;
import net.emutils.client.compat.MinescriptCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class MinescriptKeybindManager {
	private final MinescriptScriptRepository repository = new MinescriptScriptRepository();
	private MinescriptKeybindStore store = MinescriptKeybindStore.load();
	private final Set<String> pressed = new HashSet<>();
	private final Set<String> warnedMissing = new HashSet<>();

	public void reload() {
		store = MinescriptKeybindStore.load();
		pressed.clear();
	}

	public MinescriptKeybindStore store() {
		return store;
	}

	public void tick(MinecraftClient client) {
		if (!MinescriptCompat.isLoaded() || client.player == null || client.world == null || client.currentScreen != null) {
			pressed.clear();
			return;
		}

		long window = client.getWindow().getHandle();

		for (MinescriptKeyBinding binding : store.bindings()) {
			InputUtil.Key key = binding.key();
			if (key == null || key.getCategory() != InputUtil.Type.KEYSYM) {
				continue;
			}
			String id = binding.command();
			boolean down = org.lwjgl.glfw.GLFW.glfwGetKey(window, key.getCode()) == org.lwjgl.glfw.GLFW.GLFW_PRESS
				&& matchesBindingModifiers(window, binding);
			if (down && !pressed.contains(id)) {
				runBinding(client, binding);
			}
			if (down) {
				pressed.add(id);
			} else {
				pressed.remove(id);
			}
		}
	}

	private void runBinding(MinecraftClient client, MinescriptKeyBinding binding) {
		if (!repository.existsCommand(binding.command())) {
			if (warnedMissing.add(binding.command()) && client.player != null) {
				client.player.sendMessage(Text.literal("EMUtils Minescript binding is stale: " + binding.command()).formatted(Formatting.YELLOW), false);
			}
			return;
		}

		switch (MinescriptCompat.toggleCommand(binding.command())) {
			case STARTED -> {
				if (client.player != null) {
					client.player.sendMessage(Text.translatable("emutils.script_manager.running", binding.command()).formatted(Formatting.GREEN), false);
				}
			}
			case STOPPED -> {
				if (client.player != null) {
					client.player.sendMessage(Text.translatable("emutils.script_manager.stopped", binding.command()).formatted(Formatting.YELLOW), false);
				}
			}
			case FAILED -> {
			}
		}
	}

	private static boolean matchesBindingModifiers(long window, MinescriptKeyBinding binding) {
		return binding.ctrl() == isCtrlOrCmdDown(window)
			&& binding.alt() == isAltDown(window)
			&& binding.shift() == isShiftDown(window);
	}

	private static boolean isCtrlOrCmdDown(long window) {
		return org.lwjgl.glfw.GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
			|| org.lwjgl.glfw.GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
			|| org.lwjgl.glfw.GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_LEFT_SUPER) == org.lwjgl.glfw.GLFW.GLFW_PRESS
			|| org.lwjgl.glfw.GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_RIGHT_SUPER) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
	}

	private static boolean isAltDown(long window) {
		return org.lwjgl.glfw.GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_LEFT_ALT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
			|| org.lwjgl.glfw.GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_RIGHT_ALT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
	}

	private static boolean isShiftDown(long window) {
		return org.lwjgl.glfw.GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
			|| org.lwjgl.glfw.GLFW.glfwGetKey(window, InputUtil.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
	}
}
