package net.emutils.client.emutils.gui.ui;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.versioned.VersionedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Icons resampled to the exact number of screen pixels they cover. Drawing a 32px icon at another
 * size lets the GPU pick nearest pixels, which makes thin lines break up; averaging the source pixels
 * once per size keeps them smooth at every GUI scale.
 */
public final class UiIcons {
	private static final Map<String, Identifier> CACHE = new HashMap<>();
	private static int cacheScale;

	private UiIcons() {
	}

	/** Draws a white icon texture at {@code size} GUI pixels, tinted with {@code color}. */
	public static void draw(GuiGraphicsExtractor context, Identifier icon, int x, int y, int size, int color) {
		int scale = Math.max(1, (int) Math.ceil(Minecraft.getInstance().getWindow().getGuiScale()));
		if (scale != cacheScale) {
			clearCache();
			cacheScale = scale;
		}
		int pixels = Math.round(size * scale * UiRasterScale.get());
		Identifier texture = CACHE.computeIfAbsent(icon + "@" + pixels, ignored -> resample(icon, pixels));
		if (texture == null) {
			return;
		}
		context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, pixels, pixels, pixels, pixels, UiOpacity.apply(color));
	}

	public static void clearCache() {
		Minecraft client = Minecraft.getInstance();
		for (Identifier texture : CACHE.values()) {
			if (texture != null) {
				client.getTextureManager().release(texture);
			}
		}
		CACHE.clear();
		cacheScale = 0;
	}

	private static @Nullable Identifier resample(Identifier icon, int pixels) {
		NativeImage source;
		try (InputStream stream = Minecraft.getInstance().getResourceManager().open(icon)) {
			source = NativeImage.read(stream);
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not load UI icon {}", icon, exception);
			return null;
		}

		NativeImage target = new NativeImage(pixels, pixels, false);
		try (source) {
			float step = (float) source.getWidth() / pixels;
			for (int ty = 0; ty < pixels; ty++) {
				for (int tx = 0; tx < pixels; tx++) {
					target.setPixel(tx, ty, step >= 1.0F
						? average(source, tx * step, ty * step, step)
						: bilinear(source, (tx + 0.5F) * step - 0.5F, (ty + 0.5F) * step - 0.5F));
				}
			}
		}

		Identifier id = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "ui_icon/" + icon.getPath().replace('/', '_').replace(".png", "") + "_" + pixels);
		Minecraft.getInstance().getTextureManager().register(id, VersionedTextures.smoothTexture(() -> "EMUtils UI icon " + id.getPath(), target));
		return id;
	}

	/** Area-weighted average of the source pixels under one target pixel, in premultiplied alpha. */
	private static int average(NativeImage source, float left, float top, float step) {
		float right = left + step;
		float bottom = top + step;
		float alpha = 0.0F;
		float red = 0.0F;
		float green = 0.0F;
		float blue = 0.0F;
		float area = 0.0F;
		for (int sy = (int) Math.floor(top); sy < Math.ceil(bottom); sy++) {
			float wy = Math.min(bottom, sy + 1) - Math.max(top, sy);
			for (int sx = (int) Math.floor(left); sx < Math.ceil(right); sx++) {
				float wx = Math.min(right, sx + 1) - Math.max(left, sx);
				int pixel = sampleClamped(source, sx, sy);
				float weight = wx * wy;
				float a = ((pixel >>> 24) & 0xFF) / 255.0F * weight;
				alpha += a;
				red += ((pixel >>> 16) & 0xFF) * a;
				green += ((pixel >>> 8) & 0xFF) * a;
				blue += (pixel & 0xFF) * a;
				area += weight;
			}
		}
		if (alpha <= 0.0F) {
			return 0;
		}
		int outAlpha = Math.round(alpha / area * 255.0F);
		return (outAlpha << 24)
			| (Math.round(red / alpha) << 16)
			| (Math.round(green / alpha) << 8)
			| Math.round(blue / alpha);
	}

	/** Bilinear sample for icons drawn larger than their source, in premultiplied alpha. */
	private static int bilinear(NativeImage source, float x, float y) {
		int x0 = (int) Math.floor(x);
		int y0 = (int) Math.floor(y);
		float fx = x - x0;
		float fy = y - y0;
		float alpha = 0.0F;
		float red = 0.0F;
		float green = 0.0F;
		float blue = 0.0F;
		for (int dy = 0; dy <= 1; dy++) {
			for (int dx = 0; dx <= 1; dx++) {
				float weight = (dx == 0 ? 1.0F - fx : fx) * (dy == 0 ? 1.0F - fy : fy);
				int pixel = sampleClamped(source, x0 + dx, y0 + dy);
				float a = ((pixel >>> 24) & 0xFF) / 255.0F * weight;
				alpha += a;
				red += ((pixel >>> 16) & 0xFF) * a;
				green += ((pixel >>> 8) & 0xFF) * a;
				blue += (pixel & 0xFF) * a;
			}
		}
		if (alpha <= 0.0F) {
			return 0;
		}
		return (Math.round(alpha * 255.0F) << 24)
			| (Math.round(red / alpha) << 16)
			| (Math.round(green / alpha) << 8)
			| Math.round(blue / alpha);
	}

	private static int sampleClamped(NativeImage image, int x, int y) {
		return image.getPixel(Math.clamp(x, 0, image.getWidth() - 1), Math.clamp(y, 0, image.getHeight() - 1));
	}
}
