package net.emutils.client.emutils.gui.hub;

final class HubColorMath {
	private HubColorMath() {
	}

	static float hueFromPoint(double x, double y) {
		return (float) ((Math.atan2(-y, x) / (Math.PI * 2.0) + 1.0) % 1.0);
	}

	static float saturationFromX(float x, int radius) {
		return Math.clamp((x / radius + 1.0F) / 2.0F, 0.0F, 1.0F);
	}

	static float brightnessFromY(float y, int radius) {
		return Math.clamp(1.0F - (y / radius + 1.0F) / 2.0F, 0.0F, 1.0F);
	}

	static double[] clampToDisc(double x, double y, double radius) {
		double distance = Math.hypot(x, y);
		if (distance <= radius || distance <= 0.0) {
			return new double[] { x, y };
		}

		double scale = radius / distance;
		return new double[] { x * scale, y * scale };
	}

	static float[] discToSaturationBrightness(double x, double y, int radius) {
		double[] clamped = clampToDisc(x, y, radius);
		return new float[] {
			saturationFromX((float) clamped[0], radius),
			brightnessFromY((float) clamped[1], radius)
		};
	}

	static double[] saturationBrightnessToDisc(float saturation, float brightness, int radius) {
		double x = (saturation * 2.0F - 1.0F) * radius;
		double y = (1.0F - brightness * 2.0F) * radius;
		return clampToDisc(x, y, radius);
	}

	static float[] rgbToHsv(int red, int green, int blue) {
		float r = red / 255.0F;
		float g = green / 255.0F;
		float b = blue / 255.0F;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float hueValue = 0.0F;
		if (delta > 1.0E-5F) {
			if (max == r) {
				hueValue = ((g - b) / delta) % 6.0F;
			} else if (max == g) {
				hueValue = (b - r) / delta + 2.0F;
			} else {
				hueValue = (r - g) / delta + 4.0F;
			}

			hueValue /= 6.0F;
		}

		float saturationValue = max <= 1.0E-5F ? 0.0F : delta / max;
		return new float[] { (hueValue + 1.0F) % 1.0F, saturationValue, max };
	}

	static int hsvToRgb(float hueValue, float saturationValue, float brightnessValue) {
		float chroma = brightnessValue * saturationValue;
		float h = hueValue * 6.0F;
		float intermediate = chroma * (1.0F - Math.abs(h % 2.0F - 1.0F));
		float red;
		float green;
		float blue;
		if (h < 1.0F) {
			red = chroma;
			green = intermediate;
			blue = 0.0F;
		} else if (h < 2.0F) {
			red = intermediate;
			green = chroma;
			blue = 0.0F;
		} else if (h < 3.0F) {
			red = 0.0F;
			green = chroma;
			blue = intermediate;
		} else if (h < 4.0F) {
			red = 0.0F;
			green = intermediate;
			blue = chroma;
		} else if (h < 5.0F) {
			red = intermediate;
			green = 0.0F;
			blue = chroma;
		} else {
			red = chroma;
			green = 0.0F;
			blue = intermediate;
		}

		float match = brightnessValue - chroma;
		int redByte = Math.round((red + match) * 255.0F);
		int greenByte = Math.round((green + match) * 255.0F);
		int blueByte = Math.round((blue + match) * 255.0F);
		return (redByte << 16) | (greenByte << 8) | blueByte;
	}
}
