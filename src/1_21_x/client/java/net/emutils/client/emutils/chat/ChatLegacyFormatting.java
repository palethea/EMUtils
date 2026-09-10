package net.emutils.client.emutils.chat;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class ChatLegacyFormatting {
	private static final char SECTION_FORMAT_PREFIX = '\u00A7';
	private static final char AMPERSAND_FORMAT_PREFIX = '&';

	private ChatLegacyFormatting() {
	}

	public static String toAmpersandString(Text text) {
		StringBuilder builder = new StringBuilder();
		Style[] previous = new Style[] {Style.EMPTY};
		boolean[] forceStyleRefresh = new boolean[] {false};
		text.visit((style, string) -> {
			if (forceStyleRefresh[0] || !stylesEqual(previous[0], style)) {
				if (forceStyleRefresh[0] || !previous[0].isEmpty()) {
					builder.append(AMPERSAND_FORMAT_PREFIX).append(Formatting.RESET.getCode());
				}
				appendCodes(builder, style);
				previous[0] = style;
				forceStyleRefresh[0] = false;
			}
			if (appendWithAmpersandFormattingCodes(builder, string)) {
				forceStyleRefresh[0] = true;
			}
			return Optional.empty();
		}, Style.EMPTY);
		return builder.toString();
	}

	public static String stripSectionFormattingCodes(String string) {
		StringBuilder builder = new StringBuilder(string.length());
		for (int index = 0; index < string.length(); index++) {
			char character = string.charAt(index);
			if (character == SECTION_FORMAT_PREFIX && index + 1 < string.length() && isLegacyFormattingCode(string.charAt(index + 1))) {
				index++;
				continue;
			}
			builder.append(character);
		}
		return builder.toString();
	}

	private static boolean appendWithAmpersandFormattingCodes(StringBuilder builder, String string) {
		boolean foundFormattingCode = false;
		for (int index = 0; index < string.length(); index++) {
			char character = string.charAt(index);
			if (character == SECTION_FORMAT_PREFIX && index + 1 < string.length() && isLegacyFormattingCode(string.charAt(index + 1))) {
				builder.append(AMPERSAND_FORMAT_PREFIX).append(Character.toLowerCase(string.charAt(index + 1)));
				index++;
				foundFormattingCode = true;
				continue;
			}
			builder.append(character);
		}
		return foundFormattingCode;
	}

	private static boolean isLegacyFormattingCode(char code) {
		return "0123456789abcdefklmnorx".indexOf(Character.toLowerCase(code)) >= 0;
	}

	private static void appendCodes(StringBuilder builder, Style style) {
		TextColor color = style.getColor();
		if (color != null && !isHexColor(color)) {
			Formatting legacyColor = legacyColorFormatting(color);
			if (legacyColor != null) {
				builder.append(AMPERSAND_FORMAT_PREFIX).append(legacyColor.getCode());
			}
		}

		if (style.isObfuscated()) {
			builder.append(AMPERSAND_FORMAT_PREFIX).append(Formatting.OBFUSCATED.getCode());
		}
		if (style.isBold()) {
			builder.append(AMPERSAND_FORMAT_PREFIX).append(Formatting.BOLD.getCode());
		}
		if (style.isStrikethrough()) {
			builder.append(AMPERSAND_FORMAT_PREFIX).append(Formatting.STRIKETHROUGH.getCode());
		}
		if (style.isUnderlined()) {
			builder.append(AMPERSAND_FORMAT_PREFIX).append(Formatting.UNDERLINE.getCode());
		}
		if (style.isItalic()) {
			builder.append(AMPERSAND_FORMAT_PREFIX).append(Formatting.ITALIC.getCode());
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
