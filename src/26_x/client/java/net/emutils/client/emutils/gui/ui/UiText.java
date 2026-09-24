package net.emutils.client.emutils.gui.ui;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Text in the settings UI. It uses the bundled UI fonts when their font definitions are present and
 * falls back to Minecraft's font otherwise, scaling it for larger styles.
 */
public final class UiText {
	private static Boolean customFonts;

	private UiText() {
	}

	public enum Size {
		/** Descriptions and secondary text. */
		BODY("ui_body", 1.0F),
		/** Feature names, buttons and headings. */
		BOLD("ui_bold", 1.0F),
		/** Small labels such as badges. */
		SMALL("ui_small", 0.75F),
		/** The screen title. */
		TITLE("ui_title", 1.6F);

		private final Identifier font;
		private final float fallbackScale;

		Size(String font, float fallbackScale) {
			this.font = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, font);
			this.fallbackScale = fallbackScale;
		}
	}

	/** Re-checks whether the UI fonts exist, for example after a resource reload. */
	public static void refreshFonts() {
		customFonts = Minecraft.getInstance()
			.getResourceManager()
			.getResource(Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "font/ui_body.json"))
			.isPresent();
	}

	private static boolean customFonts() {
		if (customFonts == null) {
			refreshFonts();
		}
		return customFonts;
	}

	private static float scale(Size size) {
		return customFonts() ? 1.0F : size.fallbackScale;
	}

	public static Component styled(Component text, Size size) {
		if (!customFonts()) {
			return text;
		}
		MutableComponent copy = text.copy();
		return copy.withStyle(style -> style.withFont(new FontDescription.Resource(size.font)));
	}

	public static Component styled(String text, Size size) {
		return styled(Component.literal(text), size);
	}

	public static int width(Font font, Component text, Size size) {
		return Math.round(font.width(styled(text, size)) * scale(size));
	}

	/** Height of a line of this size, used to center text vertically. */
	public static int lineHeight(Font font, Size size) {
		return Math.round((font.lineHeight - 2) * scale(size));
	}

	public static void draw(GuiGraphicsExtractor context, Font font, Component text, Size size, int x, int y, int color) {
		float scale = scale(size);
		Component styled = styled(text, size);
		if (scale == 1.0F) {
			context.text(font, styled, x, y, color, false);
			return;
		}
		context.pose().pushMatrix();
		context.pose().translate(x, y);
		context.pose().scale(scale, scale);
		context.text(font, styled, 0, 0, color, false);
		context.pose().popMatrix();
	}

	/** Draws text vertically centered on {@code centerY}. */
	public static void drawCentered(GuiGraphicsExtractor context, Font font, Component text, Size size, int x, int centerY, int color) {
		draw(context, font, text, size, x, centerY - lineHeight(font, size) / 2, color);
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
		return Component.literal(value.substring(0, end).stripTrailing() + ellipsis).withStyle(Style.EMPTY);
	}
}
