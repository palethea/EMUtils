package net.emutils.client.emskyblock.features.inventory.estimateditemvalue;

import java.util.List;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public record EstimatedItemValueResult(
	List<EstimatedItemValueLine> lines,
	double baseValue,
	double totalValue
) {
	public static EstimatedItemValueResult empty() {
		return new EstimatedItemValueResult(List.of(), 0.0D, 0.0D);
	}

	public boolean isEmpty() {
		return lines.isEmpty();
	}

	public record EstimatedItemValueLine(Text text, double valueContribution, boolean countsTowardTotal) {
		public static EstimatedItemValueLine of(Text text, double contribution, boolean counts) {
			return new EstimatedItemValueLine(text, contribution, counts);
		}

		public static EstimatedItemValueLine of(String rawText, double contribution, boolean counts) {
			return new EstimatedItemValueLine(parseLegacy(rawText), contribution, counts);
		}

		public static EstimatedItemValueLine header(String rawText) {
			return new EstimatedItemValueLine(parseLegacy(rawText), 0.0D, false);
		}

		public static Text appendLegacy(Text parent, String legacy) {
			if (legacy.isEmpty()) {
				return parent;
			}

			Text legacyText = parseLegacy(legacy);
			if (parent instanceof MutableText mutable) {
				return mutable.append(legacyText);
			}

			return Text.empty().append(parent).append(legacyText);
		}

		private static Text parseLegacy(String raw) {
			MutableText result = null;
			StringBuilder current = new StringBuilder();
			Formatting currentColor = Formatting.GRAY;
			boolean bold = false;

			for (int index = 0; index < raw.length(); index++) {
				char character = raw.charAt(index);
				if (character == '\u00A7' && index + 1 < raw.length()) {
					result = appendSegment(result, current, currentColor, bold);
					current.setLength(0);

					char code = Character.toLowerCase(raw.charAt(index + 1));
					index++;
					switch (code) {
						case '0' -> currentColor = Formatting.BLACK;
						case '1' -> currentColor = Formatting.DARK_BLUE;
						case '2' -> currentColor = Formatting.DARK_GREEN;
						case '3' -> currentColor = Formatting.DARK_AQUA;
						case '4' -> currentColor = Formatting.DARK_RED;
						case '5' -> currentColor = Formatting.DARK_PURPLE;
						case '6' -> currentColor = Formatting.GOLD;
						case '7' -> currentColor = Formatting.GRAY;
						case '8' -> currentColor = Formatting.DARK_GRAY;
						case '9' -> currentColor = Formatting.BLUE;
						case 'a' -> currentColor = Formatting.GREEN;
						case 'b' -> currentColor = Formatting.AQUA;
						case 'c' -> currentColor = Formatting.RED;
						case 'd' -> currentColor = Formatting.LIGHT_PURPLE;
						case 'e' -> currentColor = Formatting.YELLOW;
						case 'f' -> currentColor = Formatting.WHITE;
						case 'l' -> bold = true;
						case 'r' -> {
							currentColor = Formatting.GRAY;
							bold = false;
						}
						default -> {
						}
					}
					continue;
				}

				current.append(character);
			}

			result = appendSegment(result, current, currentColor, bold);
			return result == null ? Text.literal("") : result;
		}

		@Nullable
		private static MutableText appendSegment(
			@Nullable MutableText result,
			StringBuilder current,
			Formatting color,
			boolean bold
		) {
			if (current.isEmpty()) {
				return result;
			}

			MutableText segment = Text.literal(current.toString()).formatted(color);
			if (bold) {
				segment = segment.formatted(Formatting.BOLD);
			}

			current.setLength(0);
			if (result == null) {
				return segment;
			}

			return result.append(segment);
		}
	}
}
