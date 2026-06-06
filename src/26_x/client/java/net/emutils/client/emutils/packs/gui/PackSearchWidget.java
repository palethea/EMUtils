package net.emutils.client.emutils.packs.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.emutils.client.emutils.compat.IrisCompat;
import net.emutils.client.emutils.screenshot.gui.GalleryIconButtonWidget;
import net.emutils.client.emutils.screenshot.gui.GalleryLoadingSpinner;
import net.emutils.client.emutils.packs.PackType;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

public final class PackSearchWidget extends ObjectSelectionList<PackSearchWidget.Entry> implements AutoCloseable {
	public enum ViewMode {
		INSTALLED,
		MODRINTH
	}

	private static final int ROW_HEIGHT = 76;
	private static final int ICON_SIZE = 48;

	private final Actions actions;
	private final PackIconLoader iconLoader;
	private ViewMode viewMode = ViewMode.INSTALLED;

	public PackSearchWidget(
		Minecraft client,
		int width,
		int height,
		Consumer<PackListItem> download,
		Consumer<PackListItem> enable,
		Consumer<PackListItem> disable,
		Consumer<PackListItem> delete,
		Consumer<PackListItem> applyShader,
		Consumer<PackListItem> disableShader
	) {
		super(client, width, height, 0, ROW_HEIGHT);
		centerListVertically = false;
		actions = new Actions(download, enable, disable, delete, applyShader, disableShader);
		iconLoader = new PackIconLoader(client, this::refreshIcons);
	}

	public void setItems(List<PackListItem> items, ViewMode viewMode, Component emptyHint) {
		this.viewMode = viewMode;
		clearEntries();
		if (items.isEmpty()) {
			if (!emptyHint.getString().isEmpty()) {
				addEntry(new HintEntry(emptyHint));
			}
			return;
		}

		for (PackListItem item : items) {
			addEntry(new ItemEntry(minecraft, item, viewMode, actions, iconLoader));
		}
	}

	@Override
	public int getRowWidth() {
		return Math.min(width - 36, 760);
	}

	@Override
	public void close() {
		iconLoader.close();
	}

	private void refreshIcons() {
		children().forEach(child -> {
			if (child instanceof ItemEntry itemEntry) {
				itemEntry.refreshIcon();
			}
		});
	}

	abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
	}

	private static final class HintEntry extends Entry {
		private final Component text;

		private HintEntry(Component text) {
			this.text = text;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			context.centeredText(
				Minecraft.getInstance().font,
				text,
				getContentX() + getContentWidth() / 2,
				getContentYMiddle() - 4,
				CommonColors.LIGHT_GRAY
			);
		}

		@Override
		public Component getNarration() {
			return text;
		}
	}

	private static final class ItemEntry extends Entry {
		private static final int BUTTON_SIZE = 20;
		private static final int BUTTON_GAP = 4;

		private final Minecraft client;
		private final PackListItem item;
		private final ViewMode viewMode;
		private final Actions actions;
		private final PackIconLoader iconLoader;
		private final List<GalleryIconButtonWidget> buttons = new ArrayList<>();
		private Identifier icon;
		private PackIconLoader.State iconState = PackIconLoader.State.NONE;

		private ItemEntry(Minecraft client, PackListItem item, ViewMode viewMode, Actions actions, PackIconLoader iconLoader) {
			this.client = client;
			this.item = item;
			this.viewMode = viewMode;
			this.actions = actions;
			this.iconLoader = iconLoader;
			this.icon = item.fallbackIcon();
			refreshIcon();
			rebuildButtons();
		}

		private void refreshIcon() {
			PackIconLoader.IconResult result = iconLoader.resolve(item.iconUrl(), item.fallbackIcon());
			this.icon = result.texture();
			this.iconState = result.state();
		}

		private void rebuildButtons() {
			buttons.clear();
			if (viewMode == ViewMode.MODRINTH && item.installed() == null) {
				buttons.add(createButton(Component.translatable(EMUtilsTexts.PACK_DOWNLOAD), PackIcons.DOWNLOAD, () -> actions.download().accept(item)));
				return;
			}

			if (item.type() == PackType.RESOURCE) {
				boolean enabled = item.installed() != null && item.installed().enabled();
				buttons.add(createButton(
					Component.translatable(enabled ? EMUtilsTexts.PACK_DISABLE : EMUtilsTexts.PACK_ENABLE),
					enabled ? PackIcons.DISABLE : PackIcons.ENABLE,
					() -> {
						if (enabled) {
							actions.disable().accept(item);
						} else {
							actions.enable().accept(item);
						}
					}
				));
			} else if (item.installed() != null) {
				boolean selected = IrisCompat.isActiveShaderPack(item.installed().filename());
				buttons.add(createButton(
					Component.translatable(selected
						? EMUtilsTexts.PACK_TURN_OFF
						: IrisCompat.isIrisLoaded() ? EMUtilsTexts.PACK_APPLY : EMUtilsTexts.PACK_STATUS_IRIS_REQUIRED),
					selected ? PackIcons.DISABLE : PackIcons.SHADER,
					() -> {
						if (selected) {
							actions.disableShader().accept(item);
						} else {
							actions.applyShader().accept(item);
						}
					}
				));
			}

			if (item.installed() != null) {
				buttons.add(createButton(Component.translatable(EMUtilsTexts.PACK_DELETE), PackIcons.DELETE, () -> actions.delete().accept(item)));
			}
		}

		@Override
		public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int x = getContentX();
			int y = getContentY() + 4;
			int width = getContentWidth();
			context.fill(x, y, x + width, y + getContentHeight() - 8, hovered ? 0xAA2C2C2C : 0xAA202020);

			int iconX = x + 8;
			int iconY = y + 8;
			context.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, 0xFF303030);
			if (iconState == PackIconLoader.State.LOADING) {
				GalleryLoadingSpinner.render(context, iconX + ICON_SIZE / 2, iconY + ICON_SIZE / 2);
			} else {
				context.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
			}

			int textX = iconX + ICON_SIZE + 8;
			int textWidth = Math.max(80, width - ICON_SIZE - 24 - buttons.size() * (BUTTON_SIZE + BUTTON_GAP));
			context.text(client.font, titleText(textWidth), textX, y + 8, CommonColors.WHITE);
			context.text(client.font, metaText(textWidth), textX, y + 21, CommonColors.LIGHT_GRAY);
			context.text(client.font, descriptionText(textWidth), textX, y + 34, CommonColors.GRAY);
			context.text(client.font, statusText(), textX, y + 49, statusColor());

			int buttonX = x + width - BUTTON_SIZE - 8;
			int buttonY = y + 10;
			for (int index = buttons.size() - 1; index >= 0; index--) {
				GalleryIconButtonWidget button = buttons.get(index);
				button.setPosition(buttonX, buttonY);
				button.extractRenderState(context, mouseX, mouseY, deltaTicks);
				buttonX -= BUTTON_SIZE + BUTTON_GAP;
			}
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
			for (GalleryIconButtonWidget button : buttons) {
				if (button.mouseClicked(click, doubled)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean mouseReleased(MouseButtonEvent click) {
			for (GalleryIconButtonWidget button : buttons) {
				if (button.mouseReleased(click)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void visitWidgets(Consumer<AbstractWidget> consumer) {
			buttons.forEach(consumer);
		}

		@Override
		public Component getNarration() {
			return Component.literal(item.title());
		}

		private Component titleText(int maxWidth) {
			return Component.literal(client.font.plainSubstrByWidth(item.title(), maxWidth)).withStyle(ChatFormatting.WHITE);
		}

		private Component metaText(int maxWidth) {
			String meta = item.author().isBlank() ? typeLabel() : item.author() + " • " + typeLabel();
			if (item.result() != null) {
				meta += " • " + item.result().downloads() + " downloads";
			}
			return Component.literal(client.font.plainSubstrByWidth(meta, maxWidth)).withStyle(ChatFormatting.GRAY);
		}

		private Component descriptionText(int maxWidth) {
			return Component.literal(client.font.plainSubstrByWidth(item.description(), maxWidth)).withStyle(ChatFormatting.DARK_GRAY);
		}

		private Component statusText() {
			if (viewMode == ViewMode.MODRINTH && item.installed() == null) {
				return Component.translatable(EMUtilsTexts.PACK_STATUS_NOT_INSTALLED).withStyle(ChatFormatting.YELLOW);
			}
			if (item.type() == PackType.RESOURCE && item.installed() != null && item.installed().enabled()) {
				return Component.translatable(EMUtilsTexts.PACK_STATUS_ENABLED).withStyle(ChatFormatting.GREEN);
			}
			if (item.type() == PackType.SHADER
				&& item.installed() != null
				&& IrisCompat.isActiveShaderPack(item.installed().filename())) {
				return Component.translatable(EMUtilsTexts.PACK_STATUS_SELECTED).withStyle(ChatFormatting.GREEN);
			}
			return Component.translatable(EMUtilsTexts.PACK_STATUS_INSTALLED).withStyle(ChatFormatting.AQUA);
		}

		private int statusColor() {
			if (viewMode == ViewMode.MODRINTH && item.installed() == null) {
				return CommonColors.YELLOW;
			}
			if (item.installed() != null && item.installed().enabled()) {
				return CommonColors.GREEN;
			}
			return 0xFF55FFFF;
		}

		private String typeLabel() {
			return item.type() == PackType.RESOURCE ? "Resource Pack" : "Shader Pack";
		}

		private static GalleryIconButtonWidget createButton(Component tooltip, Identifier icon, Runnable action) {
			return GalleryIconButtonWidget.create(tooltip, icon, PackIcons.SIZE, button -> action.run());
		}
	}

	private record Actions(
		Consumer<PackListItem> download,
		Consumer<PackListItem> enable,
		Consumer<PackListItem> disable,
		Consumer<PackListItem> delete,
		Consumer<PackListItem> applyShader,
		Consumer<PackListItem> disableShader
	) {
	}
}
