package net.emutils.client.emutils.gui.ui;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.versioned.VersionedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A color picker popover: a saturation/brightness square, a hue bar, and a preview with the hex value.
 * The square is the hue color with baked white and black gradient masks on top, so it stays smooth and
 * needs no new texture while dragging. The color is saved when the mouse is released.
 */
public final class UiColorPicker {
	private static final int WIDTH = 156;
	private static final int PADDING = 10;
	private static final int FIELD_HEIGHT = 92;
	private static final int FIELD_RADIUS = 7;
	private static final int HUE_HEIGHT = 10;
	private static final int GAP = 9;
	private static final int PREVIEW = 18;
	private static final int HEIGHT = PADDING + FIELD_HEIGHT + GAP + HUE_HEIGHT + GAP + PREVIEW + PADDING;
	private static final int HANDLE = 11;
	private static final Map<String, Identifier> TEXTURES = new HashMap<>();
	private static int textureScale;

	private final IntSupplier getter;
	private final IntConsumer setter;
	private final int alpha;
	private int x;
	private int y;
	private float hue;
	private float saturation;
	private float brightness;
	private boolean draggingField;
	private boolean draggingHue;
	private boolean dirty;

	/** Opens the picker beside the point {@code anchorX, anchorY} (usually the left edge of a swatch). */
	public UiColorPicker(IntSupplier getter, IntConsumer setter, int anchorX, int anchorY, int screenWidth, int screenHeight) {
		this.getter = getter;
		this.setter = setter;
		int color = getter.getAsInt();
		int originalAlpha = (color >>> 24) & 0xFF;
		this.alpha = originalAlpha == 0 ? 0xFF : originalAlpha;
		float[] hsv = toHsv(color);
		hue = hsv[0];
		saturation = hsv[1];
		brightness = hsv[2];
		x = Math.clamp(anchorX - WIDTH - 8, 8, Math.max(8, screenWidth - WIDTH - 8));
		y = Math.clamp(anchorY - HEIGHT / 2, 8, Math.max(8, screenHeight - HEIGHT - 8));
	}

	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
	}

	/** The color currently shown, with the original alpha kept. */
	public int color() {
		return (alpha << 24) | (fromHsv(hue, saturation, brightness) & 0xFFFFFF);
	}

	public void render(GuiGraphicsExtractor context, Font font, UiTheme theme) {
		UiShapes.shadow(context, x, y, WIDTH, HEIGHT, 12, 14, theme.shadow());
		UiShapes.borderedRect(context, x, y, WIDTH, HEIGHT, 12, theme.surface(), theme.line());

		int fieldX = x + PADDING;
		int fieldY = y + PADDING;
		int fieldWidth = WIDTH - PADDING * 2;
		// Hue color, then white fading out to the right, then black fading in towards the bottom.
		UiShapes.roundedRect(context, fieldX, fieldY, fieldWidth, FIELD_HEIGHT, FIELD_RADIUS, 0xFF000000 | fromHsv(hue, 1.0F, 1.0F));
		blitTexture(context, texture(Layer.WHITE, fieldWidth, FIELD_HEIGHT), fieldX, fieldY, fieldWidth, FIELD_HEIGHT, 0xFFFFFFFF);
		blitTexture(context, texture(Layer.BLACK, fieldWidth, FIELD_HEIGHT), fieldX, fieldY, fieldWidth, FIELD_HEIGHT, 0xFF000000);
		handle(context, theme, fieldX + saturation * fieldWidth, fieldY + (1.0F - brightness) * FIELD_HEIGHT, 0xFF000000 | fromHsv(hue, saturation, brightness));

		int hueY = fieldY + FIELD_HEIGHT + GAP;
		blitTexture(context, texture(Layer.HUE, fieldWidth, HUE_HEIGHT), fieldX, hueY, fieldWidth, HUE_HEIGHT, 0xFFFFFFFF);
		handle(context, theme, fieldX + hue * fieldWidth, hueY + HUE_HEIGHT / 2.0F, 0xFF000000 | fromHsv(hue, 1.0F, 1.0F));

		int previewY = hueY + HUE_HEIGHT + GAP;
		UiShapes.roundedRect(context, fieldX, previewY, PREVIEW, PREVIEW, 5, theme.line());
		UiShapes.roundedRect(context, fieldX + 1, previewY + 1, PREVIEW - 2, PREVIEW - 2, 4, 0xFF000000 | color());
		Component hex = Component.literal(String.format(Locale.ROOT, "#%06X", color() & 0xFFFFFF));
		UiText.drawCentered(context, font, hex, UiText.Size.LABEL, fieldX + PREVIEW + 8, previewY + PREVIEW / 2, theme.text());
	}

	/** A round handle centered on a point, which may be fractional so dragging glides. */
	private static void handle(GuiGraphicsExtractor context, UiTheme theme, float centerX, float centerY, int color) {
		float left = centerX - HANDLE / 2.0F;
		float top = centerY - HANDLE / 2.0F;
		int wholeX = (int) Math.floor(left);
		int wholeY = (int) Math.floor(top);
		context.pose().pushMatrix();
		context.pose().translate(left - wholeX, top - wholeY);
		UiShapes.shadow(context, wholeX, wholeY, HANDLE, HANDLE, HANDLE / 2, 3, UiTheme.fade(theme.shadow(), 0.9F));
		UiShapes.circle(context, wholeX, wholeY, HANDLE, 0xFFFFFFFF);
		UiShapes.circle(context, wholeX + 2, wholeY + 2, HANDLE - 4, color);
		context.pose().popMatrix();
	}

	public boolean mouseClicked(double mouseX, double mouseY) {
		int fieldX = x + PADDING;
		int fieldY = y + PADDING;
		int fieldWidth = WIDTH - PADDING * 2;
		int hueY = fieldY + FIELD_HEIGHT + GAP;
		if (mouseX >= fieldX - 3 && mouseX < fieldX + fieldWidth + 3 && mouseY >= fieldY - 3 && mouseY < fieldY + FIELD_HEIGHT + 3) {
			draggingField = true;
			drag(mouseX, mouseY);
			return true;
		}
		if (mouseX >= fieldX - 3 && mouseX < fieldX + fieldWidth + 3 && mouseY >= hueY - 3 && mouseY < hueY + HUE_HEIGHT + 3) {
			draggingHue = true;
			drag(mouseX, mouseY);
			return true;
		}
		return contains(mouseX, mouseY);
	}

	public boolean drag(double mouseX, double mouseY) {
		int fieldX = x + PADDING;
		int fieldY = y + PADDING;
		int fieldWidth = WIDTH - PADDING * 2;
		if (draggingField) {
			saturation = (float) Math.clamp((mouseX - fieldX) / fieldWidth, 0.0, 1.0);
			brightness = 1.0F - (float) Math.clamp((mouseY - fieldY) / FIELD_HEIGHT, 0.0, 1.0);
			dirty = true;
			return true;
		}
		if (draggingHue) {
			hue = (float) Math.clamp((mouseX - fieldX) / fieldWidth, 0.0, 0.999);
			dirty = true;
			return true;
		}
		return false;
	}

	/** Saves the color if it changed; call when the mouse is released and when the picker closes. */
	public void release() {
		draggingField = false;
		draggingHue = false;
		if (dirty) {
			dirty = false;
			setter.accept(color());
		}
	}

	// ---- textures -------------------------------------------------------------------------------

	private enum Layer {
		WHITE,
		BLACK,
		HUE
	}

	private static void blitTexture(GuiGraphicsExtractor context, Identifier texture, int x, int y, int width, int height, int color) {
		int scale = scale();
		context.pose().pushMatrix();
		context.pose().translate(x, y);
		context.pose().scale(1.0F / scale, 1.0F / scale);
		int pixelWidth = width * scale;
		int pixelHeight = height * scale;
		context.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, pixelWidth, pixelHeight, pixelWidth, pixelHeight, pixelWidth, pixelHeight, color);
		context.pose().popMatrix();
	}

	private static int scale() {
		return Math.max(1, (int) Math.ceil(Minecraft.getInstance().getWindow().getGuiScale()));
	}

	private static Identifier texture(Layer layer, int width, int height) {
		int scale = scale();
		if (scale != textureScale) {
			for (Identifier texture : TEXTURES.values()) {
				Minecraft.getInstance().getTextureManager().release(texture);
			}
			TEXTURES.clear();
			textureScale = scale;
		}
		return TEXTURES.computeIfAbsent(layer + "@" + width + "x" + height, ignored -> bake(layer, width * scale, height * scale, scale));
	}

	/** Bakes one layer at physical pixels, with the rounded corners anti-aliased into its alpha. */
	private static Identifier bake(Layer layer, int width, int height, int scale) {
		NativeImage image = new NativeImage(width, height, true);
		float radius = layer == Layer.HUE ? height / 2.0F : FIELD_RADIUS * scale;
		for (int py = 0; py < height; py++) {
			for (int px = 0; px < width; px++) {
				float coverage = Math.clamp(0.5F - roundedDistance(px + 0.5F, py + 0.5F, width, height, radius), 0.0F, 1.0F);
				if (coverage <= 0.0F) {
					continue;
				}
				int argb = switch (layer) {
					case WHITE -> ((int) (255 * coverage * (1.0F - px / (float) (width - 1))) << 24) | 0xFFFFFF;
					case BLACK -> ((int) (255 * coverage * (py / (float) (height - 1))) << 24) | 0xFFFFFF;
					case HUE -> ((int) (255 * coverage) << 24) | (fromHsv(px / (float) width, 1.0F, 1.0F) & 0xFFFFFF);
				};
				image.setPixel(px, py, argb);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "ui_color/" + layer.name().toLowerCase(Locale.ROOT) + "_" + width + "x" + height);
		Minecraft.getInstance().getTextureManager().register(id, VersionedTextures.smoothTexture(() -> "EMUtils color picker", image));
		return id;
	}

	private static float roundedDistance(float x, float y, float width, float height, float radius) {
		float ax = Math.abs(x - width / 2.0F) - (width / 2.0F - radius);
		float ay = Math.abs(y - height / 2.0F) - (height / 2.0F - radius);
		float outside = (float) Math.sqrt(Math.max(ax, 0.0F) * Math.max(ax, 0.0F) + Math.max(ay, 0.0F) * Math.max(ay, 0.0F));
		return outside + Math.min(Math.max(ax, ay), 0.0F) - radius;
	}

	// ---- color math -----------------------------------------------------------------------------

	/** HSV (each 0..1) to RGB. */
	private static int fromHsv(float h, float s, float v) {
		float hue = (h - (float) Math.floor(h)) * 6.0F;
		int sector = (int) hue;
		float fraction = hue - sector;
		float p = v * (1.0F - s);
		float q = v * (1.0F - s * fraction);
		float t = v * (1.0F - s * (1.0F - fraction));
		float r;
		float g;
		float b;
		switch (sector) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return (Math.round(r * 255.0F) << 16) | (Math.round(g * 255.0F) << 8) | Math.round(b * 255.0F);
	}

	private static float[] toHsv(int color) {
		float r = ((color >>> 16) & 0xFF) / 255.0F;
		float g = ((color >>> 8) & 0xFF) / 255.0F;
		float b = (color & 0xFF) / 255.0F;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float h = 0.0F;
		if (delta > 0.0F) {
			if (max == r) {
				h = ((g - b) / delta) / 6.0F;
			} else if (max == g) {
				h = ((b - r) / delta + 2.0F) / 6.0F;
			} else {
				h = ((r - g) / delta + 4.0F) / 6.0F;
			}
			if (h < 0.0F) {
				h += 1.0F;
			}
		}
		return new float[] {h, max <= 0.0F ? 0.0F : delta / max, max};
	}
}
