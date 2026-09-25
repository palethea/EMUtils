package net.emutils.client.emutils.gui.ui;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Text in the settings UI, set in the bundled Nunito font. It is normally drawn by
 * {@link UiFontRenderer}, which renders each string with FreeType at the physical pixel size. If that
 * is unavailable, it falls back to Minecraft's text rendering with one font definition per GUI scale
 * ({@code ui_body_s3} renders glyphs at 3x, since font atlases use nearest sampling), and without the
 * font definitions to Minecraft's own font, scaled for larger styles.
 */
public final class UiText {
	private static final int MAX_FONT_SCALE = 6;
	/** Minecraft draws every glyph with its baseline this far below the text's y. */
	private static final float BASELINE = 7.0F;

	private static Boolean customFonts;
	private static int lastScale;

	private UiText() {
	}

	public enum Size {
		/** Descriptions and secondary text. */
		BODY("ui_body", 9.0F, 1.0F, UiFontRenderer.Weight.SEMIBOLD),
		/** Feature names and headings. */
		BOLD("ui_bold", 9.5F, 1.0F, UiFontRenderer.Weight.EXTRABOLD),
		/** Buttons and category labels. */
		LABEL("ui_label", 8.5F, 1.0F, UiFontRenderer.Weight.EXTRABOLD),
		/** Small labels such as badges. */
		SMALL("ui_small", 7.0F, 0.75F, UiFontRenderer.Weight.EXTRABOLD),
		/** Sheet titles. */
		HEADING("ui_heading", 13.0F, 1.3F, UiFontRenderer.Weight.BLACK),
		/** The screen title. */
		TITLE("ui_title", 17.0F, 1.6F, UiFontRenderer.Weight.BLACK),
		/** Code, in JetBrains Mono so columns line up. */
		CODE("ui_code", 8.5F, 1.0F, UiFontRenderer.Weight.MONO);

		private final String font;
		private final float em;
		private final float fallbackScale;
		private final UiFontRenderer.Weight weight;

		Size(String font, float em, float fallbackScale, UiFontRenderer.Weight weight) {
			this.font = font;
			this.em = em;
			this.fallbackScale = fallbackScale;
			this.weight = weight;
		}
	}

