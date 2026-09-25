package net.emutils.client.emutils.gui.ui;

import java.util.Optional;
import java.util.function.Consumer;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * A loading card in the new UI's style for applying packs (#112), replacing Minecraft's Mojang screen.
 * It fades in over the dimmed game, shows the reload's progress, shows it's done for a moment, and fades
 * out. Its finishing follows Minecraft's loading screen: when the reload is done it calls
 * {@code onFinish} and re-initializes the open screen.
 *
 * <p>Shader packs compile on the render thread, so no frame can be drawn while that runs. For them the
 * reload is started only once the card is fully shown (its last frame then is a clean card), and the bar
 * is indeterminate instead of a made-up percentage.
 */
public final class UiLoadingOverlay extends Overlay {
	private static final long FADE_IN_MILLIS = 160L;
	private static final long DONE_MILLIS = 260L;
	private static final long FADE_OUT_MILLIS = 200L;
	/** A reload that finishes faster than this still shows the card this long, so it doesn't flash. */
	private static final long MIN_VISIBLE_MILLIS = 450L;
	private static final int WIDTH = 280;
	private static final int PADDING = 16;
	private static final int BAR_HEIGHT = 4;

	private final Minecraft minecraft;
	private final ReloadInstance reload;
	private final Consumer<Optional<Throwable>> onFinish;
	private final Component title;
	private final Component subtitle;
	/** Starts a reload that waits for the card to be shown first, or null when it's already running. */
	private final @Nullable Runnable start;
	private final long openedAt = Util.getMillis();
	private int framesShown;
	private boolean started;
	private long doneAt = -1L;
	private boolean failed;
	private float progress;

	private UiLoadingOverlay(Minecraft minecraft, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, Component title, Component subtitle, @Nullable Runnable start) {
		this.minecraft = minecraft;
		this.reload = reload;
		this.onFinish = onFinish;
		this.title = title;
		this.subtitle = subtitle;
		this.start = start;
		this.started = start == null;
	}

	/** A card for a reload that's already running and reports real progress, such as resource packs. */
	public static UiLoadingOverlay running(Minecraft minecraft, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, Component title, Component subtitle) {
		return new UiLoadingOverlay(minecraft, reload, onFinish, title, subtitle, null);
	}

	/** A card for a reload that blocks the game while it runs, such as a shader compile; it's started once the card is shown. */
	public static UiLoadingOverlay blocking(Minecraft minecraft, ReloadInstance reload, Runnable start, Consumer<Optional<Throwable>> onFinish, Component title, Component subtitle) {
		return new UiLoadingOverlay(minecraft, reload, onFinish, title, subtitle, start);
	}

