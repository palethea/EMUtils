package net.emutils.client.emskyblock.features.gui.statshud;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Formatting;

public final class SkyblockActionBarParser {
	private static final Pattern HEALTH = Pattern.compile("([\\d,]+)/([\\d,]+)\\s*[❤\u2764]");
	private static final Pattern DEFENSE = Pattern.compile("([\\d,]+)\\s*[❈\u2748](?:\\s*Defense)?", Pattern.CASE_INSENSITIVE);
	private static final Pattern MANA = Pattern.compile("([\\d,]+)/([\\d,]+)\\s*[✎\u270E]");
	private static final Pattern SOULFLOW = Pattern.compile("([\\d,]+)\\s*[ʬ\u028C]");

	private static final List<Pattern> STRIP_PATTERNS = List.of(
		Pattern.compile("[\\d,]+/[\\d,]+\\s*[❤\u2764]"),
		Pattern.compile("[\\d,]+\\s*[❈\u2748](?:\\s*Defense)?", Pattern.CASE_INSENSITIVE),
		Pattern.compile("[\\d,]+/[\\d,]+\\s*[✎\u270E]"),
		Pattern.compile("[\\d,]+\\s*[ʬ\u028C]")
	);

	private SkyblockActionBarParser() {
	}

	public static SkyblockActionBarStats parse(String text) {
		String plain = Formatting.strip(text);
		if (plain == null || plain.isBlank()) {
			return SkyblockActionBarStats.EMPTY;
		}

		Integer healthCurrent = null;
		Integer healthMax = null;
		Integer defense = null;
		Integer manaCurrent = null;
		Integer manaMax = null;
		Integer soulflow = null;

		Matcher health = HEALTH.matcher(plain);
		if (health.find()) {
			healthCurrent = parseInt(health.group(1));
			healthMax = parseInt(health.group(2));
		}

		Matcher defenseMatcher = DEFENSE.matcher(plain);
		if (defenseMatcher.find()) {
			defense = parseInt(defenseMatcher.group(1));
		}

		Matcher mana = MANA.matcher(plain);
		if (mana.find()) {
			manaCurrent = parseInt(mana.group(1));
			manaMax = parseInt(mana.group(2));
		}

		Matcher soulflowMatcher = SOULFLOW.matcher(plain);
		if (soulflowMatcher.find()) {
			soulflow = parseInt(soulflowMatcher.group(1));
		}

		return new SkyblockActionBarStats(healthCurrent, healthMax, defense, manaCurrent, manaMax, soulflow);
	}

	public static String stripStats(String text) {
		String stripped = Formatting.strip(text);
		if (stripped == null || stripped.isBlank()) {
			return "";
		}

		for (Pattern pattern : STRIP_PATTERNS) {
			stripped = pattern.matcher(stripped).replaceAll(" ");
		}

		return stripped.replaceAll("\\s{2,}", " ").trim();
	}

	public static boolean containsStats(String text) {
		return parse(text).hasAny();
	}

	private static @org.jspecify.annotations.Nullable Integer parseInt(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return Integer.parseInt(value.replace(",", ""));
		} catch (NumberFormatException ignored) {
			return null;
		}
	}
}
