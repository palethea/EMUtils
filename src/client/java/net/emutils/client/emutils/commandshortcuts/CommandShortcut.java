package net.emutils.client.emutils.commandshortcuts;

import java.util.UUID;
import net.emutils.client.emhelpers.input.StoredKeyCombo;

public record CommandShortcut(String id, String name, String text, StoredKeyCombo keyCombo) {
	public static CommandShortcut create(String name, String text, StoredKeyCombo keyCombo) {
		return new CommandShortcut(UUID.randomUUID().toString(), clean(name), clean(text), keyCombo);
	}

	public CommandShortcut with(String name, String text, StoredKeyCombo keyCombo) {
		return new CommandShortcut(id, clean(name), clean(text), keyCombo);
	}

	public String displayName() {
		String cleanedName = clean(name);
		return cleanedName.isEmpty() ? clean(text) : cleanedName;
	}

	public String displayText() {
		return clean(text);
	}

	public boolean isCommand() {
		return displayText().startsWith("/");
	}

	public boolean valid() {
		return id != null && !id.isBlank() && !displayText().isBlank() && keyCombo != null;
	}

	private static String clean(String value) {
		return value == null ? "" : value.trim();
	}
}
