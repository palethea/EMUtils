package net.emutils.client.emutils.gui.hub;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class HubColorPickerGraphics {
	public static final int WHEEL_INNER_RADIUS = 34;
	public static final int WHEEL_OUTER_RADIUS = 52;
	public static final int INNER_RADIUS = 30;

	private static final int HUE_SCREEN_SIZE = WHEEL_OUTER_RADIUS * 2 + 2;
	private static final int SV_SCREEN_SIZE = INNER_RADIUS * 2 + 1;
	private static final float EDGE_SOFTNESS = 1.0F;

	private static Identifier hueWheelId;
	private static int activeSupersample;
	private static boolean hueWheelReady;

	private HubColorPickerGraphics() {
	}

	public static void clearHueWheel(MinecraftClient client) {
		if (hueWheelReady && client != null) {
			client.getTextureManager().destroyTexture(hueWheelId);
		}

		hueWheelReady = false;
		activeSupersample = 0;
	}

	public static void ensureHueWheel(MinecraftClient client) {
		int supersample = supersample(client);
		if (hueWheelReady && supersample == activeSupersample) {
			return;
		}

		clearHueWheel(client);
		activeSupersample = supersample;

		int texSize = HUE_SCREEN_SIZE * supersample;
		float center = texSize / 2.0F;
		float inner = WHEEL_INNER_RADIUS * supersample;
		float outer = WHEEL_OUTER_RADIUS * supersample;
		float softness = EDGE_SOFTNESS * supersample;

		NativeImage image = new NativeImage(texSize, texSize, false);
		for (int py = 0; py < texSize; py++) {
			for (int px = 0; px < texSize; px++) {
				float x = px + 0.5F - center;
				float y = py + 0.5F - center;
				float distance = (float) Math.sqrt(x * x + y * y);
				float coverage = ringCoverage(distance, inner, outer, softness);
				if (coverage <= 0.0F) {
					image.setColorArgb(px, py, 0);
					continue;
				}

				float logicalX = x / supersample;
				float logicalY = y / supersample;
				float angleHue = HubColorMath.hueFromPoint(logicalX, logicalY);
				int rgb = HubColorMath.hsvToRgb(angleHue, 1.0F, 1.0F);
				int alpha = Math.round(255.0F * coverage);
				image.setColorArgb(px, py, (alpha << 24) | (rgb & 0x00FFFFFF));
			}
		}

		hueWheelId = Identifier.of(EMUtilsClient.MOD_ID, "hub_color_hue_wheel");
		NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "EMUtils hub hue wheel", image);
		applySmoothFiltering(texture);
		client.getTextureManager().registerTexture(hueWheelId, texture);
		hueWheelReady = true;
	}

	public static void drawHueWheel(DrawContext context, int centerX, int centerY) {
		int left = centerX - HUE_SCREEN_SIZE / 2;
		int top = centerY - HUE_SCREEN_SIZE / 2;
		int texSize = HUE_SCREEN_SIZE * activeSupersample;
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			hueWheelId,
			left,
			top,
			0.0F,
			0.0F,
			HUE_SCREEN_SIZE,
			HUE_SCREEN_SIZE,
			texSize,
			texSize,
			texSize,
			texSize
		);
	}

	public static final class SaturationValueField {
		private final NativeImage image;
		private final NativeImageBackedTexture texture;
		private final Identifier textureId;
		private final int supersample;
		private float bakedHue = -1.0F;

		public SaturationValueField(MinecraftClient client) {
			this.supersample = supersample(client);
			int texSize = SV_SCREEN_SIZE * supersample;
			image = new NativeImage(texSize, texSize, false);
			textureId = Identifier.of(EMUtilsClient.MOD_ID, "hub_color_sv/" + System.nanoTime());
			texture = new NativeImageBackedTexture(() -> "EMUtils hub SV field", image);
			applySmoothFiltering(texture);
			client.getTextureManager().registerTexture(textureId, texture);
		}

		public void ensure(float hue, MinecraftClient client) {
			if (Math.abs(hue - bakedHue) < 0.002F) {
				return;
			}

			bakedHue = hue;
			int texSize = SV_SCREEN_SIZE * supersample;
			float center = texSize / 2.0F;
			float radius = INNER_RADIUS * supersample;
			float softness = EDGE_SOFTNESS * supersample;

			for (int py = 0; py < texSize; py++) {
				for (int px = 0; px < texSize; px++) {
					float x = px + 0.5F - center;
					float y = py + 0.5F - center;
					float distance = (float) Math.sqrt(x * x + y * y);
					float coverage = smoothEdge(radius, distance, softness);
					if (coverage <= 0.0F) {
						image.setColorArgb(px, py, 0);
						continue;
					}

					float logicalX = x / supersample;
					float logicalY = y / supersample;
					float sat = HubColorMath.saturationFromX(logicalX, INNER_RADIUS);
					float value = HubColorMath.brightnessFromY(logicalY, INNER_RADIUS);
					int rgb = HubColorMath.hsvToRgb(hue, sat, value);
					int alpha = Math.round(255.0F * coverage);
					image.setColorArgb(px, py, (alpha << 24) | (rgb & 0x00FFFFFF));
				}
			}

			texture.upload();
		}

		public void draw(DrawContext context, int centerX, int centerY) {
			int left = centerX - SV_SCREEN_SIZE / 2;
			int top = centerY - SV_SCREEN_SIZE / 2;
			int texSize = SV_SCREEN_SIZE * supersample;
			context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				textureId,
				left,
				top,
				0.0F,
				0.0F,
				SV_SCREEN_SIZE,
				SV_SCREEN_SIZE,
				texSize,
				texSize,
				texSize,
				texSize
			);
		}

		public void close(MinecraftClient client) {
			client.getTextureManager().destroyTexture(textureId);
			texture.close();
		}
	}

	private static int supersample(MinecraftClient client) {
		double guiScale = client.getWindow().getScaleFactor();
		return (int) MathHelper.clamp(Math.round(guiScale) * 2L, 4L, 8L);
	}

	private static float ringCoverage(float distance, float inner, float outer, float softness) {
		float outsideOuter = smoothEdge(outer, distance, softness);
		float insideInner = 1.0F - smoothEdge(inner, distance, softness);
		return Math.clamp(outsideOuter * insideInner, 0.0F, 1.0F);
	}

	private static float smoothEdge(float edge, float distance, float softness) {
		return Math.clamp((edge + softness - distance) / softness, 0.0F, 1.0F);
	}

	private static void applySmoothFiltering(NativeImageBackedTexture texture) {
		try {
			var method = texture.getClass().getMethod("setFilter", boolean.class, boolean.class);
			method.invoke(texture, false, true);
		} catch (ReflectiveOperationException ignored) {
		}
	}
}
