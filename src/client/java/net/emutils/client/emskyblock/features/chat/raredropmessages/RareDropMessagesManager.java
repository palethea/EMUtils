package net.emutils.client.emskyblock.features.chat.raredropmessages;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.emskyblock.context.SkyblockFeatures;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emhelpers.text.FormattedText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class RareDropMessagesManager {
	private RareDropMessagesManager() {
	}

	private static final Pattern PET_DROPPED_PATTERN = Pattern.compile(
		"(?<start>(?:\u00a7.)*PET DROP! )(?:\u00a7.)*\u00a7(?<rarityColor>.)(?<petName>[^\u00a7(.]+)(?<end>(?: .*)?)"
	);

	private static final Pattern PET_FISHED_PATTERN = Pattern.compile(
		"(?<start>(?:\u00a7.)*\u26c3 (?:\u00a7.)*(?:GOOD|GREAT|OUTSTANDING) CATCH! (?:\u00a7.)*You caught a (?:\u00a7.)*\\[Lvl 1] )(?:\u00a7.)*\u00a7(?<rarityColor>.)(?<petName>[^\u00a7(.]+)(?<end>.*)"
	);

	private static final Pattern PET_CLAIMED_PATTERN = Pattern.compile(
		"(?<start>(?:\u00a7.)*You claimed a )(?:\u00a7.)*\u00a7(?<rarityColor>.)(?<petName>[^\u00a7(.]+)(?<end>.*You can manage your Pets.*)"
	);

	private static final Pattern PET_OBTAINED_PATTERN = Pattern.compile(
		"(?<start>.*has obtained (?:\u00a7.)*\\[Lvl 1] )(?:\u00a7.)*\u00a7(?<rarityColor>.)(?<petName>[^\u00a7(.]+)(?<end>.*)"
	);

	private static final Pattern ORINGO_PATTERN = Pattern.compile(
		"(?<start>\u00a7e\\[NPC] Oringo\u00a7f: \u00a7b\u2631 \u00a7f\u00a7r\u00a78\u2022 \u00a7f)\u00a7(?<rarityColor>.)(?<petName>[^\u00a7(.]+)(?<end> Pet)"
	);

	private static final List<Pattern> PET_PATTERNS = List.of(
		PET_DROPPED_PATTERN,
		PET_FISHED_PATTERN,
		PET_CLAIMED_PATTERN,
		PET_OBTAINED_PATTERN,
		ORINGO_PATTERN
	);

	public static Text onChat(Text message) {
		if (!SkyblockFeatures.inSkyBlock() || !EMSkyblockSettings.skyblockEnabled()) {
			return null;
		}

		if (!EMSkyblockSettings.chatPetRarity()) {
			return null;
		}

		return handlePetDrop(message);
	}

	private static Text handlePetDrop(Text message) {
		String formatted = FormattedText.format(message);
		if (formatted.isEmpty()) {
			return null;
		}

		for (Pattern pattern : PET_PATTERNS) {
			Matcher matcher = pattern.matcher(formatted);
			if (matcher.matches()) {
				String start = matcher.group("start");
				String rarityColorCode = matcher.group("rarityColor");
				String petName = matcher.group("petName");
				String end = matcher.group("end");

				if (rarityColorCode == null || rarityColorCode.isEmpty()) {
					continue;
				}

				Formatting rarityFormatting = Formatting.byCode(rarityColorCode.charAt(0));
				if (rarityFormatting == null) {
					continue;
				}

				String rarityName = getRarityName(rarityFormatting);
				if (rarityName.isEmpty()) {
					continue;
				}

				if (start != null && start.endsWith("a ") && isVowel(rarityName.charAt(0))) {
					start = start.substring(0, start.length() - 2) + "n ";
				}

				String startStr = start != null ? start : "";
				String endStr = end != null ? end : "";

				MutableText result = Text.empty();
				result.append(Text.literal(startStr));
				result.append(Text.literal(rarityName + " ")
					.setStyle(net.minecraft.text.Style.EMPTY
						.withFormatting(rarityFormatting)
						.withBold(true)));
				result.append(Text.literal(petName)
					.setStyle(net.minecraft.text.Style.EMPTY
						.withFormatting(rarityFormatting)));
				result.append(Text.literal(endStr));
				return result;
			}
		}
		return null;
	}

	private static String getRarityName(Formatting formatting) {
		return switch (formatting) {
			case WHITE -> "COMMON";
			case GREEN -> "UNCOMMON";
			case BLUE -> "RARE";
			case DARK_PURPLE -> "EPIC";
			case GOLD -> "LEGENDARY";
			case LIGHT_PURPLE -> "MYTHIC";
			case RED -> "DIVINE";
			case DARK_RED -> "SPECIAL";
			default -> "";
		};
	}

	private static boolean isVowel(char c) {
		return "AEIOUaeiou".indexOf(c) >= 0;
	}
}
