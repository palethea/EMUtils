package net.emutils.client.emutils.minescript.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.emutils.client.emutils.minescript.MinescriptScript;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;

public final class ScriptTreeWidget extends ObjectSelectionList<ScriptTreeWidget.Entry> {
	private static final int ROW_HEIGHT = 22;
	private final Minecraft client;
	private final Consumer<MinescriptScript> onSelected;
	private final Set<String> collapsedDirectories = new HashSet<>();
	private List<MinescriptScript> scripts = List.of();
	private MinescriptScript selectedScript;

	public ScriptTreeWidget(Minecraft client, int width, int height, Consumer<MinescriptScript> onSelected) {
		super(client, width, height, 0, ROW_HEIGHT);
		centerListVertically = false;
		this.client = client;
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
	public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		context.fill(getX(), getY(), getX() + width, getY() + height, 0xDD101010);
		context.outline(getX(), getY(), width, height, isFocused() ? 0xFF69A7D8 : 0xFF444444);
		super.extractWidgetRenderState(context, mouseX, mouseY, deltaTicks);
	}

	@Override
	protected void extractListBackground(GuiGraphicsExtractor context) {
	}

	@Override
	protected void extractListSeparators(GuiGraphicsExtractor context) {
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

	abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
	}

	private final class ScriptEntry extends Entry {
		private final Minecraft client;
		private final MinescriptScript script;

		private ScriptEntry(Minecraft client, MinescriptScript script) {
			this.client = client;
			this.script = script;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int x = getContentX();
			int y = getContentY();
			boolean selected = script.equals(selectedScript);
			if (selected || hovered) {
				context.fill(x, y, x + getContentWidth(), y + getContentHeight(), selected ? 0xAA315A7D : 0x66303030);
			}
			String prefix = script.directory() ? (collapsedDirectories.contains(script.relativePath()) ? "[+] " : "[-] ") : "    ";
			String label = "  ".repeat(Math.max(0, script.depth())) + prefix + script.displayName();
			ChatFormatting color = script.directory() ? ChatFormatting.AQUA : script.editable() ? ChatFormatting.WHITE : ChatFormatting.GRAY;
			context.text(client.font, Component.literal(client.font.plainSubstrByWidth(label, getContentWidth() - 8)).withStyle(color), x + 4, y + 6, CommonColors.WHITE);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
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
		public Component getNarration() {
			return Component.literal(script.displayName());
		}
	}

	private static final class EmptyEntry extends Entry {
		private final Minecraft client;

		private EmptyEntry(Minecraft client) {
			this.client = client;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			Component text = Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_EMPTY).withStyle(ChatFormatting.GRAY);
			context.centeredText(client.font, text, getContentXMiddle(), getContentYMiddle() - 4, CommonColors.LIGHT_GRAY);
		}

		@Override
		public Component getNarration() {
			return Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_EMPTY);
		}
	}
}
