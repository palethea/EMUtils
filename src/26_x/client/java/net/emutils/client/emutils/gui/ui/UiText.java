package net.emutils.client.emutils.gui.ui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Text in the settings UI, set in the bundled Nunito font. Minecraft samples font atlases with nearest
 * filtering, so each style ships one font definition per GUI scale ({@code ui_body_s3} renders its
 * glyphs at 3x), and the one matching the current scale is used so text stays crisp. Without the font
 * definitions it falls back to Minecraft's font, scaled for larger styles.
 */
public final class UiText {
	private static final int MAX_FONT_SCALE = 6;
	/** Minecraft draws every glyph with its baseline this far below the text's y. */
	private static final float BASELINE = 7.0F;
	/** Nunito's cap height as a fraction of its em size. */
	private static final float CAP_HEIGHT = 0.705F;

	private static Boolean customFonts;

	private UiText() {
	}

	public enum Size {
		/** Descriptions and secondary text. */
		BODY("ui_body", 9.0F, 1.0F),
		/** Feature names and headings. */
		BOLD("ui_bold", 9.5F, 1.0F),
		/** Buttons and category labels. */
		LABEL("ui_label", 8.5F, 1.0F),
		/** Small labels such as badges. */
		SMALL("ui_small", 7.0F, 0.75F),
		/** The screen title. */
		TITLE("ui_title", 17.0F, 1.6F);

		private final String font;
		private final float em;
		private final float fallbackScale;

		Size(String font, float em, float fallbackScale) {
			this.font = font;
			this.em = em;
			this.fallbackScale = fallbackScale;
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

	private static Identifier fontFor(Size size) {
		int scale = (int) Math.ceil(Minecraft.getInstance().getWindow().getGuiScale());
		scale = Math.clamp(scale, 1, MAX_FONT_SCALE);
		return Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, size.font + "_s" + scale);
	}

	private static float fallbackScale(Size size) {
		return customFonts() ? 1.0F : size.fallbackScale;
	}

	/** Height of capital letters, used to center text and size things around it. */
	private static float capHeight(Size size) {
		return customFonts() ? size.em * CAP_HEIGHT : BASELINE * size.fallbackScale;
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
		return Math.round(font.width(styled(text, size)) * fallbackScale(size));
	}

	/** Cap height rounded to whole pixels. */
	public static int lineHeight(Font font, Size size) {
		return Math.round(capHeight(size));
	}

	/** Draws text with the top of its capital letters at {@code top}. */
	public static void draw(GuiGraphicsExtractor context, Font font, Component text, Size size, int x, int top, int color) {
		drawAt(context, font, text, size, x, top - capTop(size), color);
	}

	/** Draws text with its capital letters vertically centered on {@code centerY}. */
	public static void drawCentered(GuiGraphicsExtractor context, Font font, Component text, Size size, int x, int centerY, int color) {
		drawAt(context, font, text, size, x, centerY - capHeight(size) / 2.0F - capTop(size), color);
	}

	private static void drawAt(GuiGraphicsExtractor context, Font font, Component text, Size size, int x, float y, int color) {
		float scale = fallbackScale(size);
		Component styled = styled(text, size);
		if (scale == 1.0F) {
			context.text(font, styled, x, Math.round(y), color, false);
			return;
		}
		context.pose().pushMatrix();
		context.pose().translate(x, Math.round(y));
		context.pose().scale(scale, scale);
		context.text(font, styled, 0, 0, color, false);
		context.pose().popMatrix();
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
