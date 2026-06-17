package net.emutils.client.emutils.chat;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

public final class ChatLegacyFormatting {
	private ChatLegacyFormatting() {
	}

	public static String toAmpersandString(Component text) {
		StringBuilder builder = new StringBuilder();
		Style[] previous = new Style[] {Style.EMPTY};
		text.visit((style, string) -> {
			if (!stylesEqual(previous[0], style)) {
				if (!previous[0].isEmpty()) {
					builder.append('&').append(formattingCode(ChatFormatting.RESET));
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
			ChatFormatting legacyColor = legacyColorChatFormatting(color);
			if (legacyColor != null) {
				builder.append('&').append(formattingCode(legacyColor));
			}
		}

		if (style.isObfuscated()) {
			builder.append('&').append(formattingCode(ChatFormatting.OBFUSCATED));
		}
		if (style.isBold()) {
			builder.append('&').append(formattingCode(ChatFormatting.BOLD));
		}
		if (style.isStrikethrough()) {
			builder.append('&').append(formattingCode(ChatFormatting.STRIKETHROUGH));
		}
		if (style.isUnderlined()) {
			builder.append('&').append(formattingCode(ChatFormatting.UNDERLINE));
		}
		if (style.isItalic()) {
			builder.append('&').append(formattingCode(ChatFormatting.ITALIC));
		}
	}

	private static boolean isHexColor(TextColor color) {
		String name = color.serialize();
		return name.startsWith("#");
	}

	@Nullable
	private static ChatFormatting legacyColorChatFormatting(TextColor color) {
		for (ChatFormatting formatting : ChatFormatting.values()) {
			TextColor legacy = TextColor.fromLegacyFormat(formatting);
			if (legacy != null && color.equals(legacy)) {
				return formatting;
			}
		}

		return null;
	}

	private static char formattingCode(ChatFormatting formatting) {
		return formatting.toString().charAt(1);
	}

	private static boolean stylesEqual(Style left, Style right) {
		return Objects.equals(left, right);
	}
}
