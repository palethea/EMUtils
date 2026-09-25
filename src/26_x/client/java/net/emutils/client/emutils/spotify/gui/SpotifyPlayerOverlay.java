package net.emutils.client.emutils.spotify.gui;

import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiRasterScale;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.spotify.SpotifyArtLoader;
import net.emutils.client.emutils.spotify.SpotifyTrackState;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The Spotify now-playing card, in the look of the new UI: on the HUD, and with playback controls at
 * the bottom of the pause menu. The song fades in when it changes, and the cover fades in once loaded.
 */
public final class SpotifyPlayerOverlay {
	private static final int MARGIN = 8;
	private static final int PADDING = 8;
	private static final int RADIUS = 10;
	private static final int SHADOW_BLUR = 10;
	private static final int ART_SIZE = SpotifyArtLoader.DISPLAY_SIZE;
	private static final int ART_RADIUS = Math.round(SpotifyArtLoader.CORNER_RADIUS);
	private static final int PLACEHOLDER_ICON_SIZE = 14;
	private static final int TEXT_GAP = 9;
	private static final int CONTENT_WIDTH = 140;
	private static final int LINE_GAP = 5;
	private static final int PROGRESS_GAP = 7;
	private static final int BAR_HEIGHT = 3;
	private static final int TIME_GAP = 5;
	private static final int SECTION_GAP = 12;
	private static final int PLAY_SIZE = 22;
	private static final int SKIP_SIZE = 18;
	private static final int BUTTON_GAP = 4;
	private static final int BUTTON_ROW_WIDTH = SKIP_SIZE * 2 + PLAY_SIZE + BUTTON_GAP * 2;
	private static final long FADE_MS = 250L;

	/** The song on show and when it appeared, for fading it in; shared by the HUD and the pause menu. */
	private static String shownSong = "";
	private static long songShownAt;
	private static @Nullable Identifier shownArt;
	private static long artShownAt;

	private SpotifyControlButton previousButton;
	private SpotifyControlButton playPauseButton;
	private SpotifyControlButton nextButton;

	private SpotifyPlayerOverlay() {
	}

	public static SpotifyPlayerOverlay create(int screenWidth, int screenHeight, Consumer<AbstractWidget> addWidget) {
		SpotifyPlayerOverlay overlay = new SpotifyPlayerOverlay();
		overlay.init(pauseMenuLayout(screenWidth, screenHeight), addWidget);
		return overlay;
	}

	private void init(Layout layout, Consumer<AbstractWidget> addWidget) {
		int x = layout.buttonsX();
		int centerY = layout.centerY();
		addWidget.accept(previousButton = new SpotifyControlButton(
			x,
			centerY - SKIP_SIZE / 2,
			SKIP_SIZE,
			Component.translatable(EMUtilsTexts.SPOTIFY_PREVIOUS),
			SpotifyIcons.PREVIOUS,
			false,
			() -> EMUtilsClient.spotify().previous()
		));
		x += SKIP_SIZE + BUTTON_GAP;
		addWidget.accept(playPauseButton = new SpotifyControlButton(
			x,
			centerY - PLAY_SIZE / 2,
			PLAY_SIZE,
			Component.translatable(EMUtilsTexts.SPOTIFY_PLAY_PAUSE),
			playPauseIcon(EMUtilsClient.spotify().state()),
			true,
			() -> EMUtilsClient.spotify().playPause()
		));
		x += PLAY_SIZE + BUTTON_GAP;
		addWidget.accept(nextButton = new SpotifyControlButton(
			x,
			centerY - SKIP_SIZE / 2,
			SKIP_SIZE,
			Component.translatable(EMUtilsTexts.SPOTIFY_NEXT),
			SpotifyIcons.NEXT,
			false,
			() -> EMUtilsClient.spotify().next()
		));
	}

	public void setVisible(boolean visible) {
		previousButton.visible = visible;
		playPauseButton.visible = visible;
		nextButton.visible = visible;
	}

	public static boolean shouldDisplay(SpotifyTrackState state) {
		return state.shouldDisplay();
	}

	public void syncPlaybackState(SpotifyTrackState state) {
		playPauseButton.setIcon(playPauseIcon(state));
	}

	private static Identifier playPauseIcon(SpotifyTrackState state) {
		return state.playing() ? SpotifyIcons.PAUSE : SpotifyIcons.PLAY;
	}

	/** The card behind the pause menu controls, drawn before the buttons. */
	public static void renderBackground(GuiGraphicsExtractor context, int screenWidth, int screenHeight) {
		Layout layout = pauseMenuLayout(screenWidth, screenHeight);
		drawCard(context, layout, UiTheme.current(), 100);
	}

