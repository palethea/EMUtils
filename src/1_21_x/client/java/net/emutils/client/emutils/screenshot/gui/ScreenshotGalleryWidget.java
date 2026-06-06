package net.emutils.client.emutils.screenshot.gui;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.screenshot.ScreenshotActions;
import net.emutils.client.emutils.screenshot.ScreenshotRepository.ScreenshotEntry;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public final class ScreenshotGalleryWidget extends AlwaysSelectedEntryListWidget<ScreenshotGalleryWidget.Entry> implements AutoCloseable {
	private static final int COLUMNS = 3;
	private static final int ROW_HEIGHT = 160;
	private static final int TILE_GAP = 8;
	private static final int TOOLBAR_HEIGHT = 26;
	private static final int PREVIEW_HEIGHT = 92;
	private static final int MAX_CACHED_TEXTURES = 32;
	private static final DateTimeFormatter DATE_FORMAT = Util.getDefaultLocaleFormatter(FormatStyle.SHORT);

	private final LinkedHashMap<Path, Thumbnail> thumbnails = new LinkedHashMap<>(16, 0.75F, true);
	private final ScreenshotThumbnailLoader thumbnailLoader;
	private Runnable onScreenshotsChanged;
	private int previewTargetWidth = 1;
	private int previewTargetHeight = 1;

	public ScreenshotGalleryWidget(MinecraftClient client, int width, int height) {
		super(client, width, height, 0, ROW_HEIGHT);
		centerListVertically = false;
		thumbnailLoader = new ScreenshotThumbnailLoader(client, this::onThumbnailLoaded);
	}

	public void setOnScreenshotsChanged(Runnable onScreenshotsChanged) {
		this.onScreenshotsChanged = onScreenshotsChanged;
	}

	public void setScreenshots(List<ScreenshotEntry> screenshots) {
		updatePreviewTargetSize();
		clearEntries();
		trimThumbnailCache(screenshots);
		if (screenshots.isEmpty()) {
			addEntry(new EmptyEntry(client));
			return;
		}

		for (int index = 0; index < screenshots.size(); index += COLUMNS) {
			addEntry(new RowEntry(this, client, screenshots.subList(index, Math.min(index + COLUMNS, screenshots.size()))));
		}
	}

	@Override
	public int getRowWidth() {
		return Math.min(width - 40, 720);
	}

	@Override
	public void setDimensions(int width, int height) {
		int previousTargetWidth = previewTargetWidth;
		int previousTargetHeight = previewTargetHeight;
		super.setDimensions(width, height);
		updatePreviewTargetSize();
		if (previewTargetWidth != previousTargetWidth || previewTargetHeight != previousTargetHeight) {
			invalidateThumbnailCache();
		}
	}

	private void invalidateThumbnailCache() {
		for (Thumbnail thumbnail : thumbnails.values()) {
			client.getTextureManager().destroyTexture(thumbnail.id);
		}
		thumbnails.clear();
		thumbnailLoader.clearFailures();
	}

	private void onThumbnailLoaded(Path path, ScreenshotThumbnailLoader.LoadedThumbnail loaded) {
		if (loaded.targetWidth() != previewTargetWidth || loaded.targetHeight() != previewTargetHeight) {
			client.getTextureManager().destroyTexture(loaded.id());
			return;
		}
		Thumbnail existing = thumbnails.get(path);
		if (existing != null) {
			client.getTextureManager().destroyTexture(existing.id);
		}

		Thumbnail thumbnail = new Thumbnail(loaded.id(), loaded.width(), loaded.height(), loaded.targetWidth(), loaded.targetHeight());
		thumbnails.put(path, thumbnail);
		trimCache();
	}

	private void updatePreviewTargetSize() {
		int tileWidth = Math.max(1, (getRowWidth() - TILE_GAP * (COLUMNS - 1)) / COLUMNS);
		int previewWidth = Math.max(1, tileWidth - 12);
		double scaleFactor = client.getWindow().getScaleFactor();
		previewTargetWidth = Math.max(1, (int) Math.ceil(previewWidth * scaleFactor));
		previewTargetHeight = Math.max(1, (int) Math.ceil(PREVIEW_HEIGHT * scaleFactor));
	}

	@Override
	public void close() {
		thumbnailLoader.close();
		for (Thumbnail thumbnail : thumbnails.values()) {
			client.getTextureManager().destroyTexture(thumbnail.id);
		}
		thumbnails.clear();
	}

	private void requestThumbnail(ScreenshotEntry screenshot) {
		thumbnailLoader.request(screenshot, previewTargetWidth, previewTargetHeight);
	}

	private Thumbnail thumbnail(ScreenshotEntry screenshot) {
		Path path = screenshot.path();
		Thumbnail existing = thumbnails.get(path);
		if (existing != null) {
			if (existing.targetWidth() == previewTargetWidth && existing.targetHeight() == previewTargetHeight) {
				return existing;
			}

			client.getTextureManager().destroyTexture(existing.id());
			thumbnails.remove(path);
		}

		if (!hasFailed(screenshot) && !isLoading(screenshot)) {
			requestThumbnail(screenshot);
		}

		return null;
	}

	private boolean isLoading(ScreenshotEntry screenshot) {
		return thumbnailLoader.isActive(screenshot, previewTargetWidth, previewTargetHeight);
	}

	private boolean hasFailed(ScreenshotEntry screenshot) {
		return thumbnailLoader.hasFailed(screenshot, previewTargetWidth, previewTargetHeight);
	}

	private void trimThumbnailCache(List<ScreenshotEntry> screenshots) {
		Set<Path> validPaths = new HashSet<>();
		for (ScreenshotEntry screenshot : screenshots) {
			validPaths.add(screenshot.path());
		}

		Iterator<Map.Entry<Path, Thumbnail>> iterator = thumbnails.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Path, Thumbnail> entry = iterator.next();
			if (!validPaths.contains(entry.getKey())) {
				client.getTextureManager().destroyTexture(entry.getValue().id);
				iterator.remove();
			}
		}
	}

	private void trimCache() {
		Iterator<Thumbnail> iterator = thumbnails.values().iterator();
		while (thumbnails.size() > MAX_CACHED_TEXTURES && iterator.hasNext()) {
			Thumbnail thumbnail = iterator.next();
			client.getTextureManager().destroyTexture(thumbnail.id);
			iterator.remove();
		}
	}

	private void deleteScreenshot(ScreenshotEntry screenshot) {
		if (!ScreenshotActions.deleteWithFeedback(client, screenshot.path().toFile())) {
			return;
		}

		Path path = screenshot.path();
		Thumbnail thumbnail = thumbnails.remove(path);
		if (thumbnail != null) {
			client.getTextureManager().destroyTexture(thumbnail.id);
		}

		if (onScreenshotsChanged != null) {
			onScreenshotsChanged.run();
		}
	}

	abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
	}

	private static final class EmptyEntry extends Entry {
		private final MinecraftClient client;

		private EmptyEntry(MinecraftClient client) {
			this.client = client;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			Text text = Text.translatable(EMUtilsTexts.GALLERY_EMPTY).formatted(Formatting.GRAY);
			int x = getContentMiddleX() - client.textRenderer.getWidth(text) / 2;
			context.drawTextWithShadow(client.textRenderer, text, x, getContentMiddleY() - 4, Colors.LIGHT_GRAY);
		}

		@Override
		public Text getNarration() {
			return Text.translatable(EMUtilsTexts.GALLERY_EMPTY);
		}
	}

	private static final class RowEntry extends Entry {
		private final List<Tile> tiles;

		private RowEntry(ScreenshotGalleryWidget widget, MinecraftClient client, List<ScreenshotEntry> screenshots) {
			tiles = new ArrayList<>();
			for (ScreenshotEntry screenshot : screenshots) {
				tiles.add(new Tile(widget, client, screenshot));
			}
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int tileWidth = (getContentWidth() - TILE_GAP * (COLUMNS - 1)) / COLUMNS;
			for (int index = 0; index < tiles.size(); index++) {
				int x = getContentX() + index * (tileWidth + TILE_GAP);
				tiles.get(index).render(context, mouseX, mouseY, deltaTicks, x, getContentY() + 4, tileWidth, getContentHeight() - 8);
			}
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			for (Tile tile : tiles) {
				if (tile.mouseClicked(click, doubled)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean mouseReleased(Click click) {
			for (Tile tile : tiles) {
				if (tile.mouseReleased(click)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void forEachChild(Consumer<ClickableWidget> consumer) {
			for (Tile tile : tiles) {
				tile.forEachChild(consumer);
			}
		}

		@Override
		public Text getNarration() {
			return Text.literal(tiles.stream().map(Tile::filename).findFirst().orElse(""));
		}
	}

	private static final class Tile {
		private static final int BUTTON_SIZE = 20;
		private static final int BUTTON_COUNT = 4;
		private static final int BUTTON_GAP = 4;
		private final ScreenshotGalleryWidget widget;
		private final MinecraftClient client;
		private final ScreenshotEntry screenshot;
		private final GalleryIconButtonWidget copyButton;
		private final GalleryIconButtonWidget openButton;
		private final GalleryIconButtonWidget folderButton;
		private final GalleryIconButtonWidget deleteButton;

		private Tile(ScreenshotGalleryWidget widget, MinecraftClient client, ScreenshotEntry screenshot) {
			this.widget = widget;
			this.client = client;
			this.screenshot = screenshot;
			copyButton = iconButton(
				Text.translatable(EMUtilsTexts.CHAT_ACTION_COPY),
				GalleryIcons.COPY,
				button -> ScreenshotActions.copyWithFeedback(client, screenshot.path().toFile())
			);
			openButton = iconButton(
				Text.translatable(EMUtilsTexts.CHAT_ACTION_OPEN),
				GalleryIcons.OPEN,
				button -> ScreenshotActions.openImage(screenshot.path().toFile())
			);
			folderButton = iconButton(
				Text.translatable(EMUtilsTexts.CHAT_ACTION_FOLDER),
				GalleryIcons.FOLDER,
				button -> ScreenshotActions.openFolder(screenshot.path().toFile())
			);
			deleteButton = iconButton(
				Text.translatable(EMUtilsTexts.GALLERY_ACTION_DELETE),
				GalleryIcons.DELETE,
				button -> confirmOrDeleteScreenshot(screenshot)
			);
		}

		private static GalleryIconButtonWidget iconButton(Text label, Identifier texture, ButtonWidget.PressAction action) {
			return GalleryIconButtonWidget.create(label, texture, GalleryIcons.SIZE, action);
		}

		private void confirmOrDeleteScreenshot(ScreenshotEntry screenshot) {
			if (EMUtilsClient.config() == null || !EMUtilsClient.config().screenshotGalleryDeleteConfirmation()) {
				widget.deleteScreenshot(screenshot);
				return;
			}

			Screen galleryScreen = client.currentScreen;
			client.setScreen(new ConfirmScreen(
				confirmed -> {
					client.setScreen(galleryScreen);
					if (confirmed) {
						widget.deleteScreenshot(screenshot);
					}
				},
				Text.translatable(EMUtilsTexts.GALLERY_DELETE_TITLE),
				Text.translatable(EMUtilsTexts.GALLERY_DELETE_MESSAGE, screenshot.filename()),
				Text.translatable(EMUtilsTexts.GALLERY_ACTION_DELETE),
				ScreenTexts.CANCEL
			));
		}

		private void render(DrawContext context, int mouseX, int mouseY, float deltaTicks, int x, int y, int width, int height) {
			context.fill(x, y, x + width, y + height, 0x66000000);
			context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xAA202020);

			int toolbarWidth = BUTTON_COUNT * BUTTON_SIZE + (BUTTON_COUNT - 1) * BUTTON_GAP;
			int toolbarX = x + (width - toolbarWidth) / 2;
			int buttonY = y + 4;
			positionButton(copyButton, toolbarX, buttonY);
			positionButton(openButton, toolbarX + (BUTTON_SIZE + BUTTON_GAP), buttonY);
			positionButton(folderButton, toolbarX + (BUTTON_SIZE + BUTTON_GAP) * 2, buttonY);
			positionButton(deleteButton, toolbarX + (BUTTON_SIZE + BUTTON_GAP) * 3, buttonY);
			copyButton.render(context, mouseX, mouseY, deltaTicks);
			openButton.render(context, mouseX, mouseY, deltaTicks);
			folderButton.render(context, mouseX, mouseY, deltaTicks);
			deleteButton.render(context, mouseX, mouseY, deltaTicks);

			int previewX = x + 6;
			int previewY = y + TOOLBAR_HEIGHT;
			int previewWidth = width - 12;
			Thumbnail thumbnail = widget.thumbnail(screenshot);
			if (thumbnail != null) {
				double scale = Math.min(previewWidth / (double) thumbnail.width, PREVIEW_HEIGHT / (double) thumbnail.height);
				int drawWidth = Math.max(1, (int) Math.round(thumbnail.width * scale));
				int drawHeight = Math.max(1, (int) Math.round(thumbnail.height * scale));
				int drawX = previewX + (previewWidth - drawWidth) / 2;
				int drawY = previewY + (PREVIEW_HEIGHT - drawHeight) / 2;
				context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					thumbnail.id,
					drawX,
					drawY,
					0.0F,
					0.0F,
					drawWidth,
					drawHeight,
					thumbnail.width,
					thumbnail.height,
					thumbnail.width,
					thumbnail.height
				);
			} else if (widget.isLoading(screenshot)) {
				GalleryLoadingSpinner.render(context, previewX + previewWidth / 2, previewY + PREVIEW_HEIGHT / 2);
			} else {
				Text missing = Text.literal("?");
				context.drawTextWithShadow(
					client.textRenderer,
					missing,
					previewX + previewWidth / 2 - client.textRenderer.getWidth(missing) / 2,
					previewY + PREVIEW_HEIGHT / 2 - 4,
					Colors.GRAY
				);
			}

			int textY = previewY + PREVIEW_HEIGHT + 5;
			Text filename = Text.literal(client.textRenderer.trimToWidth(screenshot.filename(), previewWidth));
			context.drawCenteredTextWithShadow(client.textRenderer, filename, x + width / 2, textY, Colors.WHITE);
			String date = DATE_FORMAT.format(Instant.ofEpochMilli(screenshot.modifiedMillis()).atZone(ZoneId.systemDefault()));
			Text dateText = Text.literal(client.textRenderer.trimToWidth(date, previewWidth));
			context.drawCenteredTextWithShadow(client.textRenderer, dateText, x + width / 2, textY + 11, Colors.LIGHT_GRAY);
		}

		private static void positionButton(GalleryIconButtonWidget button, int x, int y) {
			button.setPosition(x, y);
			button.setDimensions(BUTTON_SIZE, BUTTON_SIZE);
		}

		private boolean mouseClicked(Click click, boolean doubled) {
			return copyButton.mouseClicked(click, doubled)
				|| openButton.mouseClicked(click, doubled)
				|| folderButton.mouseClicked(click, doubled)
				|| deleteButton.mouseClicked(click, doubled);
		}

		private boolean mouseReleased(Click click) {
			return copyButton.mouseReleased(click)
				|| openButton.mouseReleased(click)
				|| folderButton.mouseReleased(click)
				|| deleteButton.mouseReleased(click);
		}

		private void forEachChild(Consumer<ClickableWidget> consumer) {
			consumer.accept(copyButton);
			consumer.accept(openButton);
			consumer.accept(folderButton);
			consumer.accept(deleteButton);
		}

		private String filename() {
			return screenshot.filename();
		}
	}

	private record Thumbnail(Identifier id, int width, int height, int targetWidth, int targetHeight) {
	}
}
