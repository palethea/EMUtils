package net.emutils.client.gui.packs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.emutils.client.compat.IrisCompat;
import net.emutils.client.gui.screenshot.GalleryIconButtonWidget;
import net.emutils.client.gui.screenshot.GalleryLoadingSpinner;
import net.emutils.client.packs.PackType;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class PackSearchWidget extends AlwaysSelectedEntryListWidget<PackSearchWidget.Entry> implements AutoCloseable {
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
		MinecraftClient client,
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

	public void setItems(List<PackListItem> items, ViewMode viewMode, Text emptyHint) {
		this.viewMode = viewMode;
		clearEntries();
		if (items.isEmpty()) {
			if (!emptyHint.getString().isEmpty()) {
				addEntry(new HintEntry(emptyHint));
			}
			return;
		}

		for (PackListItem item : items) {
			addEntry(new ItemEntry(client, item, viewMode, actions, iconLoader));
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

	abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
	}

	private static final class HintEntry extends Entry {
		private final Text text;

		private HintEntry(Text text) {
			this.text = text;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			context.drawCenteredTextWithShadow(
				MinecraftClient.getInstance().textRenderer,
				text,
				getContentX() + getContentWidth() / 2,
				getContentMiddleY() - 4,
				Colors.LIGHT_GRAY
			);
		}

		@Override
		public Text getNarration() {
			return text;
		}
	}

	private static final class ItemEntry extends Entry {
		private static final int BUTTON_SIZE = 20;
		private static final int BUTTON_GAP = 4;

		private final MinecraftClient client;
		private final PackListItem item;
		private final ViewMode viewMode;
		private final Actions actions;
		private final PackIconLoader iconLoader;
		private final List<GalleryIconButtonWidget> buttons = new ArrayList<>();
		private Identifier icon;
		private PackIconLoader.State iconState = PackIconLoader.State.NONE;

		private ItemEntry(MinecraftClient client, PackListItem item, ViewMode viewMode, Actions actions, PackIconLoader iconLoader) {
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
				buttons.add(createButton(Text.translatable(EMUtilsTexts.PACK_DOWNLOAD), PackIcons.DOWNLOAD, () -> actions.download().accept(item)));
				return;
			}

			if (item.type() == PackType.RESOURCE) {
				boolean enabled = item.installed() != null && item.installed().enabled();
				buttons.add(createButton(
					Text.translatable(enabled ? EMUtilsTexts.PACK_DISABLE : EMUtilsTexts.PACK_ENABLE),
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
					Text.translatable(selected
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
				buttons.add(createButton(Text.translatable(EMUtilsTexts.PACK_DELETE), PackIcons.DELETE, () -> actions.delete().accept(item)));
			}
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
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
				context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
			}

			int textX = iconX + ICON_SIZE + 8;
			int textWidth = Math.max(80, width - ICON_SIZE - 24 - buttons.size() * (BUTTON_SIZE + BUTTON_GAP));
			context.drawTextWithShadow(client.textRenderer, titleText(textWidth), textX, y + 8, Colors.WHITE);
			context.drawTextWithShadow(client.textRenderer, metaText(textWidth), textX, y + 21, Colors.LIGHT_GRAY);
			context.drawTextWithShadow(client.textRenderer, descriptionText(textWidth), textX, y + 34, Colors.GRAY);
			context.drawTextWithShadow(client.textRenderer, statusText(), textX, y + 49, statusColor());

			int buttonX = x + width - BUTTON_SIZE - 8;
			int buttonY = y + 10;
			for (int index = buttons.size() - 1; index >= 0; index--) {
				GalleryIconButtonWidget button = buttons.get(index);
				button.setPosition(buttonX, buttonY);
				button.render(context, mouseX, mouseY, deltaTicks);
				buttonX -= BUTTON_SIZE + BUTTON_GAP;
			}
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			for (GalleryIconButtonWidget button : buttons) {
				if (button.mouseClicked(click, doubled)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean mouseReleased(Click click) {
			for (GalleryIconButtonWidget button : buttons) {
				if (button.mouseReleased(click)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void forEachChild(Consumer<ClickableWidget> consumer) {
			buttons.forEach(consumer);
		}

		@Override
		public Text getNarration() {
			return Text.literal(item.title());
		}

		private Text titleText(int maxWidth) {
			return Text.literal(client.textRenderer.trimToWidth(item.title(), maxWidth)).formatted(Formatting.WHITE);
		}

		private Text metaText(int maxWidth) {
			String meta = item.author().isBlank() ? typeLabel() : item.author() + " • " + typeLabel();
			if (item.result() != null) {
				meta += " • " + item.result().downloads() + " downloads";
			}
			return Text.literal(client.textRenderer.trimToWidth(meta, maxWidth)).formatted(Formatting.GRAY);
		}

		private Text descriptionText(int maxWidth) {
			return Text.literal(client.textRenderer.trimToWidth(item.description(), maxWidth)).formatted(Formatting.DARK_GRAY);
		}

		private Text statusText() {
			if (viewMode == ViewMode.MODRINTH && item.installed() == null) {
				return Text.translatable(EMUtilsTexts.PACK_STATUS_NOT_INSTALLED).formatted(Formatting.YELLOW);
			}
			if (item.type() == PackType.RESOURCE && item.installed() != null && item.installed().enabled()) {
				return Text.translatable(EMUtilsTexts.PACK_STATUS_ENABLED).formatted(Formatting.GREEN);
			}
			if (item.type() == PackType.SHADER
				&& item.installed() != null
				&& IrisCompat.isActiveShaderPack(item.installed().filename())) {
				return Text.translatable(EMUtilsTexts.PACK_STATUS_SELECTED).formatted(Formatting.GREEN);
			}
			return Text.translatable(EMUtilsTexts.PACK_STATUS_INSTALLED).formatted(Formatting.AQUA);
		}

		private int statusColor() {
			if (viewMode == ViewMode.MODRINTH && item.installed() == null) {
				return Colors.YELLOW;
			}
			if (item.installed() != null && item.installed().enabled()) {
				return Colors.GREEN;
			}
			return 0xFF55FFFF;
		}

		private String typeLabel() {
			return item.type() == PackType.RESOURCE ? "Resource Pack" : "Shader Pack";
		}

		private static GalleryIconButtonWidget createButton(Text tooltip, Identifier icon, Runnable action) {
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
