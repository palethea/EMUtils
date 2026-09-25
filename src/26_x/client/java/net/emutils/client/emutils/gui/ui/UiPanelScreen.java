package net.emutils.client.emutils.gui.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * A screen of the new UI (#88): a centered rounded panel that fades and scales in when it opens and
 * back out when it closes, over a dimmed and blurred world, in the current theme with a crossfade
 * when the theme switches. Subclasses lay out and draw what goes inside the panel, plus anything that
 * floats above it, such as sheets.
 */
public abstract class UiPanelScreen extends Screen {
	protected static final int PANEL_RADIUS = 14;
	private static final int MARGIN = 14;
	private static final float OPEN_SECONDS = 0.2F;
	private static final float THEME_SECONDS = 0.3F;
	private static final Identifier INWORLD_MENU_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/inworld_menu_background.png");

	protected final @Nullable Screen parent;
	protected final UiAnim anim = new UiAnim();
	protected int panelX;
	protected int panelY;
	protected int panelWidth;
	protected int panelHeight;
	private boolean prepared;
	private boolean closing;
	/** Shown again after a screen opened from this one closed; the background is already in place then. */
	private boolean returning;
	/** Closed into the game while the HUD still draws the fade-out, so it keeps its textures until then. */
	private boolean fadingOnHud;
	private float openProgress;
	/** 0 in dark mode, 1 in light mode, in between while crossfading. */
	private float lightness;

	protected UiPanelScreen(Component title, @Nullable Screen parent) {
		super(title);
		this.parent = parent;
		// Starts the open animation from nothing.
		anim.transition("open", 0.0F, OPEN_SECONDS, true);
	}

	/** The panel's largest size; it shrinks to fit smaller windows. */
	protected int maxPanelWidth() {
		return 720;
	}

	protected int maxPanelHeight() {
		return 430;
	}

	/** Lays out the panel's contents after {@link #panelX} and friends are set, and whenever the window resizes. */
	protected abstract void layout();

	/** Draws the panel's contents; the panel itself is already drawn and the open animation applied. */
	protected abstract void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY);

	/** Draws what floats above the panel, such as a sheet; called after the panel, without its animation. */
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
	}

	/** Called at the start of every frame, before anything is drawn. */
	protected void beforeFrame() {
	}

	/**
	 * Opened from gameplay, the world behind has no blur yet, so the blur fades in and out with the panel
	 * instead of appearing and vanishing in one frame. Opened from another menu, which already blurs,
	 * the blur stays as it is.
	 */
	protected boolean fadesBlur() {
		return parent == null && minecraft.level != null;
	}

	/** The current theme, crossfading for a moment after switching between dark and light. */
	protected UiTheme theme() {
		lightness = anim.transition("theme", UiTheme.current() == UiTheme.LIGHT, THEME_SECONDS);
		return UiTheme.blend(UiTheme.DARK, UiTheme.LIGHT, lightness);
	}

	/**
	 * How strongly the dimmed, blurred background shows. Moving between two screens of the new UI, it
	 * stays in place and only the panels animate, so the background doesn't dip in between.
	 */
	private float backgroundProgress() {
		if (parent instanceof UiPanelScreen || (returning && !closing)) {
			return 1.0F;
		}
		return openProgress;
	}

	/** 0 in dark mode, 1 in light mode, in between while the theme crossfades. */
	protected float lightness() {
		return lightness;
	}

	/** How far the open animation is, from 0 (closed) to 1 (open). */
	protected float openProgress() {
		return openProgress;
	}

	/** Whether the screen is fading out; it ignores input meanwhile. */
	protected boolean closing() {
		return closing;
	}

	/** Coming back from a screen opened from this one, the panel plays its open animation again. */
	@Override
	public void added() {
		super.added();
		UiClosingScreens.clear();
		if (prepared) {
			returning = true;
			prepared = false;
			closing = false;
			anim.snap("open", 0.0F);
		}
	}

	@Override
	protected final void init() {
		if (!prepared) {
			UiBlur.set(fadesBlur() ? backgroundProgress() : 1.0F);
		}
		UiText.refreshFonts();
		panelWidth = Math.min(maxPanelWidth(), width - MARGIN * 2);
		panelHeight = Math.min(maxPanelHeight(), height - MARGIN * 2);
		panelX = (width - panelWidth) / 2;
		panelY = (height - panelHeight) / 2;
		layout();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractBackground(context, mouseX, mouseY, delta);
		context.fill(0, 0, width, height, UiTheme.fade(theme().dim(), backgroundProgress()));
	}

	/**
	 * Vanilla darkens the world with this texture in the same frame the screen opens and stops the
	 * frame it closes; opened from gameplay, it fades with the panel instead, like the blur.
	 */
	@Override
	protected void extractMenuBackground(GuiGraphicsExtractor context) {
		if (!fadesBlur()) {
			super.extractMenuBackground(context);
			return;
		}
		float background = backgroundProgress();
		if (background > 0.0F) {
			context.blit(RenderPipelines.GUI_TEXTURED, INWORLD_MENU_BACKGROUND, 0, 0, 0.0F, 0.0F, width, height, width, height, 32, 32, UiTheme.fade(0xFFFFFFFF, background));
		}
	}

	@Override
	public final void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		anim.frame();
		beforeFrame();
		UiTheme theme = theme();
		// The first frame draws everything invisibly, so every text texture exists before the open
		// animation's clock starts and the animation can't hitch.
		openProgress = prepared ? anim.transition("open", closing ? 0.0F : 1.0F, OPEN_SECONDS, true) : 0.0F;
		UiOpacity.set(openProgress);
		UiBlur.set(fadesBlur() ? backgroundProgress() : 1.0F);
		float scale = 0.97F + 0.03F * openProgress;
		context.pose().pushMatrix();
		context.pose().translate(panelX + panelWidth / 2.0F, panelY + panelHeight / 2.0F);
		context.pose().scale(scale, scale);
		context.pose().translate(-(panelX + panelWidth / 2.0F), -(panelY + panelHeight / 2.0F));
		UiShapes.shadow(context, panelX, panelY, panelWidth, panelHeight, PANEL_RADIUS, 18, theme.shadow());
		UiShapes.roundedRect(context, panelX, panelY, panelWidth, panelHeight, PANEL_RADIUS, theme.panel());
		drawPanel(context, theme, mouseX, mouseY);
		context.pose().popMatrix();
		UiOpacity.reset();
		prepared = true;
		drawOverlay(context, theme, mouseX, mouseY);
	}

	/**
	 * Draws the panel's contents once, invisibly, into {@code context}, so the text, shapes and icons it
	 * uses are created and cached before the screen is first opened.
	 */
	public void prerender(GuiGraphicsExtractor context) {
		UiOpacity.set(0.0F);
		drawPanel(context, theme(), Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2);
		UiOpacity.reset();
	}

	/** Once the close animation has finished, returns to the previous screen; not while drawing, like vanilla. */
	@Override
	public void tick() {
		super.tick();
		if (closing && openProgress <= 0.0F) {
			minecraft.gui.setScreen(parent);
		}
	}

	@Override
	public void removed() {
		UiBlur.reset();
		if (!fadingOnHud) {
			dispose();
		}
		super.removed();
	}

	/**
	 * Frees what the screen holds, such as textures. Called when it's removed, or when its fade on the
	 * HUD has finished; the screen may be shown again afterwards, so recreate things lazily.
	 */
	protected void dispose() {
	}

	/** Called once the fade-out on the HUD has finished. */
	void finishFadingOnHud() {
		fadingOnHud = false;
		dispose();
	}

	/**
	 * Fades and scales the panel out, then returns to the previous screen. Going back to another screen
	 * of the new UI, it switches right away instead, and that screen's panel fades and scales in, the
	 * same way opening this one looked.
	 */
	@Override
	public void onClose() {
		if (parent instanceof UiPanelScreen) {
			minecraft.gui.setScreen(parent);
			return;
		}
		closing = true;
		if (parent == null && minecraft.level != null) {
			// Back to the game: hand control back right away, so the player can look around and move
			// while the panel is still fading; the HUD draws its last frames.
			fadingOnHud = true;
			minecraft.gui.setScreen(null);
			UiClosingScreens.add(this);
		}
	}

	/**
	 * Draws one frame of the close animation after the screen was closed into the game, with the
	 * background it would draw itself. Returns false once the animation has finished.
	 */
	boolean extractClosingFrame(GuiGraphicsExtractor context) {
		float background = backgroundProgress();
		UiBlur.set(fadesBlur() ? background : 1.0F);
		if (minecraft.options.getMenuBackgroundBlurriness() >= 1) {
			context.blurBeforeThisStratum();
		}
		extractMenuBackground(context);
		context.fill(0, 0, width, height, UiTheme.fade(theme().dim(), background));
		// The mouse is back in the game, so nothing in the panel is hovered.
		extractRenderState(context, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, 0.0F);
		return openProgress > 0.0F;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