	@Override
	public void tick() {
		long now = Util.getMillis();
		if (!started && framesShown >= 2 && now - openedAt >= FADE_IN_MILLIS) {
			started = true;
			start.run();
		}
		if (started && doneAt < 0L && reload.isDone() && now - openedAt >= MIN_VISIBLE_MILLIS) {
			try {
				reload.checkExceptions();
				onFinish.accept(Optional.empty());
			} catch (Throwable failure) {
				failed = true;
				onFinish.accept(Optional.of(failure));
			}
			doneAt = now;
			if (minecraft.gui.screen() != null) {
				minecraft.gui.screen().init(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
			}
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		long now = Util.getMillis();
		float fadeIn = Math.clamp((now - openedAt) / (float) FADE_IN_MILLIS, 0.0F, 1.0F);
		float fadeOut = doneAt < 0L ? 1.0F : 1.0F - Math.clamp((now - doneAt - DONE_MILLIS) / (float) FADE_OUT_MILLIS, 0.0F, 1.0F);
		if (doneAt >= 0L && fadeOut <= 0.0F) {
			minecraft.gui.setOverlay(null);
			return;
		}
		framesShown++;
		// Minecraft doesn't draw the screen while an overlay is up, so the card draws it underneath, like its own loading screen does.
		if (minecraft.gui.screen() != null) {
			minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(context, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, delta);
		}
		float alpha = easeOut(fadeIn) * fadeOut;
		UiTheme theme = UiTheme.current();
		int screenWidth = context.guiWidth();
		int screenHeight = context.guiHeight();
		context.fill(0, 0, screenWidth, screenHeight, UiTheme.fade(theme.overlay(), alpha));

		boolean blocking = start != null;
		Component note = blocking && doneAt < 0L ? Component.translatable(EMUtilsTexts.UI_LOADING_SHADER_NOTE) : null;
		int titleHeight = UiText.lineHeight(minecraft.font, UiText.Size.BOLD);
		int lineHeight = UiText.lineHeight(minecraft.font, UiText.Size.BODY);
		int height = PADDING + titleHeight + 7 + lineHeight + 14 + BAR_HEIGHT + (note != null ? 12 + lineHeight : 0) + PADDING;
		int x = (screenWidth - WIDTH) / 2;
		int y = (screenHeight - height) / 2;
		UiOpacity.set(alpha);
		float scale = 0.97F + 0.03F * easeOut(fadeIn);
		context.pose().pushMatrix();
		context.pose().translate(x + WIDTH / 2.0F, y + height / 2.0F);
		context.pose().scale(scale, scale);
		context.pose().translate(-(x + WIDTH / 2.0F), -(y + height / 2.0F));
		UiShapes.shadow(context, x, y, WIDTH, height, 14, 22, theme.shadow());
		UiShapes.borderedRect(context, x, y, WIDTH, height, 14, theme.surface(), theme.border());

		int left = x + PADDING;
		int textWidth = WIDTH - PADDING * 2;
		boolean done = doneAt >= 0L;
		Component heading = !done ? title : Component.translatable(failed ? EMUtilsTexts.UI_LOADING_FAILED : EMUtilsTexts.UI_LOADING_DONE);
		UiText.draw(context, minecraft.font, UiText.ellipsize(minecraft.font, heading, UiText.Size.BOLD, textWidth - 16), UiText.Size.BOLD, left, y + PADDING, theme.text());
		if (done) {
			int check = 10;
			UiIcons.draw(context, failed ? HubIcons.X : HubIcons.CHECK, x + WIDTH - PADDING - check, y + PADDING - 1, check, failed ? theme.warning() : theme.accent());
		}
		UiText.draw(context, minecraft.font, UiText.ellipsize(minecraft.font, subtitle, UiText.Size.BODY, textWidth), UiText.Size.BODY, left, y + PADDING + titleHeight + 7, theme.muted());

		int barY = y + PADDING + titleHeight + 7 + lineHeight + 14;
		UiShapes.pill(context, left, barY, textWidth, BAR_HEIGHT, theme.switchOff());
		if (blocking && !done) {
			// Indeterminate: a short piece slides along. It stops while the shaders compile, since no frame is drawn then.
			int piece = textWidth / 3;
			double phase = (System.nanoTime() / 1_100_000_000.0) % 1.0;
			float pieceX = left + (float) (phase * (textWidth + piece)) - piece;
			context.enableScissor(left, barY, left + textWidth, barY + BAR_HEIGHT);
			int whole = (int) Math.floor(pieceX);
			context.pose().pushMatrix();
			context.pose().translate(pieceX - whole, 0.0F);
			UiShapes.pill(context, whole, barY, piece, BAR_HEIGHT, theme.accent());
			context.pose().popMatrix();
			context.disableScissor();
		} else {
			float target = done ? 1.0F : Math.clamp(reload.getActualProgress(), 0.0F, 1.0F);
			// Follows the real progress smoothly instead of jumping, like Minecraft's own bar.
			progress += (target - progress) * (done ? 0.35F : 0.18F);
			if (target - progress < 0.002F) {
				progress = target;
			}
			float fill = Math.max(BAR_HEIGHT, textWidth * progress);
			int whole = (int) Math.floor(fill);
			UiShapes.pill(context, left, barY, whole, BAR_HEIGHT, theme.accent());
		}
		if (note != null) {
			UiText.draw(context, minecraft.font, UiText.ellipsize(minecraft.font, note, UiText.Size.BODY, textWidth), UiText.Size.BODY, left, barY + BAR_HEIGHT + 12, theme.textSecondary());
		}
		context.pose().popMatrix();
		UiOpacity.reset();
	}

	private static float easeOut(float t) {
		float inverse = 1.0F - t;
		return 1.0F - inverse * inverse * inverse;
	}
}
