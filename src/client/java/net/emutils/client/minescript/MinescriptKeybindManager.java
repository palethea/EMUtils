package net.emutils.client.minescript;

import java.util.HashSet;
import java.util.Set;
import net.emutils.client.accessor.KeyBindingAccess;
import net.emutils.client.compat.MinescriptCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
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

	public boolean shouldSuppressKeyBinding(KeyBinding keyBinding) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!MinescriptCompat.isLoaded() || client == null || client.currentScreen != null) {
			return false;
		}
		if (keyBinding == null || keyBinding.isUnbound()) {
			return false;
		}

		net.minecraft.client.util.InputUtil.Key boundKey = ((KeyBindingAccess) keyBinding).emutils$getBoundKey();
		if (boundKey.getCategory() != net.minecraft.client.util.InputUtil.Type.KEYSYM) {
			return false;
		}

		long window = client.getWindow().getHandle();
		int keyCode = boundKey.getCode();
		if (org.lwjgl.glfw.GLFW.glfwGetKey(window, keyCode) != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
			return false;
		}

		ModifierState modifiers = currentModifiers(client);
		for (MinescriptKeyBinding binding : store.bindings()) {
			net.minecraft.client.util.InputUtil.Key scriptKey = binding.key();
			if (scriptKey == null || scriptKey.getCode() != keyCode) {
				continue;
			}
			if (binding.matchesModifiers(modifiers.ctrl(), modifiers.alt(), modifiers.shift())) {
				return true;
			}
		}
		return false;
	}

	private static ModifierState currentModifiers(MinecraftClient client) {
		boolean ctrlDown = InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_CONTROL)
			|| InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_SUPER)
			|| InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_SUPER);
		boolean altDown = InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_ALT)
			|| InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_ALT);
		boolean shiftDown = InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_SHIFT)
			|| InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_SHIFT);
		return new ModifierState(ctrlDown, altDown, shiftDown);
	}

	public void tick(MinecraftClient client) {
		if (!MinescriptCompat.isLoaded() || client.player == null || client.world == null || client.currentScreen != null) {
			pressed.clear();
			return;
		}

		long window = client.getWindow().getHandle();
		ModifierState modifiers = currentModifiers(client);

		for (MinescriptKeyBinding binding : store.bindings()) {
			InputUtil.Key key = binding.key();
			if (key == null || key.getCategory() != InputUtil.Type.KEYSYM) {
				continue;
			}
			String id = binding.command();
			boolean down = org.lwjgl.glfw.GLFW.glfwGetKey(window, key.getCode()) == org.lwjgl.glfw.GLFW.GLFW_PRESS
				&& binding.matchesModifiers(modifiers.ctrl(), modifiers.alt(), modifiers.shift());
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

	private record ModifierState(boolean ctrl, boolean alt, boolean shift) {
	}
}
