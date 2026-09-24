package net.emutils.client.emutils.gui.ui;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.versioned.VersionedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Anti-aliased rounded shapes. Each shape is baked once as a white alpha mask at the physical pixel
 * density (one texel per screen pixel), then drawn as a nine-slice tinted with the wanted color, so
 * hover and theme colors never need new textures.
 */
public final class UiShapes {
	private static final Map<Long, Patch> CACHE = new HashMap<>();
	private static int cacheScale;

	private UiShapes() {
	}

	public static void roundedRect(GuiGraphicsExtractor context, int x, int y, int width, int height, int radius, int color) {
		if (width <= 0 || height <= 0 || (color >>> 24) == 0) {
			return;
		}

		int clamped = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
		if (clamped == 0) {
			context.fill(x, y, x + width, y + height, UiOpacity.apply(color));
			return;
		}
		drawNineSlice(context, patch(Kind.RECT, clamped, 0), x, y, width, height, color);
	}

	public static void pill(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
		roundedRect(context, x, y, width, height, Math.min(width, height) / 2, color);
	}

	/**
	 * A circle drawn as one textured quad. Nine-slice pieces leave hairline seams when a circle moves by
	 * fractions of a pixel (switch knobs, slider and color handles), so circles get their own texture.
	 */
	public static void circle(GuiGraphicsExtractor context, int x, int y, int diameter, int color) {
		if (diameter <= 0 || (color >>> 24) == 0) {
			return;
		}
		Patch patch = patch(Kind.CIRCLE, diameter, 0);
		context.blit(RenderPipelines.GUI_TEXTURED, patch.id(), x, y, 0.0F, 0.0F, diameter, diameter, patch.textureSize(), patch.textureSize(), patch.textureSize(), patch.textureSize(), UiOpacity.apply(color));
	}

	/** A rounded rect with a 1px border; the fill is drawn inside the border. */
	public static void borderedRect(GuiGraphicsExtractor context, int x, int y, int width, int height, int radius, int fill, int border) {
		if ((border >>> 24) == 0) {
			roundedRect(context, x, y, width, height, radius, fill);
			return;
		}
		roundedRect(context, x, y, width, height, radius, border);
		roundedRect(context, x + 1, y + 1, width - 2, height - 2, Math.max(0, radius - 1), fill);
	}

	/** A soft shadow around the rect {@code x, y, width, height}, spreading {@code blur} pixels outwards. */
	public static void shadow(GuiGraphicsExtractor context, int x, int y, int width, int height, int radius, int blur, int color) {
		if (width <= 0 || height <= 0 || blur <= 0 || (color >>> 24) == 0) {
			return;
		}

		int clamped = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
		drawNineSlice(context, patch(Kind.SHADOW, clamped, blur), x - blur, y - blur, width + blur * 2, height + blur * 2, color);
	}

	public static void clearCache() {
		Minecraft client = Minecraft.getInstance();
		for (Patch patch : CACHE.values()) {
			client.getTextureManager().release(patch.id());
		}
		CACHE.clear();
		cacheScale = 0;
	}

	private static Patch patch(Kind kind, int radius, int blur) {
		int scale = physicalScale();
		if (scale != cacheScale) {
			clearCache();
			cacheScale = scale;
		}

		long key = ((long) kind.ordinal() << 40) | ((long) blur << 20) | radius;
		return CACHE.computeIfAbsent(key, ignored -> bake(kind, radius, blur, scale));
	}

	private static int physicalScale() {
		return Math.max(1, (int) Math.ceil(Minecraft.getInstance().getWindow().getGuiScale()));
	}

