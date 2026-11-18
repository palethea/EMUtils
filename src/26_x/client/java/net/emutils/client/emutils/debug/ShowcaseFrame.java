package net.emutils.client.emutils.debug;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Frames a showcase screenshot for the Modrinth gallery (#272): rounded corners, a raised edge lit from above,
 * and a soft drop shadow, on a transparent background.
 */
final class ShowcaseFrame {
	// Sizes at 1920 pixels wide; other widths scale them.
	private static final double RADIUS = 34;
	private static final double EDGE = 4;
	private static final double PAD = 72;
	private static final double SHADOW_OFFSET = 26;
	private static final double SHADOW_BLUR = 34;
	private static final int SHADOW_ALPHA = 150;
	private static final double CONTACT_OFFSET = 6;
	private static final double CONTACT_BLUR = 6;
	private static final int CONTACT_ALPHA = 90;

	private ShowcaseFrame() {
	}

	static void frame(Path in, Path out) throws IOException {
		BufferedImage shot = ImageIO.read(in.toFile());
		if (shot == null) {
			throw new IOException("Not an image: " + in);
		}
		int w = shot.getWidth();
		int h = shot.getHeight();
		double scale = w / 1920.0D;
		double radius = RADIUS * scale;
		int edge = Math.max(2, (int) Math.round(EDGE * scale));
		int pad = (int) Math.round(PAD * scale);

		float[] mask = roundedMask(w, h, 0, 0, w, h, radius);
		float[] inner = roundedMask(w, h, edge, edge, w - 2 * edge, h - 2 * edge, Math.max(1, radius - edge));

		BufferedImage card = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = card.createGraphics();
		g.drawImage(shot, 0, 0, null);

		// A soft shade just inside the edge, so the picture looks set into its frame.
		float[] shadeMask = blur(inner.clone(), w, h, edge * 2);
		BufferedImage shade = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < mask.length; i++) {
			float a = Math.max(0, mask[i] - shadeMask[i]) * 60 / 255.0F;
			shade.setRGB(i % w, i / w, alpha(a) << 24);
		}
		g.drawImage(shade, 0, 0, null);

		// The raised edge: lit from above, so light along the top and dark along the bottom.
		BufferedImage rim = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			float t = y / (float) Math.max(1, h - 1);
			float light = 150 / 255.0F * (1 - t);
			float dark = 170 / 255.0F * t;
			float a = dark + light * (1 - dark);
			int grey = a <= 0 ? 0 : Math.round(255 * light * (1 - dark) / a);
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				float rimAlpha = Math.max(0, mask[i] - inner[i]);
				if (rimAlpha > 0) {
					rim.setRGB(x, y, alpha(a * rimAlpha) << 24 | grey << 16 | grey << 8 | grey);
				}
			}
		}
		g.drawImage(rim, 0, 0, null);

		// A hairline outline, so the card holds its shape on light and dark pages alike.
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setColor(new Color(0, 0, 0, 90));
		g.setStroke(new BasicStroke(1.0F));
		g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, radius * 2, radius * 2));
		g.dispose();
		for (int i = 0; i < mask.length; i++) {
			int x = i % w;
			int y = i / w;
			int argb = card.getRGB(x, y);
			card.setRGB(x, y, alpha((argb >>> 24) / 255.0F * mask[i]) << 24 | argb & 0xFFFFFF);
		}

		int cw = w + 2 * pad;
		int ch = h + 2 * pad + (int) Math.round(SHADOW_OFFSET * scale);
		BufferedImage canvas = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
		Graphics2D c = canvas.createGraphics();
		c.drawImage(shadow(mask, w, h, cw, ch, pad, (int) Math.round(SHADOW_OFFSET * scale), SHADOW_BLUR * scale, SHADOW_ALPHA), 0, 0, null);
		c.drawImage(shadow(mask, w, h, cw, ch, pad, (int) Math.round(CONTACT_OFFSET * scale), CONTACT_BLUR * scale, CONTACT_ALPHA), 0, 0, null);
		c.drawImage(card, pad, pad, null);
		c.dispose();
		ImageIO.write(canvas, "png", out.toFile());
	}

	private static BufferedImage shadow(float[] mask, int w, int h, int cw, int ch, int pad, int offset, double sigma, int alpha) {
		float[] shadow = new float[cw * ch];
		for (int y = 0; y < h; y++) {
			System.arraycopy(mask, y * w, shadow, (y + pad + offset) * cw + pad, w);
		}
		blur(shadow, cw, ch, sigma);
		BufferedImage image = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < shadow.length; i++) {
			image.setRGB(i % cw, i / cw, alpha(shadow[i] * alpha / 255.0F) << 24);
		}
		return image;
	}

	/** Coverage of a rounded rectangle, 0 to 1 per pixel, with smooth corners. */
	private static float[] roundedMask(int w, int h, int x, int y, int rw, int rh, double radius) {
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setColor(Color.WHITE);
		g.fill(new RoundRectangle2D.Double(x, y, rw, rh, radius * 2, radius * 2));
		g.dispose();
		float[] mask = new float[w * h];
		for (int i = 0; i < mask.length; i++) {
			mask[i] = (image.getRGB(i % w, i / w) >>> 24) / 255.0F;
		}
		return mask;
	}

	/** A Gaussian blur, close enough: three box blurs, each across and then down. */
	private static float[] blur(float[] values, int w, int h, double sigma) {
		int radius = Math.max(1, (int) Math.round(sigma));
		float[] scratch = new float[values.length];
		for (int pass = 0; pass < 3; pass++) {
			boxBlur(values, scratch, w, h, radius, true);
			boxBlur(scratch, values, w, h, radius, false);
		}
		return values;
	}

	private static void boxBlur(float[] from, float[] to, int w, int h, int radius, boolean across) {
		int lines = across ? h : w;
		int length = across ? w : h;
		int stride = across ? 1 : w;
		float scale = 1.0F / (radius * 2 + 1);
		for (int line = 0; line < lines; line++) {
			int start = across ? line * w : line;
			float sum = 0;
			for (int k = -radius; k <= radius; k++) {
				sum += from[start + Math.clamp(k, 0, length - 1) * stride];
			}
			for (int k = 0; k < length; k++) {
				to[start + k * stride] = sum * scale;
				sum += from[start + Math.min(k + radius + 1, length - 1) * stride] - from[start + Math.max(k - radius, 0) * stride];
			}
		}
	}

	private static int alpha(float a) {
		return Math.clamp(Math.round(a * 255), 0, 255);
	}
}
