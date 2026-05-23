package net.emutils.client.gui.screenshot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
import java.util.Set;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.screenshot.ScreenshotActions;
import net.emutils.client.screenshot.ScreenshotRepository.ScreenshotEntry;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public final class ScreenshotGalleryWidget extends AlwaysSelectedEntryListWidget<ScreenshotGalleryWidget.Entry> implements AutoCloseable {
	private static final int COLUMNS = 3;
	private static final int ROW_HEIGHT = 174;
	private static final int TILE_GAP = 8;
	private static final int PREVIEW_HEIGHT = 92;
	private static final int MAX_CACHED_TEXTURES = 32;
	private static final DateTimeFormatter DATE_FORMAT = Util.getDefaultLocaleFormatter(FormatStyle.SHORT);

	private final LinkedHashMap<Path, Thumbnail> thumbnails = new LinkedHashMap<>(16, 0.75F, true);
	private final Set<Path> failedThumbnails = new HashSet<>();
	private int previewTargetWidth = 1;
	private int previewTargetHeight = 1;

	public ScreenshotGalleryWidget(MinecraftClient client, int width, int height) {
		super(client, width, height, 0, ROW_HEIGHT);
		centerListVertically = false;
	}

	public void setScreenshots(List<ScreenshotEntry> screenshots) {
		updatePreviewTargetSize();
		clearEntries();
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
			close();
			failedThumbnails.clear();
		}
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
		for (Thumbnail thumbnail : thumbnails.values()) {
			client.getTextureManager().destroyTexture(thumbnail.id);
		}
		thumbnails.clear();
		failedThumbnails.clear();
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

		if (failedThumbnails.contains(path)) {
			return null;
		}

		try (InputStream stream = Files.newInputStream(path)) {
			NativeImage image = NativeImage.read(stream);
			NativeImage thumbnailImage = scaleToPreviewSize(image, previewTargetWidth, previewTargetHeight);
			if (thumbnailImage != image) {
				image.close();
			}

			Identifier id = Identifier.of(
				EMUtilsClient.MOD_ID,
				"screenshot_gallery/" + Integer.toUnsignedString(path.toString().hashCode(), 16) + "_" + screenshot.modifiedMillis()
			);
			client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> screenshot.filename(), thumbnailImage));
			Thumbnail thumbnail = new Thumbnail(id, thumbnailImage.getWidth(), thumbnailImage.getHeight(), previewTargetWidth, previewTargetHeight);
			thumbnails.put(path, thumbnail);
			trimCache();
			return thumbnail;
		} catch (IOException | RuntimeException exception) {
			failedThumbnails.add(path);
			EMUtilsClient.LOGGER.warn("Failed to load screenshot thumbnail {}.", path, exception);
			return null;
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

	private static NativeImage scaleToPreviewSize(NativeImage image, int maxWidth, int maxHeight) {
		int width = image.getWidth();
		int height = image.getHeight();
		if (width <= maxWidth && height <= maxHeight) {
			return image;
		}

		double scale = Math.min(maxWidth / (double) width, maxHeight / (double) height);
		int targetWidth = Math.max(1, (int) Math.round(width * scale));
		int targetHeight = Math.max(1, (int) Math.round(height * scale));
		NativeImage scaled = new NativeImage(targetWidth, targetHeight, false);
		image.resizeSubRectTo(0, 0, width, height, scaled);
		return scaled;
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
		private static final int BUTTON_HEIGHT = 18;
		private final ScreenshotGalleryWidget widget;
		private final MinecraftClient client;
		private final ScreenshotEntry screenshot;
		private final ButtonWidget copyButton;
		private final ButtonWidget openButton;
		private final ButtonWidget folderButton;

		private Tile(ScreenshotGalleryWidget widget, MinecraftClient client, ScreenshotEntry screenshot) {
			this.widget = widget;
			this.client = client;
			this.screenshot = screenshot;
			copyButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.CHAT_ACTION_COPY), button -> ScreenshotActions.copyWithFeedback(client, screenshot.path().toFile())).build();
			openButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.CHAT_ACTION_OPEN), button -> ScreenshotActions.openImage(screenshot.path().toFile())).build();
			folderButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.CHAT_ACTION_FOLDER), button -> ScreenshotActions.openFolder(screenshot.path().toFile())).build();
		}

		private void render(DrawContext context, int mouseX, int mouseY, float deltaTicks, int x, int y, int width, int height) {
			context.fill(x, y, x + width, y + height, 0x66000000);
			context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xAA202020);

			int previewX = x + 6;
			int previewY = y + 6;
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
			} else {
				Text missing = Text.literal("?");
				context.drawTextWithShadow(client.textRenderer, missing, previewX + previewWidth / 2 - client.textRenderer.getWidth(missing) / 2, previewY + PREVIEW_HEIGHT / 2 - 4, Colors.GRAY);
			}

			int textY = previewY + PREVIEW_HEIGHT + 5;
			context.drawTextWithShadow(client.textRenderer, client.textRenderer.trimToWidth(screenshot.filename(), previewWidth), previewX, textY, Colors.WHITE);
			String date = DATE_FORMAT.format(Instant.ofEpochMilli(screenshot.modifiedMillis()).atZone(ZoneId.systemDefault()));
			context.drawTextWithShadow(client.textRenderer, client.textRenderer.trimToWidth(date, previewWidth), previewX, textY + 11, Colors.LIGHT_GRAY);

			int buttonY = y + height - BUTTON_HEIGHT - 5;
			int buttonWidth = Math.max(34, (width - 18) / 3);
			copyButton.setDimensions(buttonWidth, BUTTON_HEIGHT);
			openButton.setDimensions(buttonWidth, BUTTON_HEIGHT);
			folderButton.setDimensions(buttonWidth, BUTTON_HEIGHT);
			copyButton.setPosition(x + 5, buttonY);
			openButton.setPosition(x + 9 + buttonWidth, buttonY);
			folderButton.setPosition(x + 13 + buttonWidth * 2, buttonY);
			copyButton.render(context, mouseX, mouseY, deltaTicks);
			openButton.render(context, mouseX, mouseY, deltaTicks);
			folderButton.render(context, mouseX, mouseY, deltaTicks);
		}

		private boolean mouseClicked(Click click, boolean doubled) {
			return copyButton.mouseClicked(click, doubled)
				|| openButton.mouseClicked(click, doubled)
				|| folderButton.mouseClicked(click, doubled);
		}

		private boolean mouseReleased(Click click) {
			return copyButton.mouseReleased(click) || openButton.mouseReleased(click) || folderButton.mouseReleased(click);
		}

		private void forEachChild(Consumer<ClickableWidget> consumer) {
			consumer.accept(copyButton);
			consumer.accept(openButton);
			consumer.accept(folderButton);
		}

		private String filename() {
			return screenshot.filename();
		}
	}

	private record Thumbnail(Identifier id, int width, int height, int targetWidth, int targetHeight) {
	}
}
