package net.emutils.client.emskyblock.features.inventory.estimateditemvalue;

import java.util.Locale;

public final class EivGemstoneFormat {
	private EivGemstoneFormat() {
	}

	public static String displayName(String quality, String type) {
		return qualityColor(quality) + toTitle(quality)
			+ " "
			+ typeColor(type) + toTitle(type)
			+ " Gemstone";
	}

	public static String displayName(String productId) {
		String[] parts = productId.split("_");
		if (parts.length < 3) {
			return "§f" + productId.replace('_', ' ');
		}

		return displayName(parts[0], parts[1]);
	}

	private static String qualityColor(String quality) {
		return switch (quality.toUpperCase(Locale.ROOT)) {
			case "ROUGH" -> "§f";
			case "FLAWED" -> "§a";
			case "FINE" -> "§9";
			case "FLAWLESS" -> "§5";
			case "PERFECT" -> "§6";
			default -> "§f";
		};
	}

	private static String typeColor(String type) {
		return switch (type.toUpperCase(Locale.ROOT)) {
			case "JADE" -> "§a";
			case "AMBER" -> "§6";
			case "TOPAZ" -> "§e";
			case "SAPPHIRE" -> "§b";
			case "AMETHYST" -> "§5";
			case "JASPER" -> "§d";
			case "RUBY" -> "§c";
			case "OPAL" -> "§f";
			case "ONYX" -> "§8";
			case "AQUAMARINE" -> "§3";
			case "CITRINE" -> "§4";
			case "PERIDOT" -> "§2";
			default -> "§f";
		};
	}

	private static String toTitle(String value) {
		String lower = value.toLowerCase(Locale.ROOT);
		if (lower.isEmpty()) {
			return lower;
		}

		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}
}
