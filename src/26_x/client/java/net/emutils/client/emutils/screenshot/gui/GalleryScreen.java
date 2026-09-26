package net.emutils.client.emutils.screenshot.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiConfirmDialog;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.screenshot.ScreenshotActions;
import net.emutils.client.emutils.screenshot.ScreenshotGallerySort;
import net.emutils.client.emutils.screenshot.ScreenshotPaths;
import net.emutils.client.emutils.screenshot.ScreenshotRepository;
import net.emutils.client.emutils.screenshot.ScreenshotRepository.ScreenshotEntry;
import net.emutils.client.emutils.screenshot.gui.ScreenshotThumbnailLoader.LoadedThumbnail;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.versioned.VersionedPlatform;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The Screenshot Gallery (#106): a grid of the screenshots with their names and dates,
 * actions to copy, open, show in the folder and delete each one, and a large preview.
 */
public final class GalleryScreen extends UiPanelScreen {
	private static final int PADDING = 16;
	private static final int GAP = 10;
	private static final int MIN_TILE_WIDTH = 150;
	private static final int MAX_COLUMNS = 4;
	private static final int TILE_RADIUS = 9;
	private static final int IMAGE_INSET = 6;
	private static final int FADE_HEIGHT = 12;
	private static final int ACTION = 20;
	private static final int ACTION_GAP = 2;
	private static final int HEADER_BUTTON = 20;
	static final Identifier[] ACTION_ICONS = {HubIcons.COPY, HubIcons.EXTERNAL_LINK, HubIcons.FOLDER, HubIcons.TRASH};
	static final String[] ACTION_TIPS = {EMUtilsTexts.UI_COPY_IMAGE, EMUtilsTexts.UI_OPEN_IN_VIEWER, EMUtilsTexts.UI_SHOW_IN_FOLDER, EMUtilsTexts.UI_DELETE};
	static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);

	private final UiScrollArea scroll = new UiScrollArea();
	private final List<TileBox> tiles = new ArrayList<>();
	private List<ScreenshotEntry> screenshots = List.of();
	private @Nullable GalleryPreview preview;
	/** Screenshots drawn this frame whose image was still loading; read by UI snapshots. */
	private final Set<Path> loadingThisFrame = new HashSet<>();
	private @Nullable UiConfirmDialog dialog;
	private int columns;
	private int tileWidth;
	private int imageHeight;
	private int tileHeight;
	private int headerButtonsY;
	private int folderX;
	private int sortX;
	private int @Nullable [] sortEdges;
	private @Nullable Component tooltip;
	private int tooltipX;
	private int tooltipY;

	public GalleryScreen(@Nullable Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_SCREENSHOT_GALLERY), parent);
		// Thumbnails stay cached between openings (#133); ones that failed get another try.
		thumbnails().retryFailed();
	}

	@Override
	protected void layout() {
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING) + 6 + UiText.lineHeight(font, UiText.Size.BODY);
		headerButtonsY = panelY + PADDING + (headerHeight - HEADER_BUTTON) / 2;
		int bodyY = panelY + PADDING + headerHeight + 12;
		scroll.setBounds(panelX + PADDING, bodyY, panelWidth - PADDING * 2 + UiScrollArea.GUTTER, panelY + panelHeight - PADDING / 2 - bodyY);
		int width = scroll.contentWidth();
		columns = Math.clamp((width + GAP) / (MIN_TILE_WIDTH + GAP), 1, MAX_COLUMNS);
		tileWidth = (width - GAP * (columns - 1)) / columns;
		imageHeight = (tileWidth - IMAGE_INSET * 2) * 9 / 16;
		tileHeight = IMAGE_INSET + imageHeight + 10 + UiText.lineHeight(font, UiText.Size.BOLD) + 6 + UiText.lineHeight(font, UiText.Size.BODY) + 10;
		refresh();
	}

	/** Reads the screenshots folder again, in the order and count the gallery settings ask for. */
	void refresh() {
		screenshots = ScreenshotRepository.list(minecraft);
	}

	List<ScreenshotEntry> screenshots() {
		return screenshots;
	}

	GalleryThumbnails thumbnails() {
		return GalleryThumbnails.shared();
	}

	/** Pixel size to load a screenshot at so it's drawn one pixel per screen pixel in a box of this GUI size. */
	int pixels(int guiSize) {
		return Math.max(1, (int) Math.ceil(guiSize * minecraft.getWindow().getGuiScale()));
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		tooltip = null;
		loadingThisFrame.clear();
		boolean interactive = preview == null && dialog == null && !closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		drawHeader(context, theme, hoverX, hoverY);
		drawGrid(context, theme, hoverX, hoverY);
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());
		int count = screenshots.size();
		Component countText = Component.translatable(count == 1 ? EMUtilsTexts.UI_SCREENSHOT_COUNT_ONE : EMUtilsTexts.UI_SCREENSHOT_COUNT, count);
		UiText.draw(context, font, countText, UiText.Size.BODY, left, top + UiText.lineHeight(font, UiText.Size.HEADING) + 6, theme.muted());

		folderX = panelX + panelWidth - PADDING - HEADER_BUTTON;
		boolean folderHovered = contains(mouseX, mouseY, folderX, headerButtonsY, HEADER_BUTTON, HEADER_BUTTON);
		UiWidgets.iconButton(context, theme, folderX, headerButtonsY, HEADER_BUTTON, HubIcons.FOLDER, folderHovered ? 1.0F : 0.0F);
		if (folderHovered) {
			showTooltip(Component.translatable(EMUtilsTexts.UI_OPEN_SCREENSHOTS_FOLDER), mouseX, mouseY);
		}

		List<Component> labels = List.of(Component.translatable(EMUtilsTexts.UI_SORT_NEWEST), Component.translatable(EMUtilsTexts.UI_SORT_OLDEST));
		sortX = folderX - 8 - UiWidgets.segmentedWidth(font, labels);
		int sortY = headerButtonsY + (HEADER_BUTTON - UiWidgets.SEGMENT_HEIGHT) / 2;
		int selected = EMUtilsClient.config().screenshotGallerySort() == ScreenshotGallerySort.OLDEST_FIRST ? 1 : 0;
		int[] edges = UiWidgets.segmentEdges(font, sortX, labels);
		int hovered = -1;
		for (int i = 0; i < labels.size(); i++) {
			if (contains(mouseX, mouseY, edges[i], sortY, edges[i + 1] - edges[i], UiWidgets.SEGMENT_HEIGHT)) {
				hovered = i;
			}
		}
		sortEdges = UiWidgets.segmented(context, font, theme, sortX, sortY, labels, anim.transition("gallery-sort", selected, 0.18F), hovered);
	}

	private void drawGrid(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int rows = (screenshots.size() + columns - 1) / columns;
		scroll.setContentHeight(rows == 0 ? 0 : rows * tileHeight + (rows - 1) * GAP + FADE_HEIGHT);
		scroll.animate(anim, "gallery-scroll", mouseX, mouseY);
		tiles.clear();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		scroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int top = scroll.y() + FADE_HEIGHT / 2 - scroll.offset();
		for (int i = 0; i < screenshots.size(); i++) {
			int x = scroll.x() + (i % columns) * (tileWidth + GAP);
			int y = top + (i / columns) * (tileHeight + GAP);
			if (y + tileHeight >= scroll.y() && y <= scroll.y() + scroll.height()) {
				tiles.add(drawTile(context, theme, i, x, y, mouseInList ? mouseX : Integer.MIN_VALUE / 2, mouseY));
			}
		}
		context.pose().popMatrix();
		if (screenshots.isEmpty()) {
			int centerX = scroll.x() + scroll.contentWidth() / 2;
			int iconSize = 22;
			int iconTop = scroll.y() + Math.max(20, scroll.height() / 2 - 30);
			UiIcons.draw(context, HubIcons.IMAGE, centerX - iconSize / 2, iconTop, iconSize, theme.muted());
			Component empty = Component.translatable(EMUtilsTexts.GALLERY_EMPTY);
			UiText.draw(context, font, empty, UiText.Size.BODY, centerX - UiText.width(font, empty, UiText.Size.BODY) / 2, iconTop + iconSize + 10, theme.muted());
		}
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	private TileBox drawTile(GuiGraphicsExtractor context, UiTheme theme, int index, int x, int y, int mouseX, int mouseY) {
		ScreenshotEntry screenshot = screenshots.get(index);
		boolean hovered = contains(mouseX, mouseY, x, y, tileWidth, tileHeight);
		float hover = anim.towards("tile:" + screenshot.path(), hovered, 16.0F);
		// Hovered cards rise onto a soft shadow, like the settings cards. Clicks use the resting position.
		context.pose().pushMatrix();
		context.pose().translate(0.0F, -hover);
		UiShapes.shadow(context, x, y + 2, tileWidth, tileHeight, TILE_RADIUS, 8, UiTheme.fade(theme.shadow(), hover * 0.9F));
		UiShapes.borderedRect(context, x, y, tileWidth, tileHeight, TILE_RADIUS, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover), theme.border());

		int imageX = x + IMAGE_INSET;
		int imageY = y + IMAGE_INSET;
		int imageWidth = tileWidth - IMAGE_INSET * 2;
		drawScreenshot(context, theme, screenshot, imageX, imageY, imageWidth, imageHeight);

		// The actions show over the bottom of the image while the card is hovered.
		int actionsWidth = ACTION_ICONS.length * ACTION + (ACTION_ICONS.length - 1) * ACTION_GAP;
		int actionsX = imageX + imageWidth - 4 - actionsWidth;
		int actionsY = imageY + imageHeight - 4 - ACTION;
		if (hover > 0.01F) {
			context.fillGradient(imageX, imageY + imageHeight - ACTION - 14, imageX + imageWidth, imageY + imageHeight, 0x00000000, UiOpacity.apply(UiTheme.fade(0xB0000000, hover)));
			for (int i = 0; i < ACTION_ICONS.length; i++) {
				int actionX = actionsX + i * (ACTION + ACTION_GAP);
				boolean actionHovered = contains(mouseX, mouseY, actionX, actionsY, ACTION, ACTION);
				// The actions sit on a dark shade in both themes, so they use the dark theme's colors.
				int iconColor = UiTheme.fade(i == 3 && actionHovered ? UiTheme.DARK.warning() : 0xFFFFFFFF, hover);
				UiShapes.roundedRect(context, actionX, actionsY, ACTION, ACTION, 6, UiTheme.fade(0x33FFFFFF, actionHovered ? hover : 0.0F));
				int iconSize = Math.round(ACTION * 0.6F);
				UiIcons.draw(context, ACTION_ICONS[i], actionX + (ACTION - iconSize) / 2, actionsY + (ACTION - iconSize) / 2, iconSize, iconColor);
				if (actionHovered) {
					showTooltip(Component.translatable(ACTION_TIPS[i]), mouseX, mouseY);
				}
			}
		}

		int textTop = imageY + imageHeight + 10;
		Component name = UiText.ellipsize(font, Component.literal(screenshot.filename()), UiText.Size.BOLD, tileWidth - IMAGE_INSET * 2 - 4);
		UiText.draw(context, font, name, UiText.Size.BOLD, x + IMAGE_INSET + 2, textTop, theme.text());
		Component date = Component.literal(DATE_FORMAT.format(Instant.ofEpochMilli(screenshot.modifiedMillis()).atZone(ZoneId.systemDefault())));
		UiText.draw(context, font, date, UiText.Size.BODY, x + IMAGE_INSET + 2, textTop + UiText.lineHeight(font, UiText.Size.BOLD) + 6, theme.muted());
		context.pose().popMatrix();
		return new TileBox(index, x, y, actionsX, actionsY);
	}

	/**
	 * Draws a screenshot fitted into a box, on a dark backdrop: loaded at the box's pixel size, a
	 * placeholder while loading, or a note if it can't be read.
	 */
	void drawScreenshot(GuiGraphicsExtractor context, UiTheme theme, ScreenshotEntry screenshot, int x, int y, int width, int height) {
		UiShapes.roundedRect(context, x, y, width, height, 6, theme.segmentBackground());
		int targetWidth = pixels(width);
		int targetHeight = pixels(height);
		LoadedThumbnail image = thumbnails().get(screenshot, targetWidth, targetHeight);
		if (image != null) {
			// Fit the whole image in the box, at fractional positions so it isn't nudged by rounding.
			float scale = Math.min(width / (float) image.width(), height / (float) image.height());
			float drawWidth = image.width() * scale;
			float drawHeight = image.height() * scale;
			context.pose().pushMatrix();
			context.pose().translate(x + (width - drawWidth) / 2.0F, y + (height - drawHeight) / 2.0F);
			context.pose().scale(scale, scale);
			context.blit(RenderPipelines.GUI_TEXTURED, image.id(), 0, 0, 0.0F, 0.0F, image.width(), image.height(), image.width(), image.height(), image.width(), image.height(), UiOpacity.apply(0xFFFFFFFF));
			context.pose().popMatrix();
			return;
		}
		boolean failed = thumbnails().failed(screenshot, targetWidth, targetHeight);
		if (!failed) {
			loadingThisFrame.add(screenshot.path());
		}
		int iconSize = Math.min(18, height / 3);
		// A gently pulsing icon while the image loads.
		float pulse = failed ? 1.0F : 0.55F + 0.45F * (float) Math.sin(System.nanoTime() / 250_000_000.0);
		UiIcons.draw(context, HubIcons.IMAGE, x + (width - iconSize) / 2, y + (height - iconSize) / 2 - (failed ? 6 : 0), iconSize, UiTheme.fade(theme.muted(), pulse));
		if (failed) {
			Component note = UiText.ellipsize(font, Component.translatable(EMUtilsTexts.UI_CANT_READ_SCREENSHOT), UiText.Size.SMALL, width - 8);
			UiText.draw(context, font, note, UiText.Size.SMALL, x + (width - UiText.width(font, note, UiText.Size.SMALL)) / 2, y + (height + iconSize) / 2, theme.muted());
		}
	}

	private void showTooltip(Component text, int mouseX, int mouseY) {
		tooltip = text;
		tooltipX = mouseX;
		tooltipY = mouseY;
	}

	@Override
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		if (tooltip != null && preview == null && dialog == null) {
			UiWidgets.tooltip(context, font, theme, tooltip, tooltipX, tooltipY, width, height);
		}
		if (preview != null) {
			preview.render(context, theme, dialog == null ? mouseX : Integer.MIN_VALUE / 2, mouseY, width, height);
			if (preview.isClosed()) {
				preview = null;
			}
		}
		if (dialog != null) {
			dialog.render(context, theme, mouseX, mouseY, width, height);
			if (dialog.isClosed()) {
				dialog = null;
			}
		}
	}

	// ---- actions --------------------------------------------------------------------------------

	/** Runs one of the actions: 0 copy, 1 open in the image viewer, 2 show in folder, 3 delete. */
	void runAction(int index, int action) {
		if (index < 0 || index >= screenshots.size()) {
			return;
		}
		ScreenshotEntry screenshot = screenshots.get(index);
		switch (action) {
			case 0 -> ScreenshotActions.copyWithFeedback(minecraft, screenshot.path().toFile());
			case 1 -> ScreenshotActions.openImage(screenshot.path().toFile());
			case 2 -> ScreenshotActions.openFolder(screenshot.path().toFile());
			default -> askToDelete(screenshot);
		}
	}

	private void askToDelete(ScreenshotEntry screenshot) {
		if (!EMUtilsClient.config().screenshotGalleryDeleteConfirmation()) {
			delete(screenshot);
			return;
		}
		dialog = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.GALLERY_DELETE_TITLE),
			Component.translatable(EMUtilsTexts.GALLERY_DELETE_MESSAGE, screenshot.filename()),
			Component.translatable(EMUtilsTexts.GALLERY_ACTION_DELETE),
			() -> delete(screenshot)
		);
	}

	private void delete(ScreenshotEntry screenshot) {
		if (!ScreenshotActions.deleteWithFeedback(minecraft, screenshot.path().toFile())) {
			return;
		}
		thumbnails().forget(screenshot.path());
		refresh();
		if (preview != null) {
			preview.screenshotsChanged();
		}
	}

	// ---- input ----------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (closing()) {
			return true;
		}
		double mouseX = click.x();
		double mouseY = click.y();
		boolean left = click.button() == InputConstants.MOUSE_BUTTON_LEFT;
		if (dialog != null) {
			if (left) {
				dialog.mouseClicked(mouseX, mouseY);
			}
			return true;
		}
		if (preview != null) {
			if (left) {
				preview.mouseClicked(mouseX, mouseY);
			}
			return true;
		}
		if (!left) {
			return super.mouseClicked(click, doubled);
		}
		if (contains(mouseX, mouseY, folderX, headerButtonsY, HEADER_BUTTON, HEADER_BUTTON)) {
			VersionedPlatform.openFile(ScreenshotPaths.screenshotsDir(minecraft).toFile());
			return true;
		}
		if (sortEdges != null) {
			for (int i = 0; i < 2; i++) {
				if (mouseX >= sortEdges[i] && mouseX < sortEdges[i + 1] && contains(mouseX, mouseY, sortX, headerButtonsY, sortEdges[2] - sortX, HEADER_BUTTON)) {
					ScreenshotGallerySort sort = i == 0 ? ScreenshotGallerySort.NEWEST_FIRST : ScreenshotGallerySort.OLDEST_FIRST;
					if (EMUtilsClient.config().screenshotGallerySort() != sort) {
						EMUtilsClient.config().setScreenshotGallerySort(sort);
						refresh();
						scroll.reset();
					}
					return true;
				}
			}
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (scroll.contains(mouseX, mouseY)) {
			for (TileBox tile : tiles) {
				for (int i = 0; i < ACTION_ICONS.length; i++) {
					if (contains(mouseX, mouseY, tile.actionsX() + i * (ACTION + ACTION_GAP), tile.actionsY(), ACTION, ACTION)) {
						runAction(tile.index(), i);
						return true;
					}
				}
				if (contains(mouseX, mouseY, tile.x(), tile.y(), tileWidth, tileHeight)) {
					preview = new GalleryPreview(font, anim, this, tile.index());
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (preview == null && dialog == null && scroll.mouseDragged(click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (scroll.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (closing() || dialog != null) {
			return true;
		}
		if (preview != null) {
			preview.scrolled(verticalAmount);
			return true;
		}
		if (scroll.scroll(mouseX, mouseY, verticalAmount)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (closing()) {
			return true;
		}
		if (dialog != null) {
			dialog.keyPressed(input);
			return true;
		}
		if (preview != null) {
			preview.keyPressed(input);
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		return closing() || preview != null || dialog != null || super.charTyped(input);
	}

	@Override
	public void onClose() {
		if (preview != null) {
			preview.close();
		}
		super.onClose();
	}

	/** Opens the large preview of a screenshot; used by UI snapshots. */
	public void openPreviewForSnapshot(int index) {
		if (index < screenshots.size()) {
			preview = new GalleryPreview(font, anim, this, index);
		}
	}

	/** The screenshots drawn in the last frame that were still loading; used by UI snapshots. */
	public Set<Path> loadingThumbnailsForSnapshot() {
		return Set.copyOf(loadingThisFrame);
	}

	/** The screenshots the gallery lists; used by UI snapshots. */
	public List<Path> screenshotsForSnapshot() {
		return screenshots.stream().map(ScreenshotEntry::path).toList();
	}

	private record TileBox(int index, int x, int y, int actionsX, int actionsY) {
	}
}
