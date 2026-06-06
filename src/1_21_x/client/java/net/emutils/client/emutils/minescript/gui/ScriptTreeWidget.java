package net.emutils.client.emutils.minescript.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.emutils.client.emutils.minescript.MinescriptScript;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;

public final class ScriptTreeWidget extends AlwaysSelectedEntryListWidget<ScriptTreeWidget.Entry> {
	private static final int ROW_HEIGHT = 22;
	private final Consumer<MinescriptScript> onSelected;
	private final Set<String> collapsedDirectories = new HashSet<>();
	private List<MinescriptScript> scripts = List.of();
	private MinescriptScript selectedScript;

	public ScriptTreeWidget(MinecraftClient client, int width, int height, Consumer<MinescriptScript> onSelected) {
		super(client, width, height, 0, ROW_HEIGHT);
		centerListVertically = false;
		this.onSelected = onSelected;
	}

	public void setScripts(List<MinescriptScript> scripts) {
		this.scripts = scripts;
		rebuild();
	}

	public MinescriptScript selectedScript() {
		return selectedScript;
	}

	public String selectedDirectory() {
		if (selectedScript == null) {
			return "";
		}
		if (selectedScript.directory()) {
			return selectedScript.relativePath();
		}
		int slash = selectedScript.relativePath().lastIndexOf('/');
		return slash <= 0 ? "" : selectedScript.relativePath().substring(0, slash);
	}

	@Override
	public int getRowWidth() {
		return Math.min(width - 12, 260);
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.fill(getX(), getY(), getX() + width, getY() + height, 0xDD101010);
		context.drawStrokedRectangle(getX(), getY(), width, height, isFocused() ? 0xFF69A7D8 : 0xFF444444);
		super.renderWidget(context, mouseX, mouseY, deltaTicks);
	}

	@Override
	protected void drawMenuListBackground(DrawContext context) {
	}

	@Override
	protected void drawHeaderAndFooterSeparators(DrawContext context) {
	}

	private void rebuild() {
		clearEntries();
		if (scripts.isEmpty()) {
			addEntry(new EmptyEntry(client));
			return;
		}
		for (MinescriptScript script : scripts) {
			if (isHiddenByCollapsedParent(script)) {
				continue;
			}
			addEntry(new ScriptEntry(client, script));
		}
	}

	private boolean isHiddenByCollapsedParent(MinescriptScript script) {
		String relative = script.relativePath();
		for (String collapsed : collapsedDirectories) {
			if (!relative.equals(collapsed) && relative.startsWith(collapsed + "/")) {
				return true;
			}
		}
		return false;
	}

	abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
	}

	private final class ScriptEntry extends Entry {
		private final MinecraftClient client;
		private final MinescriptScript script;

		private ScriptEntry(MinecraftClient client, MinescriptScript script) {
			this.client = client;
			this.script = script;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int x = getContentX();
			int y = getContentY();
			boolean selected = script.equals(selectedScript);
			if (selected || hovered) {
				context.fill(x, y, x + getContentWidth(), y + getContentHeight(), selected ? 0xAA315A7D : 0x66303030);
			}
			String prefix = script.directory() ? (collapsedDirectories.contains(script.relativePath()) ? "[+] " : "[-] ") : "    ";
			String label = "  ".repeat(Math.max(0, script.depth())) + prefix + script.displayName();
			Formatting color = script.directory() ? Formatting.AQUA : script.editable() ? Formatting.WHITE : Formatting.GRAY;
			context.drawTextWithShadow(client.textRenderer, Text.literal(client.textRenderer.trimToWidth(label, getContentWidth() - 8)).formatted(color), x + 4, y + 6, Colors.WHITE);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			selectedScript = script;
			ScriptTreeWidget.this.setSelected(this);
			if (script.directory()) {
				if (collapsedDirectories.contains(script.relativePath())) {
					collapsedDirectories.remove(script.relativePath());
				} else {
					collapsedDirectories.add(script.relativePath());
				}
				rebuild();
			} else {
				onSelected.accept(script);
			}
			return true;
		}

		@Override
		public Text getNarration() {
			return Text.literal(script.displayName());
		}
	}

	private static final class EmptyEntry extends Entry {
		private final MinecraftClient client;

		private EmptyEntry(MinecraftClient client) {
			this.client = client;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			Text text = Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_EMPTY).formatted(Formatting.GRAY);
			context.drawCenteredTextWithShadow(client.textRenderer, text, getContentMiddleX(), getContentMiddleY() - 4, Colors.LIGHT_GRAY);
		}

		@Override
		public Text getNarration() {
			return Text.translatable(EMUtilsTexts.SCRIPT_MANAGER_EMPTY);
		}
	}
}
