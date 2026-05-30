package net.emutils.client.emutils.spotify.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.screenshot.gui.GalleryLoadingSpinner;
import net.emutils.client.emutils.spotify.SpotifyArtLoader;
import net.emutils.client.emutils.spotify.SpotifyTrackState;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public final class SpotifyPlayerOverlay {
	public enum DisplayMode {
		PAUSE_MENU,
		HUD
	}

	private static final int BOTTOM_MARGIN = 8;
	private static final int PANEL_PADDING_X = 8;
	private static final int PANEL_PADDING_Y = 7;
	private static final int ART_SIZE = SpotifyArtLoader.DISPLAY_SIZE;
	private static final int TEXT_GAP = 8;
	private static final int SECTION_GAP = 10;
	private static final int CONTENT_WIDTH = 150;
	private static final int BUTTON_SIZE = 20;
	private static final int BUTTON_GAP = 4;
	private static final int FONT_HEIGHT = 9;
	private static final int LINE_SPACING = 10;
	private static final int PROGRESS_ROW_HEIGHT = FONT_HEIGHT;
	private static final int PROGRESS_BAR_HEIGHT = 3;
	private static final int PROGRESS_TIME_GAP = 4;
	private static final int BACKGROUND_COLOR = 0xB5222B3D;
	private static final int SHADOW_COLOR = 0x66000000;
	private static final int BORDER_COLOR = 0xCC101725;
	private static final int PROGRESS_TRACK_COLOR = 0xFF101725;
	private static final int PROGRESS_FILL_COLOR = 0xFF20F050;

	private SpotifyIconButtonWidget previousButton;
	private SpotifyIconButtonWidget playPauseButton;
	private SpotifyIconButtonWidget nextButton;

	private SpotifyPlayerOverlay() {
	}

	public static SpotifyPlayerOverlay create(int screenWidth, int screenHeight, java.util.function.Consumer<ClickableWidget> addWidget) {
		SpotifyPlayerOverlay overlay = new SpotifyPlayerOverlay();
		overlay.init(screenWidth, screenHeight, addWidget);
		return overlay;
	}

	private void init(int screenWidth, int screenHeight, java.util.function.Consumer<ClickableWidget> addWidget) {
		Layout layout = layout(screenWidth, screenHeight, DisplayMode.PAUSE_MENU);

		addWidget.accept(previousButton = SpotifyIconButtonWidget.create(
			layout.buttonX(),
			layout.buttonY(),
			Text.translatable(EMUtilsTexts.SPOTIFY_PREVIOUS),
			SpotifyIcons.PREVIOUS,
			ignored -> EMUtilsClient.spotify().previous()
		));
		addWidget.accept(playPauseButton = createPlayPauseButton(layout.buttonX() + BUTTON_SIZE + BUTTON_GAP, layout.buttonY()));
		addWidget.accept(nextButton = SpotifyIconButtonWidget.create(
			layout.buttonX() + (BUTTON_SIZE + BUTTON_GAP) * 2,
			layout.buttonY(),
			Text.translatable(EMUtilsTexts.SPOTIFY_NEXT),
			SpotifyIcons.NEXT,
			ignored -> EMUtilsClient.spotify().next()
		));
	}

	public void setVisible(boolean visible) {
		if (previousButton != null) {
			previousButton.visible = visible;
		}
		if (playPauseButton != null) {
			playPauseButton.visible = visible;
		}
		if (nextButton != null) {
			nextButton.visible = visible;
		}
	}

	public static boolean shouldDisplay(SpotifyTrackState state) {
		return state.shouldDisplay();
	}

	public void syncPlaybackState(SpotifyTrackState state) {
		if (playPauseButton != null) {
			playPauseButton.setIcon(state.playing() ? SpotifyIcons.PAUSE : SpotifyIcons.PLAY);
		}
	}

	public static void renderBackground(DrawContext context, int screenWidth, int screenHeight) {
		Layout layout = layout(screenWidth, screenHeight, DisplayMode.PAUSE_MENU);
		drawPanelBackground(context, layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight());
	}

	public static void renderContent(DrawContext context, int screenWidth, int screenHeight, SpotifyTrackState state) {
		renderPanelContent(context, layout(screenWidth, screenHeight, DisplayMode.PAUSE_MENU), state);
	}

	public static int hudPanelWidth() {
		return layoutAtOrigin(DisplayMode.HUD).panelWidth();
	}

	public static int hudPanelHeight() {
		return layoutAtOrigin(DisplayMode.HUD).panelHeight();
	}

	public static void renderHud(
		DrawContext context,
		int x,
		int y,
		SpotifyTrackState state,
		int opacityPercent,
		float scale
	) {
		Layout layout = layoutAtOrigin(DisplayMode.HUD);

		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(x, y);
			context.getMatrices().scale(scale, scale);
			drawPanelBackground(context, 0, 0, layout.panelWidth(), layout.panelHeight(), opacityPercent);
			renderPanelContent(context, layout, state);
		} finally {
			context.getMatrices().popMatrix();
		}
	}

	private static void renderPanelContent(DrawContext context, Layout layout, SpotifyTrackState state) {
		MinecraftClient client = MinecraftClient.getInstance();
		int contentHeight = contentBlockHeight(state);
		int textY = layout.innerTop() + (layout.innerHeight() - contentHeight) / 2;

		drawArt(context, layout.artX(), layout.artY(), state);
		drawTrackText(context, client, layout.textX(), textY, state);
		if (state.hasTrack() && state.durationMs() > 0L) {
			drawProgress(context, client, layout, textY + LINE_SPACING * 2, state);
		}
	}

	private static void drawArt(DrawContext context, int x, int y, SpotifyTrackState state) {
		SpotifyArtLoader.ArtResult art = EMUtilsClient.spotify().art(state);
		int textureWidth = art.width();
		int textureHeight = art.height();
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			art.texture(),
			x,
			y,
			0.0F,
			0.0F,
			ART_SIZE,
			ART_SIZE,
			textureWidth,
			textureHeight,
			textureWidth,
			textureHeight
		);
		if (art.state() == SpotifyArtLoader.State.LOADING) {
			GalleryLoadingSpinner.render(context, x + ART_SIZE / 2, y + ART_SIZE / 2, 5);
		}
	}

	private static void drawTrackText(DrawContext context, MinecraftClient client, int textX, int textY, SpotifyTrackState state) {
		Text primaryText;
		Text secondaryText;
		if (state.kind() == SpotifyTrackState.Kind.UNAVAILABLE) {
			primaryText = Text.translatable(EMUtilsTexts.SPOTIFY_PLAYER_UNAVAILABLE);
			secondaryText = Text.empty();
		} else if (!state.hasTrack()) {
			primaryText = Text.translatable(EMUtilsTexts.SPOTIFY_PLAYER_NO_TRACK);
			secondaryText = Text.empty();
		} else {
			primaryText = Text.literal(client.textRenderer.trimToWidth(state.title(), CONTENT_WIDTH));
			secondaryText = state.artist().isBlank()
				? Text.empty()
				: Text.literal(client.textRenderer.trimToWidth(state.artist(), CONTENT_WIDTH));
		}

		context.drawTextWithShadow(client.textRenderer, primaryText, textX, textY, Colors.WHITE);
		if (!secondaryText.getString().isEmpty()) {
			context.drawTextWithShadow(
				client.textRenderer,
				secondaryText,
				textX,
				textY + LINE_SPACING,
				Colors.LIGHT_GRAY
			);
		}
	}

	private static int contentBlockHeight(SpotifyTrackState state) {
		if (state.hasTrack() && state.durationMs() > 0L) {
			return LINE_SPACING * 2 + PROGRESS_ROW_HEIGHT;
		}
		if (state.hasTrack() && !state.artist().isBlank()) {
			return LINE_SPACING * 2;
		}

		return FONT_HEIGHT;
	}

	private static void drawProgress(DrawContext context, MinecraftClient client, Layout layout, int rowY, SpotifyTrackState state) {
		long positionMs = state.effectivePositionMs();
		String elapsed = formatDuration(positionMs);
		String total = formatDuration(state.durationMs());
		int elapsedWidth = client.textRenderer.getWidth(elapsed);
		int totalWidth = client.textRenderer.getWidth(total);
		int contentRight = layout.textX() + CONTENT_WIDTH;
		int totalX = contentRight - totalWidth;
		int barX = layout.textX() + elapsedWidth + PROGRESS_TIME_GAP;
		int barWidth = Math.max(24, totalX - PROGRESS_TIME_GAP - barX);
		int barY = rowY + (PROGRESS_ROW_HEIGHT - PROGRESS_BAR_HEIGHT) / 2;

		context.drawTextWithShadow(client.textRenderer, Text.literal(elapsed), layout.textX(), rowY, Colors.LIGHT_GRAY);
		drawProgressBar(context, barX, barY, barWidth, state.progressPercent());
		context.drawTextWithShadow(client.textRenderer, Text.literal(total), totalX, rowY, Colors.LIGHT_GRAY);
	}

	private static void drawPanelBackground(DrawContext context, int x, int y, int width, int height) {
		drawPanelBackground(context, x, y, width, height, 100);
	}

	private static void drawPanelBackground(DrawContext context, int x, int y, int width, int height, int opacityPercent) {
		context.fill(x + 2, y + 2, x + width + 2, y + height + 2, withOpacity(SHADOW_COLOR, opacityPercent));
		context.fill(x, y, x + width, y + height, withOpacity(BORDER_COLOR, opacityPercent));
		context.fill(x + 1, y + 1, x + width - 1, y + height - 1, withOpacity(BACKGROUND_COLOR, opacityPercent));
	}

	private static int withOpacity(int color, int opacityPercent) {
		int alpha = color >>> 24;
		int scaledAlpha = Math.round(alpha * Math.min(100, Math.max(0, opacityPercent)) / 100.0F);
		return (scaledAlpha << 24) | (color & 0x00FFFFFF);
	}

	private static void drawProgressBar(DrawContext context, int x, int y, int width, int percent) {
		int fillWidth = Math.max(0, (int) Math.round(width * Math.min(100, Math.max(0, percent)) / 100.0));
		context.fill(x, y, x + width, y + PROGRESS_BAR_HEIGHT, PROGRESS_TRACK_COLOR);
		if (fillWidth > 0) {
			context.fill(x, y, x + fillWidth, y + PROGRESS_BAR_HEIGHT, PROGRESS_FILL_COLOR);
		}
	}

	private static String formatDuration(long durationMs) {
		long totalSeconds = Math.max(0L, durationMs / 1_000L);
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		return minutes + ":" + (seconds < 10L ? "0" : "") + seconds;
	}

	static Layout layout(int screenWidth, int screenHeight, DisplayMode mode) {
		Layout layout = layoutAtOrigin(mode);
		int panelX = mode == DisplayMode.PAUSE_MENU
			? screenWidth / 2 - layout.panelWidth() / 2
			: screenWidth - BOTTOM_MARGIN - layout.panelWidth();
		int panelY = screenHeight - BOTTOM_MARGIN - layout.panelHeight();
		return layout.offset(panelX, panelY);
	}

	static Layout layoutAtOrigin(DisplayMode mode) {
		boolean withButtons = mode == DisplayMode.PAUSE_MENU;
		int buttonRowWidth = withButtons ? BUTTON_SIZE * 3 + BUTTON_GAP * 2 : 0;
		int sectionGap = withButtons ? SECTION_GAP : 0;
		int textBlockHeight = LINE_SPACING * 2 + PROGRESS_ROW_HEIGHT;
		int innerHeight = Math.max(ART_SIZE, textBlockHeight);
		int innerWidth = ART_SIZE + TEXT_GAP + CONTENT_WIDTH + sectionGap + buttonRowWidth;
		int panelWidth = innerWidth + PANEL_PADDING_X * 2;
		int panelHeight = innerHeight + PANEL_PADDING_Y * 2;
		int innerTop = PANEL_PADDING_Y;
		int artX = PANEL_PADDING_X;
		int artY = innerTop + (innerHeight - ART_SIZE) / 2;
		int textX = artX + ART_SIZE + TEXT_GAP;
		int buttonX = panelWidth - PANEL_PADDING_X - buttonRowWidth;
		int buttonY = innerTop + (innerHeight - BUTTON_SIZE) / 2;

		return new Layout(
			0,
			0,
			panelWidth,
			panelHeight,
			innerTop,
			innerHeight,
			artX,
			artY,
			textX,
			buttonX,
			buttonY
		);
	}

	private SpotifyIconButtonWidget createPlayPauseButton(int x, int y) {
		SpotifyTrackState state = EMUtilsClient.spotify().state();
		return SpotifyIconButtonWidget.create(
			x,
			y,
			Text.translatable(EMUtilsTexts.SPOTIFY_PLAY_PAUSE),
			state.playing() ? SpotifyIcons.PAUSE : SpotifyIcons.PLAY,
			ignored -> EMUtilsClient.spotify().playPause()
		);
	}

	static final class Layout {
		private final int panelX;
		private final int panelY;
		private final int panelWidth;
		private final int panelHeight;
		private final int innerTop;
		private final int innerHeight;
		private final int artX;
		private final int artY;
		private final int textX;
		private final int buttonX;
		private final int buttonY;

		private Layout(
			int panelX,
			int panelY,
			int panelWidth,
			int panelHeight,
			int innerTop,
			int innerHeight,
			int artX,
			int artY,
			int textX,
			int buttonX,
			int buttonY
		) {
			this.panelX = panelX;
			this.panelY = panelY;
			this.panelWidth = panelWidth;
			this.panelHeight = panelHeight;
			this.innerTop = innerTop;
			this.innerHeight = innerHeight;
			this.artX = artX;
			this.artY = artY;
			this.textX = textX;
			this.buttonX = buttonX;
			this.buttonY = buttonY;
		}

		int panelX() {
			return panelX;
		}

		int panelY() {
			return panelY;
		}

		int panelWidth() {
			return panelWidth;
		}

		int panelHeight() {
			return panelHeight;
		}

		int innerTop() {
			return innerTop;
		}

		int innerHeight() {
			return innerHeight;
		}

		int artX() {
			return artX;
		}

		int artY() {
			return artY;
		}

		int textX() {
			return textX;
		}

		int buttonX() {
			return buttonX;
		}

		int buttonY() {
			return buttonY;
		}

		Layout offset(int offsetX, int offsetY) {
			return new Layout(
				panelX + offsetX,
				panelY + offsetY,
				panelWidth,
				panelHeight,
				innerTop + offsetY,
				innerHeight,
				artX + offsetX,
				artY + offsetY,
				textX + offsetX,
				buttonX + offsetX,
				buttonY + offsetY
			);
		}
	}
}