	private static Patch bake(Kind kind, int radius, int blur, int scale) {
		if (kind == Kind.CIRCLE) {
			return bakeCircle(radius, scale);
		}
		// The patch is the shape's corners plus a 2px stretchable middle, surrounded by the blur margin.
		int border = radius + blur;
		int logicalSize = border * 2 + 2;
		int size = logicalSize * scale;
		float shapeInset = blur * scale;
		float shapeSize = size - shapeInset * 2.0F;
		float shapeRadius = radius * scale;
		NativeImage image = new NativeImage(size, size, false);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				float distance = roundedRectDistance(px + 0.5F - shapeInset, py + 0.5F - shapeInset, shapeSize, shapeSize, shapeRadius);
				float coverage = kind == Kind.SHADOW
					? shadowCoverage(distance, blur * scale)
					: Math.clamp(0.5F - distance, 0.0F, 1.0F);
				int alpha = Math.round(coverage * 255.0F);
				image.setPixel(px, py, alpha <= 0 ? 0 : (alpha << 24) | 0xFFFFFF);
			}
		}

		Identifier id = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "ui_shape/" + kind.name().toLowerCase(Locale.ROOT) + "_" + radius + "_" + blur + "_" + scale);
		Minecraft.getInstance().getTextureManager().register(id, VersionedTextures.smoothTexture(() -> "EMUtils UI shape " + id.getPath(), image));
		return new Patch(id, size, border * scale, border);
	}

	/** A whole circle {@code diameter} pixels across, at the physical pixel density. */
	private static Patch bakeCircle(int diameter, int scale) {
		int size = diameter * scale;
		float center = size / 2.0F;
		NativeImage image = new NativeImage(size, size, false);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				float dx = px + 0.5F - center;
				float dy = py + 0.5F - center;
				float distance = (float) Math.sqrt(dx * dx + dy * dy) - center;
				int alpha = Math.round(Math.clamp(0.5F - distance, 0.0F, 1.0F) * 255.0F);
				image.setPixel(px, py, alpha <= 0 ? 0 : (alpha << 24) | 0xFFFFFF);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "ui_shape/circle_" + diameter + "_" + scale);
		Minecraft.getInstance().getTextureManager().register(id, VersionedTextures.smoothTexture(() -> "EMUtils UI circle", image));
		return new Patch(id, size, 0, 0);
	}

	/** Signed distance from a point to a rounded rect spanning {@code 0..width, 0..height}; negative inside. */
	private static float roundedRectDistance(float x, float y, float width, float height, float radius) {
		float halfW = width * 0.5F;
		float halfH = height * 0.5F;
		float ax = Math.abs(x - halfW) - (halfW - radius);
		float ay = Math.abs(y - halfH) - (halfH - radius);
		float outside = (float) Math.sqrt(Math.max(ax, 0.0F) * Math.max(ax, 0.0F) + Math.max(ay, 0.0F) * Math.max(ay, 0.0F));
		return outside + Math.min(Math.max(ax, ay), 0.0F) - radius;
	}

	private static float shadowCoverage(float distance, float spread) {
		if (spread <= 0.0F) {
			return distance <= 0.0F ? 1.0F : 0.0F;
		}
		float t = Math.clamp((distance + spread * 0.35F) / (spread * 1.35F), 0.0F, 1.0F);
		float falloff = 1.0F - t;
		return falloff * falloff;
	}

	private static void drawNineSlice(GuiGraphicsExtractor context, Patch patch, int x, int y, int width, int height, int color) {
		int border = Math.min(patch.border(), Math.min(width, height) / 2);
		int tex = patch.textureSize();
		int texBorder = patch.textureBorder();
		int middleTex = tex - texBorder * 2;
		int middleW = width - border * 2;
		int middleH = height - border * 2;

		part(context, patch, x, y, border, border, 0, 0, texBorder, texBorder, color);
		part(context, patch, x + width - border, y, border, border, tex - texBorder, 0, texBorder, texBorder, color);
		part(context, patch, x, y + height - border, border, border, 0, tex - texBorder, texBorder, texBorder, color);
		part(context, patch, x + width - border, y + height - border, border, border, tex - texBorder, tex - texBorder, texBorder, texBorder, color);
		if (middleW > 0) {
			part(context, patch, x + border, y, middleW, border, texBorder, 0, middleTex, texBorder, color);
			part(context, patch, x + border, y + height - border, middleW, border, texBorder, tex - texBorder, middleTex, texBorder, color);
		}
		if (middleH > 0) {
			part(context, patch, x, y + border, border, middleH, 0, texBorder, texBorder, middleTex, color);
			part(context, patch, x + width - border, y + border, border, middleH, tex - texBorder, texBorder, texBorder, middleTex, color);
		}
		if (middleW > 0 && middleH > 0) {
			part(context, patch, x + border, y + border, middleW, middleH, texBorder, texBorder, middleTex, middleTex, color);
		}
	}

	private static void part(GuiGraphicsExtractor context, Patch patch, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}
		context.blit(RenderPipelines.GUI_TEXTURED, patch.id(), x, y, u, v, width, height, regionWidth, regionHeight, patch.textureSize(), patch.textureSize(), UiOpacity.apply(color));
	}

	private enum Kind {
		RECT,
		SHADOW,
		CIRCLE
	}

	private record Patch(Identifier id, int textureSize, int textureBorder, int border) {
	}
}
