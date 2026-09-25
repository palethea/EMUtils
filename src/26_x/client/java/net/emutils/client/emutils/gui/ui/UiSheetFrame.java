package net.emutils.client.emutils.gui.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The frame of a sheet or dialog that floats over a screen: it dims what's behind, fades and scales
 * the sheet in over 0.2 s, and back out when closed. The owner lays out and draws the contents:
 *
 * <pre>
 * if (frame.firstFrame()) { layout(); prepareText(); }
 * if (!frame.begin(context, theme, screenWidth, screenHeight, x, y, width, height)) return;
 * ...draw the contents...
 * frame.endBody(context);
 * ...draw popups such as dropdowns, which don't scale with the sheet...
 * frame.end();
 * </pre>
 */
public final class UiSheetFrame {
	public static final float OPEN_SECONDS = 0.2F;
	private final UiAnim anim;
	private final String key;
	private final int radius;
	private boolean prepared;
	private boolean closing;
	private float progress;

	public UiSheetFrame(UiAnim anim, String key, int radius) {
		this.anim = anim;
		this.key = key;
		this.radius = radius;
		// Starts the open animation from nothing.
		anim.transition(key, 0.0F, OPEN_SECONDS, true);
	}

	/**
	 * True once, on the first frame, before the open animation's clock starts. Rendering every label
	 * then means the first frames of the animation have nothing new to create, so it runs without a hitch.
	 */
	public boolean firstFrame() {
		if (prepared) {
			return false;
		}
		prepared = true;
		return true;
	}

	/**
	 * Dims the screen, then starts drawing the sheet with its fade and scale applied. Returns false once
	 * the sheet has closed completely, when nothing else should be drawn.
	 */
	public boolean begin(GuiGraphicsExtractor context, UiTheme theme, int screenWidth, int screenHeight, int x, int y, int width, int height) {
		progress = anim.transition(key, closing ? 0.0F : 1.0F, OPEN_SECONDS, true);
		context.fill(0, 0, screenWidth, screenHeight, UiTheme.fade(theme.overlay(), progress));
		if (progress <= 0.0F) {
			return false;
		}
		// The whole sheet fades with the animation instead of popping away on its last frame.
		UiOpacity.set(progress);
		float scale = 0.96F + 0.04F * progress;
		context.pose().pushMatrix();
		context.pose().translate(x + width / 2.0F, y + height / 2.0F);
		context.pose().scale(scale, scale);
		context.pose().translate(-(x + width / 2.0F), -(y + height / 2.0F));
		UiShapes.shadow(context, x, y, width, height, radius, 22, UiTheme.fade(theme.shadow(), progress * 1.6F));
		UiShapes.borderedRect(context, x, y, width, height, radius, theme.surface(), theme.border());
		return true;
	}

	/** Ends the scaled part of the sheet; popups drawn after this still fade with it. */
	public void endBody(GuiGraphicsExtractor context) {
		context.pose().popMatrix();
	}

	public void end() {
		UiOpacity.reset();
	}

	public void close() {
		closing = true;
	}

	public boolean closing() {
		return closing;
	}

	/** Whether the close animation has finished, so the owner can drop the sheet. */
	public boolean isClosed() {
		return closing && progress <= 0.0F;
	}
}
