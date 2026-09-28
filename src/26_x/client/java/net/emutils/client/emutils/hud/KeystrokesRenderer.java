package net.emutils.client.emutils.hud;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.EMUtilsHudElements;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiRasterScale;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The Keystrokes overlay (#43): the movement keys, and optionally the mouse buttons with clicks per
 * second, the jump key and the sneak key, as keys in the settings UI's look that light up while held.
 * Each key shows what it's bound to, so rebound controls show their own keys.
 */
public final class KeystrokesRenderer {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "keystrokes");
	private static final int KEY = 24;
	private static final int GAP = 3;
	private static final int WIDTH = KEY * 3 + GAP * 2;
	private static final int MOUSE_HEIGHT = 26;
	private static final int SPACE_HEIGHT = 13;
	private static final int SNEAK_HEIGHT = 16;
	private static final int SPACE_BAR_WIDTH = 22;
	private static final int LEFT_MARGIN = 8;
	/** Below this background opacity the world shows through, so labels get a shadow to stay readable. */
	private static final int TEXT_SHADOW_BELOW_OPACITY = 50;
	private static final UiText.Size LABEL_SIZE = UiText.Size.LABEL;
	private static final UiText.Size CPS_SIZE = UiText.Size.SMALL;
	private static final UiAnim ANIM = new UiAnim();

	private KeystrokesRenderer() {
	}

	/** One key of the overlay, at its place in the unscaled layout. */
	private record Key(String id, @Nullable String label, @Nullable String detail, int x, int y, int width, int height, boolean down) {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	public static int width() {
		return WIDTH;
	}

	public static int height(EMUtilsConfig config) {
		int height = KEY * 2 + GAP;
		if (config.keystrokesMouse()) {
			height += GAP + MOUSE_HEIGHT;
		}
		if (config.keystrokesSpace()) {
			height += GAP + SPACE_HEIGHT;
		}
		if (config.keystrokesSneak()) {
			height += GAP + SNEAK_HEIGHT;
		}
		return height;
	}

	static int defaultX() {
		return LEFT_MARGIN;
	}

	static int defaultY(int screenHeight, int height) {
		return (screenHeight - height) / 2;
	}

	private static List<Key> keys(Minecraft client, EMUtilsConfig config) {
		Options options = client.options;
		List<Key> keys = new ArrayList<>();
		int column = KEY + GAP;
		keys.add(key("forward", options.keyUp, column, 0, KEY, KEY));
		keys.add(key("left", options.keyLeft, 0, column, KEY, KEY));
		keys.add(key("back", options.keyDown, column, column, KEY, KEY));
		keys.add(key("right", options.keyRight, column * 2, column, KEY, KEY));
		int y = column * 2;
		if (config.keystrokesMouse()) {
			int half = (WIDTH - GAP) / 2;
			boolean cps = config.keystrokesCps();
			keys.add(new Key("attack", text(EMUtilsTexts.HUD_KEYSTROKES_LMB), cps ? cps(ClickCounter.leftCps()) : null, 0, y, half, MOUSE_HEIGHT, options.keyAttack.isDown()));
			keys.add(new Key("use", text(EMUtilsTexts.HUD_KEYSTROKES_RMB), cps ? cps(ClickCounter.rightCps()) : null, WIDTH - half, y, half, MOUSE_HEIGHT, options.keyUse.isDown()));
			y += MOUSE_HEIGHT + GAP;
		}
		if (config.keystrokesSpace()) {
			// The jump key is drawn as a bar, like a space bar.
			keys.add(new Key("jump", null, null, 0, y, WIDTH, SPACE_HEIGHT, options.keyJump.isDown()));
			y += SPACE_HEIGHT + GAP;
		}
		if (config.keystrokesSneak()) {
			keys.add(key("sneak", options.keyShift, 0, y, WIDTH, SNEAK_HEIGHT));
		}
		return keys;
	}

	private static Key key(String id, KeyMapping mapping, int x, int y, int width, int height) {
		return new Key(id, mapping.getTranslatedKeyMessage().getString(), null, x, y, width, height, mapping.isDown());
	}

	/** Draws the keys with their top-left corner at ({@code x}, {@code y}) in the current pose. */
	static void renderKeys(GuiGraphicsExtractor context, Minecraft client, EMUtilsConfig config, int x, int y, int opacityPercent) {
		ANIM.frame();
		UiTheme theme = UiTheme.current();
		Font font = client.font;
		float opacity = Math.clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
		boolean shadow = opacityPercent < TEXT_SHADOW_BELOW_OPACITY;
		int pressedColor = config.keystrokesMenuAccent() ? theme.accent() : config.keystrokesPressedColor();
		int pressedText = UiTheme.luminance(pressedColor) > 0.6F ? 0xFF1A1A1A : 0xFFFFFFFF;
		int radius = config.keystrokesStyle().radius();
		for (Key key : keys(client, config)) {
			// Lights up at once and fades out a little slower, so quick taps still show.
			float pressed = ANIM.towards("keystrokes:" + key.id(), key.down() ? 1.0F : 0.0F, key.down() ? 60.0F : 16.0F);
			int keyX = x + key.x();
			int keyY = y + key.y();
			int fill = UiTheme.mix(UiTheme.fade(theme.panel(), opacity), pressedColor, pressed);
			int border = UiTheme.mix(UiTheme.fade(theme.border(), opacity), pressedColor, pressed);
			UiShapes.borderedRect(context, keyX, keyY, key.width(), key.height(), Math.min(radius, key.height() / 2), fill, border);
			int textColor = UiTheme.mix(theme.text(), pressedText, pressed);
			int centerX = keyX + key.width() / 2;
			if (key.label() == null) {
				int barY = keyY + key.height() / 2 - 1;
				UiShapes.pill(context, centerX - SPACE_BAR_WIDTH / 2, barY, SPACE_BAR_WIDTH, 2, textColor);
				continue;
			}
			Component label = UiText.ellipsize(font, Component.literal(key.label()), LABEL_SIZE, key.width() - 4);
			if (key.detail() == null) {
				drawCentered(context, font, label, LABEL_SIZE, centerX, keyY + key.height() / 2, textColor, shadow);
				continue;
			}
			int labelHeight = UiText.lineHeight(font, LABEL_SIZE);
			int detailHeight = UiText.lineHeight(font, CPS_SIZE);
			int top = keyY + (key.height() - labelHeight - 1 - detailHeight) / 2;
			drawCentered(context, font, label, LABEL_SIZE, centerX, top + labelHeight / 2, textColor, shadow);
			int detailColor = UiTheme.mix(theme.muted(), pressedText, pressed);
			drawCentered(context, font, Component.literal(key.detail()), CPS_SIZE, centerX, top + labelHeight + 1 + detailHeight / 2, detailColor, shadow);
		}
	}

	private static void drawCentered(GuiGraphicsExtractor context, Font font, Component text, UiText.Size size, int centerX, int centerY, int color, boolean shadow) {
		int x = centerX - UiText.width(font, text, size) / 2;
		int top = centerY - UiText.lineHeight(font, size) / 2;
		if (shadow) {
			float offset = HudOverlayRenderer.shadowOffset();
			UiText.drawExact(context, font, text, size, x + offset, top + offset, HudOverlayRenderer.shadowColor());
		}
		UiText.draw(context, font, text, size, x, top, color);
	}

	private static String cps(int clicks) {
		return Component.translatable(EMUtilsTexts.HUD_KEYSTROKES_CPS, clicks).getString();
	}

	private static String text(String key) {
		return Component.translatable(key).getString();
	}

	private static void render(GuiGraphicsExtractor context) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || client == null || client.player == null || client.level == null) {
			return;
		}
		// The layout editor draws its own preview.
		if (!config.keystrokes() || HudLayoutManager.isEditing()) {
			return;
		}
		if (MinecraftClientCompat.isHudHidden(client)) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (config.keystrokesHideInContainers() && MinecraftClientCompat.screen(client) instanceof AbstractContainerScreen<?>) {
			return;
		}

		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			EMUtilsHudElements.KEYSTROKES,
			config,
			context.guiWidth(),
			context.guiHeight(),
			client
		);
		context.pose().pushMatrix();
		UiRasterScale.set(layout.scaleFactor());
		try {
			context.pose().translate(layout.position().x(), layout.position().y());
			context.pose().scale(layout.scaleFactor(), layout.scaleFactor());
			renderKeys(context, client, config, 0, 0, layout.opacityPercent());
		} finally {
			UiRasterScale.reset();
			context.pose().popMatrix();
		}
	}
}