	/** The cover and song of the pause menu card, drawn after the buttons. */
	public static void renderContent(GuiGraphicsExtractor context, int screenWidth, int screenHeight, SpotifyTrackState state) {
		drawContent(context, pauseMenuLayout(screenWidth, screenHeight), UiTheme.current(), state);
	}

	public static int hudPanelWidth() {
		return hudLayout().width();
	}

	public static int hudPanelHeight() {
		return hudLayout().height();
	}

	/**
	 * Draws the HUD card at {@code x, y}, scaled by {@code scale}; {@code opacityPercent} is the
	 * background's opacity, the song stays fully visible.
	 */
	public static void renderHud(
		GuiGraphicsExtractor context,
		int x,
		int y,
		SpotifyTrackState state,
		int opacityPercent,
		float scale
	) {
		Layout layout = hudLayout();
		UiTheme theme = UiTheme.current();
		context.pose().pushMatrix();
		UiRasterScale.set(scale);
		try {
			context.pose().translate(x, y);
			context.pose().scale(scale, scale);
			drawCard(context, layout, theme, opacityPercent);
			drawContent(context, layout, theme, state);
		} finally {
			UiRasterScale.reset();
			context.pose().popMatrix();
		}
	}

	private static void drawCard(GuiGraphicsExtractor context, Layout layout, UiTheme theme, int opacityPercent) {
		float opacity = Math.clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
		if (opacity <= 0.0F) {
			return;
		}
		UiShapes.shadow(context, layout.x(), layout.y(), layout.width(), layout.height(), RADIUS, SHADOW_BLUR, UiTheme.fade(theme.shadow(), opacity));
		UiShapes.borderedRect(
			context,
			layout.x(),
			layout.y(),
			layout.width(),
			layout.height(),
			RADIUS,
			UiTheme.fade(theme.panel(), opacity),
			UiTheme.fade(theme.border(), opacity)
		);
	}

	private static void drawContent(GuiGraphicsExtractor context, Layout layout, UiTheme theme, SpotifyTrackState state) {
		long now = System.currentTimeMillis();
		String song = state.kind() + "\n" + state.title() + "\n" + state.artist();
		if (!song.equals(shownSong)) {
			shownSong = song;
			songShownAt = now;
		}
		float songFade = fade(now - songShownAt);

		drawArt(context, layout, theme, state, now);

		float outer = UiOpacity.get();
		UiOpacity.set(outer * songFade);
		try {
			drawText(context, layout, theme, state);
		} finally {
			UiOpacity.set(outer);
		}
	}

	private static void drawArt(GuiGraphicsExtractor context, Layout layout, UiTheme theme, SpotifyTrackState state, long now) {
		int x = layout.artX();
		int y = layout.artY();
		SpotifyArtLoader.ArtResult art = EMUtilsClient.spotify().art(state);
		Identifier cover = art.state() == SpotifyArtLoader.State.LOADED ? art.texture() : null;
		if (cover != null && !cover.equals(shownArt)) {
			shownArt = cover;
			artShownAt = now;
		}
		float coverFade = cover == null ? 0.0F : fade(now - artShownAt);

		if (coverFade < 1.0F) {
			UiShapes.roundedRect(context, x, y, ART_SIZE, ART_SIZE, ART_RADIUS, theme.surfaceAlt());
			int iconOffset = (ART_SIZE - PLACEHOLDER_ICON_SIZE) / 2;
			UiIcons.draw(context, HubIcons.MUSIC, x + iconOffset, y + iconOffset, PLACEHOLDER_ICON_SIZE, theme.muted());
		}
		if (cover != null) {
			int color = UiOpacity.apply(UiTheme.fade(0xFFFFFFFF, coverFade));
			context.blit(RenderPipelines.GUI_TEXTURED, cover, x, y, 0.0F, 0.0F, ART_SIZE, ART_SIZE, art.width(), art.height(), art.width(), art.height(), color);
		}
	}

