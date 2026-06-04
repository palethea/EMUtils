package net.emutils.client.emutils.commandshortcuts;

import java.util.HashSet;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emhelpers.client.input.StoredKeyCombo;
import net.minecraft.client.MinecraftClient;

public final class CommandShortcutsManager {
	private CommandShortcutStore store = CommandShortcutStore.load();
	private final Set<String> pressed = new HashSet<>();

	public void reload() {
		store = CommandShortcutStore.load();
		pressed.clear();
	}

	public CommandShortcutStore store() {
		return store;
	}

	public void tick(MinecraftClient client) {
		if (!active(client)) {
			pressed.clear();
			return;
		}

		long window = client.getWindow().getHandle();
		for (CommandShortcut shortcut : store.shortcuts()) {
			StoredKeyCombo keyCombo = shortcut.keyCombo();
			boolean down = keyCombo != null && keyCombo.isDown(window);
			if (down && pressed.add(shortcut.id())) {
				runShortcut(client, shortcut);
			} else if (!down) {
				pressed.remove(shortcut.id());
			}
		}
	}

	private void runShortcut(MinecraftClient client, CommandShortcut shortcut) {
		if (client == null || client.player == null || client.player.networkHandler == null || shortcut == null) {
			return;
		}

		String text = shortcut.displayText();
		if (text.isBlank()) {
			return;
		}

		if (text.startsWith("/")) {
			String command = text.substring(1).trim();
			if (!command.isBlank()) {
				client.player.networkHandler.sendChatCommand(command);
			}
			return;
		}

		client.player.networkHandler.sendChatMessage(text);
	}

	private static boolean active(MinecraftClient client) {
		return EMUtilsClient.config() != null
			&& EMUtilsClient.config().commandShortcutsEnabled()
			&& client != null
			&& client.player != null
			&& client.world != null
			&& client.currentScreen == null;
	}
}