	/** Re-checks whether the UI fonts exist, for example after a resource reload. */
	public static void refreshFonts() {
		customFonts = Minecraft.getInstance()
			.getResourceManager()
			.getResource(Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "font/ui_body_s1.json"))
			.isPresent();
	}

	private static boolean customFonts() {
		if (customFonts == null) {
			refreshFonts();
		}
		return customFonts;
	}

	private static int guiScale() {
		int scale = Math.max(1, (int) Math.ceil(Minecraft.getInstance().getWindow().getGuiScale()));
		if (scale != lastScale) {
			lastScale = scale;
			UiFontRenderer.clearCache();
		}
		return scale;
	}

	/** Our own FreeType rendering; the font definitions below are only the fallback. */
	private static boolean freeType() {
		return customFonts() && UiFontRenderer.available();
	}

	private static Identifier fontFor(Size size) {
		int scale = Math.clamp(guiScale(), 1, MAX_FONT_SCALE);
		return Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, size.font + "_s" + scale);
	}

	private static float fallbackScale(Size size) {
		return customFonts() ? 1.0F : size.fallbackScale;
	}

	/** Height of capital letters, used to center text and size things around it. */
	private static float capHeight(Size size) {
		return customFonts() ? size.em * size.weight.capHeight() : BASELINE * size.fallbackScale;
	}

	/** Distance from the text's y to the top of its capital letters. */
	private static float capTop(Size size) {
		return BASELINE * fallbackScale(size) - capHeight(size);
	}

	public static Component styled(Component text, Size size) {
		if (!customFonts()) {
			return text;
		}
		return text.copy().withStyle(style -> style.withFont(new FontDescription.Resource(fontFor(size))));
	}

	public static Component styled(String text, Size size) {
		return styled(Component.literal(text), size);
	}

	public static int width(Font font, Component text, Size size) {
		if (freeType()) {
			float scale = guiScale() * UiRasterScale.get();
			return (int) Math.ceil(UiFontRenderer.measure(size.weight, size.em * scale, text.getString()) / scale);
		}
		return Math.round(font.width(styled(text, size)) * fallbackScale(size));
	}

	/** Cap height rounded to whole pixels. */
	public static int lineHeight(Font font, Size size) {
		return Math.round(capHeight(size));
	}

	/** Draws text with the top of its capital letters at {@code top}. */
	public static void draw(GuiGraphicsExtractor context, Font font, Component text, Size size, int x, int top, int color) {
		if (freeType()) {
			drawFreeType(context, text, size, x, top, color);
			return;
		}
		drawAt(context, font, text, size, x, top - capTop(size), color);
	}

	/** Draws text with its capital letters vertically centered on {@code centerY}. */
	public static void drawCentered(GuiGraphicsExtractor context, Font font, Component text, Size size, int x, int centerY, int color) {
		if (freeType()) {
			drawFreeType(context, text, size, x, centerY - capHeight(size) / 2.0F, color);
			return;
		}
		drawAt(context, font, text, size, x, centerY - capHeight(size) / 2.0F - capTop(size), color);
	}

	/**
	 * Draws with the top of the capitals at {@code top}, placed to the nearest screen pixel instead of
	 * the nearest GUI pixel, for text that has to line up exactly, such as code in columns.
	 */
	public static void drawExact(GuiGraphicsExtractor context, Font font, Component text, Size size, float x, float top, int color) {
		if (freeType()) {
			drawFreeType(context, text, size, x, top, color);
			return;
		}
		drawAt(context, font, text, size, Math.round(x), top - capTop(size), color);
	}

	/**
	 * Width of one character in GUI pixels, with its fraction. Only meaningful for {@link Size#CODE},
	 * whose font is monospaced.
	 */
	public static float advance(Font font, Size size) {
		if (freeType()) {
			int scale = guiScale();
			return UiFontRenderer.measure(size.weight, size.em * scale, "0") / scale;
		}
		return font.width(styled("0", size)) * fallbackScale(size);
	}

	/** Draws with the top of the capitals at {@code capTop}, snapped to whole screen pixels. */
	private static void drawFreeType(GuiGraphicsExtractor context, Component text, Size size, float x, float capTop, int color) {
		String value = text.getString();
		if (value.isEmpty()) {
			return;
		}
		float scale = guiScale() * UiRasterScale.get();
		UiFontRenderer.Rendered rendered = UiFontRenderer.render(size.weight, size.em * scale, value);
		int capPixels = Math.round(size.em * size.weight.capHeight() * scale);
		int baseline = Math.round(capTop * scale) + capPixels;
		int top = baseline - rendered.baseline();
		int left = Math.round(x * scale) - rendered.left();
		context.pose().pushMatrix();
		context.pose().scale(1.0F / scale, 1.0F / scale);
		context.blit(RenderPipelines.GUI_TEXTURED, rendered.texture(), left, top, 0.0F, 0.0F, rendered.width(), rendered.height(), rendered.width(), rendered.height(), rendered.width(), rendered.height(), UiOpacity.apply(color));
		context.pose().popMatrix();
	}

	private static void drawAt(GuiGraphicsExtractor context, Font font, Component text, Size size, int x, float y, int color) {
		float scale = fallbackScale(size);
		Component styled = styled(text, size);
		if (scale == 1.0F) {
			context.text(font, styled, x, Math.round(y), UiOpacity.apply(color), false);
			return;
		}
		context.pose().pushMatrix();
		context.pose().translate(x, Math.round(y));
		context.pose().scale(scale, scale);
		context.text(font, styled, 0, 0, UiOpacity.apply(color), false);
		context.pose().popMatrix();
	}

	/**
	 * Renders {@code text} ahead of time so drawing it later does not stall a frame, for example before
	 * an animation starts.
	 */
	public static void prepare(Component text, Size size) {
		String value = text.getString();
		if (freeType() && !value.isEmpty()) {
			int scale = guiScale();
			UiFontRenderer.render(size.weight, size.em * scale, value);
		}
	}

	/** Splits {@code text} into lines that fit {@code maxWidth}, breaking at spaces. */
	public static List<Component> wrap(Font font, Component text, Size size, int maxWidth) {
		List<Component> lines = new ArrayList<>();
		String line = "";
		for (String word : text.getString().split(" ")) {
			String candidate = line.isEmpty() ? word : line + " " + word;
			if (!line.isEmpty() && width(font, Component.literal(candidate), size) > maxWidth) {
				lines.add(Component.literal(line));
				line = word;
			} else {
				line = candidate;
			}
		}
		if (!line.isEmpty()) {
			lines.add(Component.literal(line));
		}
		return lines;
	}

	/** Shortens {@code text} with an ellipsis so it fits in {@code maxWidth}. */
	public static Component ellipsize(Font font, Component text, Size size, int maxWidth) {
		if (width(font, text, size) <= maxWidth) {
			return text;
		}
		String value = text.getString();
		String ellipsis = "...";
		int end = value.length();
		while (end > 0 && width(font, Component.literal(value.substring(0, end).stripTrailing() + ellipsis), size) > maxWidth) {
			end--;
		}
		return Component.literal(value.substring(0, end).stripTrailing() + ellipsis);
	}
}
