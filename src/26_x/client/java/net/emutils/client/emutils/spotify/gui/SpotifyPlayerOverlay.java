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
	private static final int MARGIN = 6;
	/** Space kept between the pause menu's lowest button and the card. */
	private static final int MENU_GAP = 4;
	/** The pause menu card shrinks to fit below the menu, but not below this. */
	private static final float MIN_PAUSE_SCALE = 0.5F;
	private static final int PADDING = 6;
	private static final int RADIUS = 9;
	private static final int SHADOW_BLUR = 8;
	private static final int ART_SIZE = SpotifyArtLoader.DISPLAY_SIZE;
	private static final int ART_RADIUS = Math.round(SpotifyArtLoader.CORNER_RADIUS);
	private static final int PLACEHOLDER_ICON_SIZE = 12;
	private static final int TEXT_GAP = 8;
	private static final int CONTENT_WIDTH = 124;
	private static final int LINE_GAP = 4;
	private static final int PROGRESS_GAP = 5;
	private static final int BAR_HEIGHT = 3;
	private static final int TIME_GAP = 4;
	private static final int SECTION_GAP = 10;
	private static final int PLAY_SIZE = 20;
	private static final int SKIP_SIZE = 16;
	private static final int BUTTON_GAP = 3;
	private static final int BUTTON_ROW_WIDTH = SKIP_SIZE * 2 + PLAY_SIZE + BUTTON_GAP * 2;
	private static final long FADE_MS = 250L;
	/** Long titles scroll like Spotify's: hold, scroll to the end, hold, scroll back, repeat. */
	private static final long MARQUEE_HOLD_START_MS = 2_500L;
	private static final long MARQUEE_HOLD_END_MS = 1_500L;
	private static final float MARQUEE_PIXELS_PER_MS = 0.03F;
	private static final Layout HUD_LAYOUT = layout(false);

	/** The song on show and when it appeared, for fading it in; shared by the HUD and the pause menu. */
	private static String shownSong = "";
	private static long songShownAt;
	private static @Nullable Identifier shownArt;
	private static @Nullable Identifier previousArt;
	private static long artShownAt;

	/** The pause menu card: drawn at {@link #originX}, {@link #originY}, scaled by {@link #scale}. */
	private final Layout layout = layout(true);
	private final float scale;
	private final float originX;
	private final float originY;
	private SpotifyControlButton previousButton;
	private SpotifyControlButton playPauseButton;
	private SpotifyControlButton nextButton;

	private SpotifyPlayerOverlay(int screenWidth, int screenHeight, int menuBottom) {
		// Shrinks to fit between the menu's buttons and the bottom of the screen, and within its width.
		float fitHeight = (screenHeight - MARGIN - menuBottom - MENU_GAP) / (float) layout.height();
		float fitWidth = (screenWidth - MARGIN * 2) / (float) layout.width();
		scale = Math.clamp(Math.min(fitHeight, fitWidth), MIN_PAUSE_SCALE, 1.0F);
		originX = (screenWidth - layout.width() * scale) / 2.0F;
		originY = screenHeight - MARGIN - layout.height() * scale;
	}

	/**
	 * The pause menu player, with its buttons added through {@code addWidget}; {@code menuBottom} is
	 * the bottom of the menu's lowest button, which the card stays below.
	 */
	public static SpotifyPlayerOverlay create(int screenWidth, int screenHeight, int menuBottom, Consumer<AbstractWidget> addWidget) {
		SpotifyPlayerOverlay overlay = new SpotifyPlayerOverlay(screenWidth, screenHeight, menuBottom);
		overlay.init(addWidget);
		return overlay;
	}

	private void init(Consumer<AbstractWidget> addWidget) {
		int x = layout.buttonsX();
		addWidget.accept(previousButton = button(x, SKIP_SIZE, EMUtilsTexts.SPOTIFY_PREVIOUS, SpotifyIcons.PREVIOUS, false, () -> EMUtilsClient.spotify().previous()));
		x += SKIP_SIZE + BUTTON_GAP;
		addWidget.accept(playPauseButton = button(x, PLAY_SIZE, EMUtilsTexts.SPOTIFY_PLAY_PAUSE, playPauseIcon(EMUtilsClient.spotify().state()), true, () -> EMUtilsClient.spotify().playPause()));
		x += PLAY_SIZE + BUTTON_GAP;
		addWidget.accept(nextButton = button(x, SKIP_SIZE, EMUtilsTexts.SPOTIFY_NEXT, SpotifyIcons.NEXT, false, () -> EMUtilsClient.spotify().next()));
	}

	/** A button at {@code localX} in the card, vertically centered, placed on screen with the card's scale. */
	private SpotifyControlButton button(int localX, int size, String label, Identifier icon, boolean primary, Runnable action) {
		int scaledSize = Math.max(8, Math.round(size * scale));
		int centerX = Math.round(originX + (localX + size / 2.0F) * scale);
		int centerY = Math.round(originY + layout.centerY() * scale);
		return new SpotifyControlButton(centerX - scaledSize / 2, centerY - scaledSize / 2, scaledSize, Component.translatable(label), icon, primary, action);
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
	public void renderBackground(GuiGraphicsExtractor context) {
		drawScaled(context, originX, originY, scale, () -> drawCard(context, layout, UiTheme.current(), 100));
	}

	/** The cover and song of the pause menu card, drawn after the buttons. */
	public void renderContent(GuiGraphicsExtractor context, SpotifyTrackState state) {
		boolean scroll = EMUtilsClient.config().spotifyPlayerScrollTitles();
		drawScaled(context, originX, originY, scale, () -> drawContent(context, layout, UiTheme.current(), state, scroll));
	}

	public static int hudPanelWidth() {
		return HUD_LAYOUT.width();
	}

	public static int hudPanelHeight() {
		return HUD_LAYOUT.height();
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
		UiTheme theme = UiTheme.current();
		boolean scroll = EMUtilsClient.config().spotifyHudScrollTitles();
		drawScaled(context, x, y, scale, () -> {
			drawCard(context, HUD_LAYOUT, theme, opacityPercent);
			drawContent(context, HUD_LAYOUT, theme, state, scroll);
		});
	}

	/** Draws with the card's origin at {@code x, y}, scaled, with text and icons rasterized for that scale. */
	private static void drawScaled(GuiGraphicsExtractor context, float x, float y, float scale, Runnable draw) {
		context.pose().pushMatrix();
		UiRasterScale.set(scale);
		try {
			context.pose().translate(x, y);
			context.pose().scale(scale, scale);
			draw.run();
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

	private static void drawContent(GuiGraphicsExtractor context, Layout layout, UiTheme theme, SpotifyTrackState state, boolean scroll) {
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
			drawText(context, layout, theme, state, scroll ? now - songShownAt : -1L);
		} finally {
			UiOpacity.set(outer);
		}
	}

	private static void drawArt(GuiGraphicsExtractor context, Layout layout, UiTheme theme, SpotifyTrackState state, long now) {
		int x = layout.artX();
		int y = layout.artY();
		SpotifyArtLoader.ArtResult art = EMUtilsClient.spotify().art(state);
		Identifier loaded = art.state() == SpotifyArtLoader.State.LOADED ? art.texture() : null;
		if (loaded != null && !loaded.equals(shownArt)) {
			previousArt = shownArt;
			shownArt = loaded;
			artShownAt = now;
		}
		// While the next song's cover loads, the last one stays, and the new one fades in over it.
		Identifier cover = loaded != null ? loaded : art.state() == SpotifyArtLoader.State.LOADING ? shownArt : null;
		Identifier under = loaded != null ? previousArt : null;
		float coverFade = loaded == null ? 1.0F : fade(now - artShownAt);

		if (cover == null || coverFade < 1.0F) {
			if (under != null) {
				drawCover(context, under, x, y, 1.0F);
			} else {
				UiShapes.roundedRect(context, x, y, ART_SIZE, ART_SIZE, ART_RADIUS, theme.surfaceAlt());
				int iconOffset = (ART_SIZE - PLACEHOLDER_ICON_SIZE) / 2;
				UiIcons.draw(context, HubIcons.MUSIC, x + iconOffset, y + iconOffset, PLACEHOLDER_ICON_SIZE, theme.muted());
			}
		}
		if (cover != null) {
			drawCover(context, cover, x, y, coverFade);
		}
	}

	/** Covers are {@link SpotifyArtLoader#TEXTURE_SIZE} square, with their rounded corners baked in. */
	private static void drawCover(GuiGraphicsExtractor context, Identifier cover, int x, int y, float opacity) {
		int size = SpotifyArtLoader.TEXTURE_SIZE;
		int color = UiOpacity.apply(UiTheme.fade(0xFFFFFFFF, opacity));
		context.blit(RenderPipelines.GUI_TEXTURED, cover, x, y, 0.0F, 0.0F, ART_SIZE, ART_SIZE, size, size, size, size, color);
	}

	private static void drawText(GuiGraphicsExtractor context, Layout layout, UiTheme theme, SpotifyTrackState state, long shownMs) {
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

		drawMarquee(context, font, Component.literal(state.title()), UiText.Size.BOLD, x, top, titleHeight, theme.text(), shownMs);
		top += titleHeight;
		if (hasArtist) {
			top += LINE_GAP;
			drawMarquee(context, font, Component.literal(state.artist()), UiText.Size.BODY, x, top, artistHeight, theme.textSecondary(), shownMs);
			top += artistHeight;
		}
		if (hasProgress) {
			drawProgress(context, font, theme, x, top + PROGRESS_GAP + rowHeight / 2, state);
		}
	}

	/**
	 * Draws one line of the song; if it's too long for the card, it scrolls back and forth through it,
	 * or with {@code shownMs} -1, when scrolling is turned off, ends in "...".
	 */
	private static void drawMarquee(GuiGraphicsExtractor context, Font font, Component text, UiText.Size size, int x, int top, int height, int color, long shownMs) {
		int overflow = UiText.width(font, text, size) - CONTENT_WIDTH;
		if (overflow <= 0 || shownMs < 0L) {
			UiText.draw(context, font, UiText.ellipsize(font, text, size, CONTENT_WIDTH), size, x, top, color);
			return;
		}
		long scrollMs = (long) Math.ceil(overflow / MARQUEE_PIXELS_PER_MS);
		long t = shownMs % (MARQUEE_HOLD_START_MS + scrollMs + MARQUEE_HOLD_END_MS + scrollMs);
		float progress;
		if (t < MARQUEE_HOLD_START_MS) {
			progress = 0.0F;
		} else if (t < MARQUEE_HOLD_START_MS + scrollMs) {
			progress = easeInOut((t - MARQUEE_HOLD_START_MS) / (float) scrollMs);
		} else if (t < MARQUEE_HOLD_START_MS + scrollMs + MARQUEE_HOLD_END_MS) {
			progress = 1.0F;
		} else {
			progress = 1.0F - easeInOut((t - MARQUEE_HOLD_START_MS - scrollMs - MARQUEE_HOLD_END_MS) / (float) scrollMs);
		}
		// Room above and below the capitals for accents and descenders.
		context.enableScissor(x, top - height, x + CONTENT_WIDTH, top + height * 2);
		UiText.drawExact(context, font, text, size, x - overflow * progress, top, color);
		context.disableScissor();
	}

	private static float easeInOut(float t) {
		return t * t * (3.0F - 2.0F * t);
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

	/** The card at the origin; the pause menu's also has room for the buttons. */
	private static Layout layout(boolean withButtons) {
		int width = PADDING * 2 + ART_SIZE + TEXT_GAP + CONTENT_WIDTH + (withButtons ? SECTION_GAP + BUTTON_ROW_WIDTH : 0);
		int height = PADDING * 2 + ART_SIZE;
		return new Layout(
			0,
			0,
			width,
			height,
			PADDING,
			PADDING,
			PADDING + ART_SIZE + TEXT_GAP,
			width - PADDING - BUTTON_ROW_WIDTH,
			height / 2
		);
	}

	private record Layout(int x, int y, int width, int height, int artX, int artY, int textX, int buttonsX, int centerY) {
	}
}
