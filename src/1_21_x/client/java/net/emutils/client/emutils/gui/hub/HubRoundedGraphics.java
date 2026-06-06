package net.emutils.client.emutils.gui.hub;

import java.util.HashMap;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Anti-aliased rounded UI via supersampled textures. Patches are baked at several times the
 * on-screen size so Minecraft's GUI texture scaling produces smooth edges instead of chunky pixels.
 */
public final class HubRoundedGraphics {
	public static final int RADIUS_SM = 4;
	public static final int RADIUS_MD = 6;
	public static final int RADIUS_LG = 8;

	private static final int PATCH_RECT = 32;
	private static final int BORDER_RECT = 12;
	private static final int PATCH_PILL_H = 18;
	private static final float EDGE_SOFTNESS_BASE = 1.25F;

	private static final Map<Long, CachedPatch> PATCH_CACHE = new HashMap<>();
	private static int activeSupersample;

	private HubRoundedGraphics() {
	}

	public static void prewarm() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}

		ensureCacheScale(client);
		ensureRectPatch(client, HubPanelTheme.BACKGROUND);
		ensureRectPatch(client, HubPanelTheme.SURFACE);
		ensureRectPatch(client, HubPanelTheme.SURFACE_HOVER);
		ensureRectPatch(client, HubPanelTheme.SURFACE_SELECTED);
		ensureRectPatch(client, HubPanelTheme.TRACK);
		ensureRectPatch(client, HubPanelTheme.ACCENT);
		ensureRectPatch(client, HubPanelTheme.TEXT_MUTED);
		ensureRectPatch(client, HubPanelTheme.TEXT_PRIMARY);
		ensureRectPatch(client, HubPanelTheme.ACCENT_DIM);
		ensureRectPatch(client, HubPanelTheme.BORDER);
		ensurePillPatch(client, HubPanelTheme.TRACK, PATCH_PILL_H);
		ensurePillPatch(client, HubPanelTheme.ACCENT, PATCH_PILL_H);
	}

	public static void clearCache() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			for (CachedPatch patch : PATCH_CACHE.values()) {
				client.getTextureManager().destroyTexture(patch.id());
			}
		}

		PATCH_CACHE.clear();
		activeSupersample = 0;
	}

	public static void drawRoundedRect(
		DrawContext context,
		int left,
		int top,
		int right,
		int bottom,
		int color,
		int radius
	) {
		if (right <= left || bottom <= top) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		CachedPatch patch = ensureRectPatch(client, color);
		drawNineSlice(context, patch, left, top, right, bottom);
	}

	public static void drawPill(DrawContext context, int left, int top, int right, int bottom, int color) {
		if (right <= left || bottom <= top) {
			return;
		}

		int height = bottom - top;
		MinecraftClient client = MinecraftClient.getInstance();
		CachedPatch patch = ensurePillPatch(client, color, height);
		drawTextureRegion(
			context,
			patch.id(),
			left,
			top,
			right - left,
			bottom - top,
			0.0F,
			0.0F,
			patch.texWidth(),
			patch.texHeight(),
			patch.texWidth(),
			patch.texHeight()
		);
	}

	public static void drawCircle(DrawContext context, int centerX, int centerY, int diameter, int color) {
		if (diameter <= 0) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		CachedPatch patch = ensureCirclePatch(client, color, diameter);
		int left = centerX - diameter / 2;
		int top = centerY - diameter / 2;
		drawTextureRegion(
			context,
			patch.id(),
			left,
			top,
			diameter,
			diameter,
			0.0F,
			0.0F,
			patch.texWidth(),
			patch.texHeight(),
			patch.texWidth(),
			patch.texHeight()
		);
	}

	private static void ensureCacheScale(MinecraftClient client) {
		int supersample = supersample(client);
		if (supersample != activeSupersample) {
			clearCache();
			activeSupersample = supersample;
		}
	}

	private static int supersample(MinecraftClient client) {
		double guiScale = client.getWindow().getScaleFactor();
		return (int) MathHelper.clamp(Math.round(guiScale) * 2L, 4L, 8L);
	}

	private static void drawNineSlice(
		DrawContext context,
		CachedPatch patch,
		int left,
		int top,
		int right,
		int bottom
	) {
		int width = right - left;
		int height = bottom - top;
		int border = patch.screenBorder();
		int texBorder = patch.textureBorder();
		int texSize = patch.texWidth();
		int centerW = width - border * 2;
		int centerH = height - border * 2;
		int innerTex = texSize - texBorder * 2;

		int x = left;
		int y = top;

		drawTextureRegion(context, patch.id(), x, y, border, border, 0.0F, 0.0F, texBorder, texBorder, texSize, texSize);
		if (centerW > 0) {
			drawTextureRegion(
				context,
				patch.id(),
				x + border,
				y,
				centerW,
				border,
				texBorder,
				0.0F,
				innerTex,
				texBorder,
				texSize,
				texSize
			);
		}

		drawTextureRegion(
			context,
			patch.id(),
			x + width - border,
			y,
			border,
			border,
			texSize - texBorder,
			0.0F,
			texBorder,
			texBorder,
			texSize,
			texSize
		);

		if (centerH > 0) {
			drawTextureRegion(
				context,
				patch.id(),
				x,
				y + border,
				border,
				centerH,
				0.0F,
				texBorder,
				texBorder,
				innerTex,
				texSize,
				texSize
			);
			if (centerW > 0) {
				drawTextureRegion(
					context,
					patch.id(),
					x + border,
					y + border,
					centerW,
					centerH,
					texBorder,
					texBorder,
					innerTex,
					innerTex,
					texSize,
					texSize
				);
			}

			drawTextureRegion(
				context,
				patch.id(),
				x + width - border,
				y + border,
				border,
				centerH,
				texSize - texBorder,
				texBorder,
				texBorder,
				innerTex,
				texSize,
				texSize
			);
		}

		drawTextureRegion(
			context,
			patch.id(),
			x,
			y + height - border,
			border,
			border,
			0.0F,
			texSize - texBorder,
			texBorder,
			texBorder,
			texSize,
			texSize
		);
		if (centerW > 0) {
			drawTextureRegion(
				context,
				patch.id(),
				x + border,
				y + height - border,
				centerW,
				border,
				texBorder,
				texSize - texBorder,
				innerTex,
				texBorder,
				texSize,
				texSize
			);
		}

		drawTextureRegion(
			context,
			patch.id(),
			x + width - border,
			y + height - border,
			border,
			border,
			texSize - texBorder,
			texSize - texBorder,
			texBorder,
			texBorder,
			texSize,
			texSize
		);
	}

	private static void drawTextureRegion(
		DrawContext context,
		Identifier texture,
		int x,
		int y,
		int width,
		int height,
		float u,
		float v,
		int regionWidth,
		int regionHeight,
		int textureWidth,
		int textureHeight
	) {
		if (width <= 0 || height <= 0) {
			return;
		}

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			texture,
			x,
			y,
			u,
			v,
			width,
			height,
			regionWidth,
			regionHeight,
			textureWidth,
			textureHeight
		);
	}

	private static CachedPatch ensureRectPatch(MinecraftClient client, int color) {
		ensureCacheScale(client);
		int ss = activeSupersample;
		long key = packKey(0, color, PATCH_RECT, ss);
		return PATCH_CACHE.computeIfAbsent(key, k -> bakeRectPatch(client, color, PATCH_RECT, RADIUS_MD, BORDER_RECT, ss));
	}

	private static CachedPatch ensurePillPatch(MinecraftClient client, int color, int screenHeight) {
		ensureCacheScale(client);
		int ss = activeSupersample;
		int logicalHeight = Math.max(screenHeight + 2, PATCH_PILL_H);
		long key = packKey(1, color, logicalHeight, ss);
		return PATCH_CACHE.computeIfAbsent(key, k -> bakePillPatch(client, color, logicalHeight, ss));
	}

	private static CachedPatch ensureCirclePatch(MinecraftClient client, int color, int screenDiameter) {
		ensureCacheScale(client);
		int ss = activeSupersample;
		long key = packKey(2, color, screenDiameter, ss);
		return PATCH_CACHE.computeIfAbsent(key, k -> bakeCirclePatch(client, color, screenDiameter, ss));
	}

	private static long packKey(int shape, int color, int size, int supersample) {
		return ((long) shape << 60) | ((long) supersample << 52) | ((long) (color & 0xFFFFFFFFL) << 16) | (size & 0xFFFFL);
	}

	private static CachedPatch bakeRectPatch(
		MinecraftClient client,
		int color,
		int logicalSize,
		int radius,
		int screenBorder,
		int supersample
	) {
		int texSize = logicalSize * supersample;
		int texRadius = radius * supersample;
		NativeImage image = new NativeImage(texSize, texSize, false);
		fillRoundedRect(image, texSize, texSize, texRadius, color, supersample);
		return register(client, image, texSize, texSize, screenBorder, supersample, "rect");
	}

	private static CachedPatch bakePillPatch(MinecraftClient client, int color, int logicalHeight, int supersample) {
		int logicalWidth = logicalHeight * 3;
		int texHeight = logicalHeight * supersample;
		int texWidth = logicalWidth * supersample;
		int texRadius = (logicalHeight / 2) * supersample;
		int screenBorder = logicalHeight / 2;
		NativeImage image = new NativeImage(texWidth, texHeight, false);
		fillRoundedRect(image, texWidth, texHeight, texRadius, color, supersample);
		return register(client, image, texWidth, texHeight, screenBorder, supersample, "pill");
	}

	private static CachedPatch bakeCirclePatch(MinecraftClient client, int color, int screenDiameter, int supersample) {
		int texSize = screenDiameter * supersample;
		NativeImage image = new NativeImage(texSize, texSize, false);
		float radius = texSize / 2.0F - 0.5F;
		float center = texSize / 2.0F - 0.5F;
		int alpha = (color >>> 24) & 0xFF;
		int red = (color >>> 16) & 0xFF;
		int green = (color >>> 8) & 0xFF;
		int blue = color & 0xFF;
		float softness = EDGE_SOFTNESS_BASE * supersample;

		for (int y = 0; y < texSize; y++) {
			for (int x = 0; x < texSize; x++) {
				float dx = x - center;
				float dy = y - center;
				float distance = (float) Math.sqrt(dx * dx + dy * dy);
				float coverage = smoothEdge(radius, distance, softness);
				int pixelAlpha = Math.round(alpha * coverage);
				if (pixelAlpha <= 0) {
					image.setColorArgb(x, y, 0);
				} else {
					image.setColorArgb(x, y, (pixelAlpha << 24) | (red << 16) | (green << 8) | blue);
				}
			}
		}

		return register(client, image, texSize, texSize, 0, supersample, "circle");
	}

	private static void fillRoundedRect(NativeImage image, int width, int height, int radius, int color, int supersample) {
		int alpha = (color >>> 24) & 0xFF;
		int red = (color >>> 16) & 0xFF;
		int green = (color >>> 8) & 0xFF;
		int blue = color & 0xFF;
		float softness = EDGE_SOFTNESS_BASE * supersample;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float distance = roundedRectSdf(x + 0.5F, y + 0.5F, width, height, radius);
				float coverage = smoothEdge(0.0F, distance, softness);
				int pixelAlpha = Math.round(alpha * coverage);
				if (pixelAlpha <= 0) {
					image.setColorArgb(x, y, 0);
				} else {
					image.setColorArgb(x, y, (pixelAlpha << 24) | (red << 16) | (green << 8) | blue);
				}
			}
		}
	}

	private static float roundedRectSdf(float x, float y, float width, float height, float radius) {
		float halfW = width * 0.5F;
		float halfH = height * 0.5F;
		float cx = x - halfW;
		float cy = y - halfH;
		float clampedRadius = Math.min(radius, Math.min(halfW, halfH));
		float ax = Math.abs(cx) - (halfW - clampedRadius);
		float ay = Math.abs(cy) - (halfH - clampedRadius);
		float outsideX = Math.max(ax, 0.0F);
		float outsideY = Math.max(ay, 0.0F);
		float outside = (float) Math.sqrt(outsideX * outsideX + outsideY * outsideY);
		float inside = Math.min(Math.max(ax, ay), 0.0F);
		return outside + inside - clampedRadius;
	}

	private static float smoothEdge(float edge, float distance, float softness) {
		return Math.clamp((edge + softness - distance) / softness, 0.0F, 1.0F);
	}

	private static CachedPatch register(
		MinecraftClient client,
		NativeImage image,
		int texWidth,
		int texHeight,
		int screenBorder,
		int supersample,
		String kind
	) {
		Identifier id = Identifier.of(EMUtilsClient.MOD_ID, "hub_rounded/" + kind + "/" + PATCH_CACHE.size());
		NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "EMUtils hub UI " + kind, image);
		applySmoothFiltering(texture);
		client.getTextureManager().registerTexture(id, texture);
		int textureBorder = screenBorder * supersample;
		return new CachedPatch(id, texWidth, texHeight, textureBorder, screenBorder, supersample);
	}

	private static void applySmoothFiltering(NativeImageBackedTexture texture) {
		try {
			var method = texture.getClass().getMethod("setFilter", boolean.class, boolean.class);
			method.invoke(texture, false, true);
		} catch (ReflectiveOperationException ignored) {
		}
	}

	private record CachedPatch(
		Identifier id,
		int texWidth,
		int texHeight,
		int textureBorder,
		int screenBorder,
		int supersample
	) {
	}
}
