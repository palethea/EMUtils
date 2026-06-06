package net.emutils.client.emutils.chat;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class ChatLegacyFormatting {
	private ChatLegacyFormatting() {
	}

	public static String toAmpersandString(Text text) {
		StringBuilder builder = new StringBuilder();
		Style[] previous = new Style[] {Style.EMPTY};
		text.visit((style, string) -> {
			if (!stylesEqual(previous[0], style)) {
				if (!previous[0].isEmpty()) {
					builder.append('&').append(Formatting.RESET.getCode());
				}
				appendCodes(builder, style);
				previous[0] = style;
			}
			builder.append(string);
			return Optional.empty();
		}, Style.EMPTY);
		return builder.toString();
	}

	private static void appendCodes(StringBuilder builder, Style style) {
		TextColor color = style.getColor();
		if (color != null && !isHexColor(color)) {
			Formatting legacyColor = legacyColorFormatting(color);
			if (legacyColor != null) {
				builder.append('&').append(legacyColor.getCode());
			}
		}

		if (style.isObfuscated()) {
			builder.append('&').append(Formatting.OBFUSCATED.getCode());
		}
		if (style.isBold()) {
			builder.append('&').append(Formatting.BOLD.getCode());
		}
		if (style.isStrikethrough()) {
			builder.append('&').append(Formatting.STRIKETHROUGH.getCode());
		}
		if (style.isUnderlined()) {
			builder.append('&').append(Formatting.UNDERLINE.getCode());
		}
		if (style.isItalic()) {
			builder.append('&').append(Formatting.ITALIC.getCode());
		}
	}

	private static boolean isHexColor(TextColor color) {
		String name = color.getName();
		return name.startsWith("#");
	}

	@Nullable
	private static Formatting legacyColorFormatting(TextColor color) {
		for (Formatting formatting : Formatting.values()) {
			if (!formatting.isColor() || formatting.getColorValue() == null) {
				continue;
			}

			TextColor legacy = TextColor.fromFormatting(formatting);
			if (color.equals(legacy)) {
				return formatting;
			}
		}

		return null;
	}

	private static boolean stylesEqual(Style left, Style right) {
		return Objects.equals(left, right);
	}
}
