package net.emutils.client.emhelpers.text;

import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class FormattedText {
	private FormattedText() {
	}

	public static String format(Text text) {
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

	private static void appendFormatted(StringBuilder sb, Text text, boolean wasFormatted) {
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

		for (Text sibling : text.getSiblings()) {
			appendFormatted(sb, sibling, true);
		}
	}

	private static String plainText(Text text) {
		if (text.getContent() instanceof PlainTextContent content) {
			return content.string();
		}

		if (text.getContent() instanceof net.minecraft.text.TranslatableTextContent) {
			return text.getString();
		}

		return text.getString();
	}

	private static String toStyleString(Style style) {
		StringBuilder sb = new StringBuilder(8);
		TextColor color = style.getColor();
		if (color != null) {
			Formatting fmt = toFormatting(color);
			if (fmt != null) {
				sb.append("\u00a7").append(fmt.getCode());
			}
		}
		if (style.isBold()) sb.append("\u00a7l");
		if (style.isItalic()) sb.append("\u00a7o");
		if (style.isUnderlined()) sb.append("\u00a7n");
		if (style.isStrikethrough()) sb.append("\u00a7m");
		if (style.isObfuscated()) sb.append("\u00a7k");
		return sb.toString();
	}

	private static Formatting toFormatting(TextColor color) {
		return Formatting.byName(color.getName());
	}
}
