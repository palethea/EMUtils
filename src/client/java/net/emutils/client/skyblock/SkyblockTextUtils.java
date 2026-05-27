package net.emutils.client.skyblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class SkyblockTextUtils {
	private SkyblockTextUtils() {
	}

	public static String strip(@Nullable Text text) {
		if (text == null) {
			return "";
		}

		return Formatting.strip(text.getString()).trim();
	}

	/** Legacy string with section-sign formatting codes (Hypixel armor stand names). */
	public static String formattedLegacy(@Nullable Text text) {
		if (text == null) {
			return "";
		}

		return text.getString();
	}

	/** Like {@link #formattedLegacy(Text)} but strips reset codes and outer whitespace (SkyHanni-style). */
	public static String formattedLegacyLessResets(@Nullable Text text) {
		return formattedLegacy(text).replaceAll("(?i)§r", "").trim();
	}

	public static String strip(@Nullable String text) {
		if (text == null) {
			return "";
		}

		return Formatting.strip(text).trim();
	}

	public static String normalizeKey(@Nullable String text) {
		return strip(text).toLowerCase(Locale.ROOT);
	}

	public static String normalizeProfile(@Nullable String profile) {
		if (profile == null) {
			return "";
		}

		String normalized = strip(profile).toLowerCase(Locale.ROOT);
		if (normalized.endsWith(" (co-op)")) {
			normalized = normalized.substring(0, normalized.length() - " (co-op)".length()).trim();
		}

		return normalized;
	}

	public static List<String> splitLines(@Nullable Text text) {
		if (text == null) {
			return List.of();
		}

		List<String> lines = new ArrayList<>();
		for (String line : strip(text).split("\\R")) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				lines.add(trimmed);
			}
		}

		return lines;
	}

	public static double parseCoins(@Nullable String value) {
		if (value == null || value.isBlank()) {
			return 0.0D;
		}

		try {
			return Double.parseDouble(value.replace(",", ""));
		} catch (NumberFormatException ignored) {
			return 0.0D;
		}
	}
}
