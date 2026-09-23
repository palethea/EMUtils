package net.emutils.client.emutils.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class FormattedText {
	private FormattedText() {
	}

	public static String format(Component text) {
		if (text == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(50);
		appendFormatted(sb, text, false);
		String result = sb.toString();
		while (result.startsWith("\u00a7r")) {
			result = result.substring(2);
		}
		while (result.endsWith("\u00a7r")) {
			result = result.substring(0, result.length() - 2);
		}
		return result;
	}

	private static void appendFormatted(StringBuilder sb, Component text, boolean wasFormatted) {
		Style style = text.getStyle();
		String styleString = toStyleString(style);

		if (!styleString.isEmpty()) {
			if (!wasFormatted || !styleString.equals("\u00a7f")) {
				sb.append(styleString);
				wasFormatted = true;
			}
		}

		sb.append(plainText(text));

		sb.append("\u00a7r");

		for (Component sibling : text.getSiblings()) {
			appendFormatted(sb, sibling, true);
		}
	}

	private static String plainText(Component text) {
		if (text.getContents() instanceof PlainTextContents.LiteralContents content) {
			return content.text();
		}

		if (text.getContents() instanceof TranslatableContents) {
			return text.getString();
		}

		return text.getString();
	}

	private static String toStyleString(Style style) {
		StringBuilder sb = new StringBuilder(8);
		TextColor color = style.getColor();
		if (color != null) {
			ChatFormatting fmt = toFormatting(color);
			if (fmt != null) {
				sb.append(formattingCode(fmt));
			}
		}
		if (style.isBold()) sb.append("\u00a7l");
		if (style.isItalic()) sb.append("\u00a7o");
		if (style.isUnderlined()) sb.append("\u00a7n");
		if (style.isStrikethrough()) sb.append("\u00a7m");
		if (style.isObfuscated()) sb.append("\u00a7k");
		return sb.toString();
	}

	private static ChatFormatting toFormatting(TextColor color) {
		for (ChatFormatting formatting : ChatFormatting.values()) {
			TextColor legacy = TextColor.fromLegacyFormat(formatting);
			if (legacy != null && color.equals(legacy)) {
				return formatting;
			}
		}

		return null;
	}

	private static String formattingCode(ChatFormatting formatting) {
		return formatting.toString();
	}
}