	private static void drawText(GuiGraphicsExtractor context, Layout layout, UiTheme theme, SpotifyTrackState state) {
		Font font = Minecraft.getInstance().font;
		int x = layout.textX();
		if (!state.hasTrack()) {
			Component status = Component.translatable(state.kind() == SpotifyTrackState.Kind.UNAVAILABLE
				? EMUtilsTexts.SPOTIFY_PLAYER_UNAVAILABLE
				: EMUtilsTexts.SPOTIFY_PLAYER_NO_TRACK);
			UiText.drawCentered(context, font, UiText.ellipsize(font, status, UiText.Size.BODY, CONTENT_WIDTH), UiText.Size.BODY, x, layout.centerY(), theme.textSecondary());
			return;
		}

		boolean hasArtist = !state.artist().isBlank();
		boolean hasProgress = state.durationMs() > 0L;
		int titleHeight = UiText.lineHeight(font, UiText.Size.BOLD);
		int artistHeight = UiText.lineHeight(font, UiText.Size.BODY);
		int rowHeight = Math.max(BAR_HEIGHT, UiText.lineHeight(font, UiText.Size.SMALL));
		int height = titleHeight
			+ (hasArtist ? LINE_GAP + artistHeight : 0)
			+ (hasProgress ? PROGRESS_GAP + rowHeight : 0);
		int top = layout.centerY() - height / 2;

		Component title = UiText.ellipsize(font, Component.literal(state.title()), UiText.Size.BOLD, CONTENT_WIDTH);
		UiText.draw(context, font, title, UiText.Size.BOLD, x, top, theme.text());
		top += titleHeight;
		if (hasArtist) {
			top += LINE_GAP;
			Component artist = UiText.ellipsize(font, Component.literal(state.artist()), UiText.Size.BODY, CONTENT_WIDTH);
			UiText.draw(context, font, artist, UiText.Size.BODY, x, top, theme.textSecondary());
			top += artistHeight;
		}
		if (hasProgress) {
			drawProgress(context, font, theme, x, top + PROGRESS_GAP + rowHeight / 2, state);
		}
	}

	private static void drawProgress(GuiGraphicsExtractor context, Font font, UiTheme theme, int x, int centerY, SpotifyTrackState state) {
		Component elapsed = Component.literal(formatDuration(state.effectivePositionMs()));
		Component total = Component.literal(formatDuration(state.durationMs()));
		// Sized for the longest time, so the bar doesn't change length as the seconds tick.
		int timeWidth = UiText.width(font, Component.literal(formatDuration(state.durationMs()).replaceAll("\\d", "0")), UiText.Size.SMALL);
		int totalWidth = UiText.width(font, total, UiText.Size.SMALL);
		UiText.drawCentered(context, font, elapsed, UiText.Size.SMALL, x, centerY, theme.muted());
		UiText.drawCentered(context, font, total, UiText.Size.SMALL, x + CONTENT_WIDTH - totalWidth, centerY, theme.muted());

		int barX = x + timeWidth + TIME_GAP;
		int barWidth = CONTENT_WIDTH - (timeWidth + TIME_GAP) * 2;
		int barY = centerY - BAR_HEIGHT / 2;
		UiShapes.pill(context, barX, barY, barWidth, BAR_HEIGHT, theme.line());
		float progress = Math.clamp((float) state.effectivePositionMs() / state.durationMs(), 0.0F, 1.0F);
		int fill = Math.round(barWidth * progress);
		if (fill > 0) {
			UiShapes.pill(context, barX, barY, Math.max(BAR_HEIGHT, fill), BAR_HEIGHT, theme.accent());
		}
	}

	private static float fade(long elapsedMs) {
		float t = Math.clamp(elapsedMs / (float) FADE_MS, 0.0F, 1.0F);
		return 1.0F - (1.0F - t) * (1.0F - t);
	}

	private static String formatDuration(long durationMs) {
		long totalSeconds = Math.max(0L, durationMs / 1_000L);
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		return minutes + ":" + (seconds < 10L ? "0" : "") + seconds;
	}

	private static Layout hudLayout() {
		return layout(0, 0, false);
	}

	private static Layout pauseMenuLayout(int screenWidth, int screenHeight) {
		Layout origin = layout(0, 0, true);
		return layout(screenWidth / 2 - origin.width() / 2, screenHeight - MARGIN - origin.height(), true);
	}

	private static Layout layout(int x, int y, boolean withButtons) {
		int width = PADDING * 2 + ART_SIZE + TEXT_GAP + CONTENT_WIDTH + (withButtons ? SECTION_GAP + BUTTON_ROW_WIDTH : 0);
		int height = PADDING * 2 + ART_SIZE;
		return new Layout(
			x,
			y,
			width,
			height,
			x + PADDING,
			y + PADDING,
			x + PADDING + ART_SIZE + TEXT_GAP,
			x + width - PADDING - BUTTON_ROW_WIDTH,
			y + height / 2
		);
	}

	private record Layout(int x, int y, int width, int height, int artX, int artY, int textX, int buttonsX, int centerY) {
	}
}
